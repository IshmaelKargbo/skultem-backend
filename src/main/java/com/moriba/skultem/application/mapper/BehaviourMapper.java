package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.BehaviourDTO;
import com.moriba.skultem.domain.model.Behaviour;

public class BehaviourMapper {
    public static BehaviourDTO toDTO(Behaviour param) {
        var e = param.getEnrollment();
        String clazz = e.getClazz() == null ? "" : e.getClazz().getName();
        String year = e.getAcademicYear() == null ? "" : e.getAcademicYear().getName();
        String label = clazz.isEmpty() ? year : (year.isEmpty() ? clazz : clazz + " · " + year);
        return new BehaviourDTO(param.getId(), e.getStudent().getName(), param.getKind().name(), param.getCategory().getName(), param.getNote(), param.getCreatedAt(), param.getUpdatedAt(), label);
    }
}
