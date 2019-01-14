/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.server.ServerEnvironment.SystemProperty.APP_ENGINE_ENVIRONMENT;
import static io.spine.server.ServerEnvironmentKind.APP_ENGINE_CLOUD;
import static io.spine.server.ServerEnvironmentKind.APP_ENGINE_DEV;
import static io.spine.server.ServerEnvironmentKind.LOCAL;
import static io.spine.testing.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.testing.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("ServerEnvironment should")
class ServerEnvironmentTest {

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(ServerEnvironment.class);
    }

    @Test
    @DisplayName("tell when not running without any specific server environment")
    void tellIfNotInAppEngine() {
        // Tests are not run by AppEngine by default.
        ServerEnvironment environment = ServerEnvironment.getInstance();
        assertEquals(LOCAL, environment.getKind());
    }

    @Nested
    @DisplayName("when running on App Engine cloud infrastructure")
    class OnProdAppEngine extends WithAppEngineEnvironment {

        OnProdAppEngine() {
            super("Production");
        }

        @Test
        @DisplayName("obtain AppEngine environment GAE cloud infrastructure server environment")
        void getAppEngineEnvironment() {
            ServerEnvironment serverEnvironment = ServerEnvironment.getInstance();
            assertEquals(APP_ENGINE_CLOUD, serverEnvironment.getKind());
        }
    }

    @Nested
    @DisplayName("when running on App Engine local server")
    class OnDevAppEngine extends WithAppEngineEnvironment {

        OnDevAppEngine() {
            super("Development");
        }

        @Test
        @DisplayName("obtain AppEngine environment GAE local dev server environment")
        void getAppEngineEnvironment() {
            ServerEnvironment serverEnvironment = ServerEnvironment.getInstance();
            assertEquals(APP_ENGINE_DEV, serverEnvironment.getKind());
        }
    }

    @SuppressWarnings({
            "AccessOfSystemProperties" /* Testing the configuration loaded from System properties. */,
            "AbstractClassWithoutAbstractMethods" /* A test base with setUp and tearDown. */
    })
    abstract class WithAppEngineEnvironment {

        private final String targetEnvironment;

        private String initialValue;

        WithAppEngineEnvironment(String targetEnvironment) {
            this.targetEnvironment = targetEnvironment;
        }

        @BeforeEach
        void setUp() {
            initialValue = System.getProperty(APP_ENGINE_ENVIRONMENT.path());
            System.setProperty(APP_ENGINE_ENVIRONMENT.path(), targetEnvironment);
        }

        @AfterEach
        void tearDown() {
            if (initialValue == null) {
                System.clearProperty(APP_ENGINE_ENVIRONMENT.path());
            } else {
                System.setProperty(APP_ENGINE_ENVIRONMENT.path(), initialValue);
            }
        }
    }
}
