package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.BehaviourDTO;
import com.moriba.skultem.domain.model.Behaviour;

public class BehaviourMapper {
    public static BehaviourDTO toDTO(Behaviour param) {
        return new BehaviourDTO(param.getId(), param.getEnrollment().getStudent().getName(), param.getKind().name(), param.getCategory().getName(), param.getNote(), param.getCreatedAt(), param.getUpdatedAt());
    }
}
