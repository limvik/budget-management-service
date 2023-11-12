package com.limvik.econome.web.util;

import com.limvik.econome.global.exception.ErrorCode;
import com.limvik.econome.global.exception.ErrorException;
import com.limvik.econome.global.security.authentication.JwtAuthenticationToken;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserUtil {

    public static long getUserIdFromJwt(JwtAuthenticationToken token) {
        try {
            return Long.parseLong(token.getClaims().getSubject());
        } catch (Exception e) {
            log.info("Authentication 객체에 저장된 사용자 정보가 없습니다.");
            throw new ErrorException(ErrorCode.INVALID_TOKEN);
        }
    }

}
