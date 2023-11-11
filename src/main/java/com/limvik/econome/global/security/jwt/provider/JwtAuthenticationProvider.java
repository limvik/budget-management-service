package com.limvik.econome.global.security.jwt.provider;

import com.limvik.econome.global.exception.ErrorCode;
import com.limvik.econome.global.security.authentication.BearerAuthenticationToken;
import com.limvik.econome.global.security.authentication.JwtAuthenticationToken;
import com.limvik.econome.global.security.jwt.exception.JwtAuthenticationException;
import com.limvik.econome.global.security.jwt.exception.JwtError;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Objects;

/**
 * JWT의 인증작업을 수행하는 클래스입니다.
 * Spring Security의 {@link org.springframework.security.authentication.ProviderManager} 의 인수로 사용됩니다.
 */
@Slf4j
public class JwtAuthenticationProvider implements AuthenticationProvider {

    private final JwtProvider jwtProvider;

    public JwtAuthenticationProvider(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    /**
     * {@link BearerAuthenticationToken}을 받아 JWT로 parsing 및 인증한 후 {@link JwtAuthenticationToken}을 반환합니다.
     * @param authentication {@link BearerAuthenticationToken}
     * @return {@link JwtAuthenticationToken}
     */
    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {
        var bearerToken = (BearerAuthenticationToken) authentication;
        Jws<Claims> jws = parseToken(bearerToken.getToken());
        return getAuthenticatedToken(jws, bearerToken.getToken());
    }

    private Jws<Claims> parseToken(String token) {
        Jws<Claims> jws;
        try {
            jws = jwtProvider.parse(token, jwtProvider.getAccessKey());
        } catch (SignatureException e){
            jws = jwtProvider.parse(token, jwtProvider.getRefreshKey());
            log.info("Refresh Access Token By Refresh Token");
        } catch (JwtException e) {
            var error = new JwtError(ErrorCode.INVALID_TOKEN.name(), ErrorCode.INVALID_TOKEN.getHttpStatus());
            throw new JwtAuthenticationException(error, ErrorCode.INVALID_TOKEN.getMessage());
        }
        log.info("Parse Refresh Token");
        return jws;
    }

    private Authentication getAuthenticatedToken(Jws<Claims> jws, String tokenString) {
        var auth = new JwtAuthenticationToken(jws, tokenString, List.of(new SimpleGrantedAuthority("USER")));
        auth.setPrincipal(Objects.requireNonNull(jws).getPayload().getSubject());
        return auth;
    }

    /**
     * authenticate 메서드의 인수로 받아 인증을 수행할 Token을 지정하고, 사용가능 여부를 반환합니다.
     * @param authentication 인증을 수행할 Token
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return BearerAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
