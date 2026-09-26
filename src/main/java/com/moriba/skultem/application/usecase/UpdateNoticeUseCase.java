package com.moriba.skultem.application.usecase;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.NoticeDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.NoticeMapper;
import com.moriba.skultem.application.services.NoticeCalendarSync;
import com.moriba.skultem.domain.model.Notice;
import com.moriba.skultem.domain.repository.NoticeRepository;
import com.moriba.skultem.domain.vo.Audience;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateNoticeUseCase {
    private final NoticeRepository repo;
    private final NoticeCalendarSync calendarSync;

    public NoticeDTO execute(String schoolId, String id, String title, String content, Notice.Category category,
            Audience audience, Instant expiresAt, Instant eventAt, Instant eventEndsAt, String eventLocation,
            boolean addToCalendar) {
        NoticeCalendarSync.validate(category, eventAt, eventEndsAt, addToCalendar);

        var notice = repo.findByIdAndSchool(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Notice not found"));

        notice.update(title, content, category, audience, expiresAt, eventAt, eventEndsAt,
                eventLocation == null || eventLocation.isBlank() ? null : eventLocation.trim());
        calendarSync.sync(notice, addToCalendar);
        repo.save(notice);
        return NoticeMapper.toDTO(notice);
    }
}
