package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.ActivityDTO;
import com.moriba.skultem.domain.model.Activity;

public class ActivityMapper {
    public static ActivityDTO toDTO(Activity param) {
        return new ActivityDTO(
                param.getId(),
                param.getSchoolId(),
                param.getType(),
                param.getTitle(),
                param.getSubject(),
                param.getMeta(),
                param.getReferenceId(),
                param.getCreatedAt());
    }
}
