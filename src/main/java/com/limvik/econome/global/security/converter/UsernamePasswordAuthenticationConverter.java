package com.limvik.econome.global.security.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.limvik.econome.global.exception.ErrorCode;
import com.limvik.econome.global.exception.ErrorException;
import com.limvik.econome.web.user.dto.SigninRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.StringUtils;

import java.io.*;

/**
 * 사용자 이름과 비밀번호를 이용한 로그인 시 HTTP 요청의 Body에 있는 사용자 이름과 패스워드를 추출하여 유효성 검사를 수행하고,
 * Token 으로 만들어 반환합니다.
 */
public class UsernamePasswordAuthenticationConverter implements AuthenticationConverter {

    private static final String USERNAME_PROPERTY = "username";

    private static final String PASSWORD_PROPERTY = "password";

    /**
     * HTTP 요청에 포함된 이메일과 패스워드를 추출하고, {@link UsernamePasswordAuthenticationToken} 에 담아 반환합니다.
     * @param request {@link HttpServletRequest} 객체에 저장된 HTTP 요청 정보
     * @return {@link UsernamePasswordAuthenticationToken}
     */
    @Override
    public UsernamePasswordAuthenticationToken convert(HttpServletRequest request) throws ErrorException {
        var signinUserInfo = getSigninUserInfoFromBody(convertHttpRequestToString(request));
        if (isValidUserInfo(signinUserInfo))
            return UsernamePasswordAuthenticationToken
                    .unauthenticated(signinUserInfo.username(), signinUserInfo.password());
        else
            throw new ErrorException(ErrorCode.UNPROCESSABLE_USERINFO);
    }

    /**
     * {@link HttpServletRequest} 객체에 저장된 HTTP 요청 정보를 문자열(String) 형식으로 변환하여 반환합니다.
     * @param request {@link HttpServletRequest} 객체에 저장된 HTTP 요청 정보
     * @return HTTP 요청을 문자열(String) 형식으로 반환
     */
    private String convertHttpRequestToString(HttpServletRequest request) {

        StringBuilder stringBuilder = new StringBuilder();
        try (var bufferedReader = new BufferedReader(new InputStreamReader(copyInputStream(request)))) {
            bufferedReader.lines().forEach(stringBuilder::append);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }

        return stringBuilder.toString();
    }

    /**
     * {@link HttpServletRequest} 객체에 저장된 HTTP 요청 정보를 문자열(String) 형식으로 변환하기위한 사전 작업으로,
     * {@link ByteArrayInputStream} 으로 변환하여 반환합니다.
     * @param request {@link HttpServletRequest} 객체에 저장된 HTTP 요청 정보
     * @return {@link ByteArrayInputStream}
     */
    private ByteArrayInputStream copyInputStream(HttpServletRequest request) {
        var byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            request.getInputStream().transferTo(byteArrayOutputStream);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }

    /**
     * 문자열(String) 형태로 표현된 HTTP 요청의 body에서 JSON 형식을 추출하여 {@link JsonNode} 형식으로 변환하고,
     * 사용자 이름과 비밀번호를 추출합니다. 그리고 {@link SigninRequest}에 정보를 저장하여 반환합니다.
     * @param request 문자열 형태로 표현된 HTTP 요청
     * @return {@link SigninRequest}
     */
    private SigninRequest getSigninUserInfoFromBody(String request) {
        JsonNode jsonNode;
        try {
            jsonNode = new ObjectMapper().readTree(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        String username = jsonNode.get(USERNAME_PROPERTY).asText();
        String password = jsonNode.get(PASSWORD_PROPERTY).asText();

        return new SigninRequest(username, password);
    }

    /**
     * 사용자 이름과 비밀번호의 유효성을 검사한 결과를 반환합니다.
     * @param userInfo 사용자가 입력한 사용자 이름과 비밀번호 정보
     * @return 유효성 검사 통과 여부
     */
    private boolean isValidUserInfo(SigninRequest userInfo) {
        return isValidUsername(userInfo.username()) && isValidPassword(userInfo.password());
    }

    private boolean isValidUsername(String username) {
        return StringUtils.hasText(username) && username.length() <= 20;
    }

    private boolean isValidPassword(String password) {
        return StringUtils.hasText(password) && password.length() >= 8 && password.length() <= 64;
    }
}
