package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.TeacherSubjectDTO;
import com.moriba.skultem.domain.model.TeacherSubject;

public class TeacherSubjectMapper {

    public static TeacherSubjectDTO toDTO(TeacherSubject param) {
        if (param == null) return null;

        var session = param.getSession();
        var sessionId = "";
        String className = session.getClazz().getName();
        String classId = session.getClazz().getId();
        String sectionName = session.getSection().getName();
        String sectionId = session.getSection().getId();
        String subjectName = param.getSubject().getName();
        String subjectId = param.getSubject().getId();
        String streamName = "N/A", streamId = "";
        String teacherName = param.getTeacher().getUser().getName();
        String teacherId = param.getTeacher().getId();

        if (session != null && session.getStream() != null) {
            streamName = session.getStream().getName();
            streamId = session.getStream().getId();
        }

        if (session != null) {
           sessionId = session.getId();
        }

        return new TeacherSubjectDTO(param.getId(), className, sessionId, classId, sectionName, sectionId, streamName, streamId,
                teacherName, teacherId, subjectName, subjectId, param.getAssignedAt(), param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
