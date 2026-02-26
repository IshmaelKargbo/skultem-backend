package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SectionDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.SectionMapper;
import com.moriba.skultem.domain.repository.SectionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetSectionUseCase {

    private final SectionRepository repo;

    public SectionDTO execute(String schoolId, String id) {
        var record = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Section not found"));
        if (!record.getSchoolId().equals(schoolId)) {
            throw new NotFoundException("Section not found");
        }
        return SectionMapper.toDTO(record);
    }
}
