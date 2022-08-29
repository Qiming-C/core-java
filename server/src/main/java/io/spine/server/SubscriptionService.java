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
package io.spine.server;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.Ordering;
import io.grpc.stub.StreamObserver;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionUpdate;
import io.spine.client.Subscriptions;
import io.spine.client.Target;
import io.spine.client.ThreadSafeObserver;
import io.spine.client.Topic;
import io.spine.client.grpc.SubscriptionServiceGrpc;
import io.spine.core.Response;
import io.spine.logging.Logging;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.union;
import static com.google.common.flogger.LazyArgs.lazy;
import static io.spine.grpc.StreamObservers.forwardErrorsOnly;
import static io.spine.server.stand.SubscriptionCallback.forwardingTo;

/**
 * The {@code SubscriptionService} provides an asynchronous way to fetch read-side state
 * from the server.
 *
 * <p>For synchronous read-side updates please see {@link QueryService}.
 */
public final class SubscriptionService
        extends SubscriptionServiceGrpc.SubscriptionServiceImplBase
        implements Logging {

    private static final Joiner LIST_JOINER = Joiner.on(", ");

    private final TypeDictionary types;

    private SubscriptionService(TypeDictionary types) {
        super();
        this.types = types;
    }

    /**
     * Creates a new builder for the service.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Builds the service with a single Bounded Context.
     */
    public static SubscriptionService withSingle(BoundedContext context) {
        checkNotNull(context);
        var result = newBuilder().add(context).build();
        return result;
    }

    @Override
    public void subscribe(Topic topic, StreamObserver<Subscription> responseObserver) {
        _debug().log("Creating the subscription to the topic: `%s`.", topic);
        try {
            StreamObserver<Subscription> safeObserver = new ThreadSafeObserver<>(responseObserver);
            subscribeTo(topic, safeObserver);
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
            _error().withCause(e)
                    .log("Error processing subscription request.");
            responseObserver.onError(e);
        }
    }

    private void subscribeTo(Topic topic, StreamObserver<Subscription> responseObserver) {
        var target = topic.getTarget();
        var foundContext = findContextOf(target);
        if (foundContext.isPresent()) {
            var stand = foundContext.get().stand();
            stand.subscribe(topic, responseObserver);
        } else {
            List<BoundedContext> contexts = new ArrayList<>(contexts());
            contexts.sort(Ordering.natural());
            _warn().log("Unable to find a Bounded Context for type `%s`." +
                                " Creating a subscription in contexts: %s.",
                        topic.getTarget().type(),
                        LIST_JOINER.join(contexts));
            var subscription = Subscriptions.from(topic);
            for (var context : contexts) {
                var stand = context.stand();
                stand.subscribe(subscription);
            }
            responseObserver.onNext(subscription);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void activate(Subscription subscription, StreamObserver<SubscriptionUpdate> observer) {
        _debug().log("Activating the subscription: `%s`.", subscription);
        StreamObserver<SubscriptionUpdate> safeObserver = new ThreadSafeObserver<>(observer);
        try {
            var callback = forwardingTo(safeObserver);
            StreamObserver<Response> responseObserver = forwardErrorsOnly(safeObserver);
            var foundContext = findContextOf(subscription);
            if (foundContext.isPresent()) {
                var targetStand = foundContext.get().stand();
                targetStand.activate(subscription, callback, responseObserver);
            } else {
                for (var context : contexts()) {
                    var stand = context.stand();
                    stand.activate(subscription, callback, responseObserver);
                }
            }
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
            _error().withCause(e)
                    .log("Error activating the subscription.");
            safeObserver.onError(e);
        }
    }

    private ImmutableCollection<BoundedContext> contexts() {
        return types.contexts();
    }

    @Override
    public void cancel(Subscription subscription, StreamObserver<Response> responseObserver) {
        _debug().log("Incoming cancel request for the subscription topic: `%s`.", subscription);
        StreamObserver<Response> safeObserver = new ThreadSafeObserver<>(responseObserver);
        var selected = findContextOf(subscription);
        if (selected.isEmpty()) {
            _warn().log("Trying to cancel a subscription `%s` which could not be found.",
                        lazy(subscription::toShortString));
            safeObserver.onCompleted();
            return;
        }
        try {
            var context = selected.get();
            var stand = context.stand();
            stand.cancel(subscription, safeObserver);
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
            _error().withCause(e)
                    .log("Error processing cancel subscription request.");
            safeObserver.onError(e);
        }
    }

    private Optional<BoundedContext> findContextOf(Subscription subscription) {
        var target = subscription.getTopic().getTarget();
        var result = findContextOf(target);
        return result;
    }

    /**
     * Searches for the Bounded Context which provides the messages of the target type.
     *
     * @param target
     *         the type which may be available through this subscription service
     * @return the context which exposes the target type,
     *         or {@code Optional.empty} if no known context does so
     */
    @VisibleForTesting  /* Otherwise should have been `private`. */
    Optional<BoundedContext> findContextOf(Target target) {
        var type = target.type();
        var result = types.find(type);
        return result;
    }

    /**
     * The builder for the {@link SubscriptionService}.
     */
    public static class Builder extends AbstractServiceBuilder<SubscriptionService, Builder> {

        /**
         * Builds the {@link SubscriptionService}.
         *
         * @throws IllegalStateException if no Bounded Contexts were added.
         */
        @Override
        public SubscriptionService build() throws IllegalStateException {
            checkNotEmpty();
            var dictionary = TypeDictionary.newBuilder();
            contexts().forEach(
                    context -> dictionary.putAll(context, (c) ->
                            union(c.stand().exposedTypes(), c.stand().exposedEventTypes())
                    )
            );
            var result = new SubscriptionService(dictionary.build());
            return result;
        }

        @Override
        Builder self() {
            return this;
        }
    }
}
