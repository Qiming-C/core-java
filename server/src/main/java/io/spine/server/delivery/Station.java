/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.delivery;

/**
 * A stage of processing the messages on the {@link Conveyor}.
 *
 * <p>During the processing, some of the conveyor messages may be delivered, removed or modified.
 */
abstract class Station {

    /**
     * Processes the conveyor messages.
     *
     * @param conveyor
     *         the conveyor on which the messages are travelling
     * @return the processing result
     */
    abstract Result process(Conveyor conveyor);

    /**
     * Tells what the results of the processing at a particular station were.
     */
    static class Result {

        private final int deliveredCount;
        private final DeliveryErrors errors;

        Result(int count, DeliveryErrors errors) {
            deliveredCount = count;
            this.errors = errors;
        }

        /**
         * Tells how many messages were delivered during the processing at the station.
         */
        int deliveredCount() {
            return deliveredCount;
        }

        /**
         * Returns the errors occurred during the delivery of the messages by the station, if any.
         */
        DeliveryErrors errors() {
            return errors;
        }
    }

    /**
     * Returns an empty result with no errors.
     */
    static Result emptyResult() {
        DeliveryErrors noErrors = DeliveryErrors.newBuilder()
                                                .build();
        return new Result(0, noErrors);
    }
}
