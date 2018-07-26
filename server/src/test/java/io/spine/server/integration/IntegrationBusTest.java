/*
 * Copyright 2018, TeamDev. All rights reserved.
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
package io.spine.server.integration;

import com.google.protobuf.Message;
import io.spine.base.Error;
import io.spine.core.Ack;
import io.spine.core.BoundedContextName;
import io.spine.core.BoundedContextNames;
import io.spine.core.Event;
import io.spine.core.Rejection;
import io.spine.grpc.MemoizingObserver;
import io.spine.grpc.StreamObservers;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.event.EventBus;
import io.spine.server.integration.given.IntegrationBusTestEnv.ContextAwareProjectDetails;
import io.spine.server.integration.given.IntegrationBusTestEnv.ExternalMismatchSubscriber;
import io.spine.server.integration.given.IntegrationBusTestEnv.ProjectCountAggregate;
import io.spine.server.integration.given.IntegrationBusTestEnv.ProjectDetails;
import io.spine.server.integration.given.IntegrationBusTestEnv.ProjectEventsSubscriber;
import io.spine.server.integration.given.IntegrationBusTestEnv.ProjectRejectionsExtSubscriber;
import io.spine.server.integration.given.IntegrationBusTestEnv.ProjectStartedExtSubscriber;
import io.spine.server.integration.given.IntegrationBusTestEnv.ProjectWizard;
import io.spine.server.rejection.RejectionBus;
import io.spine.server.rejection.RejectionSubscriber;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import io.spine.validate.Validate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static io.spine.server.integration.ExternalMessageValidationError.UNSUPPORTED_EXTERNAL_MESSAGE;
import static io.spine.server.integration.given.IntegrationBusTestEnv.cannotStartArchivedProject;
import static io.spine.server.integration.given.IntegrationBusTestEnv.contextWithContextAwareEntitySubscriber;
import static io.spine.server.integration.given.IntegrationBusTestEnv.contextWithExtEntitySubscribers;
import static io.spine.server.integration.given.IntegrationBusTestEnv.contextWithExternalSubscribers;
import static io.spine.server.integration.given.IntegrationBusTestEnv.contextWithProjectCreatedNeeds;
import static io.spine.server.integration.given.IntegrationBusTestEnv.contextWithProjectStartedNeeds;
import static io.spine.server.integration.given.IntegrationBusTestEnv.contextWithTransport;
import static io.spine.server.integration.given.IntegrationBusTestEnv.projectCreated;
import static io.spine.server.integration.given.IntegrationBusTestEnv.projectStarted;
import static io.spine.testing.Verify.assertContains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Alex Tymchenko
 */
@SuppressWarnings({"InnerClassMayBeStatic", "ClassCanBeStatic"
        /* JUnit nested classes cannot be static. */,
        "DuplicateStringLiteralInspection" /* Common test display names. */})
@DisplayName("IntegrationBus should")
class IntegrationBusTest {

    @BeforeEach
    void setUp() {
        ProjectDetails.clear();
        ProjectWizard.clear();
        ProjectCountAggregate.clear();
        ContextAwareProjectDetails.clear();
        ProjectEventsSubscriber.clear();
        ProjectStartedExtSubscriber.clear();
        ProjectRejectionsExtSubscriber.clear();
    }

    @Nested
    @DisplayName("dispatch events from one BC")
    class DispatchEvents {

        @Test
        @DisplayName("to entities with external subscribers of another BC")
        void toEntitiesOfBc() {
            InMemoryTransportFactory transportFactory = InMemoryTransportFactory.newInstance();

            BoundedContext sourceContext = contextWithTransport(transportFactory);
            contextWithExtEntitySubscribers(transportFactory);

            assertNull(ProjectDetails.getExternalEvent());
            assertNull(ProjectWizard.getExternalEvent());
            assertNull(ProjectCountAggregate.getExternalEvent());

            Event event = projectCreated();
            sourceContext.getEventBus()
                         .post(event);

            Message expectedMessage = AnyPacker.unpack(event.getMessage());
            assertEquals(expectedMessage, ProjectDetails.getExternalEvent());
            assertEquals(expectedMessage, ProjectWizard.getExternalEvent());
            assertEquals(expectedMessage, ProjectCountAggregate.getExternalEvent());
        }

        @Test
        @DisplayName("to external subscribers of another BC")
        void toBcSubscribers() {
            InMemoryTransportFactory transportFactory = InMemoryTransportFactory.newInstance();

            BoundedContext sourceContext = contextWithTransport(transportFactory);
            contextWithExternalSubscribers(transportFactory);

            assertNull(ProjectEventsSubscriber.getExternalEvent());

            Event event = projectCreated();
            sourceContext.getEventBus()
                         .post(event);
            assertEquals(AnyPacker.unpack(event.getMessage()),
                         ProjectEventsSubscriber.getExternalEvent());
        }

        @Test
        @DisplayName("to entities with external subscribers of multiple BCs")
        void toEntitiesOfMultipleBcs() {
            InMemoryTransportFactory transportFactory = InMemoryTransportFactory.newInstance();

            Set<BoundedContextName> destinationNames = newHashSet();
            BoundedContext sourceContext = contextWithTransport(transportFactory);
            for (int i = 0; i < 42; i++) {
                BoundedContext destinationCtx =
                        contextWithContextAwareEntitySubscriber(transportFactory);
                BoundedContextName name = destinationCtx.getName();
                destinationNames.add(name);
            }

            assertTrue(ContextAwareProjectDetails.getExternalContexts()
                                                 .isEmpty());

            Event event = projectCreated();
            sourceContext.getEventBus()
                         .post(event);

            assertEquals(destinationNames.size(),
                         ContextAwareProjectDetails.getExternalContexts()
                                                   .size());

            assertEquals(destinationNames.size(),
                         ContextAwareProjectDetails.getExternalEvents()
                                                   .size());

        }

        @SuppressWarnings("unused") // Variables declared for readability.
        @Test
        @DisplayName("to two BCs with different needs")
        void toTwoBcSubscribers() {
            InMemoryTransportFactory transportFactory = InMemoryTransportFactory.newInstance();

            BoundedContext sourceContext = contextWithTransport(transportFactory);
            BoundedContext destA = contextWithProjectCreatedNeeds(transportFactory);
            BoundedContext destB = contextWithProjectStartedNeeds(transportFactory);

            assertNull(ProjectStartedExtSubscriber.getExternalEvent());
            assertNull(ProjectEventsSubscriber.getExternalEvent());

            EventBus sourceEventBus = sourceContext.getEventBus();
            Event eventA = projectCreated();
            sourceEventBus.post(eventA);
            Event eventB = projectStarted();
            sourceEventBus.post(eventB);

            assertEquals(AnyPacker.unpack(eventA.getMessage()),
                         ProjectEventsSubscriber.getExternalEvent());

            assertEquals(AnyPacker.unpack(eventB.getMessage()),
                         ProjectStartedExtSubscriber.getExternalEvent());
        }
    }

    @Test
    @DisplayName("dispatch rejections from one BC to external subscribers of another BC")
    void dispatchRejectionsToOtherBc() {
        InMemoryTransportFactory transportFactory = InMemoryTransportFactory.newInstance();

        BoundedContext sourceContext = contextWithTransport(transportFactory);
        contextWithExternalSubscribers(transportFactory);

        assertNull(ProjectRejectionsExtSubscriber.getExternalRejection());
        assertNull(ProjectCountAggregate.getExternalRejection());
        assertNull(ProjectWizard.getExternalRejection());

        Rejection rejection = cannotStartArchivedProject();
        sourceContext.getRejectionBus()
                     .post(rejection);
        Message rejectionMessage = AnyPacker.unpack(rejection.getMessage());

        assertEquals(rejectionMessage, ProjectRejectionsExtSubscriber.getExternalRejection());
        assertEquals(rejectionMessage, ProjectCountAggregate.getExternalRejection());
        assertEquals(rejectionMessage, ProjectWizard.getExternalRejection());
    }

    @Nested
    @DisplayName("avoid dispatching events from one BC")
    class AvoidDispatching {

        @Test
        @DisplayName("to domestic entity subscribers of another BC")
        void toDomesticEntitySubscribers() {
            InMemoryTransportFactory transportFactory = InMemoryTransportFactory.newInstance();

            BoundedContext sourceContext = contextWithTransport(transportFactory);
            BoundedContext destContext = contextWithExtEntitySubscribers(transportFactory);

            assertNull(ProjectDetails.getDomesticEvent());

            Event event = projectStarted();
            sourceContext.getEventBus()
                         .post(event);

            assertNotEquals(AnyPacker.unpack(event.getMessage()),
                            ProjectDetails.getDomesticEvent());
            assertNull(ProjectDetails.getDomesticEvent());

            destContext.getEventBus()
                       .post(event);
            assertEquals(AnyPacker.unpack(event.getMessage()), ProjectDetails.getDomesticEvent());
        }

        @Test
        @DisplayName("to domestic standalone subscribers of another BC")
        void toDomesticStandaloneSubscribers() {
            InMemoryTransportFactory transportFactory = InMemoryTransportFactory.newInstance();

            BoundedContext sourceContext = contextWithTransport(transportFactory);
            BoundedContext destContext = contextWithExternalSubscribers(transportFactory);

            assertNull(ProjectEventsSubscriber.getDomesticEvent());

            Event event = projectStarted();
            sourceContext.getEventBus()
                         .post(event);

            assertNotEquals(AnyPacker.unpack(event.getMessage()),
                            ProjectEventsSubscriber.getDomesticEvent());
            assertNull(ProjectEventsSubscriber.getDomesticEvent());

            destContext.getEventBus()
                       .post(event);
            assertEquals(AnyPacker.unpack(event.getMessage()),
                         ProjectEventsSubscriber.getDomesticEvent());
        }
    }

    @Test
    @DisplayName("update local subscriptions upon repeated RequestedMessageTypes")
    void updateLocalSubscriptions() {
        InMemoryTransportFactory transportFactory = InMemoryTransportFactory.newInstance();

        BoundedContext sourceContext = contextWithTransport(transportFactory);
        BoundedContext destinationCtx = contextWithTransport(transportFactory);

        // Prepare two external subscribers for the different events in the the `destinationCtx`.
        ProjectEventsSubscriber projectCreatedSubscriber
                = new ProjectEventsSubscriber();
        ProjectStartedExtSubscriber projectStartedSubscriber
                = new ProjectStartedExtSubscriber();

        // Before anything happens, there were no events received by those.
        assertNull(ProjectEventsSubscriber.getExternalEvent());
        assertNull(ProjectStartedExtSubscriber.getExternalEvent());

        // Both events are prepared along with the `EventBus` of the source bounded context.
        EventBus sourceEventBus = sourceContext.getEventBus();
        Event eventA = projectCreated();
        Event eventB = projectStarted();

        // Both events are emitted, `ProjectCreated` subscriber only is present.
        destinationCtx.getIntegrationBus()
                      .register(projectCreatedSubscriber);
        sourceEventBus.post(eventA);
        sourceEventBus.post(eventB);
        // Only `ProjectCreated` should have been dispatched.
        assertEquals(AnyPacker.unpack(eventA.getMessage()),
                     ProjectEventsSubscriber.getExternalEvent());
        assertNull(ProjectStartedExtSubscriber.getExternalEvent());

        // Clear before the next round starts.
        ProjectStartedExtSubscriber.clear();
        ProjectEventsSubscriber.clear();

        // Both events are emitted, No external subscribers at all.
        destinationCtx.getIntegrationBus()
                      .unregister(projectCreatedSubscriber);
        sourceEventBus.post(eventA);
        sourceEventBus.post(eventB);
        // No events should have been dispatched.
        assertNull(ProjectEventsSubscriber.getExternalEvent());
        assertNull(ProjectStartedExtSubscriber.getExternalEvent());

        // Both events are emitted, `ProjectStarted` subscriber only is present
        destinationCtx.getIntegrationBus()
                      .register(projectStartedSubscriber);
        sourceEventBus.post(eventA);
        sourceEventBus.post(eventB);
        // This time `ProjectStarted` event should only have been dispatched.
        assertNull(ProjectEventsSubscriber.getExternalEvent());
        assertEquals(AnyPacker.unpack(eventB.getMessage()),
                     ProjectStartedExtSubscriber.getExternalEvent());
    }

    @Test
    @DisplayName("throw exception on mismatch of external attribute during dispatching")
    void throwOnExtAttributeMismatch() {
        InMemoryTransportFactory transportFactory = InMemoryTransportFactory.newInstance();

        BoundedContext sourceContext = contextWithTransport(transportFactory);
        RejectionSubscriber rejectionSubscriber = new ExternalMismatchSubscriber();
        sourceContext.getRejectionBus()
                     .register(rejectionSubscriber);
        sourceContext.getIntegrationBus()
                     .register(rejectionSubscriber);
        Rejection rejection = cannotStartArchivedProject();
        try {
            sourceContext.getRejectionBus()
                         .post(rejection);
            fail("An exception is expected.");
        } catch (Exception e) {
            String exceptionMsg = e.getMessage();
            assertContains("external", exceptionMsg);
        }
    }

    @Nested
    @DisplayName("not dispatch to domestic subscribers if they requested external")
    class NotDispatchToDomestic {

        @Test
        @DisplayName("events")
        void eventsIfNeedExternal() {
            InMemoryTransportFactory transportFactory = InMemoryTransportFactory.newInstance();

            BoundedContext context = contextWithExtEntitySubscribers(transportFactory);
            ProjectEventsSubscriber eventSubscriber = new ProjectEventsSubscriber();
            EventBus eventBus = context.getEventBus();
            eventBus.register(eventSubscriber);

            assertNull(ProjectEventsSubscriber.getExternalEvent());
            assertNull(ProjectDetails.getExternalEvent());
            assertNull(ProjectWizard.getExternalEvent());
            assertNull(ProjectCountAggregate.getExternalEvent());

            Event projectCreated = projectCreated();
            eventBus.post(projectCreated);

            assertNull(ProjectEventsSubscriber.getExternalEvent());
            assertNull(ProjectDetails.getExternalEvent());
            assertNull(ProjectWizard.getExternalEvent());
            assertNull(ProjectCountAggregate.getExternalEvent());
        }

        @Test
        @DisplayName("rejections")
        void rejectionsIfNeedExternal() {
            InMemoryTransportFactory transportFactory = InMemoryTransportFactory.newInstance();

            BoundedContext sourceContext = contextWithExtEntitySubscribers(transportFactory);
            ProjectRejectionsExtSubscriber standaloneSubscriber =
                    new ProjectRejectionsExtSubscriber();

            RejectionBus rejectionBus = sourceContext.getRejectionBus();
            rejectionBus.register(standaloneSubscriber);

            assertNull(ProjectRejectionsExtSubscriber.getExternalRejection());
            assertNull(ProjectWizard.getExternalRejection());
            assertNull(ProjectCountAggregate.getExternalRejection());

            Rejection rejection = cannotStartArchivedProject();
            rejectionBus.post(rejection);

            assertNull(ProjectRejectionsExtSubscriber.getExternalRejection());
            assertNull(ProjectWizard.getExternalRejection());
            assertNull(ProjectCountAggregate.getExternalRejection());
        }
    }

    @Test
    @DisplayName("emit unsupported external message exception if message type is unknown")
    void throwOnUnknownMessage() {
        InMemoryTransportFactory transportFactory = InMemoryTransportFactory.newInstance();
        BoundedContext boundedContext = contextWithTransport(transportFactory);

        Event event = projectCreated();
        BoundedContextName boundedContextName = BoundedContextNames.newName("External context ID");
        ExternalMessage externalMessage = ExternalMessages.of(event,
                                                                    boundedContextName);
        MemoizingObserver<Ack> observer = StreamObservers.memoizingObserver();
        boundedContext.getIntegrationBus()
                      .post(externalMessage, observer);
        Error error = observer.firstResponse()
                              .getStatus()
                              .getError();
        assertFalse(Validate.isDefault(error));
        assertEquals(ExternalMessageValidationError.getDescriptor()
                                                   .getFullName(),
                     error.getType());
        assertTrue(UNSUPPORTED_EXTERNAL_MESSAGE.getNumber() == error.getCode());
    }
}