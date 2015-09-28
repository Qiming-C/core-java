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

package org.spine3.server.storage.filesystem;

import org.junit.Before;
import org.junit.Test;
import org.spine3.server.EntityShould;
import org.spine3.server.aggregate.AggregateShould;
import org.spine3.server.storage.*;
import org.spine3.test.project.Project;

import static org.junit.Assert.assertNotNull;

/**
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class FileSystemStorageFactoryShould {

    @Before
    public void setUpTest() {
        FileSystemStoragePathHelper.configure(FileSystemStorageFactoryShould.class);
        FileSystemHelper.cleanTestData();
    }

    @Test
    public void create_storages_successfully() {
        final StorageFactory factory = new FileSystemStorageFactory();

        final EventStorage eventStorage = factory.createEventStorage();
        final CommandStorage commandStorage = factory.createCommandStorage();
        final EntityStorage<String, Project> entityStorage =
                factory.createEntityStorage(EntityShould.TestEntity.class);
        final AggregateStorage<String> aggregateRootStorage =
                factory.createAggregateRootStorage(AggregateShould.TestAggregateWithIdString.class);

        assertNotNull(eventStorage);
        assertNotNull(commandStorage);
        assertNotNull(entityStorage);
        assertNotNull(aggregateRootStorage);
    }
}
