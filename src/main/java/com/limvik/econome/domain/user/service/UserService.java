package com.limvik.econome.domain.user.service;

import com.limvik.econome.domain.user.entity.User;
import com.limvik.econome.global.exception.ErrorCode;
import com.limvik.econome.global.exception.ErrorException;
import com.limvik.econome.global.security.AuthUser;
import com.limvik.econome.global.security.jwt.provider.JwtProvider;
import com.limvik.econome.infrastructure.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@RequiredArgsConstructor
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Transactional
    public User createUser(User user) {
        checkDuplicatedUsername(user.getUsername());
        checkDuplicatedEmail(user.getEmail());
        return userRepository.save(user);
    }

    private void checkDuplicatedUsername(String username) {
        if (userRepository.existsByUsername(username))
            throw new ErrorException(ErrorCode.DUPLICATED_USERNAME);
    }

    private void checkDuplicatedEmail(String email) {
        if (userRepository.existsByEmail(email))
            throw new ErrorException(ErrorCode.DUPLICATED_EMAIL);
    }

    public Map<String, String> getTokens(User user) {
        return Map.of("accessToken", jwtProvider.generateAccessToken(user),
                      "refreshToken", jwtProvider.generateRefreshToken(user));
    }

    @Transactional(readOnly = true)
    public boolean matchRefreshToken(User user) {
        return userRepository.existsByIdAndRefreshToken(user.getId(), user.getRefreshToken());

    }

    @Transactional
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(ErrorCode.NOT_EXIST_USER.getMessage()));
        return new AuthUser(user);
    }
}
