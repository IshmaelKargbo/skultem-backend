package com.moriba.skultem.application.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.services.SupportService.Status;

class SupportWorkflowTest {

    @Test
    void anOpenTicketCanBeWorkedAndFinished() {
        assertTrue(SupportService.canMove(Status.OPEN, Status.IN_PROGRESS));
        assertTrue(SupportService.canMove(Status.IN_PROGRESS, Status.WAITING));
        assertTrue(SupportService.canMove(Status.WAITING, Status.IN_PROGRESS));
        assertTrue(SupportService.canMove(Status.IN_PROGRESS, Status.RESOLVED));
    }

    @Test
    void aResolvedTicketCanBeReopenedOrClosedButNotRestartedMidway() {
        assertTrue(SupportService.canMove(Status.RESOLVED, Status.OPEN));
        assertTrue(SupportService.canMove(Status.RESOLVED, Status.CLOSED));
        assertFalse(SupportService.canMove(Status.RESOLVED, Status.IN_PROGRESS));
    }

    @Test
    void aClosedTicketIsFinal() {
        for (Status s : Status.values()) {
            assertFalse(SupportService.canMove(Status.CLOSED, s));
        }
    }

    @Test
    void aTicketCannotMoveToWhereItAlreadyIs() {
        for (Status s : Status.values()) {
            assertFalse(SupportService.canMove(s, s));
        }
    }
}
