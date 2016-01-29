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

package org.spine3.client;

import com.google.protobuf.StringValue;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.util.Tests;

import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("InstanceMethodNamingConvention")
public class CommandsShould {

    @SuppressWarnings("MethodWithTooExceptionsDeclared")
    @Test
    public void have_private_ctor()
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Tests.callPrivateUtilityConstructor(Commands.class);
    }

    @Test
    public void extract_message_from_command() {
        final StringValue message = StringValue.newBuilder().setValue("extract_message_from_command").build();
        final Command command = Commands.newCommand(message, CommandContext.getDefaultInstance());
        assertEquals(message, Commands.getMessage(command));
    }
}
