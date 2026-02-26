package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SubjectDTO;
import com.moriba.skultem.application.mapper.SubjectMapper;
import com.moriba.skultem.domain.repository.SubjectRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListSubjectBySchoolUseCase {
    private final SubjectRepository repo;

    public Page<SubjectDTO> execute(String schoolId, int page, int size) {
        Pageable pageable = Pageable.unpaged();
        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }
        return repo.findBySchool(schoolId, pageable).map(SubjectMapper::toDTO);
    }
}
