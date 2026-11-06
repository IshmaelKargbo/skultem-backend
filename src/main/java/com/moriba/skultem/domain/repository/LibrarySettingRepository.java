package com.moriba.skultem.domain.repository;

import java.util.Optional;

import com.moriba.skultem.domain.model.LibrarySetting;

public interface LibrarySettingRepository {
    void save(LibrarySetting domain);

    // The school's own rules (the row with no section).
    Optional<LibrarySetting> findBySchool(String schoolId);

    // One section's own rules, if it has any.
    Optional<LibrarySetting> findBySchoolAndSection(String schoolId, String sectionId);

    void delete(LibrarySetting domain);
}
