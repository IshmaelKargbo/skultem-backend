package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.FeeDiscount;
import com.moriba.skultem.domain.model.FeeStructure;
import com.moriba.skultem.infrastructure.persistence.entity.EnrollmentEntity;
import com.moriba.skultem.infrastructure.persistence.entity.FeeDiscountEntity;
import com.moriba.skultem.infrastructure.persistence.entity.FeeStructureEntity;

public class FeeDiscountMapper {
    public static FeeDiscount toDomain(FeeDiscountEntity param) {
        if (param == null)
            return null;

        Enrollment enrollment = EnrollmentMapper.toDomain(param.getEnrollment());
        FeeStructure fee = FeeStructureMapper.toDomain(param.getFee());

        return new FeeDiscount(param.getId(), param.getSchoolId(), param.getName(), param.getKind(), param.getValue(),
                enrollment, fee, param.getReason(), param.getExpiryDate(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static FeeDiscountEntity toEntity(FeeDiscount param) {
        if (param == null)
            return null;

        EnrollmentEntity enrollment = EnrollmentMapper.toEntity(param.getEnrollment());
        FeeStructureEntity fee = FeeStructureMapper.toEntity(param.getFee());

        return FeeDiscountEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .name(param.getName())
                .expiryDate(param.getExpiryDate())
                .enrollment(enrollment)
                .reason(param.getReason())
                .kind(param.getKind())
                .fee(fee)
                .value(param.getValue())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
