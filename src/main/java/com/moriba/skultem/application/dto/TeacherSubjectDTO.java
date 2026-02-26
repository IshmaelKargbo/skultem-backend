package com.moriba.skultem.application.dto;

import java.time.Instant;

public record TeacherSubjectDTO(String id, String className, String classId, String sectionName, String sectionId,
                String streamName, String streamId, String teacherName, String teacherId, String subjectName,
                String subjectId, Instant assignedAt, Instant createdAt, Instant updatedAt) {

}
