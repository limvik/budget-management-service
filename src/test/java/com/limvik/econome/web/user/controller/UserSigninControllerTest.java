package com.limvik.econome.web.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.limvik.econome.domain.user.entity.User;
import com.limvik.econome.domain.user.service.UserService;
import com.limvik.econome.global.config.WebAuthorizationConfig;
import com.limvik.econome.global.exception.ErrorCode;
import com.limvik.econome.infrastructure.user.UserRepository;
import com.limvik.econome.web.user.dto.SigninRequest;
import com.limvik.econome.web.user.dto.SigninResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.org.apache.commons.lang3.ObjectUtils;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import(WebAuthorizationConfig.class)
public class UserSigninControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserService userService;

    @MockBean
    PasswordEncoder passwordEncoder;

    @MockBean
    UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    JacksonTester<SigninRequest> requestJson;

    @BeforeEach
    public void setup() {
        JacksonTester.initFields(this, objectMapper);
    }

    @Test
    @DisplayName("정상적인 로그인 요청 및 AccessToken과 RefershToken 반환")
    void shouldReturnAccessTokenAndRefreshTokenIfExistUser() throws Exception {

        var username = "test";
        var password = "password";
        var signinRequest = new SigninRequest(username, password);
        var tokens = Map.of("accessToken", "access",
                            "refreshToken", "refresh");
        given(userService.getTokens(any(User.class))).willReturn(tokens);
        given(passwordEncoder.encode(password)).willReturn(anyString());
        given(passwordEncoder.matches(password, anyString())).willReturn(true);
        var user = User.builder().username(username).password(password).build();
        given(userRepository.findByUsername(username)).willReturn(Optional.of(user));

        var signinResponse = new SigninResponse(tokens.get("accessToken"), tokens.get("refreshToken"));

        mockMvc.perform(post("/api/v1/users/signin")
                .contentType("application/json")
                .content(requestJson.write(signinRequest).getJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['accessToken']").value(signinResponse.accessToken()))
                .andExpect(jsonPath("$.['refreshToken']").value(signinResponse.refreshToken()));
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 로그인 요청")
    void shouldReturn401IfNotExistUser() throws Exception {

        var username = "test";
        var password = "password";
        var signinRequest = new SigninRequest(username, password);
        given(userService.getTokens(any(User.class))).willReturn(anyMap());
        given(passwordEncoder.encode(password)).willReturn(anyString());
        given(passwordEncoder.matches(password, anyString())).willReturn(false);
        given(userRepository.findByUsername(username)).willReturn(Optional.ofNullable(null));

        mockMvc.perform(post("/api/v1/users/signin")
                        .contentType("application/json")
                        .content(requestJson.write(signinRequest).getJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.['errorCode']").value(ErrorCode.NOT_EXIST_USER.name()))
                .andExpect(jsonPath("$.['errorReason']").value(ErrorCode.NOT_EXIST_USER.getMessage()));
    }

    @Test
    @DisplayName("유효성 검사에 실패하는 로그인 요청")
    void shouldReturn422IfNotValidUsernameOrPassword() throws Exception {

        var username = "testtesttesttesttesttesttest";
        var password = "pass";
        var signinRequest = new SigninRequest(username, password);

        mockMvc.perform(post("/api/v1/users/signin")
                        .contentType("application/json")
                        .content(requestJson.write(signinRequest).getJson()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.['errorCode']").value(ErrorCode.UNPROCESSABLE_USERINFO.name()))
                .andExpect(jsonPath("$.['errorReason']").value(ErrorCode.UNPROCESSABLE_USERINFO.getMessage()));
    }

}
