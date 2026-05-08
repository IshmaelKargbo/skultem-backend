package com.moriba.skultem.infrastructure.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.LoginResponse;
import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.application.usecase.GetSchoolUseCase;
import com.moriba.skultem.application.usecase.LoginUseCase;
import com.moriba.skultem.application.usecase.LogoutUseCase;
import com.moriba.skultem.application.usecase.RefreshTokenUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.LoginDTO;
import com.moriba.skultem.infrastructure.rest.dto.RefreshTokenRequest;
import com.moriba.skultem.infrastructure.security.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ua_parser.Client;
import ua_parser.Parser;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final JwtUtil jwt;
    private final LogoutUseCase logoutUseCase;
    private final GetSchoolUseCase getSchoolUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;

    private final Parser uaParser = new Parser();

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginDTO param,
            HttpServletRequest request) {

        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isBlank()) {
            ipAddress = request.getRemoteAddr();
        }

        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            userAgent = "Unknown";
        }

        Client client = uaParser.parse(userAgent);

        String os = client.os.family +
                (client.os.major != null ? " " + client.os.major : "");

        String browser = client.userAgent.family +
                (client.userAgent.major != null ? " " + client.userAgent.major : "");

        String ua = userAgent.toLowerCase();

        String deviceType;
        if (ua.contains("mobile")) {
            deviceType = "Mobile";
        } else if (ua.contains("tablet") || ua.contains("ipad")) {
            deviceType = "Tablet";
        } else {
            deviceType = "Desktop";
        }

        String device = deviceType;

        LoginResponse res = loginUseCase.execute(
                param.domain(),
                param.email(),
                param.password(),
                ipAddress,
                device,
                deviceType,
                os,
                browser,
                userAgent);

        return new ApiResponse<>("success", 200, "Login successful", res);
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest param) {

        LoginResponse res = refreshTokenUseCase.execute(param.refreshToken());

        return new ApiResponse<>("success", 200, "Token refreshed", res);
    }

    @GetMapping("/tenant")
    public ApiResponse<SchoolDTO> tenant(@Valid @RequestParam(required = true) String domain) {
        SchoolDTO res = getSchoolUseCase.findByDomain(domain);
        return new ApiResponse<>("success", 200, "Get tenant successfully", res);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid token");
        }

        String token = authHeader.substring(7);

        String sessionId = jwt.extractSessionId(token);

        logoutUseCase.execute(sessionId);

        return new ApiResponse<>("success", 200, "Logout successful", null);
    }
}