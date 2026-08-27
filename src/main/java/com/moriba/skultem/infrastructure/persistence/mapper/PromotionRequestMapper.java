package com.moriba.skultem.infrastructure.persistence.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.ClassMaster;
import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.PromotionRequest;
import com.moriba.skultem.domain.model.PromotionRequestItem;
import com.moriba.skultem.domain.model.Stream;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.infrastructure.persistence.entity.AcademicYearEntity;
import com.moriba.skultem.infrastructure.persistence.entity.ClassMasterEntity;
import com.moriba.skultem.infrastructure.persistence.entity.ClassSessionEntity;
import com.moriba.skultem.infrastructure.persistence.entity.EnrollmentEntity;
import com.moriba.skultem.infrastructure.persistence.entity.PromotionRequestEntity;
import com.moriba.skultem.infrastructure.persistence.entity.PromotionRequestItemEntity;
import com.moriba.skultem.infrastructure.persistence.entity.StreamEntity;
import com.moriba.skultem.infrastructure.persistence.entity.StudentEntity;

public class PromotionRequestMapper {

    public static PromotionRequest toDomain(PromotionRequestEntity param) {
        if (param == null) {
            return null;
        }

        ClassSession session = ClassSessionMapper.toDomain(param.getSession());
        ClassMaster master = ClassMasterMapper.toDomain(param.getMaster());
        AcademicYear academicYear = AcademicYearMapper.toDomain(param.getAcademicYear());

        List<PromotionRequestItem> items = new ArrayList<>();
        if (param.getItems() != null) {
            for (var item : param.getItems()) {
                items.add(toDomainItem(item));
            }
        }

        return new PromotionRequest(param.getId(), param.getSchoolId(), session, master, academicYear, items,
                param.getTeacherNote(), param.getReturnReason(), param.getApprovalNote(), param.getStatus(),
                param.getExecutedAt(), param.getPromotedCount(), param.getRepeatedCount(), param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static PromotionRequestEntity toEntity(PromotionRequest param) {
        ClassSessionEntity session = param.getSession() != null ? ClassSessionMapper.toEntity(param.getSession())
                : null;
        ClassMasterEntity master = param.getMaster() != null ? ClassMasterMapper.toEntity(param.getMaster()) : null;
        AcademicYearEntity academicYear = param.getAcademicYear() != null
                ? AcademicYearMapper.toEntity(param.getAcademicYear())
                : null;

        var entity = PromotionRequestEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .session(session)
                .master(master)
                .academicYear(academicYear)
                .status(param.getStatus())
                .teacherNote(param.getTeacherNote())
                .returnReason(param.getReturnReason())
                .approvalNote(param.getApprovalNote())
                .executedAt(param.getExecutedAt())
                .promotedCount(param.getPromotedCount())
                .repeatedCount(param.getRepeatedCount())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();

        List<PromotionRequestItemEntity> items = new ArrayList<>();
        if (param.getItems() != null) {
            for (var item : param.getItems()) {
                items.add(toEntityItem(item, param.getSchoolId(), entity));
            }
        }
        entity.setItems(items);

        return entity;
    }

    private static PromotionRequestItem toDomainItem(PromotionRequestItemEntity param) {
        Student student = StudentMapper.toDomain(param.getStudent());
        Enrollment enrollment = EnrollmentMapper.toDomain(param.getEnrollment());
        Stream targetStream = param.getTargetStream() != null ? StreamMapper.toDomain(param.getTargetStream()) : null;

        return new PromotionRequestItem(param.getId(), student, enrollment, param.getOutcome(), param.getRemark(),
                targetStream);
    }

    private static PromotionRequestItemEntity toEntityItem(PromotionRequestItem param, String schoolId,
            PromotionRequestEntity request) {
        StudentEntity student = param.getStudent() != null ? StudentMapper.toEntity(param.getStudent()) : null;
        EnrollmentEntity enrollment = param.getEnrollment() != null ? EnrollmentMapper.toEntity(param.getEnrollment())
                : null;
        StreamEntity targetStream = param.getTargetStream() != null ? StreamMapper.toEntity(param.getTargetStream())
                : null;

        return PromotionRequestItemEntity.builder()
                .id(param.getId() == null ? UUID.randomUUID().toString() : param.getId())
                .schoolId(schoolId)
                .request(request)
                .student(student)
                .enrollment(enrollment)
                .outcome(param.getOutcome())
                .remark(param.getRemark())
                .targetStream(targetStream)
                .build();
    }
}
