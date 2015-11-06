/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.testdata;

import org.spine3.server.storage.EventStoreRecord;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.ProjectStarted;
import org.spine3.test.project.event.TaskAdded;

import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.testdata.AggregateIdFactory.newProjectId;

/**
 * The utility class for creating EventStoreRecords for tests.
 *
 * @author Alexander Litus
 */
@SuppressWarnings("UtilityClass")
public class EventStoreRecordFactory {

    private EventStoreRecordFactory() {}

    public static EventStoreRecord projectCreated() {
        final ProjectCreated event = ProjectCreated.newBuilder().setProjectId(newProjectId()).build();
        final EventStoreRecord.Builder builder = EventStoreRecord.newBuilder()
                .setEvent(toAny(event))
                .setEventId("project_created");
        return builder.build();
    }

    public static EventStoreRecord taskAdded() {
        final TaskAdded taskAdded = TaskAdded.newBuilder().setProjectId(newProjectId()).build();
        final EventStoreRecord.Builder builder = EventStoreRecord.newBuilder()
                .setEvent(toAny(taskAdded))
                .setEventId("task_added");
        return builder.build();
    }

    public static EventStoreRecord projectStarted() {
        final ProjectStarted event = ProjectStarted.newBuilder().setProjectId(newProjectId()).build();
        final EventStoreRecord.Builder builder = EventStoreRecord.newBuilder()
                .setEvent(toAny(event))
                .setEventId("project_started");
        return builder.build();
    }
}