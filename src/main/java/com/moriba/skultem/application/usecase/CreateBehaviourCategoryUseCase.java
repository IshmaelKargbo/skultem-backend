package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.BehaviourCategoryDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.mapper.BehaviourCategoryMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.BehaviourCategory;
import com.moriba.skultem.domain.repository.BehaviourCategoryRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateBehaviourCategoryUseCase {
    private final BehaviourCategoryRepository repo;
    private final ReferenceGeneratorUsecase rg;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "BEHAVIOUR_CATEGORY_CREATED")
    public BehaviourCategoryDTO execute(String schoolId, String name, String description) {
        if (repo.existsByNameAndSchool(name, schoolId)) {
            throw new AlreadyExistsException("Behaviour category already exists");
        }

        var id = rg.generate("BEHAVIOUR_CATEGORY", "BVC");
        var domain = BehaviourCategory.create(id, schoolId, name, description);
        repo.save(domain);

        logActivityUseCase.log(
                schoolId,
                ActivityType.GRADE,
                "Behaviour category created",
                domain.getName(),
                null,
                domain.getId());

        return BehaviourCategoryMapper.toDTO(domain);
    }
}
