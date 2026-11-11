package com.moriba.skultem.infrastructure.rest;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.usecase.RenderExternalExamDocumentUseCase;
import com.moriba.skultem.application.usecase.RenderReportCardPdfUseCase;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.infrastructure.compose.MComposeClient;
import com.moriba.skultem.infrastructure.compose.MComposeSignIn;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.security.SectionNeutral;

import lombok.RequiredArgsConstructor;

// M-Compose, the document engine: whether its PDFs are on, and a sign-in link to its designer.
@RestController
@RequestMapping("/api/v1/m-compose")
@RequiredArgsConstructor
public class MComposeController {

    private final MComposeClient client;
    private final MComposeSignIn signIn;
    private final RenderReportCardPdfUseCase reportCards;
    private final RenderExternalExamDocumentUseCase externalExams;
    private final UserRepository userRepo;

    // Which documents the server makes (the app falls back to drawing the rest itself). Says nothing secret.
    @SectionNeutral
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Boolean>> status(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = Map.of(
                "enabled", client.isEnabled(),
                "reportCards", reportCards.isEnabled(school),
                "externalExams", externalExams.isEnabled(school));
        return new ApiResponse<>("success", 200, "Document service status", res);
    }

    // Platform staff: is the engine running and does it accept Skultem's key, and is designer sign-in set up?
    @GetMapping("/check")
    @PreAuthorize("@permissionService.hasPlatformPermission('SETTINGS')")
    public ApiResponse<Map<String, Object>> check() {
        var c = client.check();
        var res = new java.util.LinkedHashMap<String, Object>();
        res.put("configured", c.configured());
        res.put("reachable", c.reachable());
        res.put("keyAccepted", c.keyAccepted());
        res.put("message", c.message());
        res.put("designer", signIn.isEnabled());
        // The server defaults (a school can pick its own designs on top of these).
        res.put("reportCards", reportCards.isEnabled(null));
        res.put("externalExams", externalExams.isEnabled(null));
        return new ApiResponse<>("success", 200, "Document service checked", res);
    }

    // A one-time, 5-minute link that signs this person in to the designer. The designer edits the templates every
    // school's documents are made from, so it is for platform staff only, as a "designer" (templates and images;
    // not keys or people).
    @PostMapping("/designer-link")
    @PreAuthorize("@permissionService.hasPlatformPermission('SETTINGS')")
    public ApiResponse<Map<String, String>> designerLink(
            @AuthenticationPrincipal(expression = "userId") String userId) {
        String name = userRepo.findById(userId).map(User::getName).orElse(null);
        var res = Map.of("url", signIn.linkFor(userId, name, "designer"));
        return new ApiResponse<>("success", 200, "Designer link created", res);
    }

    @GetMapping("/designer-link")
    @PreAuthorize("@permissionService.hasPlatformPermission('SETTINGS')")
    public ApiResponse<Map<String, Boolean>> designerAvailable() {
        return new ApiResponse<>("success", 200, "Designer sign-in status", Map.of("enabled", signIn.isEnabled()));
    }
}
