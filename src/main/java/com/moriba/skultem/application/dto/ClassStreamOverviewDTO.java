package com.moriba.skultem.application.dto;

import java.util.List;

public record ClassStreamOverviewDTO(
                ClassDTO clazz,
                int sectionCount,
                int classMasterCount,
                List<ClassSectionDTO> sections,
                List<ClassMasterRecord> classMasters) {
}
