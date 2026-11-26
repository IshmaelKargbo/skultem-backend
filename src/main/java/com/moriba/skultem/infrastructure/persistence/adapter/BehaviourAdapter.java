package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.Behaviour;
import com.moriba.skultem.domain.repository.BehaviourRepository;
import com.moriba.skultem.domain.vo.KindCount;
import com.moriba.skultem.infrastructure.persistence.jpa.BehaviourJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.BehaviourMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class BehaviourAdapter implements BehaviourRepository {

    private final BehaviourJpaRepository repo;

    @Override
    public java.util.List<Behaviour> findRecentByStudent(String schoolId, String studentId, int limit) {
        return repo.findRecentByStudent(schoolId, studentId, org.springframework.data.domain.PageRequest.of(0, limit))
                .map(BehaviourMapper::toDomain).getContent();
    }

    @Override
    public org.springframework.data.domain.Page<Behaviour> findByStudent(String schoolId, String studentId,
            org.springframework.data.domain.Pageable pageable) {
        return repo.findRecentByStudent(schoolId, studentId, pageable).map(BehaviourMapper::toDomain);
    }

    @Override
    public java.util.Map<String, Long> countByKindForStudent(String schoolId, String studentId) {
        var out = new java.util.HashMap<String, Long>();
        for (Object[] row : repo.countByKindForStudent(schoolId, studentId)) {
            out.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return out;
    }

    @Override
    public boolean existsByEnrollmentIdAndSchoolId(String enrollmentId, String schoolId) {
        return repo.existsByEnrollment_IdAndSchoolId(enrollmentId, schoolId);
    }

    @Override
    public void save(Behaviour domain) {
        var entity = BehaviourMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Page<Behaviour> findAllAcademicYearAndSchoolId(String academicYearId, String schoolId,
            java.util.Collection<com.moriba.skultem.domain.vo.Level> levels, Pageable pageable) {
        return repo
                .findAllByEnrollment_AcademicYear_IdAndSchoolIdAndEnrollment_Clazz_LevelInOrderByCreatedAtAsc(academicYearId,
                        schoolId, levels, pageable)
                .map(BehaviourMapper::toDomain);
    }

    @Override
    public Page<Behaviour> findAllAcademicYearAndClassIdAndSchoolId(String academicYearId, String classId,
            String schoolId, java.util.Collection<com.moriba.skultem.domain.vo.Level> levels, Pageable pageable) {
        return repo.findAllByEnrollment_AcademicYear_IdAndEnrollment_Clazz_IdAndSchoolIdAndEnrollment_Clazz_LevelInOrderByCreatedAtAsc(
                academicYearId, classId, schoolId, levels, pageable).map(BehaviourMapper::toDomain);
    }

    @Override
    public List<KindCount> countByKindForClassOrAll(String academicYearId, String schoolId, String classId,
            java.util.Collection<com.moriba.skultem.domain.vo.Level> levels) {
        return repo.countByKindForClassOrAll(academicYearId, schoolId, classId, levels);
    }
}
