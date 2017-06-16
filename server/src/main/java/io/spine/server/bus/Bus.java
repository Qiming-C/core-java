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

package io.spine.server.bus;

import com.google.common.base.Converter;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.envelope.MessageEnvelope;
import io.spine.type.MessageClass;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Iterables.transform;
import static io.spine.validate.Validate.isNotDefault;
import static java.util.Collections.singleton;

/**
 * Abstract base for buses.
 *
 * @param <T> the type of outer objects (containing messages of interest) that are posted the bus
 * @param <E> the type of envelopes for outer objects used by this bus
 * @param <C> the type of message class
 * @param <D> the type of dispatches used by this bus
 * @author Alex Tymchenko
 * @author Alexander Yevsyukov
 */
public abstract class Bus<T extends Message,
                          E extends MessageEnvelope<?, T>,
                          C extends MessageClass,
                          D extends MessageDispatcher<C, E>> implements AutoCloseable {

    private final Converter<T, E> messageConverter = new MessageToEnvelope();

    @Nullable
    private DispatcherRegistry<C, D> registry;

    /**
     * Registers the passed dispatcher.
     *
     * @param dispatcher the dispatcher to register
     * @throws IllegalArgumentException if the set of message classes
     *                                  {@linkplain MessageDispatcher#getMessageClasses() exposed}
     *                                  by the dispatcher is empty
     */
    public void register(D dispatcher) {
        registry().register(checkNotNull(dispatcher));
    }

    /**
     * Unregisters dispatching for message classes of the passed dispatcher.
     *
     * @param dispatcher the dispatcher to unregister
     */
    public void unregister(D dispatcher) {
        registry().unregister(checkNotNull(dispatcher));
    }

    /**
     * Posts the message to the bus.
     *
     * <p>Use the {@code Bus} class abstract methods to modify the behavior of posting.
     *
     * <p>This method defines the general posting flow and should not be overridden.
     *
     * @param message          the message to post
     * @param acknowledgement the observer to receive outcome of the operation
     */
    public final void post(T message, StreamObserver<T> acknowledgement) {
        checkNotNull(message);
        checkNotNull(acknowledgement);
        checkArgument(isNotDefault(message));

        post(singleton(message), acknowledgement);
    }

    /**
     * Posts the given messages to the bus.
     *
     * <p>Use the {@code Bus} class abstract methods to modify the behavior of posting.
     *
     * @param messages         the message to post
     * @param acknowledgement the observer to receive outcome of the operation
     */
    public final void post(Iterable<T> messages, StreamObserver<T> acknowledgement) {
        checkNotNull(messages);
        checkNotNull(acknowledgement);

        final Iterable<T> filteredMessages = filter(messages, acknowledgement);
        if (!isEmpty(filteredMessages)) {
            store(messages);
            final Iterable<E> envelopes = transform(filteredMessages, toEnvelope());
            doPost(envelopes, acknowledgement);
        }
    }

    /**
     * Handles the message, for which there is no dispatchers registered in the registry.
     *
     * @param message the message that has no target dispatchers, packed into an envelope
     */
    public abstract void handleDeadMessage(E message);

    /**
     * Obtains the dispatcher registry.
     */
    protected DispatcherRegistry<C, D> registry() {
        if (registry == null) {
            registry = createRegistry();
        }
        return registry;
    }

    /**
     * Factory method for creating an instance of the registry for
     * dispatchers of the bus.
     */
    protected abstract DispatcherRegistry<C, D> createRegistry();

    /**
     * Filters the given messages.
     *
     * <p>The implementations may apply some specific validation to the given messages.
     *
     * <p>If the message does not pass the filter,
     * {@link StreamObserver#onError(Throwable) StreamObserver#onError} may be called.
     *
     * @param messages         the message to filter
     * @param acknowledgement the observer to receive the negative outcome of the operation
     * @return the message itself if it passes the filtering or
     *         {@link Optional#absent() Optional.absent()} otherwise
     */
    protected abstract Iterable<T> filter(Iterable<T> messages, StreamObserver<T> acknowledgement);

    /**
     * Packs the given message of type {@code T} into an envelope of type {@code E}.
     *
     * @param message the message to pack
     * @return new envelope with the given message inside
     */
    protected abstract E toEnvelope(T message);

    /**
     * Posts the given envelope to the bus.
     *
     * <p>Finds and invokes the {@linkplain MessageDispatcher MessageDispatcher(s)} for the given
     * message.
     *
     * <p>This method assumes that the given message has passed the filtering.
     *
     * @see #post(Message, StreamObserver) for the public API
     */
    protected abstract void doPost(E envelope, StreamObserver<?> failureObserver);

    /**
     * Posts each of the given envelopes into the bus and acknowledges the message posting with
     * the {@code acknowledgement} observer.
     *
     * @param envelopes        the envelopes to post
     * @param acknowledgement the observer of the message posting
     */
    private void doPost(Iterable<E> envelopes, StreamObserver<T> acknowledgement) {
        final CountingStreamSupervisor<T> ackingSupervisor =
                new CountingStreamSupervisor<>(acknowledgement);
        int currentErrorCount = ackingSupervisor.getErrorCount();
        for (E message : envelopes) {
            doPost(message, ackingSupervisor);
            if (currentErrorCount == ackingSupervisor.getErrorCount()) {
                ackingSupervisor.onNext(message.getOuterObject());
            } else {
                currentErrorCount = ackingSupervisor.getErrorCount();
            }
        }
        if (ackingSupervisor.getErrorCount() == 0) {
            ackingSupervisor.onCompleted();
        }
    }

    /**
     * Stores the given messages into the underlying storage.
     *
     * @param messages the messages to store
     */
    protected abstract void store(Iterable<T> messages);

    /**
     * @return a {@link Function} converting the messages into the envelopes of the specified
     *         type
     */
    protected final Function<T, E> toEnvelope() {
        return messageConverter;
    }

    /**
     * @return a {@link Function} converting the envelopes into the messages of the specified
     *         type
     */
    protected final Function<E, T> toMessage() {
        return messageConverter.reverse();
    }

    /**
     * A function creating the instances of {@link MessageEnvelope} from the given message.
     */
    private class MessageToEnvelope extends Converter<T, E> {

        @Override
        public E doForward(T message) {
            final E result = toEnvelope(message);
            return result;
        }

        @Override
        protected T doBackward(E envelope) {
            final T result = envelope.getOuterObject();
            return result;
        }
    }

    private abstract static class StreamSupervisor<T> implements StreamObserver<T> {

        private final StreamObserver<T> delegate;

        StreamSupervisor(StreamObserver<T> delegate) {
            this.delegate = delegate;
        }

        protected abstract void onErrorSpotted(Throwable error);

        @Override
        public void onNext(T value) {
            delegate.onNext(value);
        }

        @Override
        public void onCompleted() {
            delegate.onCompleted();
        }

        @Override
        public void onError(Throwable error) {
            onErrorSpotted(error);
            delegate.onError(error);
        }
    }

    private static class CountingStreamSupervisor<T> extends StreamSupervisor<T> {

        private int errorCount;

        CountingStreamSupervisor(StreamObserver<T> delegate) {
            super(delegate);
            this.errorCount = 0;
        }

        @Override
        protected void onErrorSpotted(Throwable error) {
            errorCount++;
        }

        public int getErrorCount() {
            return errorCount;
        }
    }
}
