/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.storage.memory;

import org.junit.Before;
import org.junit.Test;
import org.spine3.server.storage.AggregateStorageRecord;
import org.spine3.test.project.ProjectId;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.junit.Assert.*;
import static org.spine3.util.testutil.AggregateStorageRecordFactory.getSequentialRecords;
import static org.spine3.util.testutil.AggregateStorageRecordFactory.newAggregateStorageRecord;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "MethodMayBeStatic", "MagicNumber", "ClassWithTooManyMethods",
        "DuplicateStringLiteralInspection", "ConstantConditions"})
public class InMemoryAggregateStorageShould {

    private InMemoryAggregateStorage<ProjectId> storage;
    private ProjectId projectId;


    @Before
    public void setUp() {

        storage = new InMemoryAggregateStorage<>();
        projectId = ProjectId.newBuilder().setId("aggregateId").build();
    }

    @Test
    public void return_iterator_over_empty_collection_if_read_history_from_empty_storage() {

        final Iterator<AggregateStorageRecord> iterator = storage.historyBackward(projectId);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void return_iterator_over_empty_collection_if_read_by_null_id() {

        final Iterator<AggregateStorageRecord> iterator = storage.historyBackward(null);
        assertFalse(iterator.hasNext());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_null_record() {
        storage.write(null);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_record_with_null_aggregate_id() {

        final AggregateStorageRecord record = AggregateStorageRecord.newBuilder().setAggregateId(null).build();
        storage.write(record);
    }

    @Test
    public void save_and_read_one_record() {

        final AggregateStorageRecord expected = newAggregateStorageRecord(getCurrentTime(), projectId.getId());
        storage.write(expected);

        final Iterator<AggregateStorageRecord> iterator = storage.historyBackward(projectId);

        assertTrue(iterator.hasNext());

        final AggregateStorageRecord actual = iterator.next();

        assertEquals(expected, actual);

        assertFalse(iterator.hasNext());
    }

    @Test
    public void save_records_and_return_sorted_by_timestamp_descending() {

        final List<AggregateStorageRecord> records = getSequentialRecords(projectId.getId());

        for (AggregateStorageRecord record : records) {
            storage.write(record);
        }

        final Iterator<AggregateStorageRecord> iterator = storage.historyBackward(projectId);
        final List<AggregateStorageRecord> actual = newArrayList(iterator);

        Collections.reverse(records); // expected records should be in reverse order

        assertEquals(records, actual);
    }
}