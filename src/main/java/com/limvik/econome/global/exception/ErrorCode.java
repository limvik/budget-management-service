package com.limvik.econome.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    DUPLICATED_USERNAME(HttpStatus.CONFLICT,"이미 존재하는 사용자 이름입니다."),
    DUPLICATED_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    UNPROCESSABLE_USERINFO(HttpStatus.UNPROCESSABLE_ENTITY, "입력된 정보가 형식에 맞지 않습니다."),
    NOT_EXIST_USER(HttpStatus.UNAUTHORIZED, "일치하는 사용자 정보가 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 유효하지 않습니다."),
    DUPLICATED_BUDGET_PLAN(HttpStatus.CONFLICT, "이미 예산이 설정되었습니다. 원하신다면 수정을 요청해주세요.");

    private final HttpStatus httpStatus;
    private final String message;

}
