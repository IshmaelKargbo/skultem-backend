package com.moriba.skultem.application.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AssignHouseRecord;
import com.moriba.skultem.application.dto.HouseDTO;
import com.moriba.skultem.application.mapper.HouseMapper;
import com.moriba.skultem.application.usecase.AssignHouseUseCase;
import com.moriba.skultem.application.usecase.CreateHouseUseCase;
import com.moriba.skultem.application.usecase.PickBestHouse;
import com.moriba.skultem.domain.repository.HouseRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HouseService {

    private final HouseRepository repo;
    private final CreateHouseUseCase createHouseUseCase;
    private final AssignHouseUseCase assignHouseUseCase;
    private final PickBestHouse pickBestHouse;

    public HouseDTO createHouse(String schoolId, String name, String motto, String color, List<String> masters) {
        return createHouseUseCase.execute(schoolId, name, motto, color, masters);
    }

    public void assignHouse(List<AssignHouseRecord> records, String schoolId) {
        assignHouseUseCase.execute(records, schoolId);
    }

    public void randomAssignHouse(String schoolId, String classId) {
        pickBestHouse.assignHouse(schoolId, classId);
    }

    public Page<HouseDTO> list(String school, int page, int size, String search) {
        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(page - 1, size);
        }

        return repo.search(search, school, pageable).map(HouseMapper::toDTO);
    }
}
