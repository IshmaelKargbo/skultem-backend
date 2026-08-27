package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.CalendarEventDTO;
import com.moriba.skultem.application.mapper.CalendarEventMapper;
import com.moriba.skultem.domain.repository.CalendarEventRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListCalendarEventBySchoolUseCase {
    private final CalendarEventRepository repo;

    public Page<CalendarEventDTO> execute(String schoolId, int page, int size) {
        Pageable pageable = Pageable.unpaged();
        if (size > 0) {
            pageable = PageRequest.of(page - 1, size);
        }
        return repo.findAllBySchoolId(schoolId, pageable).map(CalendarEventMapper::toDTO);
    }
}
