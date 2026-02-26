package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StreamDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.mapper.StreamMapper;
import com.moriba.skultem.domain.model.Stream;
import com.moriba.skultem.domain.repository.StreamRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateStreamUseCase {
    private final StreamRepository repo;
    private final ReferenceGeneratorUsecase rg;

    public StreamDTO execute(String schoolId, String name, String description) {
        if (repo.existsByNameAndSchool(name, schoolId)) {
            throw new AlreadyExistsException("stream already exist");
        }
        var id = rg.generate("STREAM", "STM");
        var record = Stream.create(id, name, schoolId, description);
        repo.save(record);
        return StreamMapper.toDTO(record);
    }
}
