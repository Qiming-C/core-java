/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.testing.server.blackbox.given;

import com.google.protobuf.util.Values;
import io.spine.base.Error;
import io.spine.core.CommandId;
import io.spine.core.CommandValidationError;
import io.spine.system.server.event.CommandErrored;

import static io.spine.core.CommandValidationError.DUPLICATE_COMMAND_VALUE;
import static io.spine.core.CommandValidationError.UNSUPPORTED_COMMAND_VALUE;
import static io.spine.server.commandbus.CommandException.ATTR_COMMAND_TYPE_NAME;

public final class GivenCommandError {

    /** Prevents instantiation of this test env class. */
    private GivenCommandError() {
    }

    public static CommandErrored unsupportedError(String commandType) {
        Error error = Error
                .newBuilder()
                .setType(CommandValidationError.getDescriptor()
                                               .getFullName())
                .setCode(UNSUPPORTED_COMMAND_VALUE)
                .putAttributes(ATTR_COMMAND_TYPE_NAME, Values.of(commandType))
                .build();
        CommandErrored result = CommandErrored
                .newBuilder()
                .setId(CommandId.generate())
                .setError(error)
                .build();
        return result;
    }

    public static CommandErrored duplicationError() {
        Error error = Error
                .newBuilder()
                .setType(CommandValidationError.getDescriptor()
                                               .getFullName())
                .setCode(DUPLICATE_COMMAND_VALUE)
                .build();
        CommandErrored result = CommandErrored
                .newBuilder()
                .setId(CommandId.generate())
                .setError(error)
                .build();
        return result;
    }

    public static CommandErrored nonValidationError() {
        Error error = Error
                .newBuilder()
                .setType("some random type")
                .setCode(UNSUPPORTED_COMMAND_VALUE)
                .build();
        CommandErrored result = CommandErrored
                .newBuilder()
                .setId(CommandId.generate())
                .setError(error)
                .build();
        return result;
    }
}
