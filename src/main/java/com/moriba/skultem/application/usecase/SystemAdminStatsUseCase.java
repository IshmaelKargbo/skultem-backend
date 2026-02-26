package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SystemAdminStatsDTO;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SystemAdminStatsUseCase {
    private final SchoolRepository schoolRepo;
    private final UserRepository userRepo;
    private final StudentRepository studentRepo;

    public SystemAdminStatsDTO execute() {
        return new SystemAdminStatsDTO(schoolRepo.countAll(), userRepo.countAll(), studentRepo.countAll());
    }
}
