package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.HouseDTO;
import com.moriba.skultem.application.dto.TeacherDTO;
import com.moriba.skultem.domain.model.House;

public class HouseMapper {
    public static HouseDTO toDTO(House param) {
        TeacherDTO master = null;
        if (param.getHouseMaster() != null) {
            master = TeacherMapper.toDTO(param.getHouseMaster());
        }

        return new HouseDTO(param.getId(), param.getSchoolId(), param.getName(), param.getMotto(), param.getColor(), master, param.getStatus(), param.getCreatedAt(), param.getUpdatedAt());
    }
}
