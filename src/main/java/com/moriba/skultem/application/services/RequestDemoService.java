package com.moriba.skultem.application.services;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final LeadService leadService;

    public Page<RequestDemoDTO> list(int page, int size) {
        Pageable pageable = PageableMapper.toPage(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var found = repo.findAll(pageable).map(RequestDemoMapper::toDTO);
        // Each request shows the lead it became (its status, who has it, when the demo is).
        var leads = leadService.byDemoRequests(found.getContent().stream().map(d -> d.id().toString()).toList());
        return found.map(d -> d.withLead(leads.get(d.id().toString())));
    }

    public RequestDemoDTO get(String id) {
        var domain = repo.findById(id).orElseThrow(() -> new NotFoundException("demo not not found"));
        return RequestDemoMapper.toDTO(domain);
    }

    public RequestDemoDTO create(String name, String email, String school, String phone, String city, String address, String preferred, String priority, String message) {
        var domain = RequestDemo.create(name, email, school, phone, city, address, preferred, priority, message);
        repo.save(domain);
        // Every request is an enquiry to follow up: it becomes (or joins) a lead.
        // The request itself is already saved, so a problem making the lead must not fail the visitor's form.
        try {
            leadService.captureDemoRequest(domain.getId(), name, school, email, phone, city, priority, message);
        } catch (RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger(RequestDemoService.class)
                    .error("Could not make a lead from demo request {}: {}", domain.getId(), e.getMessage(), e);
        }
        return RequestDemoMapper.toDTO(domain);
    }
}
