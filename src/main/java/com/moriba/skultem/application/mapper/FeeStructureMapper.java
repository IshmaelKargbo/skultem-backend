package com.moriba.skultem.application.mapper;

import java.util.List;

import com.moriba.skultem.application.dto.AcademicYearDTO;
import com.moriba.skultem.application.dto.ClassDTO;
import com.moriba.skultem.application.dto.FeeCategoryDTO;
import com.moriba.skultem.application.dto.FeeStructureDTO;
import com.moriba.skultem.application.dto.FeeStructureSupplyItemDTO;
import com.moriba.skultem.application.dto.TermDTO;
import com.moriba.skultem.domain.model.FeeStructure;

public class FeeStructureMapper {
    public static FeeStructureDTO toDTO(FeeStructure param) {
        if (param == null) return null;

        ClassDTO clazz = ClassMapper.toDTO(param.getClazz());
        TermDTO term = TermMapper.toDTO(param.getTerm());
        AcademicYearDTO academicYear = AcademicYearMapper.toDTO(param.getAcademicYear());
        FeeCategoryDTO category = FeeCategoryMapper.toDTO(param.getCategory());

        List<FeeStructureSupplyItemDTO> supplyItems = param.getSupplyItems() == null
                ? List.of()
                : param.getSupplyItems().stream()
                        .map(item -> new FeeStructureSupplyItemDTO(MaterialMapper.toDTO(item.getMaterial()),
                                item.getQuantity()))
                        .toList();

        return new FeeStructureDTO(param.getId(), param.getType(), clazz, term, category, param.isAllowInstallment(),
                param.isHasSupply(), supplyItems, param.getDueDate(),
                academicYear, param.getAmount(), param.getDescription(), param.isSystem(), param.isNewStudentsOnly(),
                param.isOldStudentsOnly(), param.getGender(), param.getCreatedAt(), param.getUpdatedAt());
    }
}
