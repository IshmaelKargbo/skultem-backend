package com.moriba.skultem.infrastructure.rest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.EmailCheckDTO;
import com.moriba.skultem.application.dto.EmailVerificationResultDTO;
import com.moriba.skultem.application.dto.EmailVerificationStatusDTO;
import com.moriba.skultem.application.services.EmailValidationService;
import com.moriba.skultem.application.services.EmailVerificationService;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.EmailCheckRequestDTO;
import com.moriba.skultem.infrastructure.rest.dto.EmailVerificationConfirmDTO;
import com.moriba.skultem.infrastructure.security.AuthUser;
import com.moriba.skultem.infrastructure.security.SectionNeutral;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Email checks and ownership verification. /check and /confirm are public (rate-limited per IP - see SecurityConfig):
// forms on the public site check addresses too, and a verification link is opened by someone who may not be logged in.
// Everything else acts on the signed-in person's own account - there is no owner id to tamper with. Admins send
// verification for the accounts they manage through POST /user/{id}/email-verification.
@SectionNeutral
@RestController
@RequestMapping("/api/v1/email-verification")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailValidationService emailValidationService;
    private final EmailVerificationService emailVerificationService;

    // Format + domain check for a form field. Says nothing about whether any account uses the address.
    @PostMapping("/check")
    public ApiResponse<EmailCheckDTO> check(@Valid @RequestBody EmailCheckRequestDTO param) {
        return new ApiResponse<>("success", 200, "Email checked", emailValidationService.check(param.email()));
    }

    @PostMapping("/send")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<EmailVerificationStatusDTO> send(@AuthenticationPrincipal AuthUser user) {
        var res = emailVerificationService.send(user.userId(), user.activeSchoolId());
        return new ApiResponse<>("success", 200, "Verification email sent", res);
    }

    // Same as /send - a new link replaces any earlier one.
    @PostMapping("/resend")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<EmailVerificationStatusDTO> resend(@AuthenticationPrincipal AuthUser user) {
        var res = emailVerificationService.send(user.userId(), user.activeSchoolId());
        return new ApiResponse<>("success", 200, "Verification email sent", res);
    }

    @PostMapping("/confirm")
    public ApiResponse<EmailVerificationResultDTO> confirm(@Valid @RequestBody EmailVerificationConfirmDTO param) {
        var res = emailVerificationService.confirm(param.token());
        return new ApiResponse<>("success", 200, "Verification link checked", res);
    }

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<EmailVerificationStatusDTO> status(@AuthenticationPrincipal AuthUser user) {
        return new ApiResponse<>("success", 200, "Email verification status",
                emailVerificationService.status(user.userId()));
    }
}
