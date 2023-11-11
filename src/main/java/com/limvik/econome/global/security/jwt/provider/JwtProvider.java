package com.limvik.econome.global.security.jwt.provider;

import com.limvik.econome.domain.user.entity.User;
import com.limvik.econome.global.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * JWT 생성 및 추출하는 기능을 수행하는 클래스입니다.
 */
@RequiredArgsConstructor
@Component
public class JwtProvider {

    private final JwtConfig jwtConfig;

    public String generateAccessToken(User user) {
        return generateToken(user, Duration.ofMinutes(jwtConfig.getAccessTokenExpirationMinutes()).toMillis());
    }

    public String generateRefreshToken(User user) {
        return generateToken(user, Duration.ofDays(jwtConfig.getRefreshTokenExpirationDays()).toMillis());
    }

    private String generateToken(User user, long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);
        return Jwts.builder()
                .header().type("JWT")
                .and()
                .issuer(jwtConfig.getIssuer())
                .issuedAt(now)
                .expiration(expiryDate)
                .subject(user.getId().toString())
                .signWith(Keys.hmacShaKeyFor(jwtConfig.getKey().getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parser()
                .requireIssuer(jwtConfig.getIssuer())
                .verifyWith(Keys.hmacShaKeyFor(jwtConfig.getKey().getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token);
    }

}