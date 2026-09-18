package com.moriba.skultem.application.dto;

import java.util.List;

// Result of GenerateStudentsRequiringAttentionUseCase - flaggedCount is every enrolled student
// with at least one signal raised (not the whole roster), while students holds only the current
// page/size slice of that flagged set.
public record ClassAcademicAttentionDTO(
        int totalStudents,
        int flaggedCount,
        int page,
        int size,
        List<StudentAcademicAttentionDTO> students) {
}
