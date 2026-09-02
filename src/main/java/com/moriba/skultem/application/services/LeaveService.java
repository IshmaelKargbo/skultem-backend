package com.moriba.skultem.application.services;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.LeaveRequestDTO;
import com.moriba.skultem.application.dto.LeaveSummaryDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.LeaveRequestMapper;
import com.moriba.skultem.application.usecase.CreateLeaveRequestUseCase;
import com.moriba.skultem.application.usecase.ReviewLeaveRequestUseCase;
import com.moriba.skultem.domain.model.LeaveRequest;
import com.moriba.skultem.domain.repository.LeaveRequestRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository repo;
    private final TeacherRepository teacherRepo;
    private final CreateLeaveRequestUseCase createLeaveRequestUseCase;
    private final ReviewLeaveRequestUseCase reviewLeaveRequestUseCase;

    public LeaveRequestDTO create(String schoolId, String teacherId, LeaveRequest.Type type, LocalDate startDate,
            LocalDate endDate, String reason) {
        return createLeaveRequestUseCase.execute(schoolId, teacherId, type, startDate, endDate, reason);
    }

    // Self-service: the request is always filed under the calling user's own teacher record,
    // regardless of any teacherId a caller might otherwise try to pass.
    public LeaveRequestDTO createForMe(String schoolId, String userId, LeaveRequest.Type type, LocalDate startDate,
            LocalDate endDate, String reason) {
        var teacher = teacherRepo.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Teacher profile not found for this account"));

        return createLeaveRequestUseCase.execute(schoolId, teacher.getId(), type, startDate, endDate, reason);
    }

    public LeaveRequestDTO getById(String schoolId, String id) {
        var request = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Leave request not found"));

        return LeaveRequestMapper.toDTO(request);
    }

    public Page<LeaveRequestDTO> list(String schoolId, int page, int size, LeaveRequest.Status status,
            LeaveRequest.Type type, String search) {
        Pageable pageable = size > 0 ? PageRequest.of(page - 1, size) : Pageable.unpaged();
        return repo.search(schoolId, status, type, search, pageable).map(LeaveRequestMapper::toDTO);
    }

    public Page<LeaveRequestDTO> listMine(String schoolId, String userId, int page, int size) {
        var teacher = teacherRepo.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Teacher profile not found for this account"));

        Pageable pageable = size > 0 ? PageRequest.of(page - 1, size) : Pageable.unpaged();
        return repo.findAllByTeacherIdAndSchoolId(teacher.getId(), schoolId, pageable).map(LeaveRequestMapper::toDTO);
    }

    public LeaveRequestDTO review(String schoolId, String id, boolean approve, String note) {
        return reviewLeaveRequestUseCase.execute(schoolId, id, approve, note);
    }

    public LeaveSummaryDTO summary(String schoolId) {
        long total = repo.countBySchoolId(schoolId);
        long pending = repo.countBySchoolIdAndStatus(schoolId, LeaveRequest.Status.PENDING);
        long approved = repo.countBySchoolIdAndStatus(schoolId, LeaveRequest.Status.APPROVED);
        long rejected = repo.countBySchoolIdAndStatus(schoolId, LeaveRequest.Status.REJECTED);

        return new LeaveSummaryDTO(total, pending, approved, rejected);
    }
}
