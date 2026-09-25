package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.domain.vo.Level;

import lombok.Getter;

// A level the school offers (e.g. this school runs Nursery + Primary). One per (school, level) -
// never duplicated per management section. managementSectionId names the section that manages it
// in a SECTION_BASED school, so every level has exactly one management owner; it's null in a
// UNIFIED school, which has no sections at all.
@Getter
public class SchoolLevel extends AggregateRoot<String> {

    private String schoolId;
    private Level level;
    private String managementSectionId;

    public SchoolLevel(String id, String schoolId, Level level, String managementSectionId, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.level = level;
        this.managementSectionId = managementSectionId;
        touch(updatedAt);
    }

    public static SchoolLevel create(String schoolId, Level level) {
        Instant now = Instant.now();
        return new SchoolLevel(UUID.randomUUID().toString(), schoolId, level, null, now, now);
    }

    public void assignTo(String managementSectionId) {
        this.managementSectionId = managementSectionId;
        touch(Instant.now());
    }
}
