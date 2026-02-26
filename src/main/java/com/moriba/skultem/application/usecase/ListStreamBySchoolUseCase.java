package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StreamDTO;
import com.moriba.skultem.application.mapper.StreamMapper;
import com.moriba.skultem.domain.repository.StreamRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListStreamBySchoolUseCase {
    private final StreamRepository repo;

    public Page<StreamDTO> execute(String schoolId, int page, int size) {
        Pageable pageable = Pageable.unpaged();
        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }
        return repo.findBySchool(schoolId, pageable).map(StreamMapper::toDTO);
    }
}
