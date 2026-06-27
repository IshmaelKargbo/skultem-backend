package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.SchemeOfWorkDTO;
import com.moriba.skultem.domain.model.SchemeOfWork;

public class SchemeOfWorkMapper {
    public static SchemeOfWorkDTO toDTO(SchemeOfWork param) {
        if (param == null)
            return null;
        var subject = param.getSubject();
        var term = param.getTerm();
        var session = param.getSession();

        return new SchemeOfWorkDTO(param.getId(), subject.getName(), subject.getId(), term.getName(), param.getId(),
                session.getName(), session.getId(), param.getWeeks(), param.getCreatedAt(), param.getUpdatedAt());
    }
}
