package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.HolidayDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.HolidayMapper;
import com.moriba.skultem.domain.repository.HolidayRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetHolidayUseCase {
    private final HolidayRepository repo;

    public HolidayDTO execute(String schoolId, String id) {
        var holiday = repo.findByIdAndSchool(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Holiday not found"));
        return HolidayMapper.toDTO(holiday);
    }
}
