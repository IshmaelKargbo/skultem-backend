package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SubjectDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.SubjectMapper;
import com.moriba.skultem.domain.repository.SubjectRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetSubjectUseCase {
    private final SubjectRepository repo;

    public SubjectDTO execute(String id) {
        var res = repo.findById(id).orElseThrow(() -> new NotFoundException("subject not found"));
        return SubjectMapper.toDTO(res);
    }
}
