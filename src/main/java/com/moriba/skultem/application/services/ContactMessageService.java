package com.moriba.skultem.application.services;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ContactMessageDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.PageableMapper;
import com.moriba.skultem.infrastructure.persistence.entity.ContactMessageEntity;
import com.moriba.skultem.infrastructure.persistence.entity.ContactMessageEntity.Status;
import com.moriba.skultem.infrastructure.persistence.jpa.ContactMessageJpaRepository;
import com.moriba.skultem.infrastructure.rest.dto.CreateContactMessageDTO;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ContactMessageService {

    private final ContactMessageJpaRepository repo;
    private final LeadService leadService;

    // Public. A filled honeypot is a bot: answer as if it worked, but keep nothing.
    public void submit(CreateContactMessageDTO p) {
        if (p.website() != null && !p.website().isBlank()) {
            return;
        }

        var now = Instant.now();
        var saved = repo.save(ContactMessageEntity.builder()
                .id(UUID.randomUUID())
                .name(p.name().trim())
                .email(p.email().trim())
                .school(p.school().trim())
                .phone(p.phone().trim())
                .challenge(p.challenge().trim())
                .status(Status.NEW)
                .createdAt(now)
                .updatedAt(now)
                .build());
        // Every message is an enquiry to follow up: it becomes (or joins) a lead.
        leadService.captureContactMessage(saved.getId(), saved.getName(), saved.getSchool(), saved.getEmail(),
                saved.getPhone(), saved.getChallenge());
    }

    public Page<ContactMessageDTO> list(int page, int size, String status) {
        var pageable = PageableMapper.toPage(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var entities = status == null || status.isBlank()
                ? repo.findAll(pageable)
                : repo.findAllByStatus(Status.valueOf(status), pageable);
        var found = entities.map(ContactMessageService::toDTO);
        var leads = leadService.byContactMessages(found.getContent().stream().map(ContactMessageDTO::id).toList());
        return found.map(m -> m.withLead(leads.get(m.id())));
    }

    public long countNew() {
        return repo.countByStatus(Status.NEW);
    }

    public ContactMessageDTO updateStatus(String id, String status) {
        var entity = find(id);
        entity.setStatus(Status.valueOf(status));
        entity.setUpdatedAt(Instant.now());
        return toDTO(repo.save(entity));
    }

    public void delete(String id) {
        repo.delete(find(id));
    }

    private ContactMessageEntity find(String id) {
        return repo.findById(TestimonialService.parse(id)).orElseThrow(() -> new NotFoundException("Message not found"));
    }

    private static ContactMessageDTO toDTO(ContactMessageEntity e) {
        return new ContactMessageDTO(e.getId().toString(), e.getName(), e.getEmail(), e.getSchool(), e.getPhone(),
                e.getChallenge(), e.getStatus().name(), e.getCreatedAt(), e.getUpdatedAt(), null);
    }
}
