/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.system.server;

import io.spine.server.event.EventDispatcher;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The read side of a system bounded context.
 *
 * <p>A domain context accesses its system counterpart data via a {@code SystemReadSide}.
 */
public interface SystemReadSide {

    /**
     * Registers an event dispatcher for system events.
     *
     * <p>Any system event may be passed to such dispatcher.
     *
     * @param dispatcher
     *         a system event dispatcher
     */
    void register(EventDispatcher dispatcher);

    /**
     * Removes the given system event dispatcher.
     *
     * @param dispatcher
     *         a registered system event dispatcher
     * @see #register(EventDispatcher)
     */
    void unregister(EventDispatcher dispatcher);

    /**
     * Creates a new instance of {@code SystemReadSide} for the given system context.
     *
     * @param context
     *         the system context to fetch data from
     * @return a new instance of {@code SystemReadSide}
     */
    static SystemReadSide newInstance(SystemContext context) {
        checkNotNull(context);
        return new DefaultSystemReadSide(context);
    }
}
