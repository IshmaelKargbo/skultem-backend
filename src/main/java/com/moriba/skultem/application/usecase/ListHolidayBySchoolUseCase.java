package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.HolidayDTO;
import com.moriba.skultem.application.mapper.HolidayMapper;
import com.moriba.skultem.domain.repository.HolidayRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListHolidayBySchoolUseCase {
    private final HolidayRepository repo;

    public Page<HolidayDTO> execute(String schoolId, int page, int size) {
        Pageable pageable = Pageable.unpaged();
        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }
        return repo.findAllBySchoolId(schoolId, pageable).map(HolidayMapper::toDTO);
    }
}
