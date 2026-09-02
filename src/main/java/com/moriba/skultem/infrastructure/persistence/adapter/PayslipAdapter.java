package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.Payslip;
import com.moriba.skultem.domain.repository.PayslipRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.PayslipJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.PayslipMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PayslipAdapter implements PayslipRepository {
    private final PayslipJpaRepository repo;

    @Override
    public void save(Payslip domain) {
        repo.save(PayslipMapper.toEntity(domain));
    }

    @Override
    public void saveAll(List<Payslip> domains) {
        repo.saveAll(domains.stream().map(PayslipMapper::toEntity).toList());
    }

    @Override
    public Optional<Payslip> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(PayslipMapper::toDomain);
    }

    @Override
    public Optional<Payslip> findByPayrollRunIdAndTeacherId(String payrollRunId, String teacherId) {
        return repo.findByPayrollRunIdAndTeacher_Id(payrollRunId, teacherId).map(PayslipMapper::toDomain);
    }

    @Override
    public List<Payslip> findAllByPayrollRunId(String payrollRunId) {
        return repo.findAllByPayrollRunIdOrderByCreatedAtAsc(payrollRunId).stream().map(PayslipMapper::toDomain)
                .toList();
    }

    @Override
    public List<Payslip> findAllByTeacherIdAndSchoolIdOrderByCreatedAtDesc(String teacherId, String schoolId) {
        return repo.findAllByTeacher_IdAndSchoolIdOrderByCreatedAtDesc(teacherId, schoolId).stream()
                .map(PayslipMapper::toDomain).toList();
    }
}
