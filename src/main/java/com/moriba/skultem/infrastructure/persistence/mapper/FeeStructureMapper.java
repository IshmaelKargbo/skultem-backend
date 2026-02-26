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

        return new FeeStructure(param.getId(), param.getSchoolId(), clazz, term, category, academicYear,
                param.isAllowInstallment(), param.getDueDate(), param.getAmount(), param.getDescription(),
                param.getCreatedAt(), param.getUpdatedAt());
    }

    public static FeeStructureEntity toEntity(FeeStructure args) {
        AcademicYearEntity academicYear = null;
        FeeCategoryEntity category = null;
        ClassEntity clazz = null;
        TermEntity term = null;

        if (args.getClazz() != null) {
            clazz = ClassMapper.toEntity(args.getClazz());
        }

        if (args.getTerm() != null) {
            term = TermMapper.toEntity(args.getTerm());
        }

        if (args.getCategory() != null) {
            category = FeeCategoryMapper.toEntity(args.getCategory());
        }

        if (args.getAcademicYear() != null) {
            academicYear = AcademicYearMapper.toEntity(args.getAcademicYear());
        }

        return FeeStructureEntity.builder()
                .id(args.getId())
                .schoolId(args.getSchoolId())
                .academicYear(academicYear)
                .term(term)
                .allowInstallment(args.isAllowInstallment())
                .amount(args.getAmount())
                .category(category)
                .clazz(clazz)
                .description(args.getDescription())
                .dueDate(args.getDueDate())
                .createdAt(args.getCreatedAt())
                .updatedAt(args.getUpdatedAt())
                .build();
    }
}
