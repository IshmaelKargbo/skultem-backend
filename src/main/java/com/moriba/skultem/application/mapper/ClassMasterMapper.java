package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.ClassMasterDTO;
import com.moriba.skultem.application.dto.TeacherDTO;
import com.moriba.skultem.domain.model.ClassMaster;

public class ClassMasterMapper {
    public static ClassMasterDTO toDTO(ClassMaster param) {
        TeacherDTO teacher = TeacherMapper.toDTO(param.getTeacher());

        return new ClassMasterDTO(param.getId(), null, teacher, param.getAssignAt(),
                param.getEndedAt(), param.getCreatedAt(), param.getUpdatedAt());
    }
}
