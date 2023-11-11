package com.limvik.econome.domain.user.service;

import com.limvik.econome.domain.user.entity.User;
import com.limvik.econome.global.security.jwt.provider.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserSigninServiceTest {

    @Mock
    JwtProvider jwtProvider;

    @InjectMocks
    UserService userService;

    @Test
    @DisplayName("정상적인 로그인 요청")
    void shouldReturnAccessTokenAndRefreshTokenMap() {

        var user = User.builder().id(1L).username("test").password("password").build();
        var accessToken = "access";
        var refreshToken = "refresh";

        when(jwtProvider.generateAccessToken(user)).thenReturn(accessToken);
        when(jwtProvider.generateRefreshToken(user)).thenReturn(refreshToken);

        Map<String, String> tokens = userService.getTokens(user);

        assertThat(tokens.get("accessToken")).isEqualTo(accessToken);
        assertThat(tokens.get("refreshToken")).isEqualTo(refreshToken);

    }

}