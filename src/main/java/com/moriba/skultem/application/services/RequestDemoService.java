package com.moriba.skultem.application.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.RequestDemoDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.PageableMapper;
import com.moriba.skultem.application.mapper.RequestDemoMapper;
import com.moriba.skultem.domain.model.RequestDemo;
import com.moriba.skultem.domain.repository.RequestDemoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RequestDemoService {
    
    private final RequestDemoRepository repo;

    public Page<RequestDemoDTO> list(int page, int size) {
        Pageable pageable = PageableMapper.toPage(page, size);
        return repo.findAll(pageable).map(RequestDemoMapper::toDTO);
    }

    public RequestDemoDTO get(String id) {
        var domain = repo.findById(id).orElseThrow(() -> new NotFoundException("demo not not found"));
        return RequestDemoMapper.toDTO(domain);
    }

    public RequestDemoDTO create(String name, String email, String school, String phone, String city, String address, String preferred, String priority, String message) {
        var domain = RequestDemo.create(name, email, school, phone, city, address, preferred, priority, message);
        repo.save(domain);
        return RequestDemoMapper.toDTO(domain);
    }
}
