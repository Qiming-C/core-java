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
package io.spine.testing.server.procman;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.core.RejectionEnvelope;
import io.spine.server.procman.PmCommandEndpoint;
import io.spine.server.procman.PmEventEndpoint;
import io.spine.server.procman.PmRejectionEndpoint;
import io.spine.server.procman.ProcessManager;
import io.spine.server.procman.ProcessManagerRepository;
import io.spine.testing.server.NoOpLifecycle;
import io.spine.type.TypeUrl;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A test utility for dispatching commands and events to a {@code ProcessManager} in test purposes.
 *
 * @author Alex Tymchenko
 */
@VisibleForTesting
public class ProcessManagerDispatcher {

    /** Prevents this utility class from instantiation. */
    private ProcessManagerDispatcher() {
    }

    /**
     * Dispatches the {@linkplain CommandEnvelope command} to the given {@code ProcessManager}.
     *
     * @return the list of {@linkplain Event events}, being the command output.
     */
    public static List<Event> dispatch(ProcessManager<?, ?, ?> pm, CommandEnvelope command) {
        checkNotNull(pm);
        checkNotNull(command);
        List<Event> eventMessages = TestPmCommandEndpoint.dispatch(pm, command);
        return eventMessages;
    }

    /**
     * Dispatches an {@linkplain EventEnvelope event} to the given {@code ProcessManager}.
     *
     * @return the list of event messages generated by the given {@code ProcessManager}, or empty
     * list if none were generated.
     */
    public static List<Event> dispatch(ProcessManager<?, ?, ?> pm,
                                       EventEnvelope event) {
        checkNotNull(pm);
        checkNotNull(event);
        List<Event> eventMessages = TestPmEventEndpoint.dispatch(pm, event);
        return eventMessages;
    }

    /**
     * Dispatches a {@linkplain RejectionEnvelope rejection} to the given {@code ProcessManager}.
     */
    @SuppressWarnings("CheckReturnValue") // OK to ignore events in this test utility.
    public static void dispatch(ProcessManager<?, ?, ?> pm, RejectionEnvelope rejection) {
        checkNotNull(pm);
        checkNotNull(rejection);
        TestPmRejectionEndpoint.dispatch(pm, rejection);
    }

    /**
     * A test-only implementation of an {@link PmCommandEndpoint}, that dispatches
     * commands to an instance of {@code ProcessManager} and returns the list of events.
     *
     * @param <I> the type of {@code ProcessManager} identifier
     * @param <P> the type of {@code ProcessManager}
     * @param <S> the type of {@code ProcessManager} state object
     */
    private static class TestPmCommandEndpoint<I,
                                               P extends ProcessManager<I, S, ?>,
                                               S extends Message>
            extends PmCommandEndpoint<I, P> {

        private TestPmCommandEndpoint(CommandEnvelope envelope) {
            super(mockRepository(), envelope);
        }

        private static <I, P extends ProcessManager<I, S, ?>, S extends Message>
        List<Event> dispatch(P manager, CommandEnvelope envelope) {
            TestPmCommandEndpoint<I, P, S> endpoint = new TestPmCommandEndpoint<>(envelope);
            List<Event> events = endpoint.dispatchInTx(manager);
            return events;
        }
    }

    /**
     * A test-only implementation of an {@link PmEventEndpoint}, that dispatches
     * events to an instance of {@code ProcessManager} and returns the list of events.
     *
     * @param <I> the type of {@code ProcessManager} identifier
     * @param <P> the type of {@code ProcessManager}
     * @param <S> the type of {@code ProcessManager} state object
     */
    private static class TestPmEventEndpoint<I,
                                             P extends ProcessManager<I, S, ?>,
                                             S extends Message>
            extends PmEventEndpoint<I, P> {

        private TestPmEventEndpoint(EventEnvelope envelope) {
            super(mockRepository(), envelope);
        }

        private static <I, P extends ProcessManager<I, S, ?>, S extends Message>
        List<Event> dispatch(P manager, EventEnvelope envelope) {
            TestPmEventEndpoint<I, P, S> endpoint = new TestPmEventEndpoint<>(envelope);
            List<Event> events = endpoint.dispatchInTx(manager);
            return events;
        }
    }

    /**
     * A test-only implementation of an {@link PmRejectionEndpoint}, that dispatches
     * rejection to an instance of {@code ProcessManager} and returns the list of events.
     *
     * @param <I> the type of {@code ProcessManager} identifier
     * @param <P> the type of {@code ProcessManager}
     * @param <S> the type of {@code ProcessManager} state object
     */
    private static class TestPmRejectionEndpoint<I,
                                                 P extends ProcessManager<I, S, ?>,
                                                 S extends Message>
            extends PmRejectionEndpoint<I, P> {

        private TestPmRejectionEndpoint(RejectionEnvelope envelope) {
            super(mockRepository(), envelope);
        }

        private static <I, P extends ProcessManager<I, S, ?>, S extends Message>
        List<Event> dispatch(P manager, RejectionEnvelope envelope) {
            TestPmRejectionEndpoint<I, P, S> endpoint = new TestPmRejectionEndpoint<>(envelope);
            List<Event> messages = endpoint.dispatchInTx(manager);
            return messages;
        }
    }

    @SuppressWarnings("unchecked") // It is OK when mocking
    private static <I, P extends ProcessManager<I, S, ?>, S extends Message>
    ProcessManagerRepository<I, P, S> mockRepository() {
        TestPmRepository mockedRepo = mock(TestPmRepository.class);
        when(mockedRepo.lifecycleOf(any())).thenCallRealMethod();
        when(mockedRepo.getEntityStateType()).thenReturn(TypeUrl.of(Any.class));
        return mockedRepo;
    }

    /**
     * Test-only process manager repository that exposes {@code Repository.Lifecycle} class.
     */
    private static class TestPmRepository<I, P extends ProcessManager<I, S, ?>, S extends Message>
            extends ProcessManagerRepository<I, P, S> {

        @Override
        protected Lifecycle lifecycleOf(I id) {
            return NoOpLifecycle.INSTANCE;
        }
    }
}
