/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.projection;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.annotation.Internal;
import io.spine.core.Event;
import io.spine.server.BoundedContext;
import io.spine.server.ServerEnvironment;
import io.spine.server.delivery.Delivery;
import io.spine.server.delivery.Inbox;
import io.spine.server.delivery.InboxLabel;
import io.spine.server.entity.EventDispatchingRepository;
import io.spine.server.entity.RepositoryCache;
import io.spine.server.entity.model.StateClass;
import io.spine.server.event.EventFilter;
import io.spine.server.event.EventStore;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.event.model.SubscriberMethod;
import io.spine.server.integration.ExternalMessageClass;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.projection.model.ProjectionClass;
import io.spine.server.route.EventRouting;
import io.spine.server.route.StateUpdateRouting;
import io.spine.server.stand.Stand;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.type.TypeName;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Sets.union;
import static io.spine.option.EntityOption.Kind.PROJECTION;
import static io.spine.server.projection.model.ProjectionClass.asProjectionClass;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * Abstract base for repositories managing {@link Projection}s.
 *
 * @param <I> the type of IDs of projections
 * @param <P> the type of projections
 * @param <S> the type of projection state messages
 */
public abstract class ProjectionRepository<I, P extends Projection<I, S, ?>, S extends Message>
        extends EventDispatchingRepository<I, P, S> {

    private @MonotonicNonNull Inbox<I> inbox;

    private @MonotonicNonNull RepositoryCache<I, P> cache;

    /**
     * Initializes the repository.
     *
     * <p>Ensures there is at least one event subscriber method (external or domestic) declared
     * by the class of the projection. Throws an {@code IllegalStateException} otherwise.
     *
     * <p>If projections of this repository are {@linkplain io.spine.core.Subscribe subscribed} to
     * entity state updates, a routing for state updates is created and
     * {@linkplain #setupStateRouting(StateUpdateRouting) configured}.
     * If one of the states of entities cannot be routed during the created schema,
     * {@code IllegalStateException} will be thrown.
     *
     * @param context
     *         the {@code BoundedContext} of this repository
     * @throws IllegalStateException
     *         if the state routing does not cover one of the entity state types to which
     *         the entities are subscribed
     */
    @Override
    @OverridingMethodsMustInvokeSuper
    public void registerWith(BoundedContext context) throws IllegalStateException {
        super.registerWith(context);
        ensureDispatchesEvents();
        initCache(context.isMultitenant());
        initInbox();
    }

    private void initCache(boolean multitenant) {
        cache = new RepositoryCache<>(multitenant, this::findOrCreate, this::store);
    }

    /**
     * Initializes the {@code Inbox}.
     */
    private void initInbox() {
        Delivery delivery = ServerEnvironment.instance()
                                             .delivery();
        inbox = delivery
                .<I>newInbox(entityStateType())
                .withBatchDispatcher(new Inbox.BatchDispatcher<I>() {
                    @Override
                    public void onStart(I id) {
                        cache.startCaching(id);
                    }

                    @Override
                    public void onEnd(I id) {
                        cache.stopCaching(id);
                    }
                })
                .addEventEndpoint(InboxLabel.UPDATE_SUBSCRIBER,
                                  e -> ProjectionEndpoint.of(this, e))
                .build();
    }

    private Inbox<I> inbox() {
        return checkNotNull(inbox);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    protected void setupEventRouting(EventRouting<I> routing) {
        super.setupEventRouting(routing);
        if (projectionClass().subscribesToStates()) {
            StateUpdateRouting<I> stateRouting = createStateRouting();
            routing.routeStateUpdates(stateRouting);
        }
    }

    private void ensureDispatchesEvents() {
        boolean noEventSubscriptions = !dispatchesEvents();
        if (noEventSubscriptions) {
            boolean noExternalSubscriptions = !dispatchesExternalEvents();
            if (noExternalSubscriptions) {
                throw newIllegalStateException(
                        "Projections of the repository `%s` have neither domestic nor external" +
                                " event subscriptions.", this);
            }
        }
    }

    /**
     * Creates and configures the {@code StateUpdateRouting} used by this repository.
     *
     * <p>This method verifies that the created state routing serves all the state classes to
     * which projections of this repository are subscribed.
     *
     * @throws IllegalStateException
     *          if one of the subscribed state classes cannot be served by the created state routing
     */
    private StateUpdateRouting<I> createStateRouting() {
        StateUpdateRouting<I> routing = StateUpdateRouting.newInstance(idClass());
        setupStateRouting(routing);
        validate(routing);
        return routing;
    }

    private void validate(StateUpdateRouting<I> routing) throws IllegalStateException {
        ProjectionClass<P> cls = projectionClass();
        Set<StateClass> stateClasses = union(cls.domesticStates(), cls.externalStates());
        ImmutableList<StateClass> unsupported =
                stateClasses.stream()
                            .filter(c -> !routing.supports(c.value()))
                            .collect(toImmutableList());
        if (!unsupported.isEmpty()) {
            boolean moreThanOne = unsupported.size() > 1;
            String fmt =
                    "The repository `%s` does not provide routing for updates of the state " +
                            (moreThanOne ? "classes" : "class") +
                            " `%s` to which the class `%s` is subscribed.";
            throw newIllegalStateException(
                    fmt, this, (moreThanOne ? unsupported : unsupported.get(0)), cls
            );
        }
    }

    /**
     * A callback for derived repository classes to customize routing schema for delivering
     * updated state to subscribed entities, if the default schema does not satisfy
     * the routing needs.
     *
     * @param routing
     *         the routing to customize
     */
    @SuppressWarnings("NoopMethodInAbstractClass") // see Javadoc
    protected void setupStateRouting(StateUpdateRouting<I> routing) {
        // Do nothing by default.
    }

    @VisibleForTesting
    static Timestamp nullToDefault(@Nullable Timestamp timestamp) {
        return timestamp == null
               ? Timestamp.getDefaultInstance()
               : timestamp;
    }

    /** Obtains {@link EventStore} from which to get events during catch-up. */
    EventStore eventStore() {
        return context()
                .eventBus()
                .eventStore();
    }

    /** Obtains class information of projection managed by this repository. */
    private ProjectionClass<P> projectionClass() {
        return (ProjectionClass<P>) entityModelClass();
    }

    @Internal
    @Override
    protected final ProjectionClass<P> toModelClass(Class<P> cls) {
        return asProjectionClass(cls);
    }

    @Override
    public P create(I id) {
        P projection = super.create(id);
        lifecycleOf(id).onEntityCreated(PROJECTION);
        return projection;
    }

    @Override
    public Optional<ExternalMessageDispatcher> createExternalDispatcher() {
        if (!dispatchesExternalEvents()) {
            return Optional.empty();
        }
        return Optional.of(new ProjectionExternalEventDispatcher());
    }

    /**
     * Obtains event filters for event classes handled by projections of this repository.
     */
    private Set<EventFilter> createEventFilters() {
        ImmutableSet.Builder<EventFilter> builder = ImmutableSet.builder();
        Set<EventClass> eventClasses = messageClasses();
        for (EventClass eventClass : eventClasses) {
            String typeName = TypeName.of(eventClass.value())
                                      .value();
            builder.add(EventFilter.newBuilder()
                                   .setEventType(typeName)
                                   .build());
        }
        return builder.build();
    }

    /**
     * Obtains the {@code Stand} from the {@code BoundedContext} of this repository.
     */
    protected final Stand stand() {
        return context().stand();
    }

    /**
     * Ensures that the repository has the storage.
     *
     * @return storage instance
     * @throws IllegalStateException if the storage is null
     */
    @Override
    protected RecordStorage<I> recordStorage() {
        @SuppressWarnings("unchecked") // ensured by the type returned by `createdStorage()`.
        RecordStorage<I> recordStorage = ((ProjectionStorage<I>) storage()).recordStorage();
        return checkStorage(recordStorage);
    }

    @Override
    protected ProjectionStorage<I> createStorage() {
        StorageFactory sf = defaultStorageFactory();
        Class<P> projectionClass = entityClass();
        ProjectionStorage<I> projectionStorage =
                sf.createProjectionStorage(context().spec(), projectionClass);
        return projectionStorage;
    }

    /**
     * {@inheritDoc}
     *
     * //TODO:2019-08-25:alex.tymchenko: document.
     */
    @Override
    protected P findOrCreate(I id) {
        return cache.load(id);
    }

    @Override
    public void store(P entity) {
        cache.store(entity);
    }

    /**
     * Ensures that the repository has the storage.
     *
     * @return storage instance
     * @throws IllegalStateException if the storage is null
     */
    protected ProjectionStorage<I> projectionStorage() {
        @SuppressWarnings("unchecked") /* OK as we control the creation in createStorage(). */
        ProjectionStorage<I> storage = (ProjectionStorage<I>) storage();
        return storage;
    }

    @Override
    public Set<EventClass> messageClasses() {
        return projectionClass().domesticEvents();
    }

    @Override
    public Set<EventClass> externalEventClasses() {
        return projectionClass().externalEvents();
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public boolean canDispatch(EventEnvelope event) {
        Optional<SubscriberMethod> subscriber = projectionClass().subscriberOf(event);
        return subscriber.isPresent();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sends a system command to dispatch the given event to a subscriber.
     */
    @Override
    protected void dispatchTo(I id, Event event) {
        inbox().send(EventEnvelope.of(event))
               .toSubscriber(id);
    }

    @Internal
    public void writeLastHandledEventTime(Timestamp timestamp) {
        checkNotNull(timestamp);
        projectionStorage().writeLastHandledEventTime(timestamp);
    }

    @Internal
    public Timestamp readLastHandledEventTime() {
        Timestamp timestamp = projectionStorage().readLastHandledEventTime();
        return nullToDefault(timestamp);
    }

    @VisibleForTesting
    EventStreamQuery createStreamQuery() {
        Set<EventFilter> eventFilters = createEventFilters();

        // Gets the timestamp of the last event. This also ensures we have the storage.
        Timestamp timestamp = readLastHandledEventTime();
        EventStreamQuery.Builder builder = EventStreamQuery
                .newBuilder()
                .setAfter(timestamp)
                .addAllFilter(eventFilters);
        return builder.build();
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void close() {
        super.close();
        if (inbox != null) {
            inbox.unregister();
        }
    }

    /**
     * An implementation of an external message dispatcher feeding external events
     * to {@code Projection} instances.
     */
    private class ProjectionExternalEventDispatcher extends AbstractExternalEventDispatcher {

        @Override
        public Set<ExternalMessageClass> messageClasses() {
            Set<EventClass> eventClasses = projectionClass().externalEvents();
            return ExternalMessageClass.fromEventClasses(eventClasses);
        }
    }
}
