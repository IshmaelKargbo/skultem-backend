package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.time.LocalDate;

import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.domain.shared.AggregateRoot;
import lombok.Getter;

// One payroll cycle for a period (e.g. "June 2026"). DRAFT while its payslips can still be
// included/excluded, GENERATED once amounts are locked in, PUBLISHED once considered official.
@Getter
public class PayrollRun extends AggregateRoot<String> {

    private String schoolId;
    private String period;
    private LocalDate payDate;
    private Status status;

    public enum Status {
        DRAFT,
        GENERATED,
        PUBLISHED
    }

    public PayrollRun(
            String id,
            String schoolId,
            String period,
            LocalDate payDate,
            Status status,
            Instant createdAt,
            Instant updatedAt) {

        super(id, createdAt);
        this.schoolId = schoolId;
        this.period = period;
        this.payDate = payDate;
        this.status = status;

        touch(updatedAt);
    }

    public static PayrollRun create(String id, String schoolId, String period, LocalDate payDate) {
        Instant now = Instant.now();

        return new PayrollRun(id, schoolId, period, payDate, Status.DRAFT, now, now);
    }

    public void generate() {
        if (status != Status.DRAFT) {
            throw new BadRequestException("Only a draft payroll run can be generated");
        }

        this.status = Status.GENERATED;
        touch(Instant.now());
    }

    public void publish() {
        if (status != Status.GENERATED) {
            throw new BadRequestException("Only a generated payroll run can be published");
        }

        this.status = Status.PUBLISHED;
        touch(Instant.now());
    }

    public boolean isDraft() {
        return status == Status.DRAFT;
    }
}
