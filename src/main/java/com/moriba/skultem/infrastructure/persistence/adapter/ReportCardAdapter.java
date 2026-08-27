package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.ReportCard;
import com.moriba.skultem.domain.repository.ReportCardRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.ReportCardJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.ReportCardMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ReportCardAdapter implements ReportCardRepository {
    private final ReportCardJpaRepository repo;

    @Override
    public void save(ReportCard domain) {
        repo.save(ReportCardMapper.toEntity(domain));
    }

    @Override
    public Optional<ReportCard> findById(String id) {
        return repo.findById(id).map(ReportCardMapper::toDomain);
    }

    @Override
    public Optional<ReportCard> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(ReportCardMapper::toDomain);
    }

    @Override
    public Optional<ReportCard> findBySchoolIdAndStudentIdAndTermId(String schoolId, String studentId,
            String termId) {
        return repo.findBySchoolIdAndStudentIdAndTermId(schoolId, studentId, termId).map(ReportCardMapper::toDomain);
    }

    @Override
    public Page<ReportCard> search(String schoolId, String classId, String termId, String search,
            Pageable pageable) {
        return repo.search(schoolId, classId, termId, search, pageable).map(ReportCardMapper::toDomain);
    }

    @Override
    public long countBySchoolId(String schoolId) {
        return repo.countBySchoolId(schoolId);
    }

    @Override
    public long countBySchoolIdAndPassed(String schoolId, boolean passed) {
        return repo.countBySchoolIdAndPassed(schoolId, passed);
    }

    @Override
    public long sumDownloadsBySchoolId(String schoolId) {
        return repo.sumDownloadCount(schoolId);
    }
}
