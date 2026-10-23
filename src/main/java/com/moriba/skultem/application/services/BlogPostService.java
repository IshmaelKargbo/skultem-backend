package com.moriba.skultem.application.services;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.BlogPostDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.infrastructure.bucket.R2StorageService;
import com.moriba.skultem.application.mapper.PageableMapper;
import com.moriba.skultem.infrastructure.persistence.entity.BlogPostEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.BlogPostJpaRepository;
import com.moriba.skultem.infrastructure.rest.dto.SaveBlogPostDTO;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class BlogPostService {

    public static final int MAX_PHOTOS = 4;
    // Wide enough to fill a blog layout; phone photos are several thousand pixels across.
    private static final int MAX_PHOTO_DIMENSION = 1600;

    private final BlogPostJpaRepository repo;
    private final R2StorageService storage;

    // Admin: every post, drafts included, newest first. List rows carry no body.
    public Page<BlogPostDTO> list(int page, int size) {
        return repo.findAll(PageableMapper.toPage(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(e -> toDTO(e, false));
    }

    public BlogPostDTO get(String id) {
        return toDTO(find(id), true);
    }

    public BlogPostDTO create(SaveBlogPostDTO param) {
        var now = Instant.now();
        var entity = BlogPostEntity.builder().id(UUID.randomUUID()).createdAt(now).build();
        apply(entity, param, now);
        return toDTO(repo.save(entity), true);
    }

    public BlogPostDTO update(String id, SaveBlogPostDTO param) {
        var entity = find(id);
        apply(entity, param, Instant.now());
        return toDTO(repo.save(entity), true);
    }

    public void delete(String id) {
        repo.delete(find(id));
    }

    // Public: published posts only.
    public Page<BlogPostDTO> listPublished(int page, int size, String tag) {
        var pageable = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 50));
        return repo.findPublished(tag == null ? "" : tag.trim(), pageable).map(e -> toDTO(e, false));
    }

    public BlogPostDTO getPublished(String slug) {
        return toDTO(repo.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> new NotFoundException("Post not found")), true);
    }

    // A photo for a post: shrunk and stored, and its public URL handed back to be added to the post's photos.
    public String uploadImage(org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuleException("A photo is required");
        }
        String type = file.getContentType();
        if (type == null || !type.startsWith("image/")) {
            throw new RuleException("Only image files can be uploaded");
        }
        try {
            return storage.uploadPhoto(file, "marketing/blog/" + UUID.randomUUID(), MAX_PHOTO_DIMENSION);
        } catch (java.io.IOException e) {
            throw new RuleException("The photo could not be uploaded");
        }
    }

    private BlogPostEntity find(String id) {
        return repo.findById(TestimonialService.parse(id)).orElseThrow(() -> new NotFoundException("Post not found"));
    }

    private void apply(BlogPostEntity e, SaveBlogPostDTO p, Instant now) {
        // A slug the admin typed is used exactly as written, so it must already be valid and free. One
        // made from the title is cleaned up, and gets a number added if it is taken.
        boolean typed = p.slug() != null && !p.slug().isBlank();
        String candidate;
        if (typed) {
            candidate = p.slug().trim();
            if (!isValidSlug(candidate)) {
                throw new RuleException(
                        "Slug may only have lowercase letters, numbers and single hyphens between them (max 120)");
            }
            if (taken(candidate, e)) {
                throw new AlreadyExistsException("Another post already uses the slug '" + candidate + "'");
            }
        } else {
            String slug = slugify(p.title());
            if (slug.isBlank()) {
                throw new RuleException("Title must contain letters or numbers");
            }
            candidate = slug;
            for (int n = 2; taken(candidate, e); n++) {
                candidate = slug + "-" + n;
            }
        }

        e.setTitle(p.title().trim());
        e.setSlug(candidate);
        e.setExcerpt(blankToNull(p.excerpt()));
        // Cleaned of anything that could run in a reader's browser - see BlogHtmlSanitizer.
        String content = BlogHtmlSanitizer.clean(p.content());
        if (BlogHtmlSanitizer.isBlank(content)) {
            throw new RuleException("Content is required");
        }
        e.setContent(content);
        var images = p.images() == null ? List.<String>of()
                : p.images().stream().filter(u -> u != null && !u.isBlank()).map(String::trim).distinct().toList();
        if (images.size() > MAX_PHOTOS) {
            throw new RuleException("A post can have at most " + MAX_PHOTOS + " photos (" + images.size() + " added)");
        }
        e.setImages(images.isEmpty() ? null : String.join("\n", images));
        e.setCoverImageUrl(images.isEmpty() ? null : images.get(0));
        e.setAuthorName(blankToNull(p.authorName()));
        e.setTags(p.tags() == null ? null : p.tags().stream().map(t -> t.trim().toLowerCase(Locale.ROOT))
                .filter(t -> !t.isEmpty() && !t.contains(",")).distinct().reduce((a, b) -> a + "," + b).orElse(null));

        // Stamped the first time it goes live and kept if it is unpublished and published again.
        if (p.published() && e.getPublishedAt() == null) {
            e.setPublishedAt(now);
        }
        e.setPublished(p.published());
        e.setUpdatedAt(now);
    }

    private boolean taken(String slug, BlogPostEntity self) {
        return self.getId() != null && self.getCreatedAt() != null && repo.existsById(self.getId())
                ? repo.existsBySlugAndIdNot(slug, self.getId())
                : repo.existsBySlug(slug);
    }

    private static final java.util.regex.Pattern SLUG = java.util.regex.Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    static boolean isValidSlug(String slug) {
        return slug != null && slug.length() <= 120 && SLUG.matcher(slug).matches();
    }

    // For the form's live check: is this slug well formed and not used by another post (the one being edited
    // may keep its own).
    public boolean slugAvailable(String slug, String excludeId) {
        if (!isValidSlug(slug)) {
            return false;
        }
        if (excludeId == null || excludeId.isBlank()) {
            return !repo.existsBySlug(slug);
        }
        return !repo.existsBySlugAndIdNot(slug, TestimonialService.parse(excludeId));
    }

    static String slugify(String text) {
        String s = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return s.length() > 120 ? s.substring(0, 120).replaceAll("-+$", "") : s;
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static BlogPostDTO toDTO(BlogPostEntity e, boolean withContent) {
        List<String> tags = e.getTags() == null || e.getTags().isBlank() ? List.of()
                : Arrays.asList(e.getTags().split(","));
        List<String> images = e.getImages() == null || e.getImages().isBlank() ? List.of()
                : Arrays.asList(e.getImages().split("\n"));
        return new BlogPostDTO(e.getId().toString(), e.getTitle(), e.getSlug(), e.getExcerpt(),
                // Cleaned on the way out too, so posts saved before cleaning existed are just as safe.
                withContent ? BlogHtmlSanitizer.clean(e.getContent()) : null, e.getCoverImageUrl(), images, e.getAuthorName(), tags, e.isPublished(),
                e.getPublishedAt(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
