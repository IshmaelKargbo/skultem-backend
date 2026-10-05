package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.AssessmentApprovalRequest;
import com.moriba.skultem.domain.model.ClassMaster;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.model.TeacherSubject;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.infrastructure.persistence.entity.AssessmentApprovalRequestEntity;
import com.moriba.skultem.infrastructure.persistence.entity.ClassMasterEntity;
import com.moriba.skultem.infrastructure.persistence.entity.ClassSubjectAssessmentLifeCycleEntity;
import com.moriba.skultem.infrastructure.persistence.entity.TeacherSubjectEntity;
import com.moriba.skultem.infrastructure.persistence.entity.TermEntity;

public class AssessmentApprovalRequestMapper {
    public static AssessmentApprovalRequest toDomain(AssessmentApprovalRequestEntity param) {
        if (param == null) {
            return null;
        }

        // Null where an admin approves the grades and the class has no class master.
        ClassMaster master = param.getMaster() != null ? ClassMasterMapper.toDomain(param.getMaster()) : null;
        ClassSubjectAssessmentLifeCycle cycle = ClassSubjectAssessmentLifeCycleMapper.toDomain(param.getCycle());
        TeacherSubject teacherSubject = TeacherSubjectMapper.toDomain(param.getTeacherSubject());
        Term term = TermMapper.toDomain(param.getTerm());

        return new AssessmentApprovalRequest(param.getId(), param.getSchoolId(), master, cycle, teacherSubject, term,
                param.getTeacherNote(), param.getReturnReason(), param.getApprovalNote(), param.getStatus(),
                param.getCreatedAt(), param.getUpdatedAt());
    }

    public static AssessmentApprovalRequestEntity toEntity(AssessmentApprovalRequest param) {
        if (param == null) {
            return null;
        }

        ClassMasterEntity master = param.getMaster() != null ? ClassMasterMapper.toEntity(param.getMaster()) : null;
        ClassSubjectAssessmentLifeCycleEntity cycle = ClassSubjectAssessmentLifeCycleMapper.toEntity(param.getCycle());
        TeacherSubjectEntity teacherSubject = TeacherSubjectMapper.toEntity(param.getTeacherSubject());
        TermEntity term = TermMapper.toEntity(param.getTerm());

        return AssessmentApprovalRequestEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .cycle(cycle)
                .master(master)
                .teacherSubject(teacherSubject)
                .term(term)
                .status(param.getStatus())
                .teacherNote(param.getTeacherNote())
                .returnReason(param.getReturnReason())
                .approvalNote(param.getApprovalNote())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
