package com.moriba.skultem.infrastructure.persistence.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.FeeCategory;
import com.moriba.skultem.domain.model.FeeStructure;
import com.moriba.skultem.domain.model.FeeStructureSupplyItem;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.infrastructure.persistence.entity.AcademicYearEntity;
import com.moriba.skultem.infrastructure.persistence.entity.ClassEntity;
import com.moriba.skultem.infrastructure.persistence.entity.FeeCategoryEntity;
import com.moriba.skultem.infrastructure.persistence.entity.FeeStructureEntity;
import com.moriba.skultem.infrastructure.persistence.entity.FeeStructureSupplyItemEntity;
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

        return new FeeStructure(param.getId(), param.getSchoolId(), param.getType(), clazz, term, category, academicYear,
                param.isAllowInstallment(), toDomainItems(param.getSupplyItems()), param.isHasSupply(),
                param.getDueDate(), param.getAmount(), param.getDescription(), param.isSystem(),
                param.isNewStudentsOnly(), param.isOldStudentsOnly(), param.getGender(), param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static FeeStructureEntity toEntity(FeeStructure param) {
        if (param == null) return null;

        AcademicYearEntity academicYear = null;
        FeeCategoryEntity category = null;
        ClassEntity clazz = null;
        TermEntity term = null;

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

        var entity = FeeStructureEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .academicYear(academicYear)
                .term(term)
                .type(param.getType())
                .hasSupply(param.isHasSupply())
                .allowInstallment(param.isAllowInstallment())
                .amount(param.getAmount())
                .category(category)
                .clazz(clazz)
                .description(param.getDescription())
                .dueDate(param.getDueDate())
                .system(param.isSystem())
                .newStudentsOnly(param.isNewStudentsOnly())
                .oldStudentsOnly(param.isOldStudentsOnly())
                .gender(param.getGender())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();

        entity.setSupplyItems(toEntityItems(param.getSupplyItems(), param.getSchoolId(), entity));

        return entity;
    }

    private static List<FeeStructureSupplyItem> toDomainItems(List<FeeStructureSupplyItemEntity> items) {
        if (items == null) {
            return List.of();
        }

        List<FeeStructureSupplyItem> result = new ArrayList<>();
        for (var item : items) {
            result.add(new FeeStructureSupplyItem(item.getId(), MaterialMapper.toDomain(item.getMaterial()),
                    item.getQuantity()));
        }
        return result;
    }

    private static List<FeeStructureSupplyItemEntity> toEntityItems(List<FeeStructureSupplyItem> items,
            String schoolId, FeeStructureEntity feeStructure) {
        if (items == null) {
            return List.of();
        }

        List<FeeStructureSupplyItemEntity> result = new ArrayList<>();
        for (var item : items) {
            result.add(FeeStructureSupplyItemEntity.builder()
                    .id(item.getId() == null ? UUID.randomUUID().toString() : item.getId())
                    .schoolId(schoolId)
                    .feeStructure(feeStructure)
                    .material(MaterialMapper.toEntity(item.getMaterial()))
                    .quantity(item.getQuantity())
                    .build());
        }
        return result;
    }
}
