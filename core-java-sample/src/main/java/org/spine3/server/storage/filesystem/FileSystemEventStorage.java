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

import org.spine3.base.EventRecord;
import org.spine3.server.storage.EventStorage;
import org.spine3.server.storage.EventStoreRecord;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newLinkedList;
import static org.spine3.server.storage.filesystem.FileSystemHelper.checkFileExists;
import static org.spine3.server.storage.filesystem.FileSystemHelper.closeSilently;
import static org.spine3.server.storage.filesystem.FileSystemHelper.tryOpenFileInputStream;
import static org.spine3.server.storage.filesystem.FileSystemStoragePathHelper.*;
import static org.spine3.util.Events.toEventRecord;

public class FileSystemEventStorage extends EventStorage {

    private final List<EventRecordFileIterator> iterators = newLinkedList();


    protected static EventStorage newInstance() {
        return new FileSystemEventStorage();
    }

    private FileSystemEventStorage() {}

    @Override
    public Iterator<EventRecord> allEvents() {

        final File file = new File(getEventStoreFilePath());

        final EventRecordFileIterator iterator = new EventRecordFileIterator(file);

        iterators.add(iterator);

        return iterator;
    }

    @Override
    protected void write(EventStoreRecord record) {
        checkNotNull(record, "Record shouldn't be null.");
        FileSystemHelper.write(record);
    }

    @Override
    protected void releaseResources() {
        for (EventRecordFileIterator i : iterators) {
            i.releaseResources();
        }
    }


    private static class EventRecordFileIterator implements Iterator<EventRecord> {

        private final File file;
        private FileInputStream fileInputStream;
        private BufferedInputStream bufferedInputStream;
        private boolean areResourcesReleased;

        private EventRecordFileIterator(File file) {
            this.file = file;
        }

        @Override
        public boolean hasNext() {

            if (!file.exists() || areResourcesReleased) {
                return false;
            }

            boolean hasNext;
            try {
                final int availableBytesCount = getInputStream().available();
                hasNext = availableBytesCount > 0;
            } catch (IOException e) {
                throw new RuntimeException("Failed to get estimate of bytes available.", e);
            }
            return hasNext;
        }

        @SuppressWarnings({"ReturnOfNull", "IteratorNextCanNotThrowNoSuchElementException"})
        @Override
        public EventRecord next() {

            checkFileExists(file);
            checkHasNextBytes();

            EventStoreRecord storeRecord = parseEventRecord();
            EventRecord result = toEventRecord(storeRecord);

            if (!hasNext()) {
                releaseResources();
            }

            checkNotNull(result, "Event record from file shouldn't be null.");

            return result;
        }

        private EventStoreRecord parseEventRecord() {
            EventStoreRecord event;
            try {
                event = EventStoreRecord.parseDelimitedFrom(getInputStream());
            } catch (IOException e) {
                throw new RuntimeException("Failed to read event record from file: " + file.getAbsolutePath(), e);
            }
            return event;
        }

        private InputStream getInputStream() {

            if (bufferedInputStream == null || fileInputStream == null) {
                fileInputStream = tryOpenFileInputStream(file);
                bufferedInputStream = new BufferedInputStream(fileInputStream);
            }

            return bufferedInputStream;
        }

        private void releaseResources() {
            if (!areResourcesReleased) {
                closeSilently(fileInputStream, bufferedInputStream);
                areResourcesReleased = true;
            }
        }

        private void checkHasNextBytes() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more records to read from file.");
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("This operation is not supported for FileSystemStorage");
        }
    }
}