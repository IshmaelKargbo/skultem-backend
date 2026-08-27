package com.moriba.skultem.application.mapper;

import java.util.Map;

import com.moriba.skultem.application.dto.PromotionRequestDTO;
import com.moriba.skultem.application.dto.PromotionRequestItemDTO;
import com.moriba.skultem.domain.model.PromotionRequest;
import com.moriba.skultem.domain.model.PromotionRequestItem;

public class PromotionRequestMapper {

    public static PromotionRequestDTO toDTO(PromotionRequest request) {
        return toDTO(request, null);
    }

    /**
     * @param averagesByEnrollmentId each student's year average, keyed by enrollment id - optional
     *                               since it costs a DB round trip per student and most call sites
     *                               (submit/approve/return/list) don't need it, only the single-
     *                               request review screen does.
     */
    public static PromotionRequestDTO toDTO(PromotionRequest request, Map<String, Double> averagesByEnrollmentId) {
        var session = request.getSession();
        var clazz = session.getClazz();
        var master = request.getMaster();
        var teacherUser = master != null && master.getTeacher() != null ? master.getTeacher().getUser() : null;

        int promoteCount = 0;
        int repeatCount = 0;
        var items = request.getItems().stream().map(item -> {
            var student = item.getStudent();
            String enrollmentId = item.getEnrollment() != null ? item.getEnrollment().getId() : null;
            return new PromotionRequestItemDTO(
                    student != null ? student.getId() : null,
                    enrollmentId,
                    student != null ? student.getName() : null,
                    student != null ? student.getAdmissionNumber() : null,
                    item.getOutcome().name(),
                    item.getRemark(),
                    item.getTargetStream() != null ? item.getTargetStream().getId() : null,
                    item.getTargetStream() != null ? item.getTargetStream().getName() : null,
                    averagesByEnrollmentId != null ? averagesByEnrollmentId.get(enrollmentId) : null);
        }).toList();

        for (var item : request.getItems()) {
            if (item.getOutcome() == PromotionRequestItem.Outcome.PROMOTE) {
                promoteCount++;
            } else {
                repeatCount++;
            }
        }

        String targetClassName = clazz != null && Boolean.TRUE.equals(clazz.getTerminal())
                ? "Graduation"
                : (clazz != null && clazz.getNextClass() != null ? clazz.getNextClass().getName() : null);

        return new PromotionRequestDTO(
                request.getId(),
                session.getId(),
                session.getName(),
                teacherUser != null ? teacherUser.getName() : "Unassigned",
                request.getAcademicYear() != null ? request.getAcademicYear().getName() : null,
                targetClassName,
                request.getStatus().name(),
                request.getTeacherNote(),
                request.getReturnReason(),
                request.getApprovalNote(),
                promoteCount,
                repeatCount,
                request.getPromotedCount(),
                request.getRepeatedCount(),
                request.getCreatedAt(),
                request.getExecutedAt(),
                items);
    }
}
