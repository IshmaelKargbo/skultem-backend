package com.moriba.skultem.application.dto;

public record ClassSubjectDTO(String id, String schoolId, String className, String classId, String subjectName,
        String subjectId, String streamName, String streamId, String groupName, String groupId, String teacherName, String teacherId,
        Boolean mandatory, Boolean locked, int select) {

}
