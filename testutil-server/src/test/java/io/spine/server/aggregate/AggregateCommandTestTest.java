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

package io.spine.server.aggregate;

import com.google.protobuf.util.Timestamps;
import io.spine.server.aggregate.given.AggregateCommandTestTestEnv.TimePrinter;
import io.spine.server.aggregate.given.AggregateCommandTestTestEnv.TimePrintingTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.server.aggregate.given.AggregateCommandTestTestEnv.TimePrintingTest.TEST_COMMAND;
import static io.spine.server.aggregate.given.AggregateCommandTestTestEnv.aggregate;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("AggregateCommandTest should")
class AggregateCommandTestTest {

    private TimePrintingTest aggregateCommandTest;

    @BeforeEach
    void setUp() {
        aggregateCommandTest = new TimePrintingTest();
    }

    @Test
    @DisplayName("store tested command")
    void shouldStoreCommand() {
        aggregateCommandTest.setUp();
        assertEquals(aggregateCommandTest.storedMessage(), TEST_COMMAND);
    }

    @Test
    @DisplayName("dispatch tested command")
    void shouldDispatchCommand() {
        aggregateCommandTest.setUp();
        TimePrinter testAggregate = aggregate();
        aggregateCommandTest.expectThat(testAggregate);
        String newState = testAggregate.getState()
                                       .getValue();
        assertEquals(newState, Timestamps.toString(TEST_COMMAND));
    }

}
