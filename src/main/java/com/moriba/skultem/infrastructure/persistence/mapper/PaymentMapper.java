package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.FeeStructure;
import com.moriba.skultem.domain.model.Payment;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.infrastructure.persistence.entity.FeeStructureEntity;
import com.moriba.skultem.infrastructure.persistence.entity.PaymentEntity;
import com.moriba.skultem.infrastructure.persistence.entity.StudentEntity;

public class PaymentMapper {
    public static Payment toDomain(PaymentEntity param) {
        FeeStructure fee = null;
        Student student = null;

        if (param.getFee() != null) {
            fee = FeeStructureMapper.toDomain(param.getFee());
        }

        if (param.getStudent() != null) {
            student = StudentMapper.toDomain(param.getStudent());
        }

        return new Payment(param.getId(), param.getSchoolId(), student, fee, param.getAmount(), param.getMethod(),
                param.getReferenceNo(), param.getNote(), param.getPaidAt(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static PaymentEntity toEntity(Payment param) {
        FeeStructureEntity fee = null;
        StudentEntity student = null;

        if (param.getStudent() != null) {
            student = StudentMapper.toEntity(param.getStudent());
        }

        if (param.getFee() != null) {
            fee = FeeStructureMapper.toEntity(param.getFee());
        }

        if (param.getStudent() != null) {
            student = StudentMapper.toEntity(param.getStudent());
        }

        return PaymentEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .student(student)
                .fee(fee)
                .method(param.getMethod())
                .referenceNo(param.getReferenceNo())
                .note(param.getNote())
                .amount(param.getAmount())
                .paidAt(param.getPaidAt())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
