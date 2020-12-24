/*
 * Copyright 2020, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.base.EntityState;
import io.spine.base.Time;
import io.spine.core.ActorContext;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventId;
import io.spine.core.MessageId;
import io.spine.core.Origin;
import io.spine.core.Version;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.ContextSpec;
import io.spine.server.ServerEnvironment;
import io.spine.server.aggregate.given.StorageRecords;
import io.spine.server.aggregate.given.repo.GivenAggregate;
import io.spine.server.aggregate.given.repo.ProjectAggregate;
import io.spine.server.aggregate.given.repo.ProjectAggregateRepository;
import io.spine.server.model.Nothing;
import io.spine.server.storage.AbstractStorageTest;
import io.spine.test.aggregate.AggProject;
import io.spine.test.aggregate.IntegerProject;
import io.spine.test.aggregate.LongProject;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.StringProject;
import io.spine.testdata.Sample;
import io.spine.testing.TestValues;
import io.spine.testing.Tests;
import io.spine.testing.core.given.GivenCommandContext;
import io.spine.testing.server.TestEventFactory;
import io.spine.testing.server.model.ModelTests;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.truth.Truth.assertThat;
import static com.google.protobuf.util.Timestamps.add;
import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Time.currentTime;
import static io.spine.core.Versions.increment;
import static io.spine.core.Versions.zero;
import static io.spine.protobuf.Durations2.seconds;
import static io.spine.protobuf.Messages.isDefault;
import static io.spine.server.aggregate.given.StorageRecords.sequenceFor;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.event;
import static io.spine.testing.core.given.GivenEnrichment.withOneAttribute;
import static io.spine.testing.server.TestEventFactory.newInstance;
import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.reverse;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AggregateStorageTest
        extends AbstractStorageTest<ProjectId,
                                    AggregateHistory,
                                    AggregateStorage<ProjectId, AggProject>> {

    private final ProjectId id = Sample.messageOfType(ProjectId.class);

    private final TestEventFactory eventFactory = newInstance(AggregateStorageTest.class);
    private AggregateStorage<ProjectId, AggProject> storage;

    private static Snapshot newSnapshot(Timestamp time) {
        return Snapshot.newBuilder()
                       .setState(Any.getDefaultInstance())
                       .setTimestamp(time)
                       .build();
    }

    private static EventId newEventId() {
        return EventId.newBuilder()
                      .setValue(newUuid())
                      .build();
    }

    @Override
    @BeforeEach
    public void setUpAbstractStorageTest() {
        super.setUpAbstractStorageTest();
        ModelTests.dropAllModels();
        storage = storage();
    }

    @Override
    protected AggregateHistory newStorageRecord(ProjectId id) {
        List<AggregateEventRecord> records = sequenceFor(id);
        List<Event> expectedEvents = records.stream()
                                            .map(AggregateStorageTest::toEvent)
                                            .collect(toList());
        AggregateHistory record = AggregateHistory
                .newBuilder()
                .addAllEvent(expectedEvents)
                .build();
        return record;
    }

    private static @Nullable Event toEvent(@Nullable AggregateEventRecord record) {
        return record != null
               ? record.getEvent()
               : null;
    }

    @Override
    protected ProjectId newId() {
        return ProjectId.generate();
    }

    @Override
    protected AggregateStorage<ProjectId, AggProject> newStorage() {
        return newStorage(TestAggregate.class);
    }

    /**
     * Creates the storage for the specified ID and aggregate class.
     *
     * <p>The created storage should be closed manually.
     *
     * @param aggregateClass
     *         the aggregate class
     * @param <I>
     *         the type of aggregate IDs
     * @return a new storage instance
     */
    <I, S extends EntityState<I>> AggregateStorage<I, S>
    newStorage(Class<? extends Aggregate<I, S, ?>> aggregateClass) {
        ContextSpec spec = ContextSpec.singleTenant("`AggregateStorage` tests");
        AggregateStorage<I, S> result =
                ServerEnvironment.instance()
                                 .storageFactory()
                                 .createAggregateStorage(spec,aggregateClass);
        return result;
    }

    @Nested
    @DisplayName("being empty, return")
    class BeingEmptyReturn {

        @Test
        @DisplayName("iterator over empty collection on reading history")
        void emptyHistory() {
            Iterator<AggregateEventRecord> iterator = historyBackward();

            assertFalse(iterator.hasNext());
        }

        @Test
        @DisplayName("absent AggregateStateRecord on reading record")
        void absentRecord() {
            Optional<AggregateHistory> record = storage.read(id);

            assertFalse(record.isPresent());
        }
    }

    @Nested
    @DisplayName("not accept null")
    class NotAcceptNull {

        @Test
        @DisplayName("request ID when reading history")
        void idForReadHistory() {
            assertThrows(NullPointerException.class,
                         () -> storage.historyBackward(Tests.nullRef(), 10));
        }

        @Test
        @DisplayName("event for writing")
        void event() {
            assertThrows(NullPointerException.class,
                         () -> storage.writeEvent(id, Tests.nullRef()));
        }

        @Test
        @DisplayName("event ID for writing")
        void eventId() {
            assertThrows(NullPointerException.class,
                         () -> storage.writeEvent(Tests.nullRef(), Event.getDefaultInstance()));
        }

        @Test
        @DisplayName("snapshot for writing")
        void snapshot() {
            assertThrows(NullPointerException.class,
                         () -> storage.writeSnapshot(id, Tests.nullRef()));
        }

        @Test
        @DisplayName("snapshot ID for writing")
        void snapshotId() {
            assertThrows(NullPointerException.class,
                         () -> storage.writeSnapshot(Tests.nullRef(),
                                                     Snapshot.getDefaultInstance()));
        }
    }

    @Nested
    @DisplayName("write and read single event by ID of type")
    class WriteAndReadEvent {

        @Test
        @DisplayName("Message")
        void byMessageId() {
            writeAndReadEventTest(id, storage);
        }

        @Test
        @DisplayName("String")
        void byStringId() {
            AggregateStorage<String, ?> storage = newStorage(TestAggregateWithIdString.class);
            String id = newUuid();
            writeAndReadEventTest(id, storage);
        }

        @Test
        @DisplayName("Long")
        void byLongId() {
            AggregateStorage<Long, ?> storage = newStorage(TestAggregateWithIdLong.class);
            long id = 10L;
            writeAndReadEventTest(id, storage);
        }

        @Test
        @DisplayName("Integer")
        void byIntegerId() {
            AggregateStorage<Integer, ?> storage = newStorage(TestAggregateWithIdInteger.class);
            int id = 10;
            writeAndReadEventTest(id, storage);
        }

        private <I> void writeAndReadEventTest(I id, AggregateStorage<I, ?> storage) {
            Event expectedEvent = eventFactory.createEvent(event(AggProject.getDefaultInstance()));

            storage.writeEvent(id, expectedEvent);

            Optional<AggregateHistory> optional = storage.read(id, MAX_VALUE);
            assertTrue(optional.isPresent());
            AggregateHistory events = optional.get();
            assertEquals(1, events.getEventCount());
            Event actualEvent = events.getEvent(0);
            assertEquals(expectedEvent, actualEvent);

            close(storage);
        }
    }

    @Test
    @DisplayName("write and read one record")
    void writeAndReadRecord() {
        AggregateEventRecord expected = StorageRecords.create(id, currentTime());

        storage.writeEventRecord(id, expected);

        Iterator<AggregateEventRecord> iterator = historyBackward();
        assertTrue(iterator.hasNext());
        AggregateEventRecord actual = iterator.next();
        assertEquals(expected, actual);
        assertFalse(iterator.hasNext());
    }

    /**
     *  This test is not applicable to the aggregate storage, as several records may be stored
     *  by the same aggregate ID. That's why it is disabled.
     */
    @SuppressWarnings("RefusedBequest")
    @Override
    @Test
    @Disabled
    @DisplayName("re-write record if writing by the same ID")
    protected void rewriteRecord() {
    }

    @Test
    @Override
    protected void immutableIndex() {
        ProjectAggregate aggregate = givenAggregate().withUncommittedEvents();
        storage.writeState(aggregate);
        assertIndexImmutability();
    }

    @Test
    @Override
    protected void indexCountingAllIds() {
        GivenAggregate given = givenAggregate();
        int batchSize = 5;
        List<ProjectId> expectedIds = new ArrayList<>(batchSize);
        for(int index = 0; index < batchSize; index++) {
            ProjectId id = Sample.messageOfType(ProjectId.class);
            ProjectAggregate aggregate = given.withUncommittedEvents(id);
            storage.writeState(aggregate);
            expectedIds.add(id);
        }

        Iterator<ProjectId> index = storage.index();
        ImmutableList<ProjectId> actualIds = ImmutableList.copyOf(index);
        assertThat(actualIds).containsExactlyElementsIn(expectedIds);
    }

    @Nested
    @DisplayName("write records and return them")
    class WriteRecordsAndReturn {

        @Test
        @DisplayName("sorted by timestamp descending")
        void sortedByTimestamp() {
            List<AggregateEventRecord> records = sequenceFor(id);

            writeAll(id, records);

            Iterator<AggregateEventRecord> iterator = historyBackward();
            List<AggregateEventRecord> actual = newArrayList(iterator);
            reverse(records); // expected records should be in a reverse order
            assertEquals(records, actual);
        }

        @Test
        @DisplayName("sorted by version descending")
        void sortedByVersion() {
            int eventsNumber = 5;
            List<AggregateEventRecord> records = newLinkedList();
            Timestamp timestamp = currentTime();
            Version currentVersion = zero();
            for (int i = 0; i < eventsNumber; i++) {
                AggProject state = AggProject.getDefaultInstance();
                Event event = eventFactory.createEvent(event(state), currentVersion, timestamp);
                AggregateEventRecord record = StorageRecords.create(id, timestamp, event);
                records.add(record);
                currentVersion = increment(currentVersion);
            }
            writeAll(id, records);

            Iterator<AggregateEventRecord> iterator = historyBackward();
            List<AggregateEventRecord> actual = newArrayList(iterator);
            reverse(records); // expected records should be in a reverse order
            assertEquals(records, actual);
        }

        @Test
        @DisplayName("sorted by version rather than by timestamp")
        void sortByVersionFirstly() {
            AggProject state = AggProject.getDefaultInstance();
            Version minVersion = zero();
            Version maxVersion = increment(minVersion);
            Timestamp minTimestamp = Timestamps.MIN_VALUE;
            Timestamp maxTimestamp = Timestamps.MAX_VALUE;

            // The first event is an event, which is the oldest, i.e. with the minimal version.
            Event expectedFirst = eventFactory.createEvent(event(state), minVersion, maxTimestamp);
            Event expectedSecond = eventFactory.createEvent(event(state), maxVersion, minTimestamp);

            storage.writeEvent(id, expectedSecond);
            storage.writeEvent(id, expectedFirst);

            AggregateHistory record = readRecord(id);
            List<Event> events = record.getEventList();
            assertTrue(events.indexOf(expectedFirst) < events.indexOf(expectedSecond));
        }
    }

    @Test
    @DisplayName("write and read snapshot")
    void writeAndReadSnapshot() {
        Snapshot expected = newSnapshot(currentTime());

        storage.writeSnapshot(id, expected);

        Iterator<AggregateEventRecord> iterator = historyBackward();
        assertTrue(iterator.hasNext());
        AggregateEventRecord actual = iterator.next();
        assertEquals(expected, actual.getSnapshot());
        assertFalse(iterator.hasNext());
    }

    @Nested
    @DisplayName("write records and load history")
    class WriteRecordsAndLoadHistory {

        @Test
        @DisplayName("if there are no snapshots available")
        void withNoSnapshots() {
            testWriteRecordsAndLoadHistory(currentTime());
        }

        @Test
        @DisplayName("till last snapshot available")
        void tillLastSnapshot() {
            Duration delta = seconds(10);
            Timestamp time1 = currentTime();
            Timestamp time2 = add(time1, delta);
            Timestamp time3 = add(time2, delta);

            storage.writeEventRecord(id, StorageRecords.create(id, time1));
            storage.writeSnapshot(id, newSnapshot(time2));

            testWriteRecordsAndLoadHistory(time3);
        }
    }

    @Test
    @DisplayName("continue reading history if snapshot was not found in first batch")
    void continueReadHistoryIfSnapshotNotFound() {
        Version currentVersion = zero();
        Snapshot snapshot = Snapshot.newBuilder()
                                    .setVersion(currentVersion)
                                    .build();
        storage.writeSnapshot(id, snapshot);

        int eventsAfterSnapshot = 10;
        for (int i = 0; i < eventsAfterSnapshot; i++) {
            currentVersion = increment(currentVersion);
            AggProject state = AggProject.getDefaultInstance();
            Event event = eventFactory.createEvent(event(state), currentVersion);
            storage.writeEvent(id, event);
        }

        Optional<AggregateHistory> optionalStateRecord = storage.read(id, 1);

        assertTrue(optionalStateRecord.isPresent());
        AggregateHistory stateRecord = optionalStateRecord.get();
        assertEquals(snapshot, stateRecord.getSnapshot());
        assertEquals(eventsAfterSnapshot, stateRecord.getEventCount());
    }

    @Nested
    @DisplayName("not store enrichment")
    class NotStoreEnrichment {

        @Test
        @DisplayName("for EventContext")
        void forEventContext() {
            ActorContext context = ActorContext
                    .newBuilder()
                    .buildPartial();
            MessageId messageId = MessageId
                    .newBuilder()
                    .setId(AnyPacker.pack(newEventId()))
                    .setTypeUrl(TypeUrl.of(Nothing.class)
                                       .value())
                    .buildPartial();
            Origin origin = Origin
                    .newBuilder()
                    .setActorContext(context)
                    .setMessage(messageId)
                    .vBuild();
            EventContext enrichedContext = EventContext
                    .newBuilder()
                    .setEnrichment(withOneAttribute())
                    .setTimestamp(Time.currentTime())
                    .setProducerId(AnyPacker.pack(TestValues.newUuidValue()))
                    .setPastMessage(origin)
                    .build();
            Event event = Event
                    .newBuilder()
                    .setId(newEventId())
                    .setContext(enrichedContext)
                    .setMessage(AnyPacker.pack(TestValues.newUuidValue()))
                    .build();
            storage.writeEvent(id, event);

            AggregateHistory record = readRecord(id);
            EventContext loadedContext = record.getEvent(0)
                                               .context();
            assertTrue(isDefault(loadedContext.getEnrichment()));
        }

        @SuppressWarnings("deprecation") // For backward compatibility.
        @Test
        @DisplayName("for origin of EventContext type")
        void forEventContextOrigin() {
            EventContext origin = EventContext
                    .newBuilder()
                    .setEnrichment(withOneAttribute())
                    .setTimestamp(Time.currentTime())
                    .setProducerId(AnyPacker.pack(TestValues.newUuidValue()))
                    .setCommandContext(GivenCommandContext.withRandomActor())
                    .build();
            EventContext context = EventContext
                    .newBuilder()
                    .setEventContext(origin)
                    .setTimestamp(Time.currentTime())
                    .setProducerId(AnyPacker.pack(TestValues.newUuidValue()))
                    .build();
            Event event = Event
                    .newBuilder()
                    .setId(newEventId())
                    .setContext(context)
                    .setMessage(AnyPacker.pack(TestValues.newUuidValue()))
                    .build();
            storage.writeEvent(id, event);
            AggregateHistory record = readRecord(id);
            EventContext loadedOrigin = record.getEvent(0)
                                              .context()
                                              .getEventContext();
            assertTrue(isDefault(loadedOrigin.getEnrichment()));
        }
    }

    private AggregateHistory readRecord(ProjectId id) {
        Optional<AggregateHistory> optional = storage.read(id);
        assertTrue(optional.isPresent());
        return optional.get();
    }

    void testWriteRecordsAndLoadHistory(Timestamp firstRecordTime) {
        List<AggregateEventRecord> records = sequenceFor(id, firstRecordTime);

        writeAll(id, records);

        AggregateHistory events = readRecord(id);
        List<Event> expectedEvents = records.stream()
                                            .map(AggregateStorageTest::toEvent)
                                            .collect(Collectors.toList());
        List<Event> actualEvents = events.getEventList();
        assertEquals(expectedEvents, actualEvents);
    }

    protected void writeAll(ProjectId id, Iterable<AggregateEventRecord> records) {
        for (AggregateEventRecord record : records) {
            storage.writeEventRecord(id, record);
        }
    }

    private Iterator<AggregateEventRecord> historyBackward() {
        return storage.historyBackward(id, MAX_VALUE);
    }

    public static class TestAggregate extends Aggregate<ProjectId, AggProject, AggProject.Builder> {

        protected TestAggregate(ProjectId id) {
            super(id);
        }
    }

    private static class TestAggregateWithIdString
            extends Aggregate<String, StringProject, StringProject.Builder> {

        private TestAggregateWithIdString(String id) {
            super(id);
        }
    }

    private static class TestAggregateWithIdInteger
            extends Aggregate<Integer, IntegerProject, IntegerProject.Builder> {

        private TestAggregateWithIdInteger(Integer id) {
            super(id);
        }
    }

    private static class TestAggregateWithIdLong
            extends Aggregate<Long, LongProject, LongProject.Builder> {

        private TestAggregateWithIdLong(Long id) {
            super(id);
        }
    }

    private static GivenAggregate givenAggregate() {
        ProjectAggregateRepository repository = new ProjectAggregateRepository();
        BoundedContextBuilder.assumingTests().add(repository).build();
        return new GivenAggregate(repository);
    }
}
