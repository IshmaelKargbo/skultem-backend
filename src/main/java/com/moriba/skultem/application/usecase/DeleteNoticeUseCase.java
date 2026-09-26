package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.services.NoticeCalendarSync;
import com.moriba.skultem.domain.repository.NoticeRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class DeleteNoticeUseCase {
    private final NoticeRepository repo;
    private final NoticeCalendarSync calendarSync;

    public void execute(String schoolId, String id) {
        var notice = repo.findByIdAndSchool(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Notice not found"));
        calendarSync.remove(notice);
        repo.delete(notice);
    }
}
