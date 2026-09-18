package com.moriba.skultem.application.dto;

import java.util.List;

// Result of GenerateStudentDemographicsReportUseCase. genderNotSpecified is always 0 today since
// Student.gender is a required field (Gender has no "unspecified" value) - kept here rather than
// omitted so the report shape doesn't need to change if that ever becomes optional.
public record StudentDemographicsDTO(
        int totalStudents,
        int boys,
        int girls,
        int genderNotSpecified,
        List<ReligionCountDTO> religions) {
}
