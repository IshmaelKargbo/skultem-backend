package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.UserPayrollStatusDTO;
import com.moriba.skultem.domain.repository.TeacherRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetUserPayrollStatusUseCase {
    private final TeacherRepository teacherRepo;

    // Per school, same lookup clock-in and payslips use - a staff record at another school
    // doesn't count here (and an unscoped lookup errors for someone on staff at two schools).
    public UserPayrollStatusDTO execute(String schoolId, String userId) {
        return teacherRepo.findByUserIdAndSchoolId(userId, schoolId)
                .map(t -> new UserPayrollStatusDTO(true, t.getId(), t.getStaffId(), t.getDesignation(),
                        t.isTeaching()))
                .orElse(new UserPayrollStatusDTO(false, null, null, null, false));
    }
}
