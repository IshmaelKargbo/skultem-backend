package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.repository.StreamUsageRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StreamUsageAdapter implements StreamUsageRepository {

    private final JdbcTemplate jdbc;

    // Every table with a stream column (stream_subjects aside - see deleteOwnSubjects). A new table that points at
    // streams belongs here too.
    private static final Map<String, String> USES = new LinkedHashMap<>();
    static {
        USES.put("class(es)", "SELECT count(*) FROM class_streams WHERE stream_id = ?");
        USES.put("class session(s)", "SELECT count(*) FROM class_sessions WHERE stream_id = ?");
        USES.put("student enrollment(s)", "SELECT count(*) FROM enrollments WHERE stream_id = ?");
        USES.put("class subject(s)", "SELECT count(*) FROM class_subjects WHERE stream_id = ?");
        USES.put("subject group(s)", "SELECT count(*) FROM subject_groups WHERE stream_id = ?");
        USES.put("fee structure(s)", "SELECT count(*) FROM fee_structures WHERE stream_id = ?");
        USES.put("promotion request(s)", "SELECT count(*) FROM promotion_request_items WHERE target_stream_id = ?");
    }

    @Override
    public Map<String, Long> usage(String streamId) {
        Map<String, Long> out = new LinkedHashMap<>();
        USES.forEach((label, sql) -> {
            Long n = jdbc.queryForObject(sql, Long.class, streamId);
            if (n != null && n > 0) {
                out.put(label, n);
            }
        });
        return out;
    }

    @Override
    public void deleteOwnSubjects(String streamId) {
        jdbc.update("DELETE FROM stream_subjects WHERE stream_id = ?", streamId);
    }
}
