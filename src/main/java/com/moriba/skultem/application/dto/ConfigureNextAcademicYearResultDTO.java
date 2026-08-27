package com.moriba.skultem.application.dto;

import java.util.List;

public record ConfigureNextAcademicYearResultDTO(AcademicYearDTO academicYear, List<TermDTO> terms,
        int feeStructuresCopied) {
}
