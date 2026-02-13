package com.syntaxnow.identity.dto;

public record TokenExchangeResponse(
        String userId,
        String phone,
        String accessToken,
        String refreshToken,
        int expiresIn
) {}
