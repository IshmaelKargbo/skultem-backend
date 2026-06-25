package com.moriba.skultem.application.services;

import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.SchoolMapper;
import com.moriba.skultem.domain.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClassService {

    private final SchoolRepository repo;

    public SchoolDTO get(String id) {
       var domain = repo.findById(id).orElseThrow(() -> new NotFoundException("school not found"));
       return SchoolMapper.toDTO(domain);
    }

}
