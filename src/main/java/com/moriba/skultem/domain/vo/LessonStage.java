package com.moriba.skultem.domain.vo;

/**
 * One stage of a lesson note's presentation (typically Introduction,
 * Development and Conclusion), recording what the teacher does and what
 * pupils do at that stage.
 */
public record LessonStage(String stage, String teacherActivity, String pupilsActivity) {

}
