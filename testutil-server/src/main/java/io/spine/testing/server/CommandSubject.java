/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.testing.server;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import io.spine.base.CommandMessage;
import io.spine.core.Command;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import static com.google.common.truth.Truth.assertAbout;

/**
 * Checks for commands generated by a Bounded Context under the test.
 */
public final class CommandSubject
        extends EmittedMessageSubject<CommandSubject, Command, CommandMessage> {

    private CommandSubject(FailureMetadata metadata, @NullableDecl Iterable<Command> actual) {
        super(metadata, actual);
    }

    /** Provides the factory for creating {@code CommandSubject}. */
    public static Subject.Factory<CommandSubject, Iterable<Command>> commands() {
        return CommandSubject::new;
    }

    @Override
    protected Factory<CommandSubject, Iterable<Command>> factory() {
        return commands();
    }

    /** Creates the subject for asserting passed commands. */
    public static CommandSubject assertThat(@NullableDecl Iterable<Command> actual) {
        return assertAbout(commands()).that(actual);
    }
}
