package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.LibraryRosterChangeEntity;

public interface LibraryRosterChangeJpaRepository extends JpaRepository<LibraryRosterChangeEntity, String> {

    List<LibraryRosterChangeEntity> findBySessionIdAndSchoolIdOrderByChangedAtAsc(String sessionId, String schoolId);
}
