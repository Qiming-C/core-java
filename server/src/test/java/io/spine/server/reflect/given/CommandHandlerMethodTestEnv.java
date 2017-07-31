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

package io.spine.server.reflect.given;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.core.CommandContext;
import io.spine.server.BoundedContext;
import io.spine.server.command.Assign;
import io.spine.server.command.CommandHandler;
import io.spine.test.reflect.command.RefCreateProject;
import io.spine.test.reflect.event.RefProjectCreated;

import java.lang.reflect.Method;
import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;
import static io.spine.server.reflect.given.Given.EventMessage.projectCreated;

public class CommandHandlerMethodTestEnv {

    /** Prevents instantiation on this utility class. */
    private CommandHandlerMethodTestEnv() {
    }

    public static class ValidHandlerOneParam extends TestCommandHandler {
        @Assign
        RefProjectCreated handleTest(RefCreateProject cmd) {
            return projectCreated(cmd.getProjectId());
        }
    }

    public static class ValidHandlerOneParamReturnsList extends TestCommandHandler {
        @SuppressWarnings("UnusedReturnValue")
        @Assign
        @VisibleForTesting
        public List<Message> handleTest(RefCreateProject cmd) {
            final List<Message> result = newLinkedList();
            result.add(projectCreated(cmd.getProjectId()));
            return result;
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public static class ValidHandlerTwoParams extends TestCommandHandler {
        @Assign
        @VisibleForTesting
        public RefProjectCreated handleTest(RefCreateProject cmd, CommandContext context) {
            return projectCreated(cmd.getProjectId());
        }
    }

    public static class ValidHandlerTwoParamsReturnsList extends TestCommandHandler {
        @Assign
        @VisibleForTesting
        public List<Message> handleTest(RefCreateProject cmd, CommandContext context) {
            final List<Message> result = newLinkedList();
            result.add(projectCreated(cmd.getProjectId()));
            return result;
        }
    }

    public static class ValidHandlerButPrivate extends TestCommandHandler {
        @Assign
        @VisibleForTesting
        public RefProjectCreated handleTest(RefCreateProject cmd) {
            return projectCreated(cmd.getProjectId());
        }
    }

    @SuppressWarnings("unused")
        // because the method is not annotated, which is the purpose of this test class.
    public static class InvalidHandlerNoAnnotation extends TestCommandHandler {
        public RefProjectCreated handleTest(RefCreateProject cmd, CommandContext context) {
            return projectCreated(cmd.getProjectId());
        }
    }

    public static class InvalidHandlerNoParams extends TestCommandHandler {
        @Assign
        RefProjectCreated handleTest() {
            return RefProjectCreated.getDefaultInstance();
        }
    }

    public static class InvalidHandlerTooManyParams extends TestCommandHandler {
        @Assign
        RefProjectCreated handleTest(RefCreateProject cmd,
                                     CommandContext context,
                                     Object redundant) {
            return projectCreated(cmd.getProjectId());
        }
    }

    public static class InvalidHandlerOneNotMsgParam extends TestCommandHandler {
        @Assign
        RefProjectCreated handleTest(Exception invalid) {
            return RefProjectCreated.getDefaultInstance();
        }
    }

    public static class InvalidHandlerTwoParamsFirstInvalid extends TestCommandHandler {
        @Assign
        RefProjectCreated handleTest(Exception invalid, CommandContext context) {
            return RefProjectCreated.getDefaultInstance();
        }
    }

    public static class InvalidHandlerTwoParamsSecondInvalid extends TestCommandHandler {
        @Assign
        RefProjectCreated handleTest(RefCreateProject cmd, Exception invalid) {
            return projectCreated(cmd.getProjectId());
        }
    }

    public static class InvalidHandlerReturnsVoid extends TestCommandHandler {
        @Assign
        void handleTest(RefCreateProject cmd, CommandContext context) {
        }
    }

    /**
     * Abstract base for test environment command handlers.
     */
    public abstract static class TestCommandHandler extends CommandHandler {

        private static final String HANDLER_METHOD_NAME = "handleTest";

        protected TestCommandHandler() {
            super(BoundedContext.newBuilder()
                                .setMultitenant(true)
                                .build()
                                .getEventBus());
        }

        public Method getHandler() {
            final Method[] methods = getClass().getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName()
                          .equals(HANDLER_METHOD_NAME)) {
                    return method;
                }
            }
            throw new RuntimeException("No command handler method found: " + HANDLER_METHOD_NAME);
        }
    }
}