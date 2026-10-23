package com.moriba.skultem.application.services;

import java.time.Instant;
import java.util.ArrayList;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AuditEntryDTO;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * The platform-wide audit log, read-only. Entries are written by the audit aspect as actions happen; nothing here
 * (or anywhere in the API) changes or removes one.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AuditTrailService {

    public record Filter(String search, String status, String schoolId, Instant from, Instant to) {
    }

    private final JdbcTemplate jdbc;

    public Page<AuditEntryDTO> list(Filter f, int page, int size) {
        var where = new StringBuilder(" WHERE 1=1");
        var args = new ArrayList<Object>();

        if (f.search() != null && !f.search().isBlank()) {
            where.append(" AND (lower(a.action) LIKE ? OR lower(coalesce(a.details,'')) LIKE ? OR lower(coalesce(u.email,'')) LIKE ?"
                    + " OR lower(trim(concat_ws(' ', u.given_name, u.family_name))) LIKE ?)");
            String like = "%" + f.search().trim().toLowerCase() + "%";
            for (int i = 0; i < 4; i++) {
                args.add(like);
            }
        }
        if ("SUCCESS".equals(f.status()) || "FAILURE".equals(f.status()) || "REJECTED".equals(f.status())) {
            where.append(" AND a.status = ?");
            args.add(f.status());
        }
        if (f.schoolId() != null && !f.schoolId().isBlank()) {
            where.append(" AND a.school_id = ?");
            args.add(f.schoolId());
        }
        if (f.from() != null) {
            where.append(" AND a.created_at >= ?");
            args.add(java.sql.Timestamp.from(f.from()));
        }
        if (f.to() != null) {
            where.append(" AND a.created_at < ?");
            args.add(java.sql.Timestamp.from(f.to()));
        }

        String from = """
                FROM audit_logs a
                LEFT JOIN schools s ON s.id = a.school_id
                LEFT JOIN users u ON u.id = a.user_id
                """;
        Long total = jdbc.queryForObject("SELECT count(*) " + from + where, Long.class, args.toArray());

        var pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((long) Math.max(page - 1, 0) * size);
        var rows = jdbc.query("""
                SELECT a.id, a.action, a.status, a.details, a.school_id, s.name AS school_name, a.user_id,
                       trim(concat_ws(' ', u.given_name, u.family_name)) AS user_name, u.email,
                       a.ip_address, a.device, a.os, a.browser, a.created_at
                """ + from + where + " ORDER BY a.created_at DESC LIMIT ? OFFSET ?", (rs, i) -> {
                    var at = rs.getTimestamp("created_at");
                    String userName = rs.getString("user_name");
                    return new AuditEntryDTO(rs.getString("id"), rs.getString("action"), rs.getString("status"),
                            rs.getString("details"), rs.getString("school_id"), rs.getString("school_name"),
                            rs.getString("user_id"), userName == null || userName.isBlank() ? null : userName,
                            rs.getString("email"), rs.getString("ip_address"), rs.getString("device"),
                            rs.getString("os"), rs.getString("browser"),
                            at == null ? null : Instant.ofEpochMilli(at.getTime()));
                }, pageArgs.toArray());
        return new PageImpl<>(rows, PageRequest.of(Math.max(page - 1, 0), size), total == null ? 0 : total);
    }
}
