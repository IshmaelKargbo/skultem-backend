package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.BlogPostDTO;
import com.moriba.skultem.application.dto.ContactMessageDTO;
import com.moriba.skultem.application.dto.TestimonialDTO;
import com.moriba.skultem.application.services.BlogPostService;
import com.moriba.skultem.application.services.ContactMessageService;
import com.moriba.skultem.application.services.TestimonialService;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.SaveBlogPostDTO;
import com.moriba.skultem.infrastructure.rest.dto.SaveTestimonialDTO;
import com.moriba.skultem.infrastructure.rest.dto.UpdateContactStatusDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// The system admin's side of the marketing site's content. The site itself reads it through
// PublicMarketingController.
@RestController
@RequestMapping("/api/v1/marketing")
@PreAuthorize("@permissionService.hasPlatformPermission('MARKETING')")
@RequiredArgsConstructor
public class MarketingAdminController {

    private final TestimonialService testimonialService;
    private final BlogPostService blogPostService;
    private final ContactMessageService contactMessageService;

    private static Map<String, Object> meta(Page<?> res) {
        return Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());
    }

    // ---- Testimonials ----

    @GetMapping("/testimonials")
    public ApiResponse<List<TestimonialDTO>> listTestimonials(
            @RequestParam(defaultValue = "10") Integer size, @RequestParam(defaultValue = "1") Integer page) {
        var res = testimonialService.list(page, size);
        return new ApiResponse<>("success", 200, "Testimonials fetched successfully", res.getContent(), meta(res));
    }

    @PostMapping("/testimonials")
    public ApiResponse<TestimonialDTO> createTestimonial(@Valid @RequestBody SaveTestimonialDTO param) {
        return new ApiResponse<>("success", 200, "Testimonial created successfully", testimonialService.create(param));
    }

    // The author's photo (multipart "photo"); returns its URL for the testimonial's photoUrl.
    @PostMapping(value = "/testimonials/photo", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> uploadTestimonialPhoto(
            @org.springframework.web.bind.annotation.RequestPart("photo") org.springframework.web.multipart.MultipartFile photo) {
        return new ApiResponse<>("success", 200, "Photo uploaded successfully",
                Map.of("url", testimonialService.uploadPhoto(photo)));
    }

    @PutMapping("/testimonials/{id}")
    public ApiResponse<TestimonialDTO> updateTestimonial(@PathVariable String id,
            @Valid @RequestBody SaveTestimonialDTO param) {
        return new ApiResponse<>("success", 200, "Testimonial updated successfully",
                testimonialService.update(id, param));
    }

    @DeleteMapping("/testimonials/{id}")
    public ApiResponse<Object> deleteTestimonial(@PathVariable String id) {
        testimonialService.delete(id);
        return new ApiResponse<>("success", 200, "Testimonial deleted successfully", null);
    }

    // ---- Blog ----

    @GetMapping("/blog")
    public ApiResponse<List<BlogPostDTO>> listPosts(
            @RequestParam(defaultValue = "10") Integer size, @RequestParam(defaultValue = "1") Integer page) {
        var res = blogPostService.list(page, size);
        return new ApiResponse<>("success", 200, "Blog posts fetched successfully", res.getContent(), meta(res));
    }

    @GetMapping("/blog/slug-available")
    public ApiResponse<Boolean> slugAvailable(@RequestParam String slug,
            @RequestParam(required = false) String excludeId) {
        return new ApiResponse<>("success", 200, "Slug checked", blogPostService.slugAvailable(slug, excludeId));
    }

    @GetMapping("/blog/{id}")
    public ApiResponse<BlogPostDTO> getPost(@PathVariable String id) {
        return new ApiResponse<>("success", 200, "Blog post fetched successfully", blogPostService.get(id));
    }

    @PostMapping("/blog")
    public ApiResponse<BlogPostDTO> createPost(@Valid @RequestBody SaveBlogPostDTO param) {
        return new ApiResponse<>("success", 200, "Blog post created successfully", blogPostService.create(param));
    }

    // One photo per call (multipart "image"); returns its URL for the post's photos.
    @PostMapping(value = "/blog/images", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> uploadPostImage(
            @org.springframework.web.bind.annotation.RequestPart("image") org.springframework.web.multipart.MultipartFile image) {
        return new ApiResponse<>("success", 200, "Photo uploaded successfully",
                Map.of("url", blogPostService.uploadImage(image)));
    }

    @PutMapping("/blog/{id}")
    public ApiResponse<BlogPostDTO> updatePost(@PathVariable String id, @Valid @RequestBody SaveBlogPostDTO param) {
        return new ApiResponse<>("success", 200, "Blog post updated successfully", blogPostService.update(id, param));
    }

    @DeleteMapping("/blog/{id}")
    public ApiResponse<Object> deletePost(@PathVariable String id) {
        blogPostService.delete(id);
        return new ApiResponse<>("success", 200, "Blog post deleted successfully", null);
    }

    // ---- Contact messages ----

    @GetMapping("/contact")
    public ApiResponse<List<ContactMessageDTO>> listMessages(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "10") Integer size, @RequestParam(defaultValue = "1") Integer page) {
        var res = contactMessageService.list(page, size, status);
        return new ApiResponse<>("success", 200, "Messages fetched successfully", res.getContent(), meta(res));
    }

    @GetMapping("/contact/unread")
    public ApiResponse<Long> unreadMessages() {
        return new ApiResponse<>("success", 200, "Unread messages counted", contactMessageService.countNew());
    }

    @PatchMapping("/contact/{id}/status")
    public ApiResponse<ContactMessageDTO> updateMessageStatus(@PathVariable String id,
            @Valid @RequestBody UpdateContactStatusDTO param) {
        return new ApiResponse<>("success", 200, "Message updated successfully",
                contactMessageService.updateStatus(id, param.status()));
    }

    @DeleteMapping("/contact/{id}")
    public ApiResponse<Object> deleteMessage(@PathVariable String id) {
        contactMessageService.delete(id);
        return new ApiResponse<>("success", 200, "Message deleted successfully", null);
    }
}
