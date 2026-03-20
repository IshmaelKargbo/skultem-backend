package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.BehaviourDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.BehaviourMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Behaviour;
import com.moriba.skultem.domain.model.Parent;
import com.moriba.skultem.domain.model.Notification;
import com.moriba.skultem.domain.model.Notification.Type;
import com.moriba.skultem.domain.repository.BehaviourCategoryRepository;
import com.moriba.skultem.domain.repository.BehaviourRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.NotificationRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Kind;
import com.moriba.skultem.domain.vo.Priority;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateBehaviourUseCase {
    private final BehaviourCategoryRepository categoryRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final BehaviourRepository repo;
    private final LogActivityUseCase logActivityUseCase;
    private final NotificationRepository notificationRepo;

    @AuditLogAnnotation(action = "BEHAVIOUR_CREATED")
    public BehaviourDTO execute(String schoolId, String enrollmentId, String categoryId, Kind kind, String note) {

        var enrollment = enrollmentRepo.findByIdAndSchoolId(enrollmentId, schoolId)
                .orElseThrow(() -> new NotFoundException("Student not found"));
        var category = categoryRepo.findByIdAndSchoolId(categoryId, schoolId)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        var id = UUID.randomUUID().toString();
        var domain = Behaviour.create(id, schoolId, enrollment, kind, category, note);
        repo.save(domain);

        var student = enrollment.getStudent();
        logActivityUseCase.log(
                schoolId,
                ActivityType.GRADE,
                "Behaviour recorded",
                student.getGivenNames() + " " + student.getFamilyName() + " - " + category.getName(),
                null,
                domain.getId());

        notifyParentForBehaviour(domain, student.getParent());
        return BehaviourMapper.toDTO(domain);
    }

    private void notifyParentForBehaviour(Behaviour behaviour, Parent parent) {
        if (parent == null) return;

        var student = behaviour.getEnrollment().getStudent();
        var category = behaviour.getCategory();
        var kind = behaviour.getKind();
        var note = behaviour.getNote();

        Map<String, String> meta = Map.of(
            "student_id",      student.getId(),
            "student_name",    student.getName(),
            "behaviour_id",    behaviour.getId(),
            "behaviour_kind",  kind.name(),
            "category_id",     category.getId(),
            "category_name",   category.getName(),
            "note",            note != null ? note : ""
        );

        String title = "Behaviour Update: " + student.getName();

        String message = "A behaviour record has been added for " + student.getName()
                + " under \"" + category.getName() + "\""
                + " (" + kind.name().charAt(0) + kind.name().substring(1).toLowerCase() + ")"
                + (note != null && !note.isBlank() ? ": " + note : ".");

        Priority priority = kind == Kind.NEGATIVE ? Priority.HIGH : Priority.NORMAL;

        Notification notification = Notification.create(
            UUID.randomUUID().toString(),
            behaviour.getSchoolId(),
            parent.getUser(),
            Type.BEHAVIOUR,
            title,
            message,
            meta,
            priority
        );

        notificationRepo.save(notification);
    }
}