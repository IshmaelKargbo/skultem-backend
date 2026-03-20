package com.moriba.skultem.application.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import com.moriba.skultem.domain.model.User;

@RequiredArgsConstructor
@Getter
public class GradesReleasedEvent {
    private final String schoolId;
    private final User user;
    private final String studentName;
    private final String subjectName;
    private final String assessmentName;
    private final String termName;
    private final String className;
    private final int score;
    private final Map<String, String> meta;
}