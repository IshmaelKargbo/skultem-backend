package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StreamSubjectDTO;
import com.moriba.skultem.application.mapper.StreamSubjctMapper;
import com.moriba.skultem.domain.repository.StreamSubjectRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListStreamSubjectBySchoolUseCase {
    private final StreamSubjectRepository repo;

    public Page<StreamSubjectDTO> execute(String schoolId, int page, int size) {
        Pageable pageable = Pageable.unpaged();
        if (size > 0) {
            pageable = PageRequest.of(page, size, Sort.by(
                    Sort.Order.asc("stream.name"),
                    Sort.Order.asc("createdAt")));
        }

        return repo.findBySchoolId(schoolId, pageable).map(StreamSubjctMapper::toDTO);
    }
}
