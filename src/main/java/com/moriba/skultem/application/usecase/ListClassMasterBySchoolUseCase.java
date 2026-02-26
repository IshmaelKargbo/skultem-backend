package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassMasterDTO;
import com.moriba.skultem.application.mapper.ClassMasterMapper;
import com.moriba.skultem.domain.repository.ClassMasterRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListClassMasterBySchoolUseCase {

    private final ClassMasterRepository repo;

    public Page<ClassMasterDTO> execute(String school, int page, int size) {
        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }

        return repo.findBySchool(school, pageable).map(ClassMasterMapper::toDTO);
    }
}
