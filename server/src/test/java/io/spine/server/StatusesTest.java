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

package io.spine.server;

import com.google.common.testing.NullPointerTester;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.spine.base.Error;
import io.spine.grpc.MetadataConverter;
import io.spine.server.event.UnsupportedEventException;
import io.spine.server.transport.Statuses;
import io.spine.test.event.ProjectCreated;
import io.spine.testdata.Sample;
import io.spine.testing.UtilityClassTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.server.transport.Statuses.invalidArgumentWithCause;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("`Statuses` utility should")
class StatusesTest extends UtilityClassTest<Statuses> {

    StatusesTest() {
        super(Statuses.class);
    }

    @Override
    protected void configure(NullPointerTester tester) {
        super.configure(tester);
        tester.setDefault(Exception.class, new RuntimeException("Statuses test"))
              .setDefault(Error.class, Error.getDefaultInstance());
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Test
    @DisplayName("create invalid argument status exception")
    void createInvalidArgumentStatusEx() {
        MessageError rejection =
                new UnsupportedEventException(Sample.messageOfType(ProjectCreated.class));
        StatusRuntimeException statusRuntimeEx = invalidArgumentWithCause(rejection);
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        Error actualError = MetadataConverter.toError(statusRuntimeEx.getTrailers())
                                             .get();
        assertEquals(Status.INVALID_ARGUMENT.getCode(), statusRuntimeEx.getStatus()
                                                                       .getCode());
        assertEquals(rejection, statusRuntimeEx.getCause());
        assertEquals(rejection.asError(), actualError);
    }
}
