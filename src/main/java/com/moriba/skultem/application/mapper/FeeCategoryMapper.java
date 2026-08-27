package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.FeeCategoryDTO;
import com.moriba.skultem.domain.model.FeeCategory;

public class FeeCategoryMapper {
    public static FeeCategoryDTO toDTO(FeeCategory param) {
        if (param == null)
            return null;

        boolean system = com.moriba.skultem.application.usecase.SeedPlatformFeeForAcademicYearUseCase.PLATFORM_FEE_CATEGORY_NAME
                .equals(param.getName());

        return new FeeCategoryDTO(param.getId(), param.getName(), param.getDescription(), system,
                param.getCreatedAt(), param.getUpdatedAt());
    }
}
