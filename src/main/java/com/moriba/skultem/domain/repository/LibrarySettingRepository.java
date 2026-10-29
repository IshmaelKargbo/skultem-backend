package com.moriba.skultem.domain.repository;

import java.util.Optional;

import com.moriba.skultem.domain.model.LibrarySetting;

public interface LibrarySettingRepository {
    void save(LibrarySetting domain);

    Optional<LibrarySetting> findBySchool(String schoolId);
}
