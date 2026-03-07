package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.TeacherDTO;
import com.moriba.skultem.application.dto.UserDTO;
import com.moriba.skultem.domain.model.Teacher;

public class TeacherMapper {
    public static TeacherDTO toDTO(Teacher param) {
        UserDTO user = UserMapper.toDTO(param.getUser());

        return new TeacherDTO(param.getId(), param.getSchoolId(), param.getPhone(), param.getGender(), param.getTitle(),
                param.getStaffId(), user, param.getStreet(), param.getCity(), param.getStatus().toString(), param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
