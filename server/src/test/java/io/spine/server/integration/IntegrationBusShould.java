/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

import io.spine.core.BoundedContextId;
import io.spine.core.Event;
import io.spine.core.Rejection;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.event.EventBus;
import io.spine.server.integration.given.IntegrationBusTestEnv.CannotCreateProjectExtSubscriber;
import io.spine.server.integration.given.IntegrationBusTestEnv.ContextAwareProjectDetails;
import io.spine.server.integration.given.IntegrationBusTestEnv.ProjectDetails;
import io.spine.server.integration.given.IntegrationBusTestEnv.ProjectEventsSubscriber;
import io.spine.server.integration.given.IntegrationBusTestEnv.ProjectStartedExtSubscriber;
import io.spine.server.integration.local.LocalTransportFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static io.spine.server.integration.given.IntegrationBusTestEnv.cannotStartArchivedProject;
import static io.spine.server.integration.given.IntegrationBusTestEnv.contextWithContextAwareEntitySubscriber;
import static io.spine.server.integration.given.IntegrationBusTestEnv.contextWithExtEntitySubscriber;
import static io.spine.server.integration.given.IntegrationBusTestEnv.contextWithExternalSubscribers;
import static io.spine.server.integration.given.IntegrationBusTestEnv.contextWithProjectCreatedNeeds;
import static io.spine.server.integration.given.IntegrationBusTestEnv.contextWithProjectStartedNeeds;
import static io.spine.server.integration.given.IntegrationBusTestEnv.contextWithTransport;
import static io.spine.server.integration.given.IntegrationBusTestEnv.projectCreated;
import static io.spine.server.integration.given.IntegrationBusTestEnv.projectStarted;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Alex Tymchenko
 */
public class IntegrationBusShould {

    @Before
    public void setUp() {
        ProjectDetails.clear();
        ContextAwareProjectDetails.clear();
        ProjectEventsSubscriber.clear();
        ProjectStartedExtSubscriber.clear();
    }

    @Test
    public void dispatch_events_from_one_BC_to_entities_with_ext_subscribers_of_another_BC() {
        final LocalTransportFactory transportFactory = LocalTransportFactory.newInstance();

        final BoundedContext sourceContext = contextWithTransport(transportFactory);
        contextWithExtEntitySubscriber(transportFactory);

        assertNull(ProjectDetails.getExternalEvent());

        final Event event = projectCreated();
        sourceContext.getEventBus()
                     .post(event);

        assertEquals(AnyPacker.unpack(event.getMessage()), ProjectDetails.getExternalEvent());
    }

    @Test
    public void avoid_dispatch_events_from_one_BC_to_domestic_entity_subscribers_of_another_BC() {
        final LocalTransportFactory transportFactory = LocalTransportFactory.newInstance();

        final BoundedContext sourceContext = contextWithTransport(transportFactory);
        final BoundedContext destContext = contextWithExtEntitySubscriber(transportFactory);

        assertNull(ProjectDetails.getDomesticEvent());

        final Event event = projectStarted();
        sourceContext.getEventBus()
                     .post(event);

        assertNotEquals(AnyPacker.unpack(event.getMessage()), ProjectDetails.getDomesticEvent());
        assertNull(ProjectDetails.getDomesticEvent());

        destContext.getEventBus()
                   .post(event);
        assertEquals(AnyPacker.unpack(event.getMessage()), ProjectDetails.getDomesticEvent());
    }

    @Test
    public void dispatch_events_from_one_BC_to_external_subscribers_of_another_BC() {
        final LocalTransportFactory transportFactory = LocalTransportFactory.newInstance();

        final BoundedContext sourceContext = contextWithTransport(transportFactory);
        contextWithExternalSubscribers(transportFactory);

        assertNull(ProjectEventsSubscriber.getExternalEvent());

        final Event event = projectCreated();
        sourceContext.getEventBus()
                     .post(event);
        assertEquals(AnyPacker.unpack(event.getMessage()),
                     ProjectEventsSubscriber.getExternalEvent());
    }

    @Test
    public void avoid_dispatch_events_from_one_BC_to_domestic_st_alone_subscribers_of_another_BC() {
        final LocalTransportFactory transportFactory = LocalTransportFactory.newInstance();

        final BoundedContext sourceContext = contextWithTransport(transportFactory);
        final BoundedContext destContext = contextWithExternalSubscribers(transportFactory);

        assertNull(ProjectEventsSubscriber.getDomesticEvent());

        final Event event = projectStarted();
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

    @Test
    public void dispatch_events_from_one_BC_to_entities_with_ext_subscribers_of_multiple_BCs() {
        final LocalTransportFactory transportFactory = LocalTransportFactory.newInstance();

        final Set<BoundedContextId> destinationIds = newHashSet();
        final BoundedContext sourceContext = contextWithTransport(transportFactory);
        for (int i = 0; i < 42; i++) {
            final BoundedContext destinationCtx =
                    contextWithContextAwareEntitySubscriber(transportFactory);
            final BoundedContextId id = destinationCtx.getId();
            destinationIds.add(id);
        }

        assertTrue(ContextAwareProjectDetails.getExternalContexts()
                                             .isEmpty());

        final Event event = projectCreated();
        sourceContext.getEventBus()
                     .post(event);

        assertEquals(destinationIds.size(),
                     ContextAwareProjectDetails.getExternalContexts()
                                               .size());

        assertEquals(destinationIds.size(),
                     ContextAwareProjectDetails.getExternalEvents()
                                               .size());

    }

    @Test
    public void dispatch_events_from_one_BC_to_two_BCs_with_different_needs() {
        final LocalTransportFactory transportFactory = LocalTransportFactory.newInstance();

        final BoundedContext sourceContext = contextWithTransport(transportFactory);
        final BoundedContext destA = contextWithProjectCreatedNeeds(transportFactory);
        final BoundedContext destB = contextWithProjectStartedNeeds(transportFactory);

        assertNull(ProjectStartedExtSubscriber.getExternalEvent());
        assertNull(ProjectEventsSubscriber.getExternalEvent());

        final EventBus sourceEventBus = sourceContext.getEventBus();
        final Event eventA = projectCreated();
        sourceEventBus.post(eventA);
        final Event eventB = projectStarted();
        sourceEventBus.post(eventB);

        assertEquals(AnyPacker.unpack(eventA.getMessage()),
                     ProjectEventsSubscriber.getExternalEvent());

        assertEquals(AnyPacker.unpack(eventB.getMessage()),
                     ProjectStartedExtSubscriber.getExternalEvent());
    }

    @Test
    public void update_local_subscriptions_upon_repeated_RequestedMessageTypes() {
        final LocalTransportFactory transportFactory = LocalTransportFactory.newInstance();

        final BoundedContext sourceContext = contextWithTransport(transportFactory);
        final BoundedContext destinationCtx = contextWithTransport(transportFactory);

        // Prepare two external subscribers for the different events in the the `destinationCtx`.
        final ProjectEventsSubscriber projectCreatedSubscriber
                = new ProjectEventsSubscriber();
        final ProjectStartedExtSubscriber projectStartedSubscriber
                = new ProjectStartedExtSubscriber();

        // Before anything happens, there were no events received by those.
        assertNull(ProjectEventsSubscriber.getExternalEvent());
        assertNull(ProjectStartedExtSubscriber.getExternalEvent());

        // Both events are prepared along with the `EventBus` of the source bounded context.
        final EventBus sourceEventBus = sourceContext.getEventBus();
        final Event eventA = projectCreated();
        final Event eventB = projectStarted();

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
    public void dispatch_rejections_from_one_BC_to_external_subscribers_of_another_BC() {
        final LocalTransportFactory transportFactory = LocalTransportFactory.newInstance();

        final BoundedContext sourceContext = contextWithTransport(transportFactory);
        contextWithExternalSubscribers(transportFactory);

        assertNull(CannotCreateProjectExtSubscriber.getExternalRejection());

        final Rejection rejection = cannotStartArchivedProject();
        sourceContext.getRejectionBus()
                     .post(rejection);
        assertEquals(AnyPacker.unpack(rejection.getMessage()),
                     CannotCreateProjectExtSubscriber.getExternalRejection());
    }

}
