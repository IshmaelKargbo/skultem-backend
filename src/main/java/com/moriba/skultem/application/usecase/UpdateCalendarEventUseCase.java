package com.moriba.skultem.application.usecase;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.CalendarEventDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.CalendarEventMapper;
import com.moriba.skultem.domain.model.CalendarEvent;
import com.moriba.skultem.domain.repository.CalendarEventRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateCalendarEventUseCase {
    private final CalendarEventRepository repo;

    public CalendarEventDTO execute(String schoolId, String id, String title, String description,
            CalendarEvent.Type type, Instant startDate, Instant endDate, String location) {
        var entry = repo.findByIdAndSchool(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Calendar entry not found"));

        entry.update(title, description, type, startDate, endDate, location);
        repo.save(entry);
        return CalendarEventMapper.toDTO(entry);
    }
}
