package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.FeeCategoryDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.mapper.FeeCategoryMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.FeeCategory;
import com.moriba.skultem.domain.repository.FeeCategoryRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateFeeCategoryUseCase {
    private final FeeCategoryRepository repo;
    private final ReferenceGeneratorUsecase rg;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "FEE_CATEGORY_CREATED")
    public FeeCategoryDTO execute(String schoolId, String name, String description) {
        if (repo.existByNameAndSchoolId(name, schoolId)) {
            throw new AlreadyExistsException("Fee category already exists");
        }

        var id = rg.generate("FEE_CATEGORY", "FEC");
        var fee = FeeCategory.create(id, schoolId, name, description);
        repo.save(fee);

        logActivityUseCase.log(
                schoolId,
                ActivityType.FEES,
                "Fee category created",
                fee.getName(),
                null,
                fee.getId());

        return FeeCategoryMapper.toDTO(fee);
    }
}
