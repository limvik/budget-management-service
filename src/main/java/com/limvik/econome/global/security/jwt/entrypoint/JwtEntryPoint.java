package com.limvik.econome.global.security.jwt.entrypoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.limvik.econome.global.exception.ErrorCode;
import com.limvik.econome.global.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * JWT를 이용한 인증 과정에서 인증 오류 발생 시 반환할 정보를 지정하는 클래스입니다.
 */
public class JwtEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.addHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setStatus(ErrorCode.INVALID_TOKEN.getHttpStatus().value());
        String jsonResponse = new ObjectMapper().writeValueAsString(
                new ErrorResponse(ErrorCode.INVALID_TOKEN.name(), ErrorCode.INVALID_TOKEN.getMessage()));
        response.getWriter().write(jsonResponse);

    }

}
