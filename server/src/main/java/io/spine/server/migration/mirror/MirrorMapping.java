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

package io.spine.server.migration.mirror;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import io.spine.base.EntityState;
import io.spine.protobuf.AnyPacker;
import io.spine.query.Column;
import io.spine.query.ColumnName;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.storage.EntityRecordColumn;
import io.spine.server.entity.storage.EntityRecordSpec;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.system.server.Mirror;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * A transformation of a {@linkplain Mirror} into an {@linkplain EntityRecordWithColumns}.
 *
 * <p>{@code Mirror} was deprecated in Spine 2.x. Now, {@code EntityRecordWithColumns} is used
 * to store the aggregate's state for further querying.
 */
public interface MirrorMapping {

    /**
     * Transforms the passed {@linkplain Mirror} into an {@linkplain EntityRecordWithColumns}.
     *
     * @param mirror
     *         the source mirror to be transformed into an entity record
     * @param aggregateClass
     *         the destination entity type
     * @param <I>
     *         the type of aggregate's identifiers
     * @param <S>
     *         the type of aggregate's state
     * @param <A>
     *         the type of aggregate
     *
     * @implNote {@code Mirror} itself can be directly transformed into
     *         an {@linkplain EntityRecord}. In order to get {@code EntityRecordWithColumns},
     *         we need to know which columns should be fetched from the entity's state.
     *         For this reason, aggregate class is passed along the mirror itself. It contains
     *         information about which columns are defined to be stored along the aggregate's state.
     */
    <I, S extends EntityState<I>, A extends Aggregate<I, S, ?>> EntityRecordWithColumns<I>
    toRecordWithColumns(Mirror mirror, Class<A> aggregateClass);

    /**
     * The default transformation of a {@linkplain Mirror} into
     * an {@linkplain EntityRecordWithColumns}.
     */
    class Default implements MirrorMapping {

        @Override
        public <I, S extends EntityState<I>, A extends Aggregate<I, S, ?>>
        EntityRecordWithColumns<I> toRecordWithColumns(Mirror mirror, Class<A> aggregateClass) {
            var record = toRecord(mirror);
            var recordSpec = EntityRecordSpec.of(aggregateClass);
            var columns = fetchColumns(recordSpec, record);
            var result = EntityRecordWithColumns.<I>of(record, columns);
            return result;
        }

        private static EntityRecord toRecord(Mirror mirror) {
            var record = EntityRecord.newBuilder()
                    .setEntityId(mirror.getId()
                                       .getValue())
                    .setState(mirror.getState())
                    .setVersion(mirror.getVersion())
                    .setLifecycleFlags(mirror.getLifecycle())
                    .vBuild();
            return record;
        }

        private static Map<ColumnName, @Nullable Object>
        fetchColumns(EntityRecordSpec<?, ?, ?> recordSpec, EntityRecord record) {

            // fetching of lifecycle columns
            var lifecycleColumns = EntityRecordColumn.valuesIn(record);

            // fetching of state's columns
            var state = AnyPacker.unpack(record.getState());
            var stateDescriptor = state.getDescriptorForType();
            var allColumns = recordSpec.columns()
                    .stream()
                    .filter(column -> !lifecycleColumns.containsKey(column.name()))
                    .collect(Collectors.toMap(
                            Column::name,
                            column -> extractColumnValue(column.name(), state, stateDescriptor)
                    ));

            // returning of `lifecycle + state` columns
            allColumns.putAll(lifecycleColumns);
            return allColumns;
        }

        private static Object extractColumnValue(ColumnName column,
                                                 Message state,
                                                 Descriptors.Descriptor descriptor) {
            var fieldDescriptor = descriptor.findFieldByName(column.value());
            var result = state.getField(fieldDescriptor);
            return result;
        }
    }
}
