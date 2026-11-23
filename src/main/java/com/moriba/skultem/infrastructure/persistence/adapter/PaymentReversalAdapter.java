package com.moriba.skultem.infrastructure.persistence.adapter;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.Payment;
import com.moriba.skultem.domain.repository.PaymentReversalRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PaymentReversalAdapter implements PaymentReversalRepository {

    private final JdbcTemplate jdbc;

    @Override
    public void record(Payment p, String reason, String reversedByUserId, Instant reversedAt) {
        var fee = p.getFee();
        jdbc.update("""
                INSERT INTO payment_reversals (id, school_id, payment_id, student_id, fee_id, fee_name, term_name,
                    amount, method, receipt_no, external_reference, paid_at, recorded_by_user_id, reason,
                    reversed_by_user_id, reversed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID().toString(), p.getSchoolId(), p.getId(), p.getStudent().getId(), fee.getId(),
                fee.getCategory() == null ? null : fee.getCategory().getName(),
                fee.getTerm() == null ? null : fee.getTerm().getName(),
                p.getAmount(), p.getMethod() == null ? null : p.getMethod().name(), p.getReferenceNo(),
                p.getExternalReference(), p.getPaidAt() == null ? null : Timestamp.from(p.getPaidAt()),
                p.getRecordedByUserId(), reason, reversedByUserId, Timestamp.from(reversedAt));
    }
}
