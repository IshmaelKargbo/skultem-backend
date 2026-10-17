package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.BlogPostDTO;
import com.moriba.skultem.application.dto.TestimonialDTO;
import com.moriba.skultem.application.services.BlogPostService;
import com.moriba.skultem.application.services.ContactMessageService;
import com.moriba.skultem.application.services.TestimonialService;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateContactMessageDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Open to the public (see SecurityConfig): what the marketing site reads and the one thing it can send.
// Only published content is ever returned here.
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicMarketingController {

    private final TestimonialService testimonialService;
    private final BlogPostService blogPostService;
    private final ContactMessageService contactMessageService;
    private final com.moriba.skultem.application.services.PlatformSettingsService platformSettings;

    @GetMapping("/testimonials")
    public ApiResponse<List<TestimonialDTO>> testimonials() {
        return new ApiResponse<>("success", 200, "Testimonials fetched successfully",
                testimonialService.listPublished());
    }

    @GetMapping("/blog")
    public ApiResponse<List<BlogPostDTO>> posts(
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "10") Integer size, @RequestParam(defaultValue = "1") Integer page) {
        var res = blogPostService.listPublished(page, size, tag);
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());
        return new ApiResponse<>("success", 200, "Blog posts fetched successfully", res.getContent(), meta);
    }

    @GetMapping("/blog/{slug}")
    public ApiResponse<BlogPostDTO> post(@PathVariable String slug) {
        return new ApiResponse<>("success", 200, "Blog post fetched successfully", blogPostService.getPublished(slug));
    }

    // The few platform settings the marketing site shows (its name, how to reach support).
    @GetMapping("/settings")
    public ApiResponse<Map<String, String>> settings() {
        return new ApiResponse<>("success", 200, "Settings fetched successfully", platformSettings.publicValues());
    }

    @PostMapping("/contact")
    public ApiResponse<Object> contact(@Valid @RequestBody CreateContactMessageDTO param) {
        contactMessageService.submit(param);
        return new ApiResponse<>("success", 200, "Message received. We will get back to you shortly.", null);
    }
}
