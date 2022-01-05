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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.grpc.stub.StreamObserver;
import io.spine.client.grpc.CommandServiceGrpc;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.logging.Logging;
import io.spine.server.bus.MessageIdExtensions;
import io.spine.server.commandbus.UnsupportedCommandException;
import io.spine.server.type.CommandClass;

import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The {@code CommandService} allows client applications to post commands and
 * receive updates from the application backend.
 */
public final class CommandService
        extends CommandServiceGrpc.CommandServiceImplBase
        implements Logging {

    private final ImmutableMap<CommandClass, BoundedContext> commandToContext;

    /**
     * Constructs new instance using the map from a {@code CommandClass} to
     * a {@code BoundedContext} instance which handles the command.
     */
    private CommandService(Map<CommandClass, BoundedContext> map) {
        super();
        this.commandToContext = ImmutableMap.copyOf(map);
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
    public static CommandService withSingle(BoundedContext context) {
        checkNotNull(context);
        var result = newBuilder().add(context).build();
        return result;
    }

    @Override
    public void post(Command request, StreamObserver<Ack> responseObserver) {
        var commandClass = CommandClass.of(request);
        var context = commandToContext.get(commandClass);
        if (context == null) {
            handleUnsupported(request, responseObserver);
        } else {
            var commandBus = context.commandBus();
            commandBus.post(request, responseObserver);
        }
    }

    private void handleUnsupported(Command command, StreamObserver<Ack> responseObserver) {
        var unsupported = new UnsupportedCommandException(command);
        _error().withCause(unsupported)
                .log("Unsupported command posted to `CommandService`.");
        var error = unsupported.asError();
        var id = command.getId();
        var response = MessageIdExtensions.causedError(id, error);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * The builder for a {@code CommandService}.
     */
    public static class Builder {

        private final Set<BoundedContext> contexts = Sets.newHashSet();

        /**
         * Adds the {@code BoundedContext} to the builder.
         */
        @CanIgnoreReturnValue
        public Builder add(BoundedContext context) {
            // Saves it to a temporary set so that it is easy to remove it if needed.
            contexts.add(context);
            return this;
        }

        /**
         * Removes the {@code BoundedContext} from the builder.
         */
        @CanIgnoreReturnValue
        public Builder remove(BoundedContext context) {
            contexts.remove(context);
            return this;
        }

        /**
         * Verifies if the passed {@code BoundedContext} was previously added to the builder.
         *
         * @param context the instance to check
         * @return {@code true} if the instance was added to the builder, {@code false} otherwise
         */
        public boolean contains(BoundedContext context) {
            var contains = contexts.contains(context);
            return contains;
        }

        /**
         * Builds a new {@link CommandService}.
         */
        public CommandService build() {
            var map = createMap();
            var result = new CommandService(map);
            return result;
        }

        /**
         * Creates a map from {@code CommandClass}es to {@code BoundedContext}s that
         * handle such commands.
         */
        private ImmutableMap<CommandClass, BoundedContext> createMap() {
            ImmutableMap.Builder<CommandClass, BoundedContext> builder = ImmutableMap.builder();
            for (var boundedContext : contexts) {
                putIntoMap(boundedContext, builder);
            }
            return builder.build();
        }

        /**
         * Associates {@code CommandClass}es with the instance of {@code BoundedContext}
         * that handles such commands.
         */
        private static void putIntoMap(BoundedContext context,
                                       ImmutableMap.Builder<CommandClass, BoundedContext> builder) {
            var commandBus = context.commandBus();
            var cmdClasses = commandBus.registeredCommandClasses();
            for (var commandClass : cmdClasses) {
                builder.put(commandClass, context);
            }
        }
    }
}
