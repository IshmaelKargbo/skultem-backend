package com.moriba.skultem.application.dto;

public record StreamSubjectDTO(String id, String schoolId, String streamName, String streamId, String subjectName,
        String subjectId, String groupName, String groupId, Boolean mandatory, Boolean locked) {

}
