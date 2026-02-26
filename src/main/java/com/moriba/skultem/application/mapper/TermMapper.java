package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.AcademicYearDTO;
import com.moriba.skultem.application.dto.TermDTO;
import com.moriba.skultem.domain.model.Term;

public class TermMapper {
    public static TermDTO toDTO(Term param) {
        AcademicYearDTO academicYear = AcademicYearMapper.toDTO(param.getAcademicYear());
        
        return new TermDTO(param.getId(), param.getSchoolId(), param.getName(), param.getStartDate(),
                param.getEndDate(), param.getTermNumber(), academicYear, param.getStatus().toString(),
                param.getCreatedAt(), param.getUpdatedAt());
    }
}
