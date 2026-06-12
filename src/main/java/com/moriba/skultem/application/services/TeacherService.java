package com.moriba.skultem.application.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TeacherDTO;
import com.moriba.skultem.application.mapper.TeacherMapper;
import com.moriba.skultem.domain.repository.TeacherRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository repo;

    public Page<TeacherDTO> search(String search, int page, int size, String schoolId) {
         Pageable pageable = Pageable.unpaged();
        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }
        
        return repo.search(search, schoolId, pageable).map(TeacherMapper::toDTO);
    }
}
