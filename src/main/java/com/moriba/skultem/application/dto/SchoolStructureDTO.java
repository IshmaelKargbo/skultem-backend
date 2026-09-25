package com.moriba.skultem.application.dto;

import java.util.List;

import com.moriba.skultem.domain.model.School.ManagementModel;
import com.moriba.skultem.domain.vo.Address;
import com.moriba.skultem.domain.vo.Level;

// How a school is organized: the levels it offers, and - when SECTION_BASED - the management
// sections grouping them. classCount lets the settings screen say why a level can't be removed.
public record SchoolStructureDTO(ManagementModel managementModel, List<LevelDTO> levels,
        List<ManagementSectionDTO> sections) {

    public record LevelDTO(Level level, String label, String managementSectionId, int classCount) {
    }

    // logo / principalName / principalSignature / address are the section's OWN overrides (null =
    // inherits the school's), not the resolved values - the settings screen needs to tell them apart.
    public record ManagementSectionDTO(String id, String name, int displayOrder, List<Level> levels, String logo,
            String principalName, String principalSignature, Address address) {
    }
}
