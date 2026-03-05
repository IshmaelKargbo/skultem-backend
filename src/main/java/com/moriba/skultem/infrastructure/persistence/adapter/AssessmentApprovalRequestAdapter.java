package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.AssessmentApprovalRequest;
import com.moriba.skultem.domain.repository.AssessmentApprovalRequestRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.AssessmentApprovalRequestJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.AssessmentApprovalRequestMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AssessmentApprovalRequestAdapter implements AssessmentApprovalRequestRepository {
    private final AssessmentApprovalRequestJpaRepository repo;

    @Override
    public void save(AssessmentApprovalRequest domain) {
        var entity = AssessmentApprovalRequestMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public List<AssessmentApprovalRequest> findAllByClassMasterSchoolId(String masterId, String academicYearId) {
        return repo.findAllForClassMasterByTeacherId(masterId, academicYearId).stream()
                .map(AssessmentApprovalRequestMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<AssessmentApprovalRequest> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(AssessmentApprovalRequestMapper::toDomain);
    }

    @Override
    public boolean existsByCycleAndTeacherSubject(String cycleId, String subjectId) {
        return repo.existsByCycle_IdAndTeacherSubject_Id(cycleId, subjectId);
    }

    @Override
    public Optional<AssessmentApprovalRequest> findByCycleAndTeacherSubject(String cycleId, String schoolId) {
        return repo.findByCycle_IdAndTeacherSubject_Id(cycleId, schoolId).map(AssessmentApprovalRequestMapper::toDomain);
    }

}
