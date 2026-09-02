package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.UserPayrollStatusDTO;
import com.moriba.skultem.domain.repository.TeacherRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetUserPayrollStatusUseCase {
    private final TeacherRepository teacherRepo;

    public UserPayrollStatusDTO execute(String userId) {
        return teacherRepo.findByUserId(userId)
                .map(t -> new UserPayrollStatusDTO(true, t.getId(), t.getStaffId(), t.getDesignation(),
                        t.isTeaching()))
                .orElse(new UserPayrollStatusDTO(false, null, null, null, false));
    }
}
