package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.NoticeDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.NoticeMapper;
import com.moriba.skultem.domain.repository.NoticeRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class TogglePinNoticeUseCase {
    private final NoticeRepository repo;

    public NoticeDTO execute(String schoolId, String id) {
        var notice = repo.findByIdAndSchool(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Notice not found"));

        notice.togglePin();
        repo.save(notice);
        return NoticeMapper.toDTO(notice);
    }
}
