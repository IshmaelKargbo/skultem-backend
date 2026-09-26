package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.BroadcastDTO;
import com.moriba.skultem.application.mapper.BroadcastMapper;
import com.moriba.skultem.application.services.CommunicationScopeService;
import com.moriba.skultem.domain.repository.BroadcastRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListBroadcastBySchoolUseCase {
    private final BroadcastRepository repo;
    private final CommunicationScopeService scopeService;

    public Page<BroadcastDTO> execute(String schoolId, int page, int size) {
        Pageable pageable = Pageable.unpaged();
        if (size > 0) {
            pageable = PageRequest.of(page - 1, size);
        }
        return repo.findVisibleBySchoolId(schoolId, scopeService.visibleSectionIds(schoolId), pageable)
                .map(BroadcastMapper::toDTO);
    }
}
