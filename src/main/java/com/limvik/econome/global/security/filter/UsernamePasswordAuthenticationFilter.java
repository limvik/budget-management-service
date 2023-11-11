package com.limvik.econome.global.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.limvik.econome.global.exception.ErrorCode;
import com.limvik.econome.global.exception.ErrorException;
import com.limvik.econome.global.exception.ErrorResponse;
import com.limvik.econome.global.security.converter.UsernamePasswordAuthenticationConverter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
/**
 * 사용자 이름과 비밀번호를 이용한 로그인 수행 시 인증작업을 수행하는 Filter 클래스입니다.
 */
public class UsernamePasswordAuthenticationFilter extends OncePerRequestFilter {

    private final AuthenticationManager authenticationManager;

    public UsernamePasswordAuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    /**
     * HTTP 요청 정보가 저장된 {@link HttpServletRequest} 객체에서 사용자가 입력한 사용자 이름과 비밀번호를 추출하고 인증을 요청합니다.
     * @author 정성국
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        setResponseProperties(response);

        UsernamePasswordAuthenticationToken token = getTokenFromRequest(request, response);

        if (token == null)
            filterChain.doFilter(request, response);

        try {
            Authentication authenticationResult = this.authenticationManager.authenticate(token);

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authenticationResult);
            SecurityContextHolder.setContext(context);

            filterChain.doFilter(request, response);
        } catch (AuthenticationException e) {
            setUnauthorizedResponse(response);
        }
    }

    private void setResponseProperties(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
    }

    private UsernamePasswordAuthenticationToken getTokenFromRequest(HttpServletRequest request,
                                                                    HttpServletResponse response) throws IOException {
        try {
            return new UsernamePasswordAuthenticationConverter().convert(request);
        } catch (ErrorException e) {
            setUnprocessableEntityResponse(response);
        }
        return null;
    }

    private void setUnprocessableEntityResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
        String jsonResponse = new ObjectMapper().writeValueAsString(
                new ErrorResponse(ErrorCode.UNPROCESSABLE_USERINFO.name(),
                        ErrorCode.UNPROCESSABLE_USERINFO.getMessage()));
        response.getWriter().write(jsonResponse);
    }

    private void setUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        String jsonResponse = new ObjectMapper().writeValueAsString(
                new ErrorResponse(ErrorCode.NOT_EXIST_USER.name(),
                        ErrorCode.NOT_EXIST_USER.getMessage()));
        response.getWriter().write(jsonResponse);
    }

}