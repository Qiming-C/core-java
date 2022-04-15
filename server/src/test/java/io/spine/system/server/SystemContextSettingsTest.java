/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.system.server;

import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.spine.base.Identifier;
import io.spine.core.CommandId;
import io.spine.core.Event;
import io.spine.core.MessageId;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventFilter;
import io.spine.server.event.EventStreamQuery;
import io.spine.system.server.event.EntityCreated;
import io.spine.system.server.event.EntityStateChanged;
import io.spine.system.server.given.entity.HistoryEventWatcher;
import io.spine.testing.server.TestEventFactory;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static io.spine.base.Identifier.newUuid;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.option.EntityOption.Kind.ENTITY;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.system.server.SystemBoundedContexts.systemOf;
import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("System Bounded Context should")
class SystemContextSettingsTest {

    private static final TestEventFactory events =
            TestEventFactory.newInstance(SystemContextSettingsTest.class);

    @Nested
    @DisplayName("by default")
    class ByDefault {

        @Test
        @DisplayName("not store events")
        void notStoreEvents() {
            var domain = BoundedContextBuilder.assumingTests().build();
            var system = systemOf(domain);
            var event = createEvent();
            var observer = postSystemEvent(system.eventBus(), event);
            assertTrue(observer.isCompleted());
            assertThat(observer.responses()).isEmpty();
        }

        @Test
        @DisplayName("not store domain commands")
        void notStoreDomainCommands() {
            var domain = BoundedContextBuilder.assumingTests().build();
            var system = systemOf(domain);
            assertFalse(system.hasEntitiesWithState(CommandLog.class));
        }

        @Test
        @DisplayName("post system events in parallel")
        void postEventsInParallel() {
            var domain = BoundedContextBuilder.assumingTests().build();
            var system = systemOf(domain);
            var watcher = new HistoryEventWatcher();
            system.eventBus().register(watcher);
            var messageId = MessageId.newBuilder()
                    .setTypeUrl(TypeUrl.of(Empty.class).value())
                    .setId(Identifier.pack(newUuid()))
                    .build();
            var event = EntityCreated.newBuilder()
                    .setEntity(messageId)
                    .setKind(ENTITY)
                    .build();
            domain.systemClient()
                  .writeSide()
                  .postEvent(event);
            sleepUninterruptibly(ofSeconds(1));
            watcher.assertReceivedEvent(EntityCreated.class);
        }
    }

    @Nested
    @DisplayName("if required")
    class IfRequired {

        @Test
        @DisplayName("store events")
        void storeEvents() {
            var contextBuilder = BoundedContextBuilder.assumingTests();
            contextBuilder.systemSettings()
                          .persistEvents();
            var domain = contextBuilder.build();
            var system = systemOf(domain);
            var event = createEvent();
            var observer = postSystemEvent(system.eventBus(), event);
            assertTrue(observer.isCompleted());
            assertThat(observer.responses()).containsExactly(event);
        }

        @Test
        @DisplayName("store domain commands")
        void storeDomainCommands() {
            var contextBuilder = BoundedContextBuilder.assumingTests();
            contextBuilder.systemSettings()
                          .enableCommandLog();
            var domain = contextBuilder.build();
            var system = systemOf(domain);
            assertTrue(system.hasEntitiesWithState(CommandLog.class));
        }

        @Test
        @DisplayName("post system events synchronously")
        void postEventsInSync() {
            var contextBuilder = BoundedContextBuilder.assumingTests();
            contextBuilder.systemSettings()
                          .disableParallelPosting();
            var domain = contextBuilder.build();
            var system = systemOf(domain);
            var watcher = new HistoryEventWatcher();
            system.eventBus().register(watcher);
            var messageId = MessageId.newBuilder()
                    .setTypeUrl(TypeUrl.of(Empty.class).value())
                    .setId(Identifier.pack(newUuid()))
                    .build();
            var event = EntityCreated.newBuilder()
                    .setEntity(messageId)
                    .setKind(ENTITY)
                    .build();
            domain.systemClient()
                  .writeSide()
                  .postEvent(event);
            watcher.assertReceivedEvent(EntityCreated.class);
        }

        @Test
        @DisplayName("post system events with the given executor")
        void postEventsWithExecutor() {
            var calls = new AtomicInteger();
            var executor = (Executor) (command) -> {
                calls.incrementAndGet();
                command.run();
            };

            var contextBuilder = BoundedContextBuilder.assumingTests();
            contextBuilder.systemSettings()
                          .enableParallelPosting(executor);

            var domain = contextBuilder.build();
            var system = systemOf(domain);
            var watcher = new HistoryEventWatcher();
            system.eventBus().register(watcher);

            var messageId = MessageId.newBuilder()
                    .setTypeUrl(TypeUrl.of(Empty.class).value())
                    .setId(Identifier.pack(newUuid()))
                    .build();
            var event = EntityCreated.newBuilder()
                    .setEntity(messageId)
                    .setKind(ENTITY)
                    .build();

            assertThat(calls.get()).isEqualTo(0);
            domain.systemClient()
                  .writeSide()
                  .postEvent(event);

            watcher.assertReceivedEvent(EntityCreated.class);
            assertThat(calls.get()).isEqualTo(1);
        }
    }

    private static MemoizingObserver<Event> postSystemEvent(EventBus systemBus, Event event) {
        systemBus.post(event);
        var filter = EventFilter.newBuilder()
                .setEventType(event.enclosedTypeUrl()
                                   .toTypeName().value())
                .vBuild();
        var query = EventStreamQuery.newBuilder()
                .addFilter(filter)
                .vBuild();
        MemoizingObserver<Event> observer = memoizingObserver();
        systemBus.eventStore()
                 .read(query, observer);
        return observer;
    }

    private static Event createEvent() {
        var eventMessage = EntityStateChanged.newBuilder()
                .setEntity(MessageId.newBuilder()
                                    .setId(Identifier.pack(42))
                                    .setTypeUrl(TypeUrl.of(EmptyEntityState.class)
                                                       .value()))
                .setOldState(pack(StringValue.of("0")))
                .setNewState(pack(StringValue.of("42")))
                .addSignalId(MessageId.newBuilder()
                                      .setId(Identifier.pack(CommandId.generate()))
                                      .setTypeUrl(TypeUrl.of(EntityStateChanged.class)
                                                         .value()))
                .vBuild();
        var event = events.createEvent(eventMessage);
        return event;
    }
}
