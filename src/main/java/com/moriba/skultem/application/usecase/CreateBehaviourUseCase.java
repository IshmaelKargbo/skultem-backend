package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.BehaviourDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.BehaviourMapper;
import com.moriba.skultem.domain.model.Behaviour;
import com.moriba.skultem.domain.model.vo.Kind;
import com.moriba.skultem.domain.repository.BehaviourCategoryRepository;
import com.moriba.skultem.domain.repository.BehaviourRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;

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

    public BehaviourDTO execute(String schoolId, String enrollmentId, String categoryId, Kind kind, String note) {
       
        var enrollment = enrollmentRepo.findByIdAndSchoolId(enrollmentId, schoolId).orElseThrow(() -> new NotFoundException("Student not found"));
        var category = categoryRepo.findByIdAndSchoolId(categoryId, schoolId).orElseThrow(() -> new NotFoundException("Category not found"));

        var id = rg.generate("BEHAVIOUR", "BVR");
        var domain = Behaviour.create(id, schoolId, enrollment, kind, category, note);
        repo.save(domain);

        return BehaviourMapper.toDTO(domain);
    }
}
