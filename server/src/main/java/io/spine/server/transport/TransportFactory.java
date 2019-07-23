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
package io.spine.server.transport;

import io.spine.annotation.SPI;
import io.spine.type.TypeUrl;

/**
 * A factory for creating channel-based transport for {@code Message} inter-exchange between the
 * current deployment component and other application parts.
 *
 * Inspired by <a href="http://www.enterpriseintegrationpatterns.com/patterns/messaging/PublishSubscribeChannel.html">
 * Publish-Subscriber Channel pattern.</a>
 */
@SPI
public interface TransportFactory extends AutoCloseable {

    /**
     * Creates a {@link Publisher} channel for the given message type.
     *
     * @param targetType
     *         the type of the messages published by the resulting {@link Publisher}
     * @return a new {@code Publisher} instance
     */
    Publisher createPublisher(TypeUrl targetType);

    /**
     * Creates a {@link Subscriber} channel for the given message type.
     *
     * @param targetType
     *         the type of messages received by the resulting {@link Subscriber}
     * @return a new {@code Subscriber} instance
     */
    Subscriber createSubscriber(TypeUrl targetType);
}
