package com.limvik.econome.web.user.controller;

import com.limvik.econome.domain.user.entity.User;
import com.limvik.econome.domain.user.service.UserService;
import com.limvik.econome.web.user.dto.SignupRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

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
}
