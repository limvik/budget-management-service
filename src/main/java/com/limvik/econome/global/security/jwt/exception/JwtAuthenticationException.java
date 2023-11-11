package com.limvik.econome.global.security.jwt.exception;

import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

/**
 * JWT 인증 작업 중 오류 발생 시 던져지는 예외(Exception) 클래스입니다.
 */
@Getter
public class JwtAuthenticationException extends AuthenticationException {

    private final JwtError error;

    public JwtAuthenticationException(JwtError error, String message) {
        super(message);
        this.error = error;
    }

}
