package com.moriba.skultem.application.dto;

import java.time.Instant;

public record ClassSectionDTO(String id, ClassDTO clazz, SectionDTO section, String sectionName, Instant createdAt, Instant updatedAt) {

}
