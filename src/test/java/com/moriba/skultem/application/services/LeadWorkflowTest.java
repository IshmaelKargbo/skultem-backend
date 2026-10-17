package com.moriba.skultem.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.infrastructure.persistence.entity.LeadEntity.Status;
import com.moriba.skultem.infrastructure.persistence.entity.OnboardingStepEntity;

class LeadWorkflowTest {

    @Test
    void aLeadMovesForwardThroughTheSalesSteps() {
        assertTrue(LeadService.canMove(Status.NEW, Status.CONTACTED));
        assertTrue(LeadService.canMove(Status.CONTACTED, Status.DEMO_SCHEDULED));
        assertTrue(LeadService.canMove(Status.DEMO_SCHEDULED, Status.DEMO_COMPLETED));
        assertTrue(LeadService.canMove(Status.DEMO_COMPLETED, Status.INTERESTED));
    }

    @Test
    void youCannotSkipAheadOrReachOnboardingAndConvertedByHand() {
        assertFalse(LeadService.canMove(Status.NEW, Status.INTERESTED));
        assertFalse(LeadService.canMove(Status.NEW, Status.DEMO_COMPLETED));
        // those two have their own actions (start onboarding, convert to a school)
        assertFalse(LeadService.canMove(Status.INTERESTED, Status.ONBOARDING));
        assertFalse(LeadService.canMove(Status.ONBOARDING, Status.CONVERTED));
    }

    @Test
    void anOpenLeadCanBeLostAndALostOneReopened() {
        for (Status s : List.of(Status.NEW, Status.CONTACTED, Status.DEMO_SCHEDULED, Status.DEMO_COMPLETED,
                Status.INTERESTED, Status.ONBOARDING)) {
            assertTrue(LeadService.canMove(s, Status.LOST), s + " should be losable");
        }
        assertTrue(LeadService.canMove(Status.LOST, Status.CONTACTED));
        assertFalse(LeadService.canMove(Status.LOST, Status.NEW));
    }

    @Test
    void aConvertedLeadIsFinal() {
        for (Status s : Status.values()) {
            assertFalse(LeadService.canMove(Status.CONVERTED, s));
        }
    }

    private static OnboardingStepEntity step(boolean applicable, OnboardingStepEntity.Status status) {
        return OnboardingStepEntity.builder().id(UUID.randomUUID()).applicable(applicable).status(status).build();
    }

    @Test
    void onboardingIsCompleteWhenEveryStepThatAppliesIsDone() {
        assertTrue(OnboardingService.isComplete(List.of(
                step(true, OnboardingStepEntity.Status.DONE), step(true, OnboardingStepEntity.Status.DONE))));
        assertFalse(OnboardingService.isComplete(List.of(
                step(true, OnboardingStepEntity.Status.DONE), step(true, OnboardingStepEntity.Status.IN_PROGRESS))));
    }

    @Test
    void aStepSwitchedOffDoesNotHoldUpCompletion() {
        assertTrue(OnboardingService.isComplete(List.of(
                step(true, OnboardingStepEntity.Status.DONE), step(false, OnboardingStepEntity.Status.PENDING))));
    }

    @Test
    void anOnboardingWithNothingThatAppliesIsNotComplete() {
        assertFalse(OnboardingService.isComplete(List.of()));
        assertFalse(OnboardingService.isComplete(List.of(step(false, OnboardingStepEntity.Status.PENDING))));
    }

    @Test
    void theStandardStepsFollowTheSchoolJourneyInOrder() {
        var keys = OnboardingService.STANDARD_STEPS.stream().map(OnboardingService.StepTemplate::key).toList();
        assertEquals("LEAD_RECEIVED", keys.get(0));
        assertEquals("SCHOOL_LAUNCHED", keys.get(keys.size() - 1));
        assertEquals(9, keys.size());
    }
}
