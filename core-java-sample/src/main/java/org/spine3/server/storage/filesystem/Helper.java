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

import com.google.common.base.Strings;
import com.google.protobuf.Message;
import org.spine3.server.storage.CommandStoreRecord;
import org.spine3.server.storage.EventStoreRecord;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;

/**
 * @author Mikhail Mikhaylov
 */
public class Helper {

    @SuppressWarnings("StaticNonFinalField")
    private static String fileStoragePath = null;

    private static final String COMMAND_STORE_FILE_NAME = "/command-store";
    private static final String EVENT_STORE_FILE_NAME = "/event-store";

    private static final String AGGREGATE_FILE_NAME_PREFIX = "/aggregate/";
    private static final String PATH_DELIMITER = "/";

    public static final String STORAGE_PATH_IS_NOT_SET = "Storage path is not set.";

    @SuppressWarnings("StaticNonFinalField")
    private static File backup = null;

    private Helper() {
    }

    /**
     * Configures helper with file storage path.
     *
     * @param storagePath file storage path
     */
    public static void configure(String storagePath) {
        fileStoragePath = storagePath;
    }

    public static void write(CommandStoreRecord record) {
        checkConfigured();

        final String filePath = fileStoragePath + COMMAND_STORE_FILE_NAME;
        File file = new File(filePath);
        writeMessage(file, record);
    }

    public static void write(EventStoreRecord record) {
        checkConfigured();

        final String filePath = fileStoragePath + EVENT_STORE_FILE_NAME;
        File file = new File(filePath);
        writeMessage(file, record);
    }

    @SuppressWarnings({"TypeMayBeWeakened", "ResultOfMethodCallIgnored"})
    protected static void writeMessage(File file, Message message) {

        OutputStream outputStream = null;

        try {
            if (file.exists()) {
                backup = makeBackupCopy(file);
            } else {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            outputStream = getObjectOutputStream(file);

            message.writeDelimitedTo(outputStream);

            if (backup != null) {
                backup.delete();
            }
        } catch (IOException ignored) {
            restoreFromBackup(file);
        } finally {
            closeSilently(outputStream);
        }
    }

    private static void restoreFromBackup(File file) {
        boolean isDeleted = file.delete();
        if (isDeleted && backup != null) {
            //noinspection ResultOfMethodCallIgnored
            backup.renameTo(file);
        }
    }

    private static File makeBackupCopy(File sourceFile) throws IOException {
        File backupFile = new File(sourceFile.toPath() + "_backup");

        Files.copy(sourceFile.toPath(), backupFile.toPath());

        return backupFile;
    }

    private static void closeSilently(@Nullable Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            //NOP
        }
    }

    private static OutputStream getObjectOutputStream(File file) throws IOException {
        return new BufferedOutputStream(new FileOutputStream(file, true));
    }

    @SuppressWarnings("StaticVariableUsedBeforeInitialization")
    private static void checkConfigured() {
        if (Strings.isNullOrEmpty(fileStoragePath)) {
            throw new IllegalStateException(STORAGE_PATH_IS_NOT_SET);
        }
    }
}
