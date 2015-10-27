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

package org.spine3.server.storage.datastore;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spine3.server.storage.AggregateStorage;
import org.spine3.server.storage.AggregateStorageShould;
import org.spine3.test.project.ProjectId;


/**
 * NOTE: to run these tests on Windows, start local Datastore Server manually.<br>
 * See <a href="https://github.com/SpineEventEngine/core-java/wiki/Configuring-Local-Datastore-Environment">docs</a> for details.<br>
 * Reported an issue <a href="https://code.google.com/p/google-cloud-platform/issues/detail?id=10&thanks=10&ts=1443682670">here</a>.<br>
 * TODO:2015.10.07:alexander.litus: remove this comment when this issue is fixed.
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class DatastoreAggregateStorageShould extends AggregateStorageShould {

    @SuppressWarnings("ConstantConditions") // passing null because this parameter isn't used in this implementation
    private static final AggregateStorage<ProjectId> STORAGE = LocalDatastoreStorageFactory.getInstance().createAggregateStorage(null);

    public DatastoreAggregateStorageShould() {
        super(STORAGE);
    }


    @BeforeClass
    public static void setUpClass() {
        LocalDatastoreStorageFactory.getInstance().setUp();
    }

    @After
    public void tearDownTest() {
        LocalDatastoreManager.clear();
    }

    @AfterClass
    public static void tearDownClass() {
        LocalDatastoreStorageFactory.getInstance().tearDown();
    }

    @Test
    public void have_empty_release_resources_method() {
        ((DatastoreAggregateStorage) STORAGE).releaseResources();
    }
}
