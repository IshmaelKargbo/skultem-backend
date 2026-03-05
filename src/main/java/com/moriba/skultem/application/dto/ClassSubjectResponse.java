package com.moriba.skultem.application.dto;

import java.util.List;

public record ClassSubjectResponse(
                List<SubjectDTO> core,
                List<OptionGroupResponse> options) {
}