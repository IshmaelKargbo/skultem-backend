package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TermDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.TermMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetActiveTermUseCase {

    private final ResolveActiveTermUseCase resolveActiveTermUseCase;

    public TermDTO execute(String schoolId) {
        var term = resolveActiveTermUseCase.execute(schoolId)
                .orElseThrow(() -> new NotFoundException("Active term not found"));

        return TermMapper.toDTO(term);
    }
}
