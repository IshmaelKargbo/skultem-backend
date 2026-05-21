package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.FeeCategory;
import com.moriba.skultem.domain.model.FeeStructure;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.infrastructure.persistence.entity.AcademicYearEntity;
import com.moriba.skultem.infrastructure.persistence.entity.ClassEntity;
import com.moriba.skultem.infrastructure.persistence.entity.FeeCategoryEntity;
import com.moriba.skultem.infrastructure.persistence.entity.FeeStructureEntity;
import com.moriba.skultem.infrastructure.persistence.entity.TermEntity;

public class FeeStructureMapper {
    public static FeeStructure toDomain(FeeStructureEntity param) {
        if (param == null) return null;
        Clazz clazz = null;
        Term term = null;
        FeeCategory category = null;
        AcademicYear academicYear = null;

        if (param.getClazz() != null) {
            clazz = ClassMapper.toDomain(param.getClazz());
        }

        if (param.getTerm() != null) {
            term = TermMapper.toDomain(param.getTerm());
        }

        if (param.getCategory() != null) {
            category = FeeCategoryMapper.toDomain(param.getCategory());
        }

        if (param.getAcademicYear() != null) {
            academicYear = AcademicYearMapper.toDomain(param.getAcademicYear());
        }

        var material = MaterialMapper.toDomain(param.getMaterial());

        return new FeeStructure(param.getId(), param.getSchoolId(), param.getType(), clazz, term, category, academicYear,
                param.isAllowInstallment(), material, param.isHasSupply(), param.getTotalSupply(), param.getDueDate(),
                param.getAmount(), param.getDescription(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static FeeStructureEntity toEntity(FeeStructure param) {
        if (param == null) return null;

        AcademicYearEntity academicYear = null;
        FeeCategoryEntity category = null;
        ClassEntity clazz = null;
        TermEntity term = null;
        
        var material = MaterialMapper.toEntity(param.getMaterial());

        if (param.getClazz() != null) {
            clazz = ClassMapper.toEntity(param.getClazz());
        }

        if (param.getTerm() != null) {
            term = TermMapper.toEntity(param.getTerm());
        }

        if (param.getCategory() != null) {
            category = FeeCategoryMapper.toEntity(param.getCategory());
        }

        if (param.getAcademicYear() != null) {
            academicYear = AcademicYearMapper.toEntity(param.getAcademicYear());
        }

        return FeeStructureEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .academicYear(academicYear)
                .term(term)
                .type(param.getType())
                .material(material)
                .totalSupply(param.getTotalSupply())
                .hasSupply(param.isHasSupply())
                .allowInstallment(param.isAllowInstallment())
                .amount(param.getAmount())
                .category(category)
                .clazz(clazz)
                .description(param.getDescription())
                .dueDate(param.getDueDate())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
