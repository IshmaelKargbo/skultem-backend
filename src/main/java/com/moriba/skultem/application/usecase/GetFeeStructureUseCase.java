package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.FeeStructureDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.FeeStructureMapper;
import com.moriba.skultem.domain.repository.FeeStructureRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetFeeStructureUseCase {
    private final FeeStructureRepository repo;

    public FeeStructureDTO execute(String schoolId, String feeId) {
        var fee = repo.findByIdAndSchoolId(feeId, schoolId)
                .orElseThrow(() -> new NotFoundException("Fee structure not found"));
        return FeeStructureMapper.toDTO(fee);
    }
}
