package com.moriba.skultem.infrastructure.persistence.adapter;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.repository.ParentPurgeRepository;

import lombok.RequiredArgsConstructor;

// Plain SQL for the same reason as StudentPurgeAdapter: every foreign key is NO ACTION, so the rows
// are removed leaf-first inside the caller's transaction.
@Repository
@RequiredArgsConstructor
public class ParentPurgeAdapter implements ParentPurgeRepository {

    private final JdbcTemplate jdbc;

    @Override
    public long primaryStudentCount(String schoolId, String parentId) {
        Long count = jdbc.queryForObject("SELECT count(*) FROM students WHERE parent_id = ? AND school_id = ?",
                Long.class, parentId, schoolId);
        return count == null ? 0 : count;
    }

    @Override
    public Purged purge(String schoolId, String parentId) {
        String userId = jdbc.queryForObject("SELECT user_id FROM parents WHERE id = ? AND school_id = ?",
                String.class, parentId, schoolId);

        int links = jdbc.update("DELETE FROM student_parents WHERE parent_id = ?", parentId);
        jdbc.update("DELETE FROM parents WHERE id = ? AND school_id = ?", parentId, schoolId);
        jdbc.update("DELETE FROM school_users WHERE school_id = ? AND user_id = ? AND role = 'PARENT'", schoolId,
                userId);

        // The login account: only touched when this parent was its last use.
        if (count("SELECT count(*) FROM school_users WHERE user_id = ?", userId) > 0
                || count("SELECT count(*) FROM parents WHERE user_id = ?", userId) > 0
                || count("SELECT count(*) FROM teachers WHERE user_id = ?", userId) > 0) {
            return new Purged(links, AccountOutcome.KEPT);
        }

        jdbc.update("DELETE FROM user_sessions WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM notifications WHERE user_id = ?", userId);

        // Audit entries, notices and the like still point at accounts that did things - keep those
        // (the record of who did what must survive) but disable the account and free its email.
        boolean referenced = count("SELECT count(*) FROM audit_logs WHERE user_id = ?", userId) > 0
                || count("SELECT count(*) FROM notices WHERE posted_by_user_id = ?", userId) > 0
                || count("SELECT count(*) FROM calendar_events WHERE created_by_user_id = ?", userId) > 0
                || count("SELECT count(*) FROM broadcasts WHERE sent_by_user_id = ?", userId) > 0;
        if (referenced) {
            jdbc.update("UPDATE users SET status = 'DELETED', email = NULL WHERE id = ?", userId);
            return new Purged(links, AccountOutcome.DISABLED);
        }

        jdbc.update("DELETE FROM users WHERE id = ?", userId);
        return new Purged(links, AccountOutcome.DELETED);
    }

    private long count(String sql, String userId) {
        Long value = jdbc.queryForObject(sql, Long.class, userId);
        return value == null ? 0 : value;
    }
}
