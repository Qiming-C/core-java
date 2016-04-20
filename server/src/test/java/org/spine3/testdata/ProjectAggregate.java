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

package org.spine3.testdata;

import org.spine3.base.CommandContext;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.command.Assign;
import org.spine3.test.project.Project;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.command.AddTask;
import org.spine3.test.project.command.CreateProject;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.TaskAdded;

import static org.spine3.testdata.TestEventMessageFactory.projectCreatedEvent;
import static org.spine3.testdata.TestEventMessageFactory.taskAddedEvent;

public class ProjectAggregate extends Aggregate<ProjectId, Project, Project.Builder> {

    public ProjectAggregate(ProjectId id) {
        super(id);
    }

    @Assign
    public ProjectCreated handle(CreateProject cmd, CommandContext ctx) {
        return projectCreatedEvent(cmd.getProjectId());
    }

    @Assign
    public TaskAdded handle(AddTask cmd, CommandContext ctx) {
        return taskAddedEvent(cmd.getProjectId());
    }
}