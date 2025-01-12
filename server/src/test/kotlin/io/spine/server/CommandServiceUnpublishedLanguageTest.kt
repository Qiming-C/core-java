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

package io.spine.server

import com.google.common.truth.Truth.assertThat
import io.spine.core.Ack
import io.spine.core.Command
import io.spine.core.Status
import io.spine.grpc.MemoizingObserver
import io.spine.grpc.StreamObservers.memoizingObserver
import io.spine.protobuf.Messages.isNotDefault
import io.spine.test.unpublished.command.Halt
import io.spine.testing.client.TestActorRequestFactory
import io.spine.testing.logging.mute.MuteLogging
import io.spine.type.UnpublishedLanguageException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@MuteLogging
internal class `'CommandService' should prohibit using 'internal_type' commands` {

    private lateinit var service: CommandService
    private lateinit var observer: MemoizingObserver<Ack>

    @BeforeEach
    fun initServiceAndObserver() {
        service = CommandService.newBuilder().build()
        observer = memoizingObserver()
    }

    @Test
    fun `returning 'Error' when such a command posted`() {
        val command = createCommand()

        service.post(command, observer)

        assertThat(observer.error).isNull()
        assertThat(observer.isCompleted).isTrue()

        val response = observer.firstResponse()
        assertThat(isNotDefault(response)).isTrue()

        val status = response.status
        assertThat(status.statusCase).isEqualTo(Status.StatusCase.ERROR)

        val error = status.error
        assertThat(error.type)
            .isEqualTo(UnpublishedLanguageException::class.java.canonicalName)
    }

    private fun createCommand(): Command {
        val factory = TestActorRequestFactory(javaClass)
        val commandMessage = Halt.newBuilder().setValue(true).build()
        val command = factory.createCommand(commandMessage)
        return command
    }
}
