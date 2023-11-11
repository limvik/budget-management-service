package com.limvik.econome.global.security.authentication;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Transient;

import java.util.Collection;

/**
 * HTTP 요청의 Authorization 헤더에서 추출된 JWT 문자열을 변환된 Jws 객체 형태로 저장하는 클래스입니다.
 */
@Transient
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final Jws<Claims> token;

    private Object credentials;

    private Object principal;

    public JwtAuthenticationToken(Jws<Claims> token) {
        super(null);
        this.token = token;
    }

    public JwtAuthenticationToken(Jws<Claims> token, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.setAuthenticated(true);
        this.token = token;
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    public void setPrincipal(Object userDetails) {
        this.principal = userDetails;
    }

    public Claims getClaims() {
        return token.getPayload();
    }

}