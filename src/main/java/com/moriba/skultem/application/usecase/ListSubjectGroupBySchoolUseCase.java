package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SubjectGroupDTO;
import com.moriba.skultem.application.mapper.SubjectGroupMapper;
import com.moriba.skultem.domain.repository.SubjectGroupRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListSubjectGroupBySchoolUseCase {
    private final SubjectGroupRepository repo;

    public Page<SubjectGroupDTO> execute(String schoolId, int page, int size) {
        Pageable pageable = Pageable.unpaged();
        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }
        return repo.findBySchool(schoolId, pageable).map(SubjectGroupMapper::toDTO);
    }
}
