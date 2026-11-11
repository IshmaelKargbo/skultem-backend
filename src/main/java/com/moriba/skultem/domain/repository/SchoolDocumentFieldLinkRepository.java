package com.moriba.skultem.domain.repository;

import java.util.Optional;

import com.moriba.skultem.domain.vo.DocumentKind;

// A school's links from a design's placeholders to Skultem's data, as JSON (see DocumentFieldLinkService).
public interface SchoolDocumentFieldLinkRepository {
    Optional<String> find(String schoolId, DocumentKind kind, String template);

    void save(String schoolId, DocumentKind kind, String template, String linksJson, String updatedBy);

    void delete(String schoolId, DocumentKind kind, String template);
}
