/*
 * Copyright 2020, TeamDev. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.server.aggregate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.annotation.Internal;
import io.spine.annotation.SPI;
import io.spine.base.Identifier;
import io.spine.client.ResponseFormat;
import io.spine.client.TargetFilters;
import io.spine.core.Event;
import io.spine.core.Version;
import io.spine.server.ContextSpec;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.storage.EntityColumns;
import io.spine.server.entity.storage.EntityRecordStorage;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.storage.AbstractStorage;
import io.spine.server.storage.MessageQueries;
import io.spine.server.storage.MessageQuery;
import io.spine.server.storage.StorageFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Streams.stream;
import static io.spine.server.aggregate.AggregateRepository.DEFAULT_SNAPSHOT_TRIGGER;

/**
 * A storage of aggregate events, snapshots and the most recent aggregate states.
 *
 * @param <I>
 *         the type of IDs of aggregates managed by this storage
 */
@SPI
public class AggregateStorage<I> extends AbstractStorage<I, AggregateHistory> {

    private static final String TRUNCATE_ON_WRONG_SNAPSHOT_MESSAGE =
            "The specified snapshot index is incorrect";

    private final AggregateEventStorage eventStorage;
    private final EntityRecordStorage<I> stateStorage;
    private boolean mirrorEnabled = false;
    private final Truncate<Object> truncation;
    private final HistoryBackward<I> historyBackward;

    public AggregateStorage(ContextSpec spec,
                            Class<? extends Aggregate<I, ?, ?>> aggregateClass,
                            StorageFactory factory) {
        super(spec.isMultitenant());
        eventStorage = factory.createAggregateEventStorage(spec.isMultitenant());
        stateStorage = factory.createEntityRecordStorage(spec, aggregateClass);
        truncation = new Truncate<>(eventStorage);
        historyBackward = new HistoryBackward<>(eventStorage);
    }

    protected AggregateStorage(AggregateStorage<I> delegate) {
        super(delegate.isMultitenant());
        this.eventStorage = delegate.eventStorage;
        this.stateStorage = delegate.stateStorage;
        this.mirrorEnabled = delegate.mirrorEnabled;
        this.truncation = delegate.truncation;
        this.historyBackward = delegate.historyBackward;
    }

    void enableMirror() {
        mirrorEnabled = true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Opens the method for the package.
     */
    @Override
    protected void checkNotClosed() throws IllegalStateException {
        super.checkNotClosed();
    }

    /**
     * {@inheritDoc}
     *
     * <p>While it is possible to write individual event records only, in scope of the expected
     * usage scenarios, the IDs, lifecycle and versions of the {@code Aggregates} are
     * {@link #writeState(Aggregate) written} to this storage as well.
     *
     * <p>Therefore, this index contains only the identifiers of the {@code Aggregates} which
     * state was written to the storage.
     */
    @Override
    public Iterator<I> index() {
        return stateStorage.index();
    }

    /**
     * Forms and returns an {@link AggregateHistory} based on the
     * {@linkplain #historyBackward(Object, int)}  aggregate history}.
     *
     * @param id
     *         the identifier of the aggregate for which to return the history
     * @param batchSize
     *         the maximum number of the events to read
     * @return the record instance or {@code Optional.empty()} if the
     *         {@linkplain #historyBackward(Object, int) aggregate history} is empty
     * @throws IllegalStateException
     *         if the storage was closed before
     */
    @SuppressWarnings("CheckReturnValue") // calling builder method
    public Optional<AggregateHistory> read(I id, int batchSize) {
        ReadOperation<I> op = new ReadOperation<>(this, id, batchSize);
        return op.perform();
    }

    @Override
    public Optional<AggregateHistory> read(I id) {
        return read(id, DEFAULT_SNAPSHOT_TRIGGER);
    }

    /**
     * Writes events into the storage.
     *
     * <p><b>NOTE</b>: does not rewrite any events. Several events can be associated with one
     * aggregate ID.
     *
     * @param id
     *         the ID for the record
     * @param events
     *         non empty aggregate state record to store
     */
    @Override
    public void write(I id, AggregateHistory events) {
        checkNotClosedAndArguments(id, events);

        List<Event> eventList = events.getEventList();
        checkArgument(!eventList.isEmpty(), "Event list must not be empty.");

        for (Event event : eventList) {
            AggregateEventRecord record = AggregateRecords.newEventRecord(id, event);
            writeEventRecord(id, record);
        }
        if (events.hasSnapshot()) {
            writeSnapshot(id, events.getSnapshot());
        }
    }

    /**
     * Writes an event to the storage by an aggregate ID.
     *
     * <p>Before the storing, {@linkplain io.spine.core.Event#clearEnrichments() enrichments}
     * will be removed from the event.
     *
     * @param id
     *         the aggregate ID
     * @param event
     *         the event to write
     */
    void writeEvent(I id, Event event) {
        checkNotClosedAndArguments(id, event);

        Event eventWithoutEnrichments = event.clearEnrichments();
        AggregateEventRecord record = AggregateRecords.newEventRecord(id, eventWithoutEnrichments);
        writeEventRecord(id, record);
    }

    /**
     * Writes a {@code snapshot} by an {@code aggregateId} to the storage.
     *
     * @param aggregateId
     *         an ID of an aggregate of which the snapshot is made
     * @param snapshot
     *         the snapshot of the aggregate
     */
    void writeSnapshot(I aggregateId, Snapshot snapshot) {
        checkNotClosedAndArguments(aggregateId, snapshot);

        AggregateEventRecord record = AggregateRecords.newEventRecord(aggregateId, snapshot);
        writeEventRecord(aggregateId, record);
    }

    /**
     * Writes the passed record into the storage.
     *
     * @param id
     *         the aggregate ID
     * @param record
     *         the record to write
     */
    protected void writeEventRecord(I id, AggregateEventRecord record) {
        eventStorage.write(record.getId(), record);
    }

    protected Iterator<EntityRecord> readStates(TargetFilters filters, ResponseFormat format) {
        MessageQuery<I> query = MessageQueries.from(filters, stateStorage.columns());
        return stateStorage.readAll(query, format);
    }

    protected void writeState(Aggregate<I, ?, ?> aggregate) {
        EntityRecord record = AggregateRecords.newStateRecord(aggregate, mirrorEnabled);
        EntityColumns columns = EntityColumns.of(aggregate.modelClass());
        EntityRecordWithColumns<I> result =
                EntityRecordWithColumns.create(aggregate, columns, record);
        stateStorage.write(result);
    }

    protected void writeAll(Aggregate<I, ?, ?> aggregate,
                            ImmutableList<AggregateHistory> historySegments) {
        for (AggregateHistory history : historySegments) {
            write(aggregate.id(), history);
        }
        writeState(aggregate);
    }

    /**
     * Creates iterator of aggregate event history with the reverse traversal.
     *
     * <p>Records are sorted by timestamp descending (from newer to older).
     * The iterator is empty if there's no history for the aggregate with passed ID.
     *
     * @param id
     *         the identifier of the aggregate
     * @param batchSize
     *         the maximum number of the history records to read
     * @return new iterator instance
     */
    Iterator<AggregateEventRecord> historyBackward(I id, int batchSize) {
        return historyBackward(id, batchSize, null);
    }

    protected Iterator<AggregateEventRecord>
    historyBackward(I id, int batchSize, @Nullable Version startingFrom) {
        return historyBackward.read(id, batchSize, startingFrom);
    }

    /**
     * Truncates the storage, dropping all records which occur before the Nth snapshot for each
     * entity.
     *
     * <p>The snapshot index is counted from the latest to earliest, with {@code 0} representing
     * the latest snapshot.
     *
     * <p>The snapshot index higher than the overall snapshot count of the entity is allowed, the
     * entity records remain intact in this case.
     *
     * @throws IllegalArgumentException
     *         if the {@code snapshotIndex} is negative
     */
    @Internal
    public void truncateOlderThan(int snapshotIndex) {
        checkArgument(snapshotIndex >= 0, TRUNCATE_ON_WRONG_SNAPSHOT_MESSAGE);
        truncate(snapshotIndex);
    }

    /**
     * Truncates the storage, dropping all records older than {@code date} but not newer than the
     * Nth snapshot.
     *
     * <p>The snapshot index is counted from the latest to earliest, with {@code 0} representing
     * the latest snapshot for each entity.
     *
     * <p>The snapshot index higher than the overall snapshot count of the entity is allowed, the
     * records remain intact in this case.
     *
     * @throws IllegalArgumentException
     *         if the {@code snapshotIndex} is negative
     */
    @Internal
    public void truncateOlderThan(int snapshotIndex, Timestamp date) {
        checkNotNull(date);
        checkArgument(snapshotIndex >= 0, TRUNCATE_ON_WRONG_SNAPSHOT_MESSAGE);
        truncate(snapshotIndex, date);
    }

    /**
     * Drops all records which occur before the Nth snapshot for each entity.
     */
    protected void truncate(int snapshotIndex) {
        truncation.performWith(snapshotIndex, (r) -> true);
    }

    /**
     * Drops all records older than {@code date} but not newer than the Nth snapshot for each
     * entity.
     */
    protected void truncate(int snapshotIndex, Timestamp date) {
        truncation.performWith(snapshotIndex,
                               (r) -> Timestamps.compare(r.getTimestamp(), date) < 0);
    }

    /**
     * Obtains distinct aggregate IDs from the stored event records.
     */
    protected Iterator<I> distinctAggregateIds() {
        Iterator<AggregateEventRecord> iterator = eventStorage.readAll();
        ImmutableSet<I> distictIds = stream(iterator).map(this::aggregateIdOf)
                                                     .collect(toImmutableSet());
        return distictIds.iterator();
    }

    @SuppressWarnings("unchecked")
    private I aggregateIdOf(AggregateEventRecord record) {
        return (I) Identifier.unpack(record.getAggregateId());
    }

    private void checkNotClosedAndArguments(I id, Object argument) {
        checkNotClosed();
        checkNotNull(id);
        checkNotNull(argument);
    }
}
