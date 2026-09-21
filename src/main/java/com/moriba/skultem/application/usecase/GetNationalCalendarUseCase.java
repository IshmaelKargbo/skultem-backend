package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.NationalCalendarDTO;
import com.moriba.skultem.application.mapper.NationalCalendarMapper;
import com.moriba.skultem.domain.repository.NationalCalendarRepository;

import lombok.RequiredArgsConstructor;

/** The current platform-wide academic calendar, or null if none has been configured yet. */
@Service
@RequiredArgsConstructor
public class GetNationalCalendarUseCase {

    private final NationalCalendarRepository repo;

    public NationalCalendarDTO execute() {
        return repo.findCurrent().map(NationalCalendarMapper::toDTO).orElse(null);
    }
}
