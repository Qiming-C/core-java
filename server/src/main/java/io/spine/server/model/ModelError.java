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

package io.spine.server.model;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import io.spine.string.Diags;

import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * A programming error of message processing definition.
 */
public class ModelError extends Error {

    private static final long serialVersionUID = 0L;

    protected ModelError(String message) {
        super(message);
    }

    @FormatMethod
    public ModelError(@FormatString String messageFormat, Object... args) {
        super(format(messageFormat, args));
    }

    public ModelError(Throwable cause) {
        super(cause);
    }

    /**
     * An error message formatting helper.
     */
    static class MessageFormatter {

        private MessageFormatter() {
        }

        static String backtick(Object object) {
            return Diags.backtick(object);
        }

        static Collector<CharSequence, ?, String> toStringEnumeration() {
            return Collectors.joining(", ");
        }
    }
}
