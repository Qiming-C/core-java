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

package org.spine3.server.aggregate;

import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.server.internal.MessageHandlerMethod;
import org.spine3.server.reflect.MethodMap;
import org.spine3.server.reflect.Methods;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * A wrapper for event applier method.
 *
 * @author Alexander Yevsyukov
 */
class EventApplier extends MessageHandlerMethod<Aggregate, Void> {

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param target object to which the method applies
     * @param method subscriber method
     */
    protected EventApplier(Aggregate target, Method method) {
        super(target, method);
    }

    @Override
    protected <R> R invoke(Message message) throws InvocationTargetException {
        // Make this method visible to Aggregate class.
        return super.invoke(message);
    }

    /**
     * Verifiers modifiers in the methods in the passed map to be 'private'.
     *
     * <p>Logs warning for the methods with a non-private modifier.
     *
     * @param methods the map of methods to check
     */
    public static void checkModifiers(MethodMap methods) {
        for (Map.Entry<Class<? extends Message>, Method> entry : methods.entrySet()) {
            final Method method = entry.getValue();
            final boolean isPrivate = Modifier.isPrivate(method.getModifiers());
            if (!isPrivate) {
                log().warn(String.format("Event applier method %s must be declared 'private'.",
                        Methods.getFullMethodName(method)));
            }
        }
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(EventApplier.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

}
