package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import com.moriba.skultem.domain.model.Payslip;

public interface PayslipRepository {
    void save(Payslip domain);

    void saveAll(List<Payslip> domains);

    Optional<Payslip> findByIdAndSchoolId(String id, String schoolId);

    Optional<Payslip> findByPayrollRunIdAndTeacherId(String payrollRunId, String teacherId);

    List<Payslip> findAllByPayrollRunId(String payrollRunId);

    List<Payslip> findAllByTeacherIdAndSchoolIdOrderByCreatedAtDesc(String teacherId, String schoolId);
}
