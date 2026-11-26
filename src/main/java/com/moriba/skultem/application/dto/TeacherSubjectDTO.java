package com.moriba.skultem.application.dto;

import java.time.Instant;

public record TeacherSubjectDTO(String id, String className, String sessionId, String classId, String sectionName, String sectionId,
                String streamName, String streamId, String teacherName, String teacherId, String subjectName,
                String subjectId, Instant assignedAt, Instant createdAt, Instant updatedAt,
                // The academic year of the class it was for, and whether that is the school's active year.
                String academicYearName, boolean currentYear) {
        public TeacherSubjectDTO(String id, String className, String sessionId, String classId, String sectionName,
                        String sectionId, String streamName, String streamId, String teacherName, String teacherId,
                        String subjectName, String subjectId, Instant assignedAt, Instant createdAt, Instant updatedAt) {
                this(id, className, sessionId, classId, sectionName, sectionId, streamName, streamId, teacherName,
                                teacherId, subjectName, subjectId, assignedAt, createdAt, updatedAt, null, true);
        }


}
