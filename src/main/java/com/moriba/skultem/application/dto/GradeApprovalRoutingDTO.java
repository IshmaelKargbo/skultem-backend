package com.moriba.skultem.application.dto;

import java.util.List;

import com.moriba.skultem.domain.vo.GradeApprover;
import com.moriba.skultem.domain.vo.Level;

// Who approves grades, part by part: the whole school in a school without sections, else each management
// section (and any level no section owns). ownChoice: the section set its own approver rather than using the
// school's. Shared by the grades and exam office screens so they all describe the same routing.
public record GradeApprovalRoutingDTO(GradeApprover schoolApprover, boolean sectionBased, List<Route> routes) {

    public record Route(String sectionId, String name, List<Level> levels, GradeApprover approver,
            boolean ownChoice) {
    }
}
