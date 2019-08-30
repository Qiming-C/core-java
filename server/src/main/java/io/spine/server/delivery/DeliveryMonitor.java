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

import io.spine.annotation.SPI;
import io.spine.server.NodeId;

/**
 * A controlling monitor of a {@link Delivery Inbox Delivery} process.
 *
 * <p>In some cases when a funnel-like {@code Entity} (e.g. some projection of a business report
 * subscribed to half of the domain events) has a lot of messages in its {@code Inbox},
 * the delivery process may be slowed down for other targets in the same shard, while
 * all the messages are being dispatched the "busy" {@code Entity}. Depending on the number
 * of messages to deliver, it may take a lot of time.
 *
 * <p>Environments such as Google AppEngine Standard imply restrictions on the duration of the
 * request processing. Therefore, it may be necessary to stop the message delivery for this shard
 * at some point, and re-schedule the shard processing again by sending another request and
 * thus resetting the processing clock.
 */
@SPI
public class DeliveryMonitor {

    private static final DeliveryMonitor ALWAYS_CONTINUE = new DeliveryMonitor();

    /**
     * Determines if the delivery execution should be stopped after the given stage is completed.
     *
     * <p>If {@code false} is returned, the ongoing delivery run for the served shard will be
     * stopped at this application node. The node will release the previously
     * {@linkplain ShardedWorkRegistry#pickUp(ShardIndex, NodeId) picked up} shard.
     *
     * <p>To trigger the new delivery processing for this shard, use
     * {@linkplain Delivery#deliverMessagesFrom(ShardIndex)
     * Delivery.deliverMessagesFrom(ShardIndex)}.
     *
     * <p>This method is called synchronously, meaning that the delivery process will not
     * resume until a value is returned from this method call.
     *
     * @param stage
     *         the stage of delivery which has ended
     * @return {@code true} to continue the delivery, {@code false} to stop
     * @implNote The default implementation stops the execution once there were zero
     *         messages {@linkplain DeliveryStage#messagesDelivered() delivered in the given stage}.
     */
    public boolean shouldStopAfter(DeliveryStage stage) {
        return false;
    }

    /**
     * Returns an instance of {@code DeliveryMonitor} which always tells to continue.
     */
    static DeliveryMonitor alwaysContinue() {
        return ALWAYS_CONTINUE;
    }
}
