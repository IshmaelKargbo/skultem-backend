package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.ClassDTO;
import com.moriba.skultem.application.dto.ClassSubjectDTO;
import com.moriba.skultem.application.dto.SubjectDTO;
import com.moriba.skultem.application.dto.SubjectGroupDTO;
import com.moriba.skultem.domain.model.ClassSubject;
import com.moriba.skultem.domain.model.TeacherSubject;

public class ClassSubjectMapper {
    public static ClassSubjectDTO toDTO(ClassSubject param) {
        ClassDTO clazz = ClassMapper.toDTO(param.getClazz());
        SubjectDTO subject = SubjectMapper.toDTO(param.getSubject());
        SubjectGroupDTO group = SubjectGroupMapper.toDTO(param.getGroup());

        String className = clazz.name(), classId = clazz.id();
        String subjectName = subject.name(), subjectId = subject.id();
        String groupName = "N/A", groupId = "";
        int select = 0;
        String teacherName = "N/A", teacherId = "";

        if (group != null) {
            groupName = group.name();
            groupId = group.id();
            select = group.totalSelection();
        }

        return new ClassSubjectDTO(param.getId(), param.getSchoolId(), className, classId, subjectName, subjectId,
                groupName, groupId, teacherName, teacherId,
                param.getMandatory(), param.getLocked(), select);
    }

    public static ClassSubjectDTO toDTO(ClassSubject param, TeacherSubject teacher) {
        ClassDTO clazz = ClassMapper.toDTO(param.getClazz());
        SubjectDTO subject = SubjectMapper.toDTO(param.getSubject());
        SubjectGroupDTO group = SubjectGroupMapper.toDTO(param.getGroup());

        String className = clazz.name(), classId = clazz.id();
        String subjectName = subject.name(), subjectId = subject.id();
        String groupName = "N/A", groupId = "";
        int select = 0;
        String teacherName = "N/A", teacherId = "";

        if (group != null) {
            groupName = group.name();
            groupId = group.id();
            select = group.totalSelection();
        }

        if (teacher != null) {
            teacherName = teacher.getTeacher().getName();
            teacherId = teacher.getId();
        }

        return new ClassSubjectDTO(param.getId(), param.getSchoolId(), className, classId, subjectName, subjectId,
                groupName, groupId, teacherName, teacherId,
                param.getMandatory(), param.getLocked(), select);
    }
}
