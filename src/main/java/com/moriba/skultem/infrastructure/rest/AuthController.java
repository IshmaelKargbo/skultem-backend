package com.moriba.skultem.infrastructure.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import com.moriba.skultem.application.dto.LoginResponse;
import com.moriba.skultem.application.usecase.LoginUseCase;
import com.moriba.skultem.application.usecase.RefreshTokenUseCase;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.LoginDTO;
import com.moriba.skultem.infrastructure.rest.dto.RefreshTokenRequest;

import ua_parser.Parser;
import ua_parser.Client;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final Parser uaParser = new Parser();

    @PostMapping("/login")
    public ApiResponse<LoginResponse> create(
            @Valid @RequestBody LoginDTO param,
            HttpServletRequest request) {

        String ipAddress = request.getRemoteAddr();

        String userAgent = request.getHeader("User-Agent");
        Client client = uaParser.parse(userAgent);

        String os = client.os.family + " " + client.os.major;
        String browser = client.userAgent.family + " " + client.userAgent.major;
        String deviceType = client.device.family;
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
    public ApiResponse<LoginResponse> create(
            @Valid @RequestBody RefreshTokenRequest param,
            HttpServletRequest request) {

        LoginResponse res = refreshTokenUseCase.execute(param.refreshToken());

        return new ApiResponse<>("success", 200, "Login successful", res);
    }
}