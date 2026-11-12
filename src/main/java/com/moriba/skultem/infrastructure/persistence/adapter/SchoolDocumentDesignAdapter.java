package com.moriba.skultem.infrastructure.persistence.adapter;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.repository.SchoolDocumentDesignRepository;
import com.moriba.skultem.domain.vo.DocumentKind;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SchoolDocumentDesignAdapter implements SchoolDocumentDesignRepository {
    private final JdbcTemplate jdbc;

    @Override
    public Optional<String> find(String schoolId, DocumentKind kind) {
        return jdbc.queryForList("SELECT template FROM school_document_designs WHERE school_id = ? AND kind = ?",
                String.class, schoolId, kind.name()).stream().findFirst();
    }

    @Override
    public void save(String schoolId, DocumentKind kind, String template, String updatedBy) {
        jdbc.update("""
                INSERT INTO school_document_designs (school_id, kind, template, updated_by, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (school_id, kind) DO UPDATE
                SET template = EXCLUDED.template, updated_by = EXCLUDED.updated_by, updated_at = EXCLUDED.updated_at
                """, schoolId, kind.name(), template, updatedBy, Timestamp.from(Instant.now()));
    }

    @Override
    public Optional<String> findForLevel(String schoolId, DocumentKind kind, com.moriba.skultem.domain.vo.Level level) {
        if (level == null) {
            return Optional.empty();
        }
        return jdbc.queryForList(
                "SELECT template FROM school_level_document_designs WHERE school_id = ? AND kind = ? AND level = ?",
                String.class, schoolId, kind.name(), level.name()).stream().findFirst();
    }

    @Override
    public void saveForLevel(String schoolId, DocumentKind kind, com.moriba.skultem.domain.vo.Level level,
            String template, String updatedBy) {
        jdbc.update("""
                INSERT INTO school_level_document_designs (school_id, kind, level, template, updated_by, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (school_id, kind, level) DO UPDATE
                SET template = EXCLUDED.template, updated_by = EXCLUDED.updated_by, updated_at = EXCLUDED.updated_at
                """, schoolId, kind.name(), level.name(), template, updatedBy, Timestamp.from(Instant.now()));
    }

    @Override
    public void deleteForLevel(String schoolId, DocumentKind kind, com.moriba.skultem.domain.vo.Level level) {
        jdbc.update("DELETE FROM school_level_document_designs WHERE school_id = ? AND kind = ? AND level = ?",
                schoolId, kind.name(), level.name());
    }

    @Override
    public void delete(String schoolId, DocumentKind kind) {
        jdbc.update("DELETE FROM school_document_designs WHERE school_id = ? AND kind = ?", schoolId, kind.name());
    }
}
