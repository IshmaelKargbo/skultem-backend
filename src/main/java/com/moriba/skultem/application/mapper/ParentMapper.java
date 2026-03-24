package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.FeeDetail;
import com.moriba.skultem.application.dto.ParentDTO;
import com.moriba.skultem.application.dto.UserDTO;
import com.moriba.skultem.domain.model.Parent;

public class ParentMapper {
    public static ParentDTO toDTO(Parent param) {
        UserDTO user = UserMapper.toDTO(param.getUser());

        return new ParentDTO(param.getId(), param.getSchoolId(), param.getPhone(), user.name(), user.givenNames(),
                user.familyName(), user.email(), param.getStreet(), param.getCity(), param.getFatherName(),
                param.getMotherName(), param.getStatus().toString(), null, 0, param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static ParentDTO toDTO(Parent param, FeeDetail feeDetail, Integer students) {
        UserDTO user = UserMapper.toDTO(param.getUser());

        return new ParentDTO(param.getId(), param.getSchoolId(), param.getPhone(), user.name(), user.givenNames(),
                user.familyName(), user.email(), param.getStreet(), param.getCity(), param.getFatherName(),
                param.getMotherName(), param.getStatus().toString(), feeDetail, students, param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
