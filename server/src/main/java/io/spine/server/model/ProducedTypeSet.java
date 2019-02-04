/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.model;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.common.graph.Traverser;
import com.google.common.reflect.TypeToken;
import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Message;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.base.RejectionMessage;
import io.spine.core.CommandClass;
import io.spine.core.EventClass;
import io.spine.reflect.Types;
import io.spine.type.MessageClass;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * A collection of command or event types produced by a {@link HandlerMethod}.
 *
 * @param <P>
 *         the type of the produced message classes
 */
@SuppressWarnings("UnstableApiUsage") // Guava's Reflection API will most probably be OK.
@Immutable
final class ProducedTypeSet<P extends MessageClass<?>> {

    private final ImmutableSet<P> producedTypes;

    private ProducedTypeSet(ImmutableSet<P> producedTypes) {
        this.producedTypes = producedTypes;
    }

    /**
     * Collects the produced command/event types from the handler method.
     *
     * <p>If the method returns a parameterized type like {@link Iterable}, the produced messages
     * are gathered from its generic arguments.
     *
     * <p>Too broad types are ignored, so methods returning something like
     * {@code Optional<Message>} are deemed producing no types.
     *
     * @param method
     *         the handler method
     * @param <P>
     *         the type of the produced message classes
     * @return a new {@code ProducedTypeSet} instance
     */
    static <P extends MessageClass<?>> ProducedTypeSet<P> collect(Method method) {
        checkNotNull(method);
        Type returnType = method.getGenericReturnType();
        ImmutableSet<P> producedMessages = collectProducedMessages(returnType);
        return new ProducedTypeSet<>(producedMessages);
    }

    /**
     * Retrieves the produced types collection.
     */
    ImmutableSet<P> typeSet() {
        return producedTypes;
    }

    /**
     * Collects all produced commands/events from the type.
     *
     * <p>If the type is parameterized, its arguments are traversed recursively enabling correct
     * parsing of return types like {@code Pair<ProjectCreated, Optional<ProjectStarted>>}.
     */
    private static <P extends MessageClass<?>> ImmutableSet<P> collectProducedMessages(Type type) {
        Iterable<Type> allTypes = Traverser.forTree(Types::resolveArguments)
                                           .breadthFirst(type);
        ImmutableSet<P> result =
                Streams.stream(allTypes)
                       .map(TypeToken::of)
                       .map(TypeToken::getRawType)
                       .filter(new IsCommandOrEvent())
                       .map(new ToMessageClass<P>())
                       .collect(toImmutableSet());
        return result;
    }

    /**
     * Checks if the class is a concrete {@linkplain CommandMessage command} or
     * {@linkplain EventMessage event}.
     */
    private static class IsCommandOrEvent implements Predicate<Class<?>> {

        /**
         * Ignored command/event types include both too broad types like {@link EventMessage} and
         * special-case types of commands and events which should not be exposed like
         * {@link Nothing}.
         */
        private static final ImmutableSet<Class<? extends Message>> IGNORED_TYPES =
                ImmutableSet.of(
                        CommandMessage.class,
                        EventMessage.class,
                        RejectionMessage.class,
                        Nothing.class
                );

        @Override
        public boolean test(Class<?> aClass) {
            boolean isCommandOrEvent =
                    CommandMessage.class.isAssignableFrom(aClass)
                            || EventMessage.class.isAssignableFrom(aClass);
            boolean isIgnoredType = IGNORED_TYPES.contains(aClass);
            return isCommandOrEvent && !isIgnoredType;
        }
    }

    /**
     * Converts the command or event class to the corresponding {@link MessageClass}.
     *
     * @param <P>
     *         the type of the message class produced
     *
     * @apiNote
     * The class, in fact, knows to which class to convert on compilation stage already, as all
     * handler methods in the model produce either commands OR events. The class is thus
     * parameterized with a produced message class, and the runtime checks are only used for
     * convenience.
     */
    private static class ToMessageClass<P extends MessageClass<?>>
            implements Function<Class<?>, P> {

        @SuppressWarnings("unchecked") // See class doc.
        @Override
        public P apply(Class<?> aClass) {
            if (CommandMessage.class.isAssignableFrom(aClass)) {
                return (P) CommandClass.from((Class<? extends CommandMessage>) aClass);
            }
            if (EventMessage.class.isAssignableFrom(aClass)) {
                return (P) EventClass.from((Class<? extends EventMessage>) aClass);
            }
            throw newIllegalArgumentException("A given type %s is neither command nor event",
                                              aClass.getCanonicalName());
        }
    }
}
