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

package io.spine.testing.server.entity;

import com.google.common.truth.Subject;
import io.spine.server.entity.Entity;
import io.spine.testing.server.SubjectTest;
import io.spine.testing.server.blackbox.BbProjectId;
import io.spine.testing.server.blackbox.BbProjectView;
import io.spine.testing.server.entity.given.Given;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.base.Identifier.newUuid;
import static io.spine.testing.server.entity.EntitySubject.entities;

@DisplayName("EntitySubject")
class EntitySubjectTest extends SubjectTest<EntitySubject, Entity<?, ?>> {

    private Entity<?, ?> entity;

    @Override
    protected Subject.Factory<EntitySubject, Entity<?, ?>> subjectFactory() {
        return entities();
    }

    @BeforeEach
    void setUp() {
        BbProjectId id = BbProjectId
                .vBuilder()
                .setId(newUuid())
                .build();
        BbProjectView state = BbProjectView
                .vBuilder()
                .setId(id)
                .build();
        entity = Given.projectionOfClass(EntitySubjectTestEnv.ProjectView.class)
                      .withId(id)
                      .withState(state)
                      .build();
    }

    @Test
    @DisplayName("check if the entity is archived")
    void checkArchived() {
        EntitySubject assertEntity = EntitySubject.assertEntity(entity);
        assertEntity.archivedFlag()
                    .isFalse();
        expectSomeFailure(whenTesting -> whenTesting.that(entity).archivedFlag().isTrue());
    }
}
