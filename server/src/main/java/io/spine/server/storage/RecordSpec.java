/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.storage;

import io.spine.annotation.SPI;
import io.spine.query.Column;
import io.spine.query.ColumnName;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Optional;

import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * Defines the specification of a record in a storage.
 *
 * <p>Enumerates the record columns to store along with the record itself.
 *
 * @param <I>
 *         the type of the record identifier
 * @param <R>
 *         the type of the stored record
 * @param <S>
 *         the type of the source object on top of which the values of the columns are extracted
 */
@SPI
public abstract class RecordSpec<I, R, S> {

    private final Class<R> recordType;
    private final Class<I> idType;

    /**
     * Creates a new {@code RecordSpec} instance for the record of the passed type.
     *
     * @param idType the type of the record identifiers
     * @param recordType the type of the record
     */
    protected RecordSpec(Class<I> idType, Class<R> recordType) {
        this.idType = idType;
        this.recordType = recordType;
    }

    /**
     * Returns the type of the stored record.
     */
    public final Class<R> recordType() {
        return recordType;
    }

    /**
     * Returns the type of the record identifiers.
     */
    public final Class<I> idType() {
        return idType;
    }

    /**
     * Reads the values of all columns specified for the record from the passed source.
     *
     * @param source
     *         the object from which the column values are read
     * @return {@code Map} of column names and their respective values
     */
    protected abstract Map<ColumnName, @Nullable Object> valuesIn(S source);

    /**
     * Reads the identifier value of the record.
     *
     * @param source
     *         the object providing the ID value
     * @return the value of the identifier
     */
    protected abstract I idValueIn(S source);

    /**
     * Finds the column in this specification by the column name.
     *
     * @param name the name of the column to search for
     * @return the column wrapped into {@code Optional},
     * or {@code Optional.empty()} if no column is found
     */
    public abstract Optional<Column<?, ?>> findColumn(ColumnName name);

    /**
     * Finds the column in this specification by the column name.
     *
     * <p>Throws {@link IllegalArgumentException} if no such column exists.
     *
     * @param name
     *         the name of the column to search for
     * @return the column
     * @throws IllegalArgumentException
     *         if the column is not found
     */
    public final Column<?, ?> get(ColumnName name) throws IllegalArgumentException {
        return findColumn(name)
                .orElseThrow(() -> newIllegalArgumentException(
                        "Cannot find the column `%s` in the record specification of type `%s`.",
                        name, recordType));
    }
}
