package com.moriba.skultem.application.services;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TestimonialDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.PageableMapper;
import com.moriba.skultem.infrastructure.persistence.entity.TestimonialEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.TestimonialJpaRepository;
import com.moriba.skultem.infrastructure.rest.dto.SaveTestimonialDTO;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class TestimonialService {

    private final TestimonialJpaRepository repo;

    public Page<TestimonialDTO> list(int page, int size) {
        return repo.findAll(PageableMapper.toPage(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(TestimonialService::toDTO);
    }

    // What the marketing site shows: published only, featured first.
    public List<TestimonialDTO> listPublished() {
        return repo.findAllByPublishedTrueOrderByFeaturedDescDisplayOrderAscCreatedAtDesc().stream()
                .map(TestimonialService::toDTO).toList();
    }

    public TestimonialDTO create(SaveTestimonialDTO param) {
        var now = Instant.now();
        var entity = TestimonialEntity.builder().id(UUID.randomUUID()).createdAt(now).build();
        apply(entity, param, now);
        return toDTO(repo.save(entity));
    }

    public TestimonialDTO update(String id, SaveTestimonialDTO param) {
        var entity = find(id);
        apply(entity, param, Instant.now());
        return toDTO(repo.save(entity));
    }

    public void delete(String id) {
        repo.delete(find(id));
    }

    private TestimonialEntity find(String id) {
        return repo.findById(parse(id)).orElseThrow(() -> new NotFoundException("Testimonial not found"));
    }

    static UUID parse(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Not found");
        }
    }

    private void apply(TestimonialEntity e, SaveTestimonialDTO p, Instant now) {
        e.setAuthorName(p.authorName().trim());
        e.setAuthorRole(blankToNull(p.authorRole()));
        e.setSchoolName(blankToNull(p.schoolName()));
        e.setQuote(p.quote().trim());
        e.setRating(p.rating());
        e.setPhotoUrl(blankToNull(p.photoUrl()));
        e.setFeatured(p.featured());
        e.setPublished(p.published());
        e.setDisplayOrder(p.displayOrder());
        e.setUpdatedAt(now);
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    static TestimonialDTO toDTO(TestimonialEntity e) {
        return new TestimonialDTO(e.getId().toString(), e.getAuthorName(), e.getAuthorRole(), e.getSchoolName(),
                e.getQuote(), e.getRating(), e.getPhotoUrl(), e.isFeatured(), e.isPublished(), e.getDisplayOrder(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
