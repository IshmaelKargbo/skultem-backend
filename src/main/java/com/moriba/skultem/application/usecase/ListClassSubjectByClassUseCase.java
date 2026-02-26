package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassSubjectDTO;
import com.moriba.skultem.application.mapper.ClassSubjectMapper;
import com.moriba.skultem.domain.repository.ClassSubjectRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListClassSubjectByClassUseCase {

    private final ClassSubjectRepository repo;

    public Page<ClassSubjectDTO> execute(String school, String classId, int page, int size) {
        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }

        return repo.findAllByClassIdAndSchoolId(classId, school, pageable).map(ClassSubjectMapper::toDTO);
    }
}
