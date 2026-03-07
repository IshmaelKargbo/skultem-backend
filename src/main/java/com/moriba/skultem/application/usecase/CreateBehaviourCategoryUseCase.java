package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.BehaviourCategoryDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.mapper.BehaviourCategoryMapper;
import com.moriba.skultem.domain.model.BehaviourCategory;
import com.moriba.skultem.domain.repository.BehaviourCategoryRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateBehaviourCategoryUseCase {
    private final BehaviourCategoryRepository repo;
    private final ReferenceGeneratorUsecase rg;

    public BehaviourCategoryDTO execute(String schoolId, String name, String description) {
        if (repo.existsByNameAndSchool(name, schoolId)){
            throw new AlreadyExistsException("Behaviour category already exists");
        }
        
        var id = rg.generate("BEHAVIOUR_CATEGORY", "BVC");
        var domain = BehaviourCategory.create(id, schoolId, name, description);
        repo.save(domain);

        return BehaviourCategoryMapper.toDTO(domain);
    }
}
