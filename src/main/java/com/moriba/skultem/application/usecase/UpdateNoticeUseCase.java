package com.moriba.skultem.application.usecase;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.NoticeDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.NoticeMapper;
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

    public NoticeDTO execute(String schoolId, String id, String title, String content, Notice.Category category,
            Audience audience, Instant expiresAt) {
        var notice = repo.findByIdAndSchool(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Notice not found"));

        notice.update(title, content, category, audience, expiresAt);
        repo.save(notice);
        return NoticeMapper.toDTO(notice);
    }
}
