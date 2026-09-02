package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.LeaveRequestDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.LeaveRequestMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.LeaveRequestRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ReviewLeaveRequestUseCase {
    private final LeaveRequestRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "LEAVE_REQUEST_REVIEWED")
    public LeaveRequestDTO execute(String schoolId, String id, boolean approve, String note) {
        var request = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Leave request not found"));

        if (approve) {
            request.approve(note);
        } else {
            request.reject(note);
        }

        repo.save(request);

        logActivityUseCase.log(
                schoolId,
                ActivityType.TEACHER,
                approve ? "Leave request approved" : "Leave request rejected",
                request.getTeacher().getUser().getName(),
                null,
                request.getId());

        return LeaveRequestMapper.toDTO(request);
    }
}
