package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.NationalCalendarDTO;
import com.moriba.skultem.application.dto.NationalCalendarDTO.NationalTermDTO;
import com.moriba.skultem.domain.model.NationalAcademicYear;

public class NationalCalendarMapper {
    public static NationalCalendarDTO toDTO(NationalAcademicYear param) {
        if (param == null)
            return null;

        var terms = param.getTerms().stream()
                .map(t -> new NationalTermDTO(t.termNumber(), t.name(), t.startDate(), t.endDate()))
                .toList();

        return new NationalCalendarDTO(param.getId(), param.getName(), param.getStartDate(), param.getEndDate(),
                param.isCurrent(), terms, param.getUpdatedAt());
    }
}
