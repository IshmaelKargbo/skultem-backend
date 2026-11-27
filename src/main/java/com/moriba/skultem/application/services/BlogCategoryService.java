package com.moriba.skultem.application.services;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.BlogCategoryDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.infrastructure.persistence.entity.BlogCategoryEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.BlogCategoryJpaRepository;
import com.moriba.skultem.infrastructure.rest.dto.SaveBlogCategoryDTO;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Blog categories. A post sits in at most one; deleting a category leaves its posts uncategorised (the
// database sets their category to null).
@Service
@Transactional
@RequiredArgsConstructor
public class BlogCategoryService {

    private final BlogCategoryJpaRepository repo;

    // Admin: every category with how many posts (and published posts) it holds.
    public List<BlogCategoryDTO> list() {
        var counts = counts();
        return repo.findAllByOrderByDisplayOrderAscNameAsc().stream().map(c -> toDTO(c, counts)).toList();
    }

    // Public: only categories with at least one published post, so the site never links to an empty list.
    public List<BlogCategoryDTO> listPublished() {
        return list().stream().filter(c -> c.publishedCount() > 0).toList();
    }

    public BlogCategoryDTO create(SaveBlogCategoryDTO param) {
        var now = Instant.now();
        var entity = BlogCategoryEntity.builder().id(UUID.randomUUID()).createdAt(now).build();
        apply(entity, param, now, true);
        return toDTO(repo.save(entity), counts());
    }

    public BlogCategoryDTO update(String id, SaveBlogCategoryDTO param) {
        var entity = find(id);
        apply(entity, param, Instant.now(), false);
        return toDTO(repo.save(entity), counts());
    }

    public void delete(String id) {
        repo.delete(find(id));
    }

    // The category a post is being saved into: null for none; an unknown id is refused.
    public UUID resolve(String id) {
        if (id == null || id.isBlank()) return null;
        return find(id).getId();
    }

    // The id for a public ?category=<slug> filter; null for no filter. An unknown slug matches nothing.
    public UUID idForSlug(String slug) {
        if (slug == null || slug.isBlank()) return null;
        return repo.findBySlug(slug.trim().toLowerCase(java.util.Locale.ROOT))
                .map(BlogCategoryEntity::getId)
                .orElse(new UUID(0, 0));
    }

    // id -> entity, for putting each post's category on it.
    public Map<UUID, BlogCategoryEntity> byId() {
        Map<UUID, BlogCategoryEntity> map = new HashMap<>();
        repo.findAll().forEach(c -> map.put(c.getId(), c));
        return map;
    }

    private BlogCategoryEntity find(String id) {
        return repo.findById(TestimonialService.parse(id))
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }

    private void apply(BlogCategoryEntity e, SaveBlogCategoryDTO p, Instant now, boolean isNew) {
        String name = p.name().trim();
        if (isNew ? repo.existsByNameIgnoreCase(name) : repo.existsByNameIgnoreCaseAndIdNot(name, e.getId())) {
            throw new AlreadyExistsException("A category called '" + name + "' already exists");
        }

        // A typed slug is used as written; one made from the name gets a number added if it is taken.
        String candidate;
        if (p.slug() != null && !p.slug().isBlank()) {
            candidate = p.slug().trim();
            if (!BlogPostService.isValidSlug(candidate)) {
                throw new RuleException(
                        "Slug may only have lowercase letters, numbers and single hyphens between them (max 120)");
            }
            if (slugTaken(candidate, e, isNew)) {
                throw new AlreadyExistsException("Another category already uses the slug '" + candidate + "'");
            }
        } else {
            String slug = BlogPostService.slugify(name);
            if (slug.isBlank()) {
                throw new RuleException("Name must contain letters or numbers");
            }
            candidate = slug;
            for (int n = 2; slugTaken(candidate, e, isNew); n++) {
                candidate = slug + "-" + n;
            }
        }

        e.setName(name);
        e.setSlug(candidate);
        e.setDescription(p.description() == null || p.description().isBlank() ? null : p.description().trim());
        if (p.displayOrder() != null) e.setDisplayOrder(p.displayOrder());
        e.setUpdatedAt(now);
    }

    private boolean slugTaken(String slug, BlogCategoryEntity self, boolean isNew) {
        return isNew ? repo.existsBySlug(slug) : repo.existsBySlugAndIdNot(slug, self.getId());
    }

    private Map<UUID, long[]> counts() {
        Map<UUID, long[]> map = new HashMap<>();
        for (Object[] r : repo.postCounts()) {
            map.put((UUID) r[0], new long[] { ((Number) r[1]).longValue(), r[2] == null ? 0 : ((Number) r[2]).longValue() });
        }
        return map;
    }

    private static BlogCategoryDTO toDTO(BlogCategoryEntity c, Map<UUID, long[]> counts) {
        long[] n = counts.getOrDefault(c.getId(), new long[2]);
        return new BlogCategoryDTO(c.getId().toString(), c.getName(), c.getSlug(), c.getDescription(),
                c.getDisplayOrder(), n[0], n[1]);
    }
}
