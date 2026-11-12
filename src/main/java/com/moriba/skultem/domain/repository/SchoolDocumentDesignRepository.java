package com.moriba.skultem.domain.repository;

import java.util.Optional;

import com.moriba.skultem.domain.vo.DocumentKind;

// The design (an M-Compose template reference) a school picked for a kind of document.
public interface SchoolDocumentDesignRepository {
    Optional<String> find(String schoolId, DocumentKind kind);

    void save(String schoolId, DocumentKind kind, String template, String updatedBy);

    void delete(String schoolId, DocumentKind kind);

    // A level's own design (a Primary report card); none = the school's general design for the kind.
    Optional<String> findForLevel(String schoolId, DocumentKind kind, com.moriba.skultem.domain.vo.Level level);

    void saveForLevel(String schoolId, DocumentKind kind, com.moriba.skultem.domain.vo.Level level, String template,
            String updatedBy);

    void deleteForLevel(String schoolId, DocumentKind kind, com.moriba.skultem.domain.vo.Level level);
}
