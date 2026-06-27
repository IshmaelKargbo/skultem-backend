package com.moriba.skultem.application.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SchemeOfWorkDTO;
import com.moriba.skultem.application.mapper.PageableMapper;
import com.moriba.skultem.application.mapper.SchemeOfWorkMapper;
import com.moriba.skultem.application.usecase.ManageSchemeOfWorkUseCase;
import com.moriba.skultem.domain.repository.SchemeOfWorkRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurriculumService {

    private final SchemeOfWorkRepository repo;
    private final ManageSchemeOfWorkUseCase manageSchemeOfWorkUseCase;

    public Page<SchemeOfWorkDTO> search(int page, int size, String sessionId) {
        Pageable pageable = PageableMapper.toPageable(page - 1, size);
                
        return repo.findAllBySessionId(sessionId, pageable).map(SchemeOfWorkMapper::toDTO);
    }

    public SchemeOfWorkDTO create(String schoolId, String sessionId, String termId, String subjectId) {
        var res = manageSchemeOfWorkUseCase.execute(schoolId, subjectId, sessionId, termId);
        return SchemeOfWorkMapper.toDTO(res);
    }
}
