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

package io.spine.server.delivery;

import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.server.delivery.event.ShardProcessingRequested;

import java.util.Optional;

import static io.spine.protobuf.AnyPacker.pack;

/**
 * Updates the {@code ShardProcessingRequested} events residing in the {@code Conveyor} by setting
 * the passed {@link DeliveryRunInfo} into each.
 */
final class UpdateShardProcessingEvents implements ConveyorJob {

    private final DeliveryRunInfo runInfo;

    /**
     * Creates a new job with the passed delivery run info to set into the matching events.
     */
    UpdateShardProcessingEvents(DeliveryRunInfo info) {
        runInfo = info;
    }

    @Override
    public Optional<InboxMessage> modify(InboxMessage message) {
        if (message.hasEvent()) {
            Event event = message.getEvent();
            EventMessage eventMessage = event.enclosedMessage();
            if (eventMessage instanceof ShardProcessingRequested) {
                ShardProcessingRequested cast = (ShardProcessingRequested) eventMessage;
                ShardProcessingRequested updatedSignal = updateWithRunInfo(cast);
                InboxMessage modified = inject(updatedSignal, message);
                return Optional.of(modified);
            }
        }
        return Optional.empty();
    }

    /**
     * Injects the passed event message into the copye of the passed {@code InboxMessage}
     * and returns a new {@code InboxMessage} with the modified data.
     *
     * @param what
     *         the event message to inject
     * @param destination
     *         the original inbox message into which copy the passed event message should be
     *         injected
     * @return a copy of the passed {@code InboxMessage} with the passed event message injected
     */
    private static InboxMessage inject(EventMessage what, InboxMessage destination) {
        Event event = destination.getEvent();
        Event modifiedEvent =
                event.toBuilder()
                     .setMessage(pack(what))
                     .vBuild();
        InboxMessage modifiedMessage =
                destination.toBuilder()
                           .setEvent(modifiedEvent)
                           .vBuild();
        return modifiedMessage;
    }

    private ShardProcessingRequested updateWithRunInfo(ShardProcessingRequested signal) {
        ShardProcessingRequested modifiedSignal =
                signal.toBuilder()
                      .setRunInfo(runInfo)
                      .vBuild();
        return modifiedSignal;
    }
}
