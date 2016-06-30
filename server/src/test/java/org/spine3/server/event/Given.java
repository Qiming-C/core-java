/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.event;

import org.spine3.base.EventContext;
import org.spine3.test.event.ProjectCreated;
import org.spine3.test.event.ProjectId;
import org.spine3.test.event.ProjectStarted;

import static org.spine3.base.Events.createEvent;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.testdata.TestEventContextFactory.createEventContext;

/**
 * Is for usage only in tests under the `event` package.
 */
@SuppressWarnings("EmptyClass")
public class Given {

    public static class AggregateId {

        private AggregateId() {
        }

        public static ProjectId newProjectId() {
            final String uuid = newUuid();
            return ProjectId.newBuilder()
                            .setId(uuid)
                            .build();
        }
    }

    public static class EventMessage {

        private static final ProjectId DUMMY_PROJECT_ID = AggregateId.newProjectId();
        private static final ProjectCreated PROJECT_CREATED = projectCreated(DUMMY_PROJECT_ID);
        private static final ProjectStarted PROJECT_STARTED = projectStarted(DUMMY_PROJECT_ID);

        private EventMessage() {
        }

        public static ProjectCreated projectCreated() {
            return PROJECT_CREATED;
        }

        public static ProjectStarted projectStarted() {
            return PROJECT_STARTED;
        }

        public static ProjectCreated projectCreated(ProjectId id) {
            return ProjectCreated.newBuilder()
                                 .setProjectId(id)
                                 .build();
        }

        public static ProjectStarted projectStarted(ProjectId id) {
            return ProjectStarted.newBuilder()
                                 .setProjectId(id)
                                 .build();
        }
    }

    public static class Event {

        private static final ProjectId PROJECT_ID = AggregateId.newProjectId();

        private Event() {
        }

        /**
         * Creates a new event with default properties.
         */
        public static org.spine3.base.Event projectCreated() {
            return projectCreated(PROJECT_ID);
        }

        /**
         * Creates a new event with default properties.
         */
        public static org.spine3.base.Event projectStarted() {
            final ProjectStarted msg = EventMessage.projectStarted();
            final org.spine3.base.Event event = createEvent(msg, createEventContext(msg.getProjectId()));
            return event;
        }

        /**
         * Creates a new event with the given projectId.
         */
        public static org.spine3.base.Event projectCreated(ProjectId projectId) {
            return projectCreated(projectId, createEventContext(projectId));
        }

        /**
         * Creates a new event with the given projectId and eventContext.
         */
        public static org.spine3.base.Event projectCreated(ProjectId projectId, EventContext eventContext) {
            final ProjectCreated msg = EventMessage.projectCreated(projectId);
            final org.spine3.base.Event event = createEvent(msg, eventContext);
            return event;
        }
    }
}
