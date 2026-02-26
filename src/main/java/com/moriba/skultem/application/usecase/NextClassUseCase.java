package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.ClassMapper;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.repository.ClassRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class NextClassUseCase {

    private final ClassRepository repo;

    public ClassDTO execute(String schoolId, String id, String nextClass) {
        Clazz nextClazz = repo.findByIdAndSchool(nextClass, schoolId)
                .orElseThrow(() -> new NotFoundException("next class not found"));
        Clazz domain = repo.findByIdAndSchool(id, schoolId)
                .orElseThrow(() -> new NotFoundException("next class not found"));

        if (!domain.getSchoolId().equals(schoolId)) {
            throw new NotFoundException("class not in school");
        }

        domain.setNextClass(nextClazz);
        repo.save(domain);

        return ClassMapper.toDTO(domain);
    }
}
