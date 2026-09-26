package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.CalendarEventDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.CalendarEventMapper;
import com.moriba.skultem.application.services.CommunicationScopeService;
import com.moriba.skultem.domain.repository.CalendarEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetCalendarEventUseCase {
    private final CalendarEventRepository repo;
    private final CommunicationScopeService scopeService;

    public CalendarEventDTO execute(String schoolId, String id) {
        var entry = repo.findByIdAndSchool(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Calendar entry not found"));
        if (!scopeService.canSee(schoolId, entry.getManagementSectionId())) {
            throw new NotFoundException("Calendar entry not found");
        }
        return CalendarEventMapper.toDTO(entry);
    }
}
