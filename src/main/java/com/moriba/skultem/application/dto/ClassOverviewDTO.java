package com.moriba.skultem.application.dto;

import java.util.List;

public record ClassOverviewDTO(
        ClassDTO clazz,
        int sectionCount,
        int streamCount,
        int classMasterCount,
        List<ClassSectionDTO> sections,
        List<ClassStreamDTO> streams,
        List<ClassMasterDTO> classMasters) {
}
