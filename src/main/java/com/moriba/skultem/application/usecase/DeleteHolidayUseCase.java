package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.repository.HolidayRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class DeleteHolidayUseCase {
    private final HolidayRepository repo;

    public void execute(String schoolId, String id) {
        var holiday = repo.findByIdAndSchool(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Holiday not found"));
        repo.delete(holiday);
    }
}
