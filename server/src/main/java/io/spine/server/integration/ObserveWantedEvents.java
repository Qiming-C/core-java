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

package io.spine.server.integration;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.protobuf.Message;
import io.spine.core.BoundedContextName;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static io.spine.protobuf.AnyPacker.unpack;

/**
 * Reacts on {@code ExternalEventsWanted} sent by other parties (usually Bounded Contexts)
 * in a multi-component environment.
 *
 * @see #handle(ExternalMessage)
 */
final class ObserveWantedEvents extends AbstractChannelObserver implements AutoCloseable {

    private final BoundedContextName boundedContextName;
    private final BusAdapter bus;

    /**
     * Current set of message type URLs, requested by other parties via sending the
     * {@linkplain ExternalEventsWanted configuration messages}, mapped to IDs of their origin
     * bounded contexts.
     */
    private final Multimap<ExternalEventType, BoundedContextName> requestedTypes =
            HashMultimap.create();

    ObserveWantedEvents(BoundedContextName context, BusAdapter bus) {
        super(context, ExternalEventsWanted.class);
        this.boundedContextName = context;
        this.bus = bus;
    }

    /**
     * Unpacks {@code ExternalEventsWanted} from the passed {@code ExternalMessage} and
     * handles it by creating local publishers for the requested types and dismissing types
     * that are no longer needed.
     */
    @Override
    public void handle(ExternalMessage message) {
        BoundedContextName origin = message.getBoundedContextName();
        if (origin.equals(contextName())) {
            return;
        }
        ExternalEventsWanted request = unpack(
                message.getOriginalMessage(),
                ExternalEventsWanted.class
        );
        List<ExternalEventType> externalTypes = request.getTypeList();
        addNewSubscriptions(externalTypes, origin);
        clearStaleSubscriptions(externalTypes, origin);
    }

    private void addNewSubscriptions(Iterable<ExternalEventType> types,
                                     BoundedContextName origin) {
        for (ExternalEventType newType : types) {
            Collection<BoundedContextName> contextsWithSameRequest = requestedTypes.get(newType);
            if (contextsWithSameRequest.isEmpty()) {
                // This item has not been requested by anyone yet.
                // Let's create a subscription.
                registerInAdapter(newType);
            }
            requestedTypes.put(newType, origin);
        }
    }

    private void registerInAdapter(ExternalEventType type) {
        Class<? extends Message> messageClass = type.asMessageClass();
        bus.register(messageClass);
    }

    private void clearStaleSubscriptions(Collection<ExternalEventType> types,
                                         BoundedContextName origin) {
        Set<ExternalEventType> toRemove = findStale(types, origin);

        for (ExternalEventType itemForRemoval : toRemove) {
            boolean wereNonEmpty = !requestedTypes.get(itemForRemoval)
                                                  .isEmpty();
            requestedTypes.remove(itemForRemoval, origin);
            boolean emptyNow = requestedTypes.get(itemForRemoval)
                                             .isEmpty();
            if (wereNonEmpty && emptyNow) {
                unregisterInAdapter(itemForRemoval);
            }
        }
    }

    private void unregisterInAdapter(ExternalEventType type) {
        Class<? extends Message> messageClass = type.asMessageClass();
        bus.unregister(messageClass);
    }

    private Set<ExternalEventType> findStale(Collection<ExternalEventType> types,
                                             BoundedContextName origin) {
        ImmutableSet.Builder<ExternalEventType> result = ImmutableSet.builder();

        for (ExternalEventType previouslyRequestedType : requestedTypes.keySet()) {
            Collection<BoundedContextName> contextsThatRequested =
                    requestedTypes.get(previouslyRequestedType);

            if (contextsThatRequested.contains(origin) &&
                    !types.contains(previouslyRequestedType)) {

                // The `previouslyRequestedType` item is no longer requested
                // by the bounded context with `origin` name.
                result.add(previouslyRequestedType);
            }
        }
        return result.build();
    }

    @Override
    public String toString() {
        return String.format(
                "Observer of `RequestedEventType`s. Bounded Context name = `%s`.",
                boundedContextName.getValue());
    }

    /**
     * Removes all the current subscriptions from the local buses.
     */
    @Override
    public void close() {
        for (ExternalEventType currentlyRequestedMessage : requestedTypes.keySet()) {
            unregisterInAdapter(currentlyRequestedMessage);
        }
        requestedTypes.clear();
    }
}
