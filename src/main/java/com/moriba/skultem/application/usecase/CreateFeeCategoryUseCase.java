package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.FeeCategoryDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.mapper.FeeCategoryMapper;
import com.moriba.skultem.domain.model.FeeCategory;
import com.moriba.skultem.domain.repository.FeeCategoryRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateFeeCategoryUseCase {
    private final FeeCategoryRepository repo;
    private final ReferenceGeneratorUsecase rg;

    public FeeCategoryDTO execute(String schoolId, String name, String description) {
        if (repo.existByNameAndSchoolId(name, schoolId)){
            throw new AlreadyExistsException("Fee category already exists");
        }
        
        var id = rg.generate("FEE_CATEGORY", "FEC");
        var fee = FeeCategory.create(id, schoolId, name, description);
        repo.save(fee);
        return FeeCategoryMapper.toDTO(fee);
    }
}
