package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.infrastructure.persistence.entity.StudentLedgerEntryEntity;
import com.moriba.skultem.infrastructure.persistence.specs.FilterSpecificationBuilder;

public interface StudentLedgerEntryJpaRepository
                extends JpaRepository<StudentLedgerEntryEntity, String>,
                JpaSpecificationExecutor<StudentLedgerEntryEntity> {

        Optional<StudentLedgerEntryEntity> findTopByStudentIdAndSchoolIdOrderByPaidAtDesc(String studentId,
                        String schoolId);

        Page<StudentLedgerEntryEntity> findAllByStudentIdAndSchoolId(String studentId, String schoolId,
                        Pageable pageable);

        Page<StudentLedgerEntryEntity> findAllByStudentIdAndAcademicYearIdAndSchoolId(String studentId,
                        String acadmicYearId, String schoolId, Pageable pageable);

        Page<StudentLedgerEntryEntity> findAllByAcademicYearIdAndSchoolId(String acadmicYearId, String schoolId,
                        Pageable pageable);

        Page<StudentLedgerEntryEntity> findAllBySchoolIdOrderByPaidAtDesc(String schoolId, Pageable pageable);

        List<StudentLedgerEntryEntity> findAllByStudentIdAndSchoolIdOrderByPaidAtAscCreatedAtAsc(String studentId,
                        String schoolId);

        default Page<StudentLedgerEntryEntity> runReport(String schoolId, List<Filter> filters, Pageable pageable) {
                Specification<StudentLedgerEntryEntity> spec = (root, query, cb) -> cb.equal(root.get("schoolId"), schoolId);

                if (filters != null && !filters.isEmpty()) {
                        spec = spec.and(FilterSpecificationBuilder.build(filters));
                }

                return findAll(spec, pageable);
        }

}
