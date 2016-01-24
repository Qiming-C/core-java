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

package org.spine3.server;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.protobuf.Message;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.CommandContext;
import org.spine3.base.EventRecord;
import org.spine3.base.Responses;
import org.spine3.server.internal.CommandHandlerMethod;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.test.project.command.AddTask;
import org.spine3.test.project.command.CreateProject;
import org.spine3.test.project.command.StartProject;
import org.spine3.type.CommandClass;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.server.CommandValidation.isUnsupportedCommand;
import static org.spine3.testdata.TestCommandFactory.*;

@SuppressWarnings("InstanceMethodNamingConvention")
public class CommandBusShould {

    private CommandBus commandBus;

    @Before
    public void setUp() {
        final StorageFactory storageFactory = InMemoryStorageFactory.getInstance();
        final CommandStore commandStore = new CommandStore(storageFactory.createCommandStorage());
        commandBus = CommandBus.create(commandStore);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_CommandStore_on_construction() {
        //noinspection ConstantConditions
        CommandBus.create(null);
    }

    //
    // Test for empty handler
    //--------------------------
    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_empty_dispatchers() {
        commandBus.register(new EmptyDispatcher());
    }

    public static class EmptyDispatcher implements CommandDispatcher {

        @Override
        public Set<CommandClass> getCommandClasses() {
            return Collections.emptySet();
        }

        @Override
        public List<EventRecord> dispatch(Message command, CommandContext context) throws Exception, FailureThrowable {
            //noinspection ReturnOfNull
            return null;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_command_handlers_without_methods() {
        commandBus.register(new EmptyCommandHandler());
    }

    private static class EmptyCommandHandler implements CommandHandler {

        @Override
        public CommandHandlerMethod createMethod(Method method) {
            return new CommandHandlerMethod(this, method) {};
        }

        @Override
        public Predicate<Method> getHandlerMethodPredicate() {
            return Predicates.alwaysFalse();
        }
    }

    //
    // Test for duplicate dispatchers
    //----------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void do_not_allow_another_dispatcher_for_already_registered_commands() {
        commandBus.register(new TwoCommandDispatcher());
        commandBus.register(new TwoCommandDispatcher());
    }

    private static class TwoCommandDispatcher implements CommandDispatcher {

        @Override
        public Set<CommandClass> getCommandClasses() {
            return CommandClass.setOf(CreateProject.class, AddTask.class);
        }

        @Override
        public List<EventRecord> dispatch(Message command, CommandContext context) throws Exception, FailureThrowable {
            //noinspection ReturnOfNull
            return null;
        }
    }

    /**
     * Test for successful dispatcher registration.
     */
    @Test
    public void register_command_dispatcher() {
        commandBus.register(new CommandDispatcher() {
            @Override
            public Set<CommandClass> getCommandClasses() {
                return CommandClass.setOf(CreateProject.class, StartProject.class, AddTask.class);
            }

            @Override
            public List<EventRecord> dispatch(Message command, CommandContext context) throws Exception, FailureThrowable {
                //noinspection ReturnOfNull
                return null;
            }
        });

        final String projectId = "@Test register_command_dispatcher";
        assertEquals(Responses.ok(), commandBus.validate(createProject(projectId)));
        assertEquals(Responses.ok(), commandBus.validate(startProject(projectId)));
        assertEquals(Responses.ok(), commandBus.validate(addTask(projectId)));
    }

    /**
     * Test that unregistering dispatcher makes commands unsupported.
     */
    @Test
    public void turn_commands_unsupported_when_dispatcher_unregistered() {
        final CommandDispatcher dispatcher = new CommandDispatcher() {
            @Override
            public Set<CommandClass> getCommandClasses() {
                return CommandClass.setOf(CreateProject.class, StartProject.class, AddTask.class);
            }

            @Override
            public List<EventRecord> dispatch(Message command, CommandContext context) throws Exception, FailureThrowable {
                //noinspection ReturnOfNull
                return null;
            }
        };

        commandBus.register(dispatcher);
        commandBus.unregister(dispatcher);

        final String projectId = "@Test unregister_dispatcher";

        assertTrue(isUnsupportedCommand(commandBus.validate(createProject(projectId))));
        assertTrue(isUnsupportedCommand(commandBus.validate(startProject(projectId))));
        assertTrue(isUnsupportedCommand(commandBus.validate(addTask(projectId))));
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_allow_to_register_dispatcher_for_the_command_with_registered_handler() {
        final CommandHandler createProjectHandler = new CommandHandler() {

            @Assign
            public void handle(CreateProject command, CommandContext ctx) {
                // Do nothing.
            }

            @Override
            public CommandHandlerMethod createMethod(Method method) {
                return new CommandHandlerMethod(this, method) {
                    @Override
                    public <R> R invoke(Message message, CommandContext context) throws InvocationTargetException {
                        return super.invoke(message, context);
                    }
                };
            }

            @Override
            public Predicate<Method> getHandlerMethodPredicate() {
                return new Predicate<Method>() {
                    @Override
                    public boolean apply(@Nullable Method input) {
                        return input != null && input.getName().equals("handle");
                    }
                };
            }
        };

        commandBus.register(createProjectHandler);

        final CommandDispatcher createProjectDispatcher = new CommandDispatcher() {
            @Override
            public Set<CommandClass> getCommandClasses() {
                return CommandClass.setOf(CreateProject.class);
            }

            @Override
            public List<EventRecord> dispatch(Message command, CommandContext context) throws Exception, FailureThrowable {
                //noinspection ReturnOfNull
                return null;
            }
        };

        commandBus.register(createProjectDispatcher);
    }
}
