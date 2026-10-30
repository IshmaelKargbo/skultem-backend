package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.LibraryAttendanceCorrectionEntity;

public interface LibraryAttendanceCorrectionJpaRepository
        extends JpaRepository<LibraryAttendanceCorrectionEntity, String> {

    List<LibraryAttendanceCorrectionEntity> findBySessionIdAndSchoolIdOrderByCorrectedAtAsc(String sessionId,
            String schoolId);
}
