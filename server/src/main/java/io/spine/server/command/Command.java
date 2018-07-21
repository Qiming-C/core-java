/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a commanding method.
 *
 * <p>A commanding method <strong>must</strong>:
 * <ul>
 *     <li>be annotated with {@link Command @Command};
 *     <li>return a command message derived from {@link com.google.protobuf.Message Message},
 *     if there is only one command to be generated;
 *     <strong>or</strong> an {@link java.lang.Iterable Iterable} of command messages for two or
 *     more commands;
 *     <li>accept one of the following as the first parameter:
 *     <ul>
 *        <li>a {@linkplain com.google.protobuf.Message command message}
 *        <li>an {@linkplain com.google.protobuf.Message event message}
 *        <li>a {@link com.google.protobuf.Message rejection message}
 *     </ul>
 * </ul>
 *
 * <p>A commanding method <strong>may</strong> accept a context of the incoming message
 * ({@link io.spine.core.CommandContext CommandContext},
 * {@link io.spine.core.EventContext EventContext}, or
 * {@link io.spine.core.RejectionContext RejectionContext} correspondingly)
 * as the second parameter, if handling of the command requires its context.
 *
 * <p>If the annotation is applied to a method which doesn't satisfy any of these requirements,
 * this method is not considered a command handler and is <strong>not</strong> registered for
 * command generation.
 *
 * <p>A commanding method <strong>should</strong> have package-private access. It will allow
 * calling this method from tests. The method should not be {@code public} because it is not
 * supposed to be called directly.
 *
 * <h2>Handle or Transform a Command</h2>
 *
 * <p>An application can either {@linkplain io.spine.server.command.Assign handle} an incoming
 * command <strong>or</strong> transform it into one or more commands that will be handled instead.
 * Because of this, a commanding method cannot accept a command message, if there is already a
 * command handling method which accepts the command message type.
 *
 * <p>If a commanding method accepts a command type which is also handled by the application
 * by another method, a run-time error will occur. This also means that an application cannot have
 * two commanding methods that accept the same command type.
 *
 * @author Alexander Yevsyukov
 * @see io.spine.server.command.Assign Handling Commands
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Command {
}
