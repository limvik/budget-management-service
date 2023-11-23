package com.limvik.econome.web.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.limvik.econome.domain.user.entity.User;
import com.limvik.econome.domain.user.service.UserService;
import com.limvik.econome.global.config.WebAuthorizationConfig;
import com.limvik.econome.global.exception.ErrorCode;
import com.limvik.econome.global.exception.ErrorException;
import com.limvik.econome.global.security.jwt.provider.JwtProvider;
import com.limvik.econome.infrastructure.user.UserRepository;
import com.limvik.econome.web.user.dto.SignupRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@Import(WebAuthorizationConfig.class)
public class UserSignupControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserService userService;

    @MockBean
    PasswordEncoder passwordEncoder;

    @MockBean
    JwtProvider jwtProvider;

    @MockBean
    UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    JacksonTester<SignupRequest> requestJson;

    @BeforeEach
    public void setup() {
        JacksonTester.initFields(this, objectMapper);
    }


    @Test
    @DisplayName("정상적인 회원가입")
    void shouldReturn201Created() throws Exception {

        String username = "limvik";
        String email = "limvik@limvik.com";
        String password = "limvikpassword";
        String encodedPassword = "$2a$12$dNSsw8M8NoAghJ6KJKzQM.fc8p9ysnufwYGhqjbasIWfjqt6axLMW";
        long minimumDailyExpense = 10000;
        boolean agreeAlarm = true;

        var requestedUserInfo = new SignupRequest(username, email, password, minimumDailyExpense, agreeAlarm);
        var userId = 1L;
        var user = User.builder().id(userId).build();

        given(userService.createUser(any(User.class))).willReturn(user);
        given(passwordEncoder.encode(password)).willReturn(encodedPassword);

        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson.write(requestedUserInfo).getJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/users/" + userId));
    }

    @Test
    @DisplayName("중복된 username 으로 회원가입")
    void shouldReturn409Conflict() throws Exception {

        String username = "limvik";
        String email = "limvik@limvik.com";
        String password = "limvikpassword";
        String encodedPassword = "$2a$12$dNSsw8M8NoAghJ6KJKzQM.fc8p9ysnufwYGhqjbasIWfjqt6axLMW";
        long minimumDailyExpense = 10000;
        boolean agreeAlarm = true;

        var requestedUserInfo = new SignupRequest(username, email, password, minimumDailyExpense, agreeAlarm);

        given(userService.createUser(any(User.class))).willThrow(new ErrorException(ErrorCode.DUPLICATED_USERNAME));
        given(passwordEncoder.encode(password)).willReturn(encodedPassword);

        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson.write(requestedUserInfo).getJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.['errorCode']").value(ErrorCode.DUPLICATED_USERNAME.name()))
                .andExpect(jsonPath("$.['errorReason']").value(ErrorCode.DUPLICATED_USERNAME.getMessage()));

    }

    @Test
    @DisplayName("유효하지 않은 username, email, password 으로 회원가입")
    void shouldReturn422UnprocessableEntity() throws Exception {
        String username = "limvikasfkljasdjlfkasdkljfjskdl";
        String email = "test.com";
        String password = "ord";
        long minimumDailyExpense = 10000;
        boolean agreeAlarm = true;

        var requestedUserInfo = new SignupRequest(username, email, password, minimumDailyExpense, agreeAlarm);

        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson.write(requestedUserInfo).getJson()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.['errorCode']").value(ErrorCode.UNPROCESSABLE_USERINFO.name()))
                .andExpect(jsonPath("$.['errorReason']").value(ErrorCode.UNPROCESSABLE_USERINFO.getMessage()));
    }

}
