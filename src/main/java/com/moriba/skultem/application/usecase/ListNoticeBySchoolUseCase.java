package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.NoticeDTO;
import com.moriba.skultem.application.mapper.NoticeMapper;
import com.moriba.skultem.application.services.CommunicationScopeService;
import com.moriba.skultem.domain.repository.NoticeRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListNoticeBySchoolUseCase {
    private final NoticeRepository repo;
    private final CommunicationScopeService scopeService;

    public Page<NoticeDTO> execute(String schoolId, int page, int size) {
        Pageable pageable = Pageable.unpaged();
        if (size > 0) {
            pageable = PageRequest.of(page - 1, size);
        }
        // Whole-school notices plus those for the sections the caller belongs to (see CommunicationScopeService).
        return repo.findVisibleBySchoolId(schoolId, scopeService.visibleSectionIds(schoolId), pageable)
                .map(NoticeMapper::toDTO);
    }
}
