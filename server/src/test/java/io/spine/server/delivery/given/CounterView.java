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

package io.spine.server.delivery.given;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import io.spine.core.Subscribe;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.route.EventRoute;
import io.spine.server.route.EventRouting;
import io.spine.test.delivery.Counter;
import io.spine.test.delivery.NumberAdded;

/**
 * Counts the incoming events.
 *
 * <p>Allows to configure the weight of the event. I.e. if the multiplier is {@code 3}, then
 * each incoming event will increase the counter value by {@code 3}.
 *
 * <p>By default, the weight is {@code 1}.
 */
public final class CounterView extends Projection<String, Counter, Counter.Builder> {

    private static int weight = 1;

    @Subscribe
    void on(NumberAdded event) {
        Counter.Builder builder = builder();
        builder.setTotal(builder.getTotal() + weight);
    }

    public static void changeWeightTo(int value) {
        weight = value;
    }

    public static final class Repository
            extends ProjectionRepository<String, CounterView, Counter> {

        @OverridingMethodsMustInvokeSuper
        @Override
        protected void setupEventRouting(EventRouting<String> routing) {
            super.setupEventRouting(routing);
            routing.route(NumberAdded.class, (EventRoute<String, NumberAdded>) (message, context) ->
                    ImmutableSet.of(message.getCalculatorId()));
        }
    }
}
