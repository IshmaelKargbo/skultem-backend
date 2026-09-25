package com.moriba.skultem.application.dto;

import java.util.List;

import com.moriba.skultem.domain.vo.Level;
import com.moriba.skultem.domain.vo.Role;

// A staff member's management-section scope under one role. Empty sectionIds = whole school.
public record StaffScopeDTO(String userId, Role role, List<String> sectionIds) {

    // The signed-in caller's own effective scope - drives what the frontend shows them.
    public record Current(boolean wholeSchool, List<Level> levels, List<String> sectionIds,
            List<String> sectionNames) {
    }
}
