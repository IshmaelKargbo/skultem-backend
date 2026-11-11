package com.moriba.skultem.infrastructure.persistence.adapter;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.repository.SchoolDocumentFieldLinkRepository;
import com.moriba.skultem.domain.vo.DocumentKind;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SchoolDocumentFieldLinkAdapter implements SchoolDocumentFieldLinkRepository {
    private final JdbcTemplate jdbc;

    @Override
    public Optional<String> find(String schoolId, DocumentKind kind, String template) {
        return jdbc.queryForList(
                "SELECT links FROM school_document_field_links WHERE school_id = ? AND kind = ? AND template = ?",
                String.class, schoolId, kind.name(), template).stream().findFirst();
    }

    @Override
    public void save(String schoolId, DocumentKind kind, String template, String linksJson, String updatedBy) {
        jdbc.update("""
                INSERT INTO school_document_field_links (school_id, kind, template, links, updated_by, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (school_id, kind, template) DO UPDATE
                SET links = EXCLUDED.links, updated_by = EXCLUDED.updated_by, updated_at = EXCLUDED.updated_at
                """, schoolId, kind.name(), template, linksJson, updatedBy, Timestamp.from(Instant.now()));
    }

    @Override
    public void delete(String schoolId, DocumentKind kind, String template) {
        jdbc.update("DELETE FROM school_document_field_links WHERE school_id = ? AND kind = ? AND template = ?",
                schoolId, kind.name(), template);
    }
}
