package com.limvik.econome.domain.user.service;

import com.limvik.econome.domain.user.entity.User;
import com.limvik.econome.global.exception.ErrorCode;
import com.limvik.econome.global.exception.ErrorException;
import com.limvik.econome.infrastructure.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;

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

}
