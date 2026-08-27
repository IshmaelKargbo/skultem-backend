package com.moriba.skultem.application.usecase;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.CalendarEventDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.CalendarEventMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.CalendarEvent;
import com.moriba.skultem.domain.repository.CalendarEventRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateCalendarEventUseCase {
    private final CalendarEventRepository repo;
    private final UserRepository userRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "CALENDAR_EVENT_CREATED")
    public CalendarEventDTO execute(String schoolId, String userId, String title, String description,
            CalendarEvent.Type type, Instant startDate, Instant endDate, String location) {
        var user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        var id = UUID.randomUUID().toString();
        var createdByName = user.getGivenNames() + " " + user.getFamilyName();
        var entry = CalendarEvent.create(id, schoolId, title, description, type, startDate, endDate, location,
                userId, createdByName);
        repo.save(entry);

        logActivityUseCase.log(schoolId, ActivityType.SCHOOL, "Calendar entry added", entry.getTitle(), null,
                entry.getId());

        return CalendarEventMapper.toDTO(entry);
    }
}
