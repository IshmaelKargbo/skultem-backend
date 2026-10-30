package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.LibrarySessionRecordEntity;

public interface LibrarySessionRecordJpaRepository extends JpaRepository<LibrarySessionRecordEntity, String> {

    Optional<LibrarySessionRecordEntity> findByIdAndSchoolId(String id, String schoolId);

    Optional<LibrarySessionRecordEntity> findBySessionIdAndStudentIdAndSchoolId(String sessionId, String studentId,
            String schoolId);

    List<LibrarySessionRecordEntity> findBySessionIdAndSchoolIdOrderByStudentNameAsc(String sessionId,
            String schoolId);

    List<LibrarySessionRecordEntity> findBySessionIdInAndSchoolId(Collection<String> sessionIds, String schoolId);

    List<LibrarySessionRecordEntity> findByStudentIdAndSchoolId(String studentId, String schoolId);
}
