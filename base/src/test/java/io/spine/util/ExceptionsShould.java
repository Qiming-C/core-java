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

package io.spine.util;

import com.google.common.testing.NullPointerTester;
import io.spine.base.Error;
import io.spine.core.MessageRejection;
import org.junit.Test;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static io.spine.Identifier.newUuid;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static io.spine.util.Exceptions.newIllegalStateException;
import static io.spine.util.Exceptions.unsupported;
import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public class ExceptionsShould {

    @Test
    public void have_private_ctor() {
        assertHasPrivateParameterlessCtor(Exceptions.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void create_and_throw_unsupported_operation_exception() {
        unsupported();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void create_and_throw_unsupported_operation_exception_with_message() {
        unsupported(newUuid());
    }

    @Test
    public void pass_the_null_tolerance_check() {
        final Exception defaultException = new RuntimeException("");
        new NullPointerTester()
                .setDefault(Exception.class, defaultException)
                .setDefault(Throwable.class, defaultException)
                .testAllPublicStaticMethods(Exceptions.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_formatted_IAE() {
        newIllegalArgumentException("%d, %d, %s kaboom", 1, 2, "three");
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_formatted_IAE_with_cause() {
        newIllegalArgumentException(new RuntimeException("checking"), "%s", "stuff");
    }

    @Test(expected = IllegalStateException.class)
    public void throw_formatted_ISE() {
        newIllegalStateException("%s check %s", "state", "failed");
    }

    @Test(expected = IllegalStateException.class)
    public void throw_formatted_ISE_with_cause() {
        newIllegalStateException(new RuntimeException(getClass().getSimpleName()),
                                            "%s %s", "taram", "param");
    }

    @Test
    public void convert_DeliverableException_to_Error() {
        final TestException throwable = new TestException();
        final Error actualError = Exceptions.toError(throwable);
        assertEquals(throwable.asError(), actualError);
    }

    private static class TestException extends Exception implements MessageRejection {

        private static final long serialVersionUID = 0L;

        private static final String MESSAGE = TestException.class.getSimpleName();

        private static final String TYPE = TestException.class.getCanonicalName();

        private static final Error ERROR_TEMPLATE = Error.newBuilder()
                                                         .setMessage(MESSAGE)
                                                         .setType(TYPE)
                                                         .build();

        private TestException() {
            super(MESSAGE);
        }

        @Override
        public Error asError() {
            return ERROR_TEMPLATE.toBuilder()
                                 .setStacktrace(getStackTraceAsString(this))
                                 .build();
        }

        @Override
        public Throwable asThrowable() {
            return this;
        }
    }
}
