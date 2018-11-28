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

package io.spine.server.entity;

import io.spine.annotation.Internal;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.core.Version;
import io.spine.core.Versions;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The strategy of versioning of {@linkplain Entity entities} during a certain {@link Transaction}.
 */
@Internal
public enum EntityVersioning {

    /**
     * This strategy is applied to the {@link Entity} types which represent a sequence of
     * events.
     *
     * <p>One example of such entity is {@link io.spine.server.aggregate.Aggregate Aggregate}.
     * As a sequence of events, an {@code Aggregate} has no own versioning system, thus
     * inherits the versions of the {@linkplain io.spine.server.aggregate.Apply applied}
     * events. In other words, the current version of an {@code Aggregate} is
     * the {@linkplain EventContext#getVersion() version} of last applied event.
     */
    FROM_EVENT {
        @Override
        Version nextVersion(EntityVersioningContext context) {
            EventEnvelope event = context.event();
            checkNotNull(event, "Event must be set when using FROM_EVENT versioning strategy");
            Version fromEvent = event.getEventContext()
                                     .getVersion();
            return fromEvent;
        }
    },

    /**
     * This strategy is applied to the {@link Entity} types which cannot use the event versions,
     * such as {@link io.spine.server.projection.Projection Projection}s.
     *
     * <p>A {@code Projection} represents an arbitrary cast of data in a specific moment in
     * time. The events applied to a {@code Projection} are produced by different {@code Entities}
     * and have no common versioning. Thus, a {@code Projection} has its own versioning system.
     * Each event <i>increments</i> the {@code Projection} version by one.
     */
    AUTO_INCREMENT {
        @Override
        Version nextVersion(EntityVersioningContext context) {
            Version current = context.transaction()
                                     .getVersion();
            Version newVersion = Versions.increment(current);
            return newVersion;
        }
    };

    /**
     * Creates a new {@link Entity} version based on the given versioning context.
     *
     * <p>This method has no side effects, i.e. doesn't set the version to the transaction etc.
     *
     * @param context
     *         the versioning context with the information about current transaction
     * @return the advanced version
     */
    abstract Version nextVersion(EntityVersioningContext context);
}
