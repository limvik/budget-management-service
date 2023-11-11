package com.limvik.econome.global.security.jwt.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * JWT 와 관련된 에러 내용을 저장하는 클래스입니다.
 */
@Getter
public class JwtError {

    private HttpStatus httpStatus;

    private final String errorCode;

    private final String description;

    private final String uri;

    public JwtError(String errorCode, String description, String uri) {
        this.errorCode = errorCode;
        this.description = description;
        this.uri = uri;
    }

    public JwtError(String errorCode, HttpStatus httpStatus) {
        this(errorCode, httpStatus, null, null);
    }

    public JwtError(String errorCode, HttpStatus httpStatus, String description, String errorUri) {
        this(errorCode, description, errorUri);
        this.httpStatus = httpStatus;
    }

}
