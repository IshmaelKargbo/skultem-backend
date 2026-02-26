package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.FeeDiscount;
import com.moriba.skultem.domain.model.FeeStructure;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.model.StudentFee;
import com.moriba.skultem.infrastructure.persistence.entity.EnrollmentEntity;
import com.moriba.skultem.infrastructure.persistence.entity.FeeDiscountEntity;
import com.moriba.skultem.infrastructure.persistence.entity.FeeStructureEntity;
import com.moriba.skultem.infrastructure.persistence.entity.StudentEntity;
import com.moriba.skultem.infrastructure.persistence.entity.StudentFeeEntity;

public class StudentFeeMapper {
    public static StudentFee toDomain(StudentFeeEntity param) {
        if (param == null) return null;

        Enrollment enrollment = EnrollmentMapper.toDomain(param.getEnrollment());
        FeeStructure fee = FeeStructureMapper.toDomain(param.getFee());
        FeeDiscount discount = FeeDiscountMapper.toDomain(param.getDiscount());
        Student student = StudentMapper.toDomain(param.getStudent());

        return new StudentFee(param.getId(), param.getSchoolId(), enrollment, student, fee, discount, param.getCreatedAt(), param.getUpdatedAt());
    }

    public static StudentFeeEntity toEntity(StudentFee param) {
         if (param == null) return null;

        EnrollmentEntity enrollment = EnrollmentMapper.toEntity(param.getEnrollment());
        FeeStructureEntity fee = FeeStructureMapper.toEntity(param.getFee());
        StudentEntity student = StudentMapper.toEntity(param.getStudent());
        FeeDiscountEntity discount = FeeDiscountMapper.toEntity(param.getDiscount());

        return StudentFeeEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .fee(fee)
                .enrollment(enrollment)
                .discount(discount)
                .student(student)
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
