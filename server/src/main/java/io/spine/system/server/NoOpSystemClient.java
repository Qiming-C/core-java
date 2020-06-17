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

package io.spine.system.server;

import io.spine.base.entity.EntityState;
import io.spine.server.entity.Repository;

import java.util.Optional;

/**
 * An implementation of {@link SystemClient} which never performs an action.
 *
 * <p>When requested for a read/write side of a system context, always obtains a NOP implementation.
 *
 * <p>An instance of {@code NoOpSystemClient} serves as the system client in the system contexts
 * themselves.
 */
public enum NoOpSystemClient implements SystemClient {

    INSTANCE;

    @Override
    public NoOpSystemWriteSide writeSide() {
        return NoOpSystemWriteSide.INSTANCE;
    }

    @Override
    public NoOpSystemReadSide readSide() {
        return NoOpSystemReadSide.INSTANCE;
    }

    @Override
    public Optional<Repository<?, ?>> systemRepoFor(Class<? extends EntityState<?>> stateClass) {
        return Optional.empty();
    }

    @Override
    public void closeSystemContext() {
        // NOP.
    }
}
