/*
 * Copyright 2020, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.spine.core.Signal;
import io.spine.core.SignalId;
import io.spine.server.dispatch.DispatchOutcome;
import io.spine.server.event.EventDispatch;

/**
 * A phase that dispatches an event to the entity in transaction.
 *
 * @param <I>
 *         the type of entity ID
 * @param <E>
 *         the type of the entity
 */
@Internal
public final class EventDispatchingPhase<I, E extends TransactionalEntity<I, ?, ?>>
        extends Phase<I> {

    private final EventDispatch<I, E> dispatch;

    public EventDispatchingPhase(Transaction<I, ?, ?, ?> transaction,
                                 EventDispatch<I, E> dispatch,
                                 VersionIncrement increment) {
        super(transaction, increment);
        this.dispatch = dispatch;
    }

    @Override
    protected DispatchOutcome performDispatch() {
        return dispatch.perform();
    }

    @Override
    public I entityId() {
        return dispatch.entity()
                       .id();
    }

    @Override
    public SignalId messageId() {
        return dispatch.event()
                       .id();
    }

    @Override
    protected Signal<?, ?, ?> signal() {
        return dispatch.event()
                       .outerObject();
    }
}
