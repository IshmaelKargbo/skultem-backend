package com.moriba.skultem.application.dto;

import java.util.List;

/**
 * What a check-in did. When any selected student can't be checked in, nobody is - {@code checkedIn} is empty
 * and {@code refused} says who and why - unless the librarian asked to check in the eligible students only.
 * <p>
 * {@code warnings} never stop a check-in: a student checked in while their class's library session is going on
 * (AR-8, D-11). The visit stays an independent visit and is not counted on the class register.
 */
public record LibraryCheckInResultDTO(List<LibraryVisitDTO> checkedIn, List<Refusal> refused,
        List<Refusal> warnings) {

    public record Refusal(String studentId, String studentName, String reason) {
    }
}
