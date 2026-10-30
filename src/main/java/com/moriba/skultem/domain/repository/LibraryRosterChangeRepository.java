package com.moriba.skultem.domain.repository;

import java.util.List;

import com.moriba.skultem.domain.model.LibraryRosterChange;

public interface LibraryRosterChangeRepository {

    void save(LibraryRosterChange domain);

    List<LibraryRosterChange> findBySession(String sessionId, String schoolId);
}
