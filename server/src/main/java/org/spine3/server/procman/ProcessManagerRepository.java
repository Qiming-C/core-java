/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.procman;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.Internal;
import org.spine3.base.CommandContext;
import org.spine3.base.EventContext;
import org.spine3.base.EventRecord;
import org.spine3.server.*;
import org.spine3.server.internal.CommandHandlerMethod;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/**
 * The abstract base for repositories for Process Managers.
 *
 * @param <I> the type of IDs of process managers
 * @param <PM> the type of process managers
 * @param <M> the type of process manager state messages
 * @see ProcessManager
 * @author Alexander Litus
 */
public abstract class ProcessManagerRepository<I, PM extends ProcessManager<I, M>, M extends Message>
        extends EntityRepository<I, PM, M> implements CommandDispatcher, EventDispatcher, MultiHandler, CommandHandler {

    /**
     * {@inheritDoc}
     */
    protected ProcessManagerRepository(BoundedContext boundedContext) {
        super(boundedContext);
    }

    /**
     * Intended to return a process manager ID based on the command and command context.
     *
     * <p>The default implementation uses {@link #getId(Message)} method and does not use the {@code context}.
     * Override any of these methods if you need.
     *
     * @param command command which the process manager handles
     * @param context context of the command
     * @return a process manager ID
     * @see #getId(Message)
     */
    @SuppressWarnings("UnusedParameters") // Overriding implementations may use the `context` parameter.
    protected I getId(Message command, CommandContext context) {
        return getId(command);
    }

    /**
     * Intended to return a process manager ID based on the event and event context.
     *
     * <p>The default implementation uses {@link #getId(Message)} method and does not use the {@code context}.
     * Override any of these methods if you need.
     *
     * @param event event which the process manager handles
     * @param context context of the event
     * @return a process manager ID
     */
    @SuppressWarnings("UnusedParameters") // Overriding implementations may use the `context` parameter.
    protected I getId(Message event, EventContext context) {
        return getId(event);
    }

    /**
     * Returns a process manager ID based on the command/event message.
     *
     * @param message a command/event which the process manager handles
     * @return a process manager ID
     * @see ProcessManagerId#from(Message)
     */
    protected I getId(Message message) {
        // We cast to this type because assume that all commands/events for the manager refer to IDs of the same type <I>.
        // If this assumption fails, we would get ClassCastException.
        @SuppressWarnings("unchecked")
        final I result = (I) ProcessManagerId.from(message).value();
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @return a multimap from command/event handlers to command/event classes they handle (in the Process Manager class).
     */
    @Override
    public Multimap<Method, Class<? extends Message>> getHandlers() {
        final Multimap<Method, Class<? extends Message>> commandHandlers = getCommandHandlers();
        final Multimap<Method, Class<? extends Message>> eventHandlers = getEventHandlers();
        return ImmutableMultimap.<Method, Class<? extends Message>>builder()
                .putAll(commandHandlers)
                .putAll(eventHandlers)
                .build();
    }

    /**
     * Returns a map from methods to command classes they handle.
     *
     * @see ProcessManager#getHandledCommandClasses(Class)
     */
    private Multimap<Method, Class<? extends Message>> getCommandHandlers() {
        final Class<? extends ProcessManager> pmClass = getEntityClass();
        final Set<Class<? extends Message>> commandClasses = ProcessManager.getHandledCommandClasses(pmClass);
        final Method dispatcher = CommandDispatcher.DispatchMethod.of(this);
        return ImmutableMultimap.<Method, Class<? extends Message>>builder()
                .putAll(dispatcher, commandClasses)
                .build();
    }

    /**
     * Returns a map from methods to event classes they handle.
     *
     * @see ProcessManager#getHandledEventClasses(Class)
     */
    private Multimap<Method, Class<? extends Message>> getEventHandlers() {
        final Class<? extends ProcessManager> pmClass = getEntityClass();
        final Set<Class<? extends Message>> eventClasses = ProcessManager.getHandledEventClasses(pmClass);
        final Method dispatcher = EventDispatcher.DispatchMethod.of(this);
        return ImmutableMultimap.<Method, Class<? extends Message>>builder()
                .putAll(dispatcher, eventClasses)
                .build();
    }

    @Internal
    @Override
    public CommandHandlerMethod createMethod(Method method) {
        return new PmRepositoryDispatchMethod(this, method);
    }

    @Internal
    @Override
    public Predicate<Method> getHandlerMethodPredicate() {
        return PmCommandHandler.IS_PM_COMMAND_HANDLER;
    }

    /**
     * Dispatches the command to a corresponding process manager.
     *
     * <p>If there is no stored process manager with such an ID, a new process manager is created
     * and stored after it handles the passed command.
     *
     * @param command the command to dispatch
     * @param context the context of the command
     * @see ProcessManager#dispatchCommand(Message, CommandContext)
     * @see #getId(Message, CommandContext)
     */
    @Override
    public List<EventRecord> dispatch(Message command, CommandContext context)
            throws FailureThrowable, InvocationTargetException {
        final I id = getId(command, context);
        final PM manager = load(id);
        final List<EventRecord> events = manager.dispatchCommand(command, context);
        store(manager);
        return events;
    }

    /**
     * Dispatches the event to a corresponding process manager.
     *
     * <p>If there is no stored process manager with such an ID, a new process manager is created
     * and stored after it handles the passed event.
     *
     * @param event the event to dispatch
     * @param context the context of the event
     * @see ProcessManager#dispatchEvent(Message, EventContext)
     * @see #getId(Message, EventContext)
     */
    @Override
    public void dispatch(Message event, EventContext context) {
        final I id = getId(event, context);
        final PM manager = load(id);
        try {
            manager.dispatchEvent(event, context);
            store(manager);
        } catch (InvocationTargetException e) {
            log().error("Error during dispatching event", e);
        }
    }

    /**
     * Loads or creates a process manager by the passed ID.
     *
     * <p>The process manager is created if there was no manager with such an ID stored before.
     *
     * @param id the ID of the process manager to load
     * @return loaded or created process manager instance
     */
    @Nonnull
    @Override
    public PM load(I id) {
        PM result = super.load(id);
        if (result == null) {
            result = create(id);
        }
        return result;
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(ProcessManagerRepository.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

}