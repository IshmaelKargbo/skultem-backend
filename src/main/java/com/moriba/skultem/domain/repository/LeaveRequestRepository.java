package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.LeaveRequest;

public interface LeaveRequestRepository {
    void save(LeaveRequest domain);

    Optional<LeaveRequest> findByIdAndSchoolId(String id, String schoolId);

    Page<LeaveRequest> search(String schoolId, LeaveRequest.Status status, LeaveRequest.Type type, String search,
            Pageable pageable);

    Page<LeaveRequest> findAllByTeacherIdAndSchoolId(String teacherId, String schoolId, Pageable pageable);

    long countBySchoolIdAndStatus(String schoolId, LeaveRequest.Status status);

    long countBySchoolId(String schoolId);
}
