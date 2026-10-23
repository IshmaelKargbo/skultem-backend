package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SaveBlogPostDTO(
        @NotBlank(message = "Title is required") @Size(max = 255) String title,
        // Optional: made from the title when left blank.
        @Size(max = 120, message = "Slug must be at most 120 characters") @Pattern(regexp = "^$|^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug may only have lowercase letters, numbers and single hyphens between them") String slug,
        @Size(max = 1000, message = "Excerpt must be at most 1000 characters") String excerpt,
        // HTML from the admin editor - cleaned of scripts and anything else unsafe before it is stored
        // (BlogHtmlSanitizer).
        @NotBlank(message = "Content is required") String content,
        // Photo URLs from the upload endpoint; the first is the cover. Up to 4 (BlogPostService.MAX_PHOTOS);
        // none is fine, published or not.
        @Size(max = 4, message = "A post can have at most 4 photos") List<@Size(max = 1024) String> images,
        @Size(max = 255) String authorName,
        List<@Size(max = 40) String> tags,
        boolean published) {
}
