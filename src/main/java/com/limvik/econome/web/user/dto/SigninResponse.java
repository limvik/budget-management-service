package com.limvik.econome.web.user.dto;

public record SigninResponse(
        String accessToken,
        String refreshToken
) { }
