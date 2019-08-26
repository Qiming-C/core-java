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
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.reflect.Types;
import io.spine.server.type.CommandClass;
import io.spine.server.type.EventClass;
import io.spine.type.MessageClass;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * A collection of command or event types produced by a {@link HandlerMethod}.
 *
 * <p>The set contains <em>class</em>information collected from method signatures.
 * If a method result refers to an interface (directly or as a generic parameter), this
 * interface is not added to the set. Thus, for example, if a method,
 * for example, returns {@code List<EventMessage>}, the set would be empty.
 *
 * @param <P>
 *         the type of the produced message classes
 */
@Immutable
final class ProducedTypeSet<P extends MessageClass<?>> {

    private final ImmutableSet<P> producedTypes;

    /**
     * Checks if the class is a concrete {@linkplain CommandMessage command} or
     * {@linkplain EventMessage event}.
     */
    private static final Predicate<Class<?>> IS_COMMAND_OR_EVENT = cls -> {
        if (cls.isInterface()) {
            return false;
        }
        if (Nothing.class.equals(cls)) {
            return false;
        }
        boolean isCommandOrEvent =
                CommandMessage.class.isAssignableFrom(cls)
                        || EventMessage.class.isAssignableFrom(cls);
        return isCommandOrEvent;
    };

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

    private ProducedTypeSet(ImmutableSet<P> producedTypes) {
        this.producedTypes = producedTypes;
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
     *
     * @apiNote
     * The class, in fact, knows to which class to convert on compilation stage already, as all
     * handler methods in the model produce either commands OR events. The class is thus
     * parameterized with a produced message class, and the runtime checks are only used for
     * convenience.
     */
    @SuppressWarnings("unchecked")
    private static <P extends MessageClass<?>> ImmutableSet<P> collectProducedMessages(Type type) {
        Iterable<Type> allTypes = Traverser.forTree(Types::resolveArguments)
                                           .breadthFirst(type);
        ImmutableSet<P> result =
                Streams.stream(allTypes)
                       .map(TypeToken::of)
                       .map(TypeToken::getRawType)
                       .filter(IS_COMMAND_OR_EVENT)
                       .map(c -> (P) toMessageClass(c))
                       .collect(toImmutableSet());
        return result;
    }

    /**
     * Converts the command or event class to the corresponding {@link MessageClass}.
     */
    @SuppressWarnings("unchecked") // checked by `isAssignableFrom()`
    private static MessageClass<?> toMessageClass(Class<?> cls) {
        if (CommandMessage.class.isAssignableFrom(cls)) {
            return CommandClass.from((Class<? extends CommandMessage>) cls);
        }
        if (EventMessage.class.isAssignableFrom(cls)) {
            return EventClass.from((Class<? extends EventMessage>) cls);
        }
        throw newIllegalArgumentException(
                "A given type `%s` is neither a command nor an event.",
                cls.getCanonicalName()
        );
    }
}
