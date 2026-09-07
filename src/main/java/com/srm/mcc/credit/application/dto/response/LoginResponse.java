package com.srm.mcc.credit.application.dto.response;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInMs,
        String username,
        String role
) {
}
