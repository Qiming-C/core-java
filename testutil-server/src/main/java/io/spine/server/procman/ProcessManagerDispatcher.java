/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
package io.spine.server.procman;

import com.google.common.annotations.VisibleForTesting;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.core.RejectionEnvelope;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A test utility for dispatching commands and events to a {@code ProcessManager} in test purposes.
 *
 * @author Alex Tymchenko
 */
@VisibleForTesting
public class ProcessManagerDispatcher {

    /** Prevents this utility class from instantiation. */
    private ProcessManagerDispatcher() {}

    /**
     * Dispatches the {@linkplain CommandEnvelope command} to the given {@code ProcessManager}.
     *
     * @return the list of {@linkplain Event events}, being the command output.
     */
    public static List<Event> dispatch(ProcessManager<?, ?, ?> pm, CommandEnvelope command) {
        checkNotNull(pm);
        checkNotNull(command);

        final PmTransaction<?, ?, ?> tx = PmTransaction.start(pm);
        final List<Event> eventMessages = pm.dispatchCommand(command);
        tx.commit();

        return eventMessages;
    }

    /**
     * Dispatches an {@linkplain EventEnvelope event} to the given {@code ProcessManager}.
     */
    public static void dispatch(ProcessManager<?, ?, ?> pm, EventEnvelope event) {
        checkNotNull(pm);
        checkNotNull(event);

        final PmTransaction<?, ?, ?> tx = PmTransaction.start(pm);
        pm.dispatchEvent(event);
        tx.commit();
    }

    /**
     * Dispatches a {@linkplain RejectionEnvelope rejection} to the given {@code ProcessManager}.
     */
    public static void dispatch(ProcessManager<?, ?, ?> pm, RejectionEnvelope rejection) {
        checkNotNull(pm);
        checkNotNull(rejection);

        final PmTransaction<?, ?, ?> tx = PmTransaction.start(pm);
        pm.dispatchRejection(rejection);
        tx.commit();
    }
}
