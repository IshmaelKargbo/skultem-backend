package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.ClassMasterDTO;
import com.moriba.skultem.application.dto.ClassMasterRecord;
import com.moriba.skultem.application.dto.ClassSessionRecord;
import com.moriba.skultem.application.dto.TeacherDTO;
import com.moriba.skultem.domain.model.ClassMaster;

public class ClassMasterMapper {
    public static ClassMasterDTO toDTO(ClassMaster param) {
        TeacherDTO teacher = TeacherMapper.toDTO(param.getTeacher());

        return new ClassMasterDTO(param.getId(), null, teacher, param.getAssignAt(),
                param.getEndedAt(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static ClassMasterRecord toRecord(ClassMaster param) {
        TeacherDTO teacher = TeacherMapper.toDTO(param.getTeacher());
        ClassSessionRecord sessionRecord = ClassSessionMapper.toDTO(param.getSession());

        return new ClassMasterRecord(param.getId(), sessionRecord, teacher, param.getAssignAt(), param.getEndedAt(),
                param.getCreatedAt(), param.getUpdatedAt());
    }
}
