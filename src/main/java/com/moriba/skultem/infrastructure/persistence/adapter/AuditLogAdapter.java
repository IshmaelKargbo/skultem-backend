package com.moriba.skultem.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.AuditLog;
import com.moriba.skultem.domain.repository.AuditLogRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.AuditLogJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.AuditLogMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AuditLogAdapter implements AuditLogRepository {

    private final AuditLogJpaRepository repo;

    @Override
    public void save(AuditLog domain) {
        var entity = AuditLogMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Page<AuditLog> findAllBySchoolIdAndAcademicYear(String schoolId, String academicYearId,
            Pageable pageable) {
        return repo.findAllBySchoolIdAndAcademicYear_Id(schoolId, academicYearId, pageable)
                .map(AuditLogMapper::toDomain);
    }

}
