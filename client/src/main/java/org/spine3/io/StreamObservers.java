/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
package org.spine3.io;

import io.grpc.stub.StreamObserver;
import org.spine3.annotations.Internal;
import org.spine3.base.Response;

/**
 * A utility class for the routines related to
 * {@linkplain StreamObserver gRPC StreamObserver instances}.
 *
 * @author Alex Tymchenko
 */
@Internal
public class StreamObservers {

    private StreamObservers() {
        // Prevent from instantiation.
    }

    /**
     * The {@code StreamObserver} which does nothing.
     *
     * @see #emptyObserver()
     */
    private static final StreamObserver<Response> emptyObserver = new StreamObserver<Response>() {
        @Override
        public void onNext(Response value) {
            // Do nothing.
        }

        @Override
        public void onError(Throwable t) {
            // Do nothing.
        }

        @Override
        public void onCompleted() {
            // Do nothing.
        }
    };

    /**
     * Creates a {@linkplain StreamObserver<Response> observer} which does nothing upon
     * the invocation of its callback methods.
     *
     * <p>The callees which do not want to follow the responses should use this utility method
     * to eliminate boilerplate code.
     *
     * @return an instance of {@code StreamObserver} which does nothing
     */
    public static StreamObserver<Response> emptyObserver() {
        return emptyObserver;
    }
}