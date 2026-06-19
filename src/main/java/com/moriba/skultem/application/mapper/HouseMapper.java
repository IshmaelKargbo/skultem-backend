package com.moriba.skultem.application.mapper;

import java.util.List;

import com.moriba.skultem.application.dto.HouseDTO;
import com.moriba.skultem.application.dto.TeacherDTO;
import com.moriba.skultem.domain.model.House;

public class HouseMapper {
    public static HouseDTO toDTO(House param) {
        List<TeacherDTO> houseMasters = param.getHouseMasters().stream()
                .map(TeacherMapper::toDTO)
                .toList();

        return new HouseDTO(param.getId(), param.getSchoolId(), param.getName(), param.getMotto(), param.getColor(),
                houseMasters, param.getStatus(), param.getCreatedAt(), param.getUpdatedAt());
    }
}
