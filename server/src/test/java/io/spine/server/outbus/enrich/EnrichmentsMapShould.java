/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.outbus.enrich;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMultimap;
import com.google.protobuf.Message;
import io.spine.test.event.EnrichmentByContextFields;
import io.spine.test.event.EnrichmentForSeveralEvents;
import io.spine.test.event.ProjectCreated;
import io.spine.test.event.ProjectCreatedSeparateEnrichment;
import io.spine.test.event.ProjectStarted;
import io.spine.test.event.TaskAdded;
import io.spine.test.event.enrichment.EnrichmentBoundThoughFieldFqnWithFieldsWithDifferentNames;
import io.spine.test.event.enrichment.EnrichmentBoundWithFieldsSeparatedWithSpaces;
import io.spine.test.event.enrichment.EnrichmentBoundWithFieldsWithDifferentNames;
import io.spine.test.event.enrichment.EnrichmentBoundWithFieldsWithDifferentNamesOfWildcardTypes;
import io.spine.test.event.enrichment.EnrichmentBoundWithMultipleFieldsWithDifferentNames;
import io.spine.test.event.enrichment.GranterEventsEnrichment;
import io.spine.test.event.enrichment.MultiplePackageEnrichment;
import io.spine.test.event.enrichment.ProjectCreatedEnrichmentAnotherPackage;
import io.spine.test.event.enrichment.ProjectCreatedEnrichmentAnotherPackageFqn;
import io.spine.test.event.enrichment.ProjectCreatedEnrichmentAnotherPackageFqnAndMsgOpt;
import io.spine.test.event.enrichment.SelectiveComplexEnrichment;
import io.spine.test.event.enrichment.UserPackageEventsEnrichment;
import io.spine.test.event.user.UserDeletedEvent;
import io.spine.test.event.user.UserLoggedInEvent;
import io.spine.test.event.user.UserLoggedOutEvent;
import io.spine.test.event.user.UserMentionedEvent;
import io.spine.test.event.user.permission.PermissionGrantedEvent;
import io.spine.test.event.user.permission.PermissionRevokedEvent;
import io.spine.test.event.user.sharing.SharingRequestApproved;
import io.spine.test.event.user.sharing.SharingRequestSent;
import io.spine.type.TypeName;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Collection;

import static io.spine.server.outbus.enrich.EnrichmentsMap.getEventTypes;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"ClassWithTooManyMethods", "OverlyCoupledClass"})
public class EnrichmentsMapShould {

    @Test
    @DisplayName("have private constructor")
    void havePrivateConstructor() {
        assertHasPrivateParameterlessCtor(EnrichmentsMap.class);
    }

    @Test
    @DisplayName("return map instance")
    void returnMapInstance() {
        final ImmutableMultimap<String, String> map = EnrichmentsMap.getInstance();

        assertFalse(map.isEmpty());
    }

    @Test
    @DisplayName("contain ProjectCreated by ProjectCreatedEnrichment type")
    void containProjectCreatedByProjectCreatedEnrichmentType() {
        assertEnrichmentIsUsedOnlyInEvents(ProjectCreated.Enrichment.class,
                                           ProjectCreated.class);
    }

    @Test
    @DisplayName("contain ProjectCreated by ProjectCreatedSeparateEnrichment type")
    void containProjectCreatedByProjectCreatedSeparateEnrichmentType() {
        assertEnrichmentIsUsedOnlyInEvents(ProjectCreatedSeparateEnrichment.class,
                                           ProjectCreated.class);
    }

    @Test
    @DisplayName("contain ProjectCreated by ProjectCreatedEnrichmentAnotherPackage type")
    void containProjectCreatedByProjectCreatedEnrichmentAnotherPackageType() {
        assertEnrichmentIsUsedOnlyInEvents(ProjectCreatedEnrichmentAnotherPackage.class,
                                           ProjectCreated.class);
    }

    @Test
    @DisplayName("contain ProjectCreated by ProjectCreatedEnrichmentAnotherPackageFqn type")
    void containProjectCreatedByProjectCreatedEnrichmentAnotherPackageFqnType() {
        assertEnrichmentIsUsedOnlyInEvents(ProjectCreatedEnrichmentAnotherPackageFqn.class,
                                           ProjectCreated.class);
    }

    @Test
    @DisplayName("contain ProjectCreated by ProjectCreatedEnrichmentAnotherPackageFqnAndMsgOpt type")
    void containProjectCreatedByProjectCreatedEnrichmentAnotherPackageFqnAndMsgOptType() {
        assertEnrichmentIsUsedOnlyInEvents(
                ProjectCreatedEnrichmentAnotherPackageFqnAndMsgOpt.class,
                ProjectCreated.class);
    }

    @Test
    @DisplayName("contain ProjectStarted by ProjectStartedEnrichment type")
    void containProjectStartedByProjectStartedEnrichmentType() {
        assertEnrichmentIsUsedOnlyInEvents(ProjectStarted.Enrichment.class,
                                           // Event classe
                                           ProjectStarted.class);
    }

    @Test
    @DisplayName("contain events by EnrichmentForSeveralEvents type")
    void containEventsByEnrichmentForSeveralEventsType() {
        assertEnrichmentIsUsedOnlyInEvents(EnrichmentForSeveralEvents.class,
                                           // Event classes
                                           ProjectStarted.class,
                                           ProjectCreated.class,
                                           TaskAdded.class);
    }

    @Test
    @DisplayName("contain ProjectCreated by EnrichmentByContextFields type")
    void containProjectCreatedByEnrichmentByContextFieldsType() {
        assertEnrichmentIsUsedOnlyInEvents(EnrichmentByContextFields.class,
                                           ProjectCreated.class);
    }

    @Test
    @DisplayName("contain all events from package by one enrichment")
    void containAllEventsFromPackageByOneEnrichment() {
        assertEnrichmentIsUsedOnlyInEvents(UserPackageEventsEnrichment.class,
                                           // Event classes
                                           UserLoggedInEvent.class,
                                           UserMentionedEvent.class,
                                           UserLoggedOutEvent.class,
                                           PermissionGrantedEvent.class,
                                           PermissionRevokedEvent.class,
                                           SharingRequestSent.class,
                                           SharingRequestApproved.class);
    }

    @Test
    @DisplayName("contain events from subpackage by enrichment applied to root package")
    void containEventsFromSubpackageByEnrichmentAppliedToRootPackage() {
        assertEnrichmentIsAvailableForEvents(UserPackageEventsEnrichment.class,
                                             // Event classes
                                             PermissionRevokedEvent.class,
                                             PermissionGrantedEvent.class);
    }

    @Test
    @DisplayName("contain only events with target field if declared though package")
    void containOnlyEventsWithTargetFieldIfDeclaredThoughPackage() {
        assertEnrichmentIsUsedOnlyInEvents(GranterEventsEnrichment.class,
                                           // Event class
                                           PermissionGrantedEvent.class);
    }

    @Test
    @DisplayName("contain events from package and standalone event")
    void containEventsFromPackageAndStandaloneEvent() {
        assertEnrichmentIsUsedOnlyInEvents(SelectiveComplexEnrichment.class,
                                           // Event classes
                                           PermissionGrantedEvent.class,
                                           PermissionRevokedEvent.class,
                                           UserLoggedInEvent.class);
    }

    @Test
    @DisplayName("contain events from multiple packages")
    void containEventsFromMultiplePackages() {
        assertEnrichmentIsUsedOnlyInEvents(MultiplePackageEnrichment.class,
                                           // Event classes
                                           PermissionGrantedEvent.class,
                                           PermissionRevokedEvent.class,
                                           SharingRequestSent.class,
                                           SharingRequestApproved.class);
    }

    @Test
    @DisplayName("contain enrichments defined with by with two arguments")
    void containEnrichmentsDefinedWithByWithTwoArguments() {
        assertEnrichmentIsUsedOnlyInEvents(
                EnrichmentBoundWithFieldsWithDifferentNames.class,
                // Event classes
                SharingRequestApproved.class,
                PermissionGrantedEvent.class);
    }

    @Test
    @DisplayName("contain enrichments defined with by with two fqn arguments")
    void containEnrichmentsDefinedWithByWithTwoFqnArguments() {
        assertEnrichmentIsUsedOnlyInEvents(
                EnrichmentBoundThoughFieldFqnWithFieldsWithDifferentNames.class,
                // Event classes
                SharingRequestApproved.class,
                PermissionGrantedEvent.class);
    }

    @Test
    @DisplayName("contain enrichments defined with by with multiple arguments")
    void containEnrichmentsDefinedWithByWithMultipleArguments() {
        assertEnrichmentIsUsedOnlyInEvents(
                EnrichmentBoundWithMultipleFieldsWithDifferentNames.class,
                // Event classes
                SharingRequestApproved.class,
                PermissionGrantedEvent.class,
                UserDeletedEvent.class);
    }

    @Test
    @DisplayName("contain enrichments defined with by with multiple arguments using wildcard")
    void containEnrichmentsDefinedWithByWithMultipleArgumentsUsingWildcard() {
        assertEnrichmentIsUsedOnlyInEvents(
                EnrichmentBoundWithFieldsWithDifferentNamesOfWildcardTypes.class,
                // Event classes
                SharingRequestApproved.class,
                PermissionGrantedEvent.class,
                PermissionRevokedEvent.class);
    }

    @Test
    @DisplayName("contain enrichments defined with by containing separating spaces")
    void containEnrichmentsDefinedWithByContainingSeparatingSpaces() {
        assertEnrichmentIsUsedOnlyInEvents(
                EnrichmentBoundWithFieldsSeparatedWithSpaces.class,
                // Event classes
                TaskAdded.class,
                PermissionGrantedEvent.class);
    }

    @SafeVarargs
    private static void assertEnrichmentIsAvailableForEvents(
            Class<? extends Message> enrichmentClass,
            Class<? extends Message>... eventClassesExpected) {
        final Collection<String> eventTypesActual = getEventTypes(enrichmentClass);

        for (Class<? extends Message> expectedClass : FluentIterable.from(eventClassesExpected)) {
            final String expectedTypeName = TypeName.of(expectedClass)
                                                    .value();
            assertTrue(eventTypesActual.contains(expectedTypeName));
        }
    }

    @SafeVarargs
    private static void assertEnrichmentIsUsedOnlyInEvents(
            Class<? extends Message> enrichmentClass,
            Class<? extends Message>... eventClassesExpected) {
        final Collection<String> eventTypesActual = getEventTypes(enrichmentClass);
        assertEquals(eventClassesExpected.length, eventTypesActual.size());
        assertEnrichmentIsAvailableForEvents(enrichmentClass, eventClassesExpected);
    }
}
