package com.moriba.skultem.application.dto;

import java.util.List;

public record OptionGroupResponse(
        String groupId,
        String name,
        int select,
        List<SubjectDTO> list) {
}
