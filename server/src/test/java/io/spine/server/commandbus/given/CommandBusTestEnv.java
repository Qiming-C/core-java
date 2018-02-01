/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.commandbus.given;

import io.spine.core.CommandContext;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.test.commandbus.CProject;
import io.spine.test.commandbus.CProjectId;
import io.spine.test.commandbus.CProjectVBuilder;
import io.spine.test.commandbus.command.CAddTask;
import io.spine.test.commandbus.event.CTaskAdded;
import io.spine.testdata.Sample;

import static com.google.common.base.Preconditions.checkNotNull;

public class CommandBusTestEnv {

    private CommandBusTestEnv() {
        // Prevent instantiation.
    }

    public static CProjectId projectId() {
        return ((CProjectId.Builder) Sample.builderForType(CProjectId.class))
                .build();
    }

    public static CAddTask addTask(CProjectId id) {
        checkNotNull(id);

        final CAddTask task = CAddTask.newBuilder()
                                      .setProjectId(id)
                                      .build();
        return task;
    }

    public static class ProjectAggregateRepository
            extends AggregateRepository<CProjectId, ProjectAggregate> { }

    public static class ProjectAggregate extends Aggregate<CProjectId, CProject, CProjectVBuilder> {

        protected ProjectAggregate(CProjectId id) {
            super(id);
        }

        @Assign
        private CTaskAdded on(CAddTask command, CommandContext context) {
            final CTaskAdded event = taskAdded();
            return event;
        }

        @Apply
        private void event(CTaskAdded event) {
            final Integer currentCount = getBuilder().getTaskCount();
            getBuilder().setTaskCount(currentCount + 1);
        }

        private CTaskAdded taskAdded() {
            return CTaskAdded.newBuilder()
                             .setProjectId(getId())
                             .build();
        }
    }
}
