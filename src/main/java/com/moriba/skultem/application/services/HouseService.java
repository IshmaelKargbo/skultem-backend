package com.moriba.skultem.application.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.HouseDTO;
import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.mapper.HouseMapper;
import com.moriba.skultem.application.usecase.CreateHouseUseCase;
import com.moriba.skultem.domain.repository.HouseRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HouseService {

    private final HouseRepository repo;
    private final CreateHouseUseCase createHouseUseCase;

    public HouseDTO createHouse(String schoolId, String name, String motto, String color, List<String> masters) {
        return createHouseUseCase.execute(schoolId, name, motto, color, masters);
    }

    public Page<HouseDTO> list(String school, int page, int size, String search) {
        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(page - 1, size);
        }

        return repo.search(search, school, pageable).map(HouseMapper::toDTO);
    }
}
