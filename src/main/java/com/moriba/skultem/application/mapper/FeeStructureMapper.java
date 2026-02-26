package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.AcademicYearDTO;
import com.moriba.skultem.application.dto.ClassDTO;
import com.moriba.skultem.application.dto.FeeCategoryDTO;
import com.moriba.skultem.application.dto.FeeStructureDTO;
import com.moriba.skultem.application.dto.TermDTO;
import com.moriba.skultem.domain.model.FeeStructure;

public class FeeStructureMapper {
    public static FeeStructureDTO toDTO(FeeStructure param) {
        ClassDTO clazz = ClassMapper.toDTO(param.getClazz());
        TermDTO term = TermMapper.toDTO(param.getTerm());
        AcademicYearDTO academicYear = AcademicYearMapper.toDTO(param.getAcademicYear());
        FeeCategoryDTO category = FeeCategoryMapper.toDTO(param.getCategory());

        return new FeeStructureDTO(param.getId(), clazz, term, category, param.isAllowInstallment(), param.getDueDate(),
                academicYear, param.getAmount(), param.getDescription(), param.getCreatedAt(), param.getUpdatedAt());
    }
}
