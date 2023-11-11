package com.limvik.econome.global.security.resolver;

import com.limvik.econome.global.security.jwt.exception.JwtAuthenticationException;
import com.limvik.econome.global.security.jwt.exception.JwtError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Authorization 헤더에서 Bearer 뒤의 JWT를 추출하는 클래스입니다.
 */
public class BearerResolver {

    private static final Pattern authorizationPattern = Pattern.compile("^Bearer (?<token>[a-zA-Z0-9-._~+/]+)=*$");

    /**
     * {@link HttpServletRequest} 객체에 저장된 HTTP 요청 정보로부터 Authorization 헤더 정보를 받아 JWT 토큰을 추출하고 반환합니다.
     * @param request HTTP 요청 정보가 저장된 객체
     * @return 문자열(String) 형식의 JWT 토큰
     */
    public String resolve(HttpServletRequest request) {
        return resolveFromAuthorizationHeader(request);
    }

    private String resolveFromAuthorizationHeader(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer")) {
            Matcher matcher = authorizationPattern.matcher(authorization);

            if ( !matcher.matches() ) {
                JwtError error = new JwtError("invalid_request", HttpStatus.BAD_REQUEST);
                throw new JwtAuthenticationException(error, "토큰 형식 오류");
            }

            return matcher.group("token");
        }
        return null;
    }

}

