/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.test;

import com.google.protobuf.Descriptors;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.protobuf.Timestamps;
import org.spine3.users.UserId;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertEquals;

/**
 * Utilities for testing.
 *
 * @author Alexander Yevsyukov
 */
public class Tests {

    private Tests() {}

    /**
     * Verifies if the passed class has private parameterless constructor and invokes it
     * using Reflection.
     *
     * <p>Use this method to add utility constructor into covered code:
     * <pre>
     * public class MyUtilityShould
     *     ...
     *     {@literal @}Test
     *     public void have_private_utility_ctor() {
     *         assertTrue(hasPrivateUtilityConstructor(MyUtility.class));
     *     }
     * </pre>
     * @return true if the class has private parameterless constructor
     */
    public static boolean hasPrivateUtilityConstructor(Class<?> utilityClass) {
        final Constructor constructor;
        try {
            constructor = utilityClass.getDeclaredConstructor();
        } catch (NoSuchMethodException ignored) {
            return false;
        }

        if (!Modifier.isPrivate(constructor.getModifiers())) {
            return false;
        }

        constructor.setAccessible(true);

        //noinspection OverlyBroadCatchBlock
        try {
            // Call the constructor to include it into the coverage.

            // Some of the coding conventions may encourage throwing AssertionError to prevent the instantiation
            // of the utility class from within the class.
            constructor.newInstance();
        } catch (Exception ignored) {
            return true;
        }
        return true;
    }

    /**
     * Returns the current time in seconds via {@link Timestamps#getCurrentTime()}.
     *
     * @return a seconds value
     */
    public static long currentTimeSeconds() {
        final long secs = Timestamps.getCurrentTime().getSeconds();
        return secs;
    }

    /**
     * Returns {@code null}.
     * Use it when it is needed to pass {@code null} to a method in tests so that no warnings suppression is needed.
     */
    public static <T> T nullRef() {
        final T nullRef = null;
        //noinspection ConstantConditions
        return nullRef;
    }

    /**
     * Creates a new user ID instance by passed string value.
     *
     * @param value new user ID value
     * @return new instance
     */
    public static UserId newUserId(String value) {
        checkNotNull(value);

        return UserId.newBuilder()
                .setValue(value)
                .build();
    }

    public static void assertMatchesMask(Message message, FieldMask fieldMask) {
        final List<String> paths = fieldMask.getPathsList();

        for (Descriptors.FieldDescriptor field : message.getDescriptorForType()
                                                      .getFields()) {
            if (field.isRepeated()) {
                continue;
            }
            assertEquals(message.hasField(field), paths.contains(field.getFullName()));
        }
    }

    /** The provider of current time, which is always the same. */
    public static class FrozenMadHatterParty implements Timestamps.Provider {
        private final Timestamp frozenTime;

        public FrozenMadHatterParty(Timestamp frozenTime) {
            this.frozenTime = frozenTime;
        }

        /** Returns the value passed to the constructor. */
        @Override
        public Timestamp getCurrentTime() {
            return frozenTime;
        }
    }
}
