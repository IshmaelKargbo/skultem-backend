package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.SchemeOfWorkDTO;
import com.moriba.skultem.domain.model.SchemeOfWork;
import com.moriba.skultem.domain.model.Week;

public class SchemeOfWorkMapper {
    public static SchemeOfWorkDTO toDTO(SchemeOfWork param) {
        return toDTO(param, Week.State.NOT_STARTED);
    }

    public static SchemeOfWorkDTO toDTO(SchemeOfWork param, Week.State progressState) {
        if (param == null)
            return null;
        var subject = param.getSubject();
        var term = param.getTerm();
        var session = param.getSession();

        return new SchemeOfWorkDTO(param.getId(), subject.getName(), subject.getId(), term.getName(), term.getId(),
                session.getName(), session.getId(), param.getWeeks(), term.getStartDate(), term.getEndDate(),
                param.getState(), progressState, param.getCreatedAt(), param.getUpdatedAt());
    }
}
