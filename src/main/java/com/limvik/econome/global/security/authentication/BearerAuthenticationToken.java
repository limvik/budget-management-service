package com.limvik.econome.global.security.authentication;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Transient;

/**
 * Authorization 헤더에 지정된 Bearer 정보를 저장하는 클래스입니다.
 */
@Getter
@Transient
public class BearerAuthenticationToken extends AbstractAuthenticationToken {

    private final String token;

    public BearerAuthenticationToken(String token) {
        super(null);
        this.token = token;
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return token;
    }

}
