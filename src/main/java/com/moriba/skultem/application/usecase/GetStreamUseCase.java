package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StreamDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.StreamMapper;
import com.moriba.skultem.domain.repository.StreamRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetStreamUseCase {
    private final StreamRepository repo;

    public StreamDTO execute(String id, String schoolId) {
        var student = repo.findByIdAndSchoolId(id, schoolId).orElseThrow(() -> new NotFoundException("stream not found"));
        return StreamMapper.toDTO(student);
    }
}
