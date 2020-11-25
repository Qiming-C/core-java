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

package io.spine.server.aggregate.given.importado;

import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.route.EventRoute;
import io.spine.server.route.EventRouting;

/**
 * A repository for {@link Dot} objects.
 */
public final class DotSpace extends AggregateRepository<ObjectId, Dot, Point> {

    /**
     * Replaces event import routing to take first message field.
     *
     * @implNote Default behaviour defined in {@link AggregateRepository#eventImportRoute}
     *         is to take producer ID from an {@code EventContext}. We redefine this to avoid the
     *         need of creating {@code Event} instances. Real imports would need to create those.
     */
    @Override
    protected void setupImportRouting(EventRouting<ObjectId> routing) {
        super.setupImportRouting(routing);
        routing.replaceDefault(EventRoute.byFirstMessageField(idClass()));
    }
}
