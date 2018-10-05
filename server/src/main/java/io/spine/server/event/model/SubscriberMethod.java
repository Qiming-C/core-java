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

package io.spine.server.event.model;

import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import io.spine.base.EventMessage;
import io.spine.base.FieldPath;
import io.spine.core.ByField;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.protobuf.FieldPaths;
import io.spine.server.event.EventSubscriber;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.HandlerToken;
import io.spine.server.model.MessageFilter;
import io.spine.server.model.MethodResult;
import io.spine.server.model.declare.ParameterSpec;
import io.spine.string.Stringifiers;

import java.lang.reflect.Method;

import static io.spine.protobuf.FieldPaths.fieldAt;
import static io.spine.protobuf.FieldPaths.parse;
import static io.spine.protobuf.TypeConverter.toAny;
import static io.spine.string.Stringifiers.fromString;

/**
 * @author Dmytro Dashenkov
 */
public abstract class SubscriberMethod extends AbstractHandlerMethod<EventSubscriber,
                                                                     EventMessage,
                                                                     EventClass,
                                                                     EventEnvelope,
                                                                     MethodResult<Empty>> {

    protected SubscriberMethod(Method method, ParameterSpec<EventEnvelope> parameterSpec) {
        super(method, parameterSpec);
    }

    @Override
    protected final MethodResult<Empty> toResult(EventSubscriber target, Object rawMethodOutput) {
        return MethodResult.empty();
    }

    @Override
    public HandlerToken token() {
        HandlerToken typeBasedToken = super.token();
        ByField filter = getFilter();
        if (filter.path().isEmpty()) {
            return typeBasedToken;
        } else {
            FieldPath field = parse(filter.path());
            Class<?> fieldType = FieldPaths.typeOfFieldAt(field);
            Object expectedValue = Stringifiers.fromString(filter.value(), fieldType);
            Any packedValue = toAny(expectedValue);
            MessageFilter messageFilter = MessageFilter
                    .newBuilder()
                    .setField(field)
                    .setValue(packedValue)
                    .build();
            return typeBasedToken.toBuilder()
                                 .setFilter(messageFilter)
                                 .build();
        }
    }

    protected abstract ByField getFilter();

    public final boolean canHandle(EventEnvelope envelope) {
        ByField filter = getFilter();
        String fieldPath = filter.path();
        if (fieldPath.isEmpty()) {
            return true;
        } else {
            EventMessage event = envelope.getMessage();
            return match(event, filter);
        }
    }

    private static boolean match(EventMessage event, ByField filter) {
        FieldPath path = parse(filter.path());
        Object valueOfField = fieldAt(event, path);
        String expectedValueString = filter.value();
        Object expectedValue = fromString(expectedValueString, valueOfField.getClass());
        boolean filterMatches = valueOfField.equals(expectedValue);
        return filterMatches;
    }
}
