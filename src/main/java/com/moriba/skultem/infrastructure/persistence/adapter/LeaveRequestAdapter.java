package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.LeaveRequest;
import com.moriba.skultem.domain.repository.LeaveRequestRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.LeaveRequestJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.LeaveRequestMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class LeaveRequestAdapter implements LeaveRequestRepository {
    private final LeaveRequestJpaRepository repo;

    @Override
    public void save(LeaveRequest domain) {
        repo.save(LeaveRequestMapper.toEntity(domain));
    }

    @Override
    public Optional<LeaveRequest> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(LeaveRequestMapper::toDomain);
    }

    @Override
    public Page<LeaveRequest> search(String schoolId, LeaveRequest.Status status, LeaveRequest.Type type,
            String search, Pageable pageable) {
        return repo.search(schoolId, status, type, search, pageable).map(LeaveRequestMapper::toDomain);
    }

    @Override
    public Page<LeaveRequest> findAllByTeacherIdAndSchoolId(String teacherId, String schoolId, Pageable pageable) {
        return repo.findAllByTeacher_IdAndSchoolIdOrderByCreatedAtDesc(teacherId, schoolId, pageable)
                .map(LeaveRequestMapper::toDomain);
    }

    @Override
    public long countBySchoolIdAndStatus(String schoolId, LeaveRequest.Status status) {
        return repo.countBySchoolIdAndStatus(schoolId, status);
    }

    @Override
    public long countBySchoolId(String schoolId) {
        return repo.countBySchoolId(schoolId);
    }
}
