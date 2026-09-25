package com.moriba.skultem.infrastructure.idempotency;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

// Every method commits on its own (REQUIRES_NEW): the key has to be visible to a concurrent retry
// while the real operation is still running, and must survive that operation failing.
@Component
@RequiredArgsConstructor
public class IdempotencyStore {

    // A finished response is replayable for a day - long enough for any realistic client retry.
    private static final long COMPLETED_TTL_SECONDS = 24 * 60 * 60;
    // A request that never finished (server crashed mid-way) frees its key after this long.
    private static final long IN_PROGRESS_TTL_SECONDS = 10 * 60;

    public enum State {
        NEW, REPLAY, IN_PROGRESS, MISMATCH
    }

    public record Outcome(State state, String responseBody) {
    }

    private final JdbcTemplate jdbc;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Outcome begin(String schoolId, String userId, String operation, String key, String requestHash) {
        var now = Instant.now();

        jdbc.update("""
                DELETE FROM idempotency_keys
                WHERE school_id = ? AND operation = ? AND idem_key = ?
                  AND ((status = 'COMPLETED' AND created_at < ?) OR (status = 'IN_PROGRESS' AND created_at < ?))
                """, schoolId, operation, key, java.sql.Timestamp.from(now.minusSeconds(COMPLETED_TTL_SECONDS)),
                java.sql.Timestamp.from(now.minusSeconds(IN_PROGRESS_TTL_SECONDS)));

        int inserted = jdbc.update("""
                INSERT INTO idempotency_keys (id, school_id, user_id, operation, idem_key, request_hash, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, 'IN_PROGRESS', ?)
                ON CONFLICT (school_id, operation, idem_key) DO NOTHING
                """, UUID.randomUUID().toString(), schoolId, userId, operation, key, requestHash,
                java.sql.Timestamp.from(now));
        if (inserted == 1) {
            return new Outcome(State.NEW, null);
        }

        List<Object[]> rows = jdbc.query("""
                SELECT request_hash, status, response_body FROM idempotency_keys
                WHERE school_id = ? AND operation = ? AND idem_key = ?
                """, (rs, i) -> new Object[] { rs.getString(1), rs.getString(2), rs.getString(3) },
                schoolId, operation, key);
        if (rows.isEmpty()) {
            // Expired between the insert and the read - treat it as unseen.
            return begin(schoolId, userId, operation, key, requestHash);
        }
        var row = rows.get(0);
        if (!requestHash.equals(row[0])) {
            return new Outcome(State.MISMATCH, null);
        }
        if ("COMPLETED".equals(row[1])) {
            return new Outcome(State.REPLAY, (String) row[2]);
        }
        return new Outcome(State.IN_PROGRESS, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(String schoolId, String operation, String key, String responseBody) {
        jdbc.update("""
                UPDATE idempotency_keys SET status = 'COMPLETED', response_body = ?, completed_at = ?
                WHERE school_id = ? AND operation = ? AND idem_key = ?
                """, responseBody, java.sql.Timestamp.from(Instant.now()), schoolId, operation, key);
    }

    // The operation failed - free the key so the client can correct the request and try again.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(String schoolId, String operation, String key) {
        jdbc.update("DELETE FROM idempotency_keys WHERE school_id = ? AND operation = ? AND idem_key = ?",
                schoolId, operation, key);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int purgeExpired() {
        return jdbc.update("DELETE FROM idempotency_keys WHERE created_at < ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(COMPLETED_TTL_SECONDS)));
    }
}
