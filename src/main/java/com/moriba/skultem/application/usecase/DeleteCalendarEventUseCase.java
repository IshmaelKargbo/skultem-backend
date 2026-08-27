package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.repository.CalendarEventRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class DeleteCalendarEventUseCase {
    private final CalendarEventRepository repo;

    public void execute(String schoolId, String id) {
        var entry = repo.findByIdAndSchool(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Calendar entry not found"));
        repo.delete(entry);
    }
}
