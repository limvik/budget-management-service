package com.limvik.econome.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    DUPLICATED_USERNAME(HttpStatus.CONFLICT,"이미 존재하는 사용자 이름입니다."),
    DUPLICATED_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    UNPROCESSABLE_USERINFO(HttpStatus.UNPROCESSABLE_ENTITY, "입력된 정보가 형식에 맞지 않습니다.");

    private final HttpStatus httpStatus;
    private final String message;

}