package com.limvik.econome.web.user.controller;

import com.limvik.econome.domain.user.entity.User;
import com.limvik.econome.domain.user.service.UserService;
import com.limvik.econome.global.exception.ErrorCode;
import com.limvik.econome.global.exception.ErrorException;
import com.limvik.econome.global.security.AuthUser;
import com.limvik.econome.global.security.authentication.JwtAuthenticationToken;
import com.limvik.econome.web.user.dto.SigninResponse;
import com.limvik.econome.web.user.dto.SignupRequest;
import com.limvik.econome.web.user.dto.TokenResponse;
import com.limvik.econome.web.util.UserUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest signupRequest) {
        User user = userService.createUser(mapSignupRequestToUser(signupRequest));
        return ResponseEntity.created(URI.create("/api/v1/users/" + user.getId())).build();
    }

    private User mapSignupRequestToUser(SignupRequest signupRequest) {
        return User.builder()
                .username(signupRequest.username())
                .email(signupRequest.email())
                .password(passwordEncoder.encode(signupRequest.password()))
                .minimumDailyExpense(signupRequest.minimumDailyExpense())
                .agreeAlarm(signupRequest.agreeAlarm())
                .build();
    }

    @PostMapping("/signin")
    public ResponseEntity<SigninResponse> signin(@AuthenticationPrincipal AuthUser authUser) {
        var user = authUser.getUser();
        Map<String, String> tokens = userService.getTokens(user);
        String accessToken = tokens.get("accessToken");
        String refreshToken = tokens.get("refreshToken");
        updateRefreshToken(user, refreshToken);
        return ResponseEntity.ok(new SigninResponse(accessToken, refreshToken));
    }

    private void updateRefreshToken(User user, String refreshToken) {
        user.setRefreshToken(refreshToken);
        userService.updateUser(user);
    }

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> token(Authentication authentication) {
        JwtAuthenticationToken token = (JwtAuthenticationToken) authentication;
        long userId = UserUtil.getUserIdFromJwt(token);
        log.info("refresh token userId: {}", userId);
        User user = User.builder().id(userId).refreshToken(token.getTokenString()).build();
        if (userService.matchRefreshToken(user)) {
            Map<String, String> tokens = userService.getTokens(user);
            return ResponseEntity.ok(new TokenResponse(tokens.get("accessToken")));
        } else {
            log.info("유효한 토큰이지만 데이터베이스 refersh token과 다름");
            throw new ErrorException(ErrorCode.INVALID_TOKEN);
        }
    }

}
