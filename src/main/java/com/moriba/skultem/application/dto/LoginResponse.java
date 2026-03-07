package com.moriba.skultem.application.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken) {

}
