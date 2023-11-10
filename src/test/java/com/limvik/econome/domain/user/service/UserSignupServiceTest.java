package com.limvik.econome.domain.user.service;

import com.limvik.econome.domain.user.entity.User;
import com.limvik.econome.global.exception.ErrorCode;
import com.limvik.econome.global.exception.ErrorException;
import com.limvik.econome.infrastructure.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserSignupServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserService userService;

    @Test
    @DisplayName("중복 username 으로 회원가입하여 예외 발생")
    public void shouldThrowErrorExceptionForDuplicatedUsername() {

        var user = User.builder().username("test").email("test@test.com").build();

        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(user))
                .isInstanceOf(ErrorException.class)
                .hasMessage(ErrorCode.DUPLICATED_USERNAME.getMessage());
    }

    @Test
    @DisplayName("중복 email 으로 회원가입하여 예외 발생")
    public void shouldThrowErrorExceptionForDuplicatedEmail() {

        var user = User.builder().username("test").email("test@test.com").build();

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(user))
                .isInstanceOf(ErrorException.class)
                .hasMessage(ErrorCode.DUPLICATED_EMAIL.getMessage());
    }
}
