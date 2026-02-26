package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.ClassMapper;
import com.moriba.skultem.domain.repository.ClassRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetClassUseCase {

    private final ClassRepository repo;

    public ClassDTO execute(String schoolId, String id) {
        var record = repo.findByIdAndSchool(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Class not found"));
        return ClassMapper.toDTO(record);
    }
}
