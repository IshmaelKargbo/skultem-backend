package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.ExternalExamCandidateEntity;

public interface ExternalExamCandidateJpaRepository extends JpaRepository<ExternalExamCandidateEntity, String> {
    List<ExternalExamCandidateEntity> findAllBySittingIdOrderByStudentNameAsc(String sittingId);
}
