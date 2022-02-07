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

package io.spine.server.migration.mirror.given;

import io.spine.server.migration.mirror.MigrationStep;
import io.spine.server.migration.mirror.MigrationSupervisor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MemoizingSupervisor extends MigrationSupervisor {

    private final List<MigrationStep> completedSteps = new ArrayList<>();

    private int startedTimes = 0;
    private int completedTimes = 0;
    private int stepStartedTimes = 0;

    public MemoizingSupervisor(int batchSize) {
        super(batchSize);
    }

    @Override
    public void onStepCompleted(MigrationStep step) {
        completedSteps.add(step);
    }

    public List<MigrationStep> completedSteps() {
        return Collections.unmodifiableList(completedSteps);
    }

    @Override
    public void onMigrationStarted() {
        startedTimes++;
    }

    public int startedTimes() {
        return startedTimes;
    }

    @Override
    public void onMigrationCompleted() {
        completedTimes++;
    }

    public int completedTimes() {
        return completedTimes;
    }

    @Override
    public void onStepStarted() {
        stepStartedTimes++;
    }

    public int stepStartedTimes() {
        return stepStartedTimes;
    }
}
