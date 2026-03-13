package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.BehaviourDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.BehaviourMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Behaviour;
import com.moriba.skultem.domain.repository.BehaviourCategoryRepository;
import com.moriba.skultem.domain.repository.BehaviourRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Kind;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateBehaviourUseCase {
    private final BehaviourCategoryRepository categoryRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final BehaviourRepository repo;
    private final ReferenceGeneratorUsecase rg;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "BEHAVIOUR_CREATED")
    public BehaviourDTO execute(String schoolId, String enrollmentId, String categoryId, Kind kind, String note) {

        var enrollment = enrollmentRepo.findByIdAndSchoolId(enrollmentId, schoolId)
                .orElseThrow(() -> new NotFoundException("Student not found"));
        var category = categoryRepo.findByIdAndSchoolId(categoryId, schoolId)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        var id = rg.generate("BEHAVIOUR", "BVR");
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

        return BehaviourMapper.toDTO(domain);
    }
}
