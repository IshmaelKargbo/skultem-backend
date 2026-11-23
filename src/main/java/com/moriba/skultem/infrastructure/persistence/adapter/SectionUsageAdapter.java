package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.repository.SectionUsageRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SectionUsageAdapter implements SectionUsageRepository {

    private final JdbcTemplate jdbc;

    // Every table with a section_id column. (management_section_id is a different thing - school sections like
    // Primary/Secondary - and is not checked here.) A new table that points at sections belongs here too.
    private static final Map<String, String> USES = new LinkedHashMap<>();
    static {
        USES.put("class(es)", "SELECT count(*) FROM class_sections WHERE section_id = ?");
        USES.put("class session(s)", "SELECT count(*) FROM class_sessions WHERE section_id = ?");
        USES.put("student enrollment(s)", "SELECT count(*) FROM enrollments WHERE section_id = ?");
    }

    @Override
    public Map<String, Long> usage(String sectionId) {
        Map<String, Long> out = new LinkedHashMap<>();
        USES.forEach((label, sql) -> {
            Long n = jdbc.queryForObject(sql, Long.class, sectionId);
            if (n != null && n > 0) {
                out.put(label, n);
            }
        });
        return out;
    }
}
