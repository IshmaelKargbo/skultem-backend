package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.NoticeDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.NoticeMapper;
import com.moriba.skultem.application.services.CommunicationScopeService;
import com.moriba.skultem.domain.repository.NoticeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetNoticeUseCase {
    private final NoticeRepository repo;
    private final CommunicationScopeService scopeService;

    public NoticeDTO execute(String schoolId, String id) {
        var notice = repo.findByIdAndSchool(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Notice not found"));
        if (!scopeService.canSee(schoolId, notice.getManagementSectionId())) {
            throw new NotFoundException("Notice not found");
        }
        return NoticeMapper.toDTO(notice);
    }
}
