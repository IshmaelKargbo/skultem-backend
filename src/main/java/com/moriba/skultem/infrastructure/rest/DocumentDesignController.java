package com.moriba.skultem.infrastructure.rest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.services.DocumentDesignService;
import com.moriba.skultem.domain.vo.DocumentKind;
import com.moriba.skultem.infrastructure.compose.MComposeClient;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.security.SectionNeutral;

import lombok.RequiredArgsConstructor;

/**
 * A school picks the M-Compose design it prints each kind of document with (report-card, exam-result-slip,
 * exam-statement), from the designs on the engine. Report cards are the school's leadership's; the exam documents
 * the exam office's too.
 */
@SectionNeutral
@RestController
@RequestMapping("/api/v1/document-designs")
@RequiredArgsConstructor
public class DocumentDesignController {

    private static final String CHOOSE = "@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PRINCIPAL', 'SUPER_ADMIN', 'OWNER', 'PROPRIETOR')"
            + " or (#kind != 'report-card' and @permissionService.hasAnySchoolRole(#school, 'EXAM_OFFICER'))";
    private static final String VIEW = "@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PRINCIPAL', 'EXAM_OFFICER', 'SUPER_ADMIN', 'OWNER', 'PROPRIETOR')";

    private final DocumentDesignService designs;
    private final MComposeClient client;
    private final com.moriba.skultem.application.services.DocumentFieldLinkService fieldLinks;

    public record ChooseDesign(String template) {
    }

    public record SaveLinks(String template,
            java.util.Map<String, com.moriba.skultem.application.services.DocumentFieldLinkService.Link> links) {
    }

    // ---- Linking a design's placeholders to Skultem's data. template: a design's ref; empty = the school's current
    // design for this kind.

    @GetMapping("/{kind}/fields")
    @PreAuthorize(VIEW)
    public ApiResponse<com.moriba.skultem.application.services.DocumentFieldLinkService.Editor> fields(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school, @PathVariable String kind,
            @RequestParam(required = false) String template) {
        var k = kind(kind);
        String ref = template == null || template.isBlank() ? designs.templateFor(school, k) : template.trim();
        if (ref == null || ref.isBlank()) {
            throw new NotFoundException("Pick a design first");
        }
        return new ApiResponse<>("success", 200, "Placeholders fetched", fieldLinks.editor(school, k, ref));
    }

    @PutMapping("/{kind}/fields")
    @PreAuthorize(CHOOSE)
    public ApiResponse<com.moriba.skultem.application.services.DocumentFieldLinkService.Editor> saveFields(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId, @PathVariable String kind,
            @RequestBody SaveLinks body) {
        if (body == null || body.template() == null || body.template().isBlank()) {
            throw new com.moriba.skultem.application.error.RuleException("Say which design the links are for");
        }
        var res = fieldLinks.save(school, kind(kind), body.template().trim(), body.links(), userId);
        return new ApiResponse<>("success", 200, "Data links saved", res);
    }

    // The school's current design for this kind and the designs it can pick. all=true lists every design, not only
    // the ones made for this kind of document.
    @GetMapping("/{kind}")
    @PreAuthorize(VIEW)
    public ApiResponse<DocumentDesignService.Choice> choice(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school, @PathVariable String kind,
            @RequestParam(defaultValue = "false") boolean all) {
        return new ApiResponse<>("success", 200, "Designs fetched", designs.choice(school, kind(kind), all));
    }

    // template: a design's ref from the list; empty = go back to the default design.
    @PutMapping("/{kind}")
    @PreAuthorize(CHOOSE)
    public ApiResponse<DocumentDesignService.Choice> choose(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId, @PathVariable String kind,
            @RequestBody ChooseDesign body) {
        var res = designs.choose(school, kind(kind), body == null ? null : body.template(), userId);
        return new ApiResponse<>("success", 200, "Design saved", res);
    }

    // A saved template's picture (its thumbnail on the engine), for the picker.
    @GetMapping("/preview/{sampleId}")
    @PreAuthorize(VIEW)
    public ResponseEntity<byte[]> preview(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school, @PathVariable String sampleId) {
        byte[] png = client.designPreview(sampleId);
        if (png == null) {
            throw new NotFoundException("This design has no picture");
        }
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "image/png")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600").body(png);
    }

    private static DocumentKind kind(String key) {
        return DocumentKind.fromKey(key).orElseThrow(() -> new NotFoundException("Unknown kind of document: " + key));
    }
}
