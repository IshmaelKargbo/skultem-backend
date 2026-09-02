package com.moriba.skultem.application.usecase;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.LeaveRequestDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.LeaveRequestMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.LeaveRequest;
import com.moriba.skultem.domain.repository.LeaveRequestRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateLeaveRequestUseCase {
    private final LeaveRequestRepository repo;
    private final TeacherRepository teacherRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "LEAVE_REQUEST_CREATED")
    public LeaveRequestDTO execute(String schoolId, String teacherId, LeaveRequest.Type type, LocalDate startDate,
            LocalDate endDate, String reason) {

        var teacher = teacherRepo.findByIdAndSchoolId(teacherId, schoolId)
                .orElseThrow(() -> new NotFoundException("Teacher not found"));

        var request = LeaveRequest.create(UUID.randomUUID().toString(), schoolId, teacher, type, startDate, endDate,
                reason);
        repo.save(request);

        logActivityUseCase.log(
                schoolId,
                ActivityType.TEACHER,
                "Leave request submitted",
                teacher.getUser().getName() + " - " + type,
                null,
                request.getId());

        return LeaveRequestMapper.toDTO(request);
    }
}
