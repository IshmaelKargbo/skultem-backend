package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.SchoolMapper;
import com.moriba.skultem.domain.repository.SchoolRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetSchoolUseCase {

    private final SchoolRepository repo;

    public SchoolDTO execute(String id) {
        var record = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("School not found"));
        return SchoolMapper.toDTO(record);
    }
}
