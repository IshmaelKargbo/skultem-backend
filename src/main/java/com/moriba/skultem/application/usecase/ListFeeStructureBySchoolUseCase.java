package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.FeeStructureDTO;
import com.moriba.skultem.application.mapper.FeeStructureMapper;
import com.moriba.skultem.domain.repository.FeeStructureRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListFeeStructureBySchoolUseCase {
    private final FeeStructureRepository repo;

    public Page<FeeStructureDTO> execute(String schoolId, int page, int size) {
        Pageable pageable = Pageable.unpaged();
        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }
        return repo.findAllBySchool(schoolId, pageable).map(FeeStructureMapper::toDTO);
    }
}
