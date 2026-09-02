package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.PayslipEntity;

public interface PayslipJpaRepository extends JpaRepository<PayslipEntity, String> {
    Optional<PayslipEntity> findByIdAndSchoolId(String id, String schoolId);

    Optional<PayslipEntity> findByPayrollRunIdAndTeacher_Id(String payrollRunId, String teacherId);

    List<PayslipEntity> findAllByPayrollRunIdOrderByCreatedAtAsc(String payrollRunId);

    List<PayslipEntity> findAllByTeacher_IdAndSchoolIdOrderByCreatedAtDesc(String teacherId, String schoolId);
}
