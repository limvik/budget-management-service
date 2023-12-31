package com.limvik.econome.global.security.jwt.provider;

import com.limvik.econome.domain.user.entity.User;
import com.limvik.econome.global.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
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
        long expirationTime = Duration.ofMinutes(jwtConfig.getAccessTokenExpirationMinutes()).toMillis();
        SecretKey secretKey = Keys.hmacShaKeyFor(jwtConfig.getAccessKey().getBytes(StandardCharsets.UTF_8));
        return generateToken(user, expirationTime, secretKey);
    }

    public String generateRefreshToken(User user) {
        long expirationTime = Duration.ofDays(jwtConfig.getRefreshTokenExpirationDays()).toMillis();
        SecretKey secretKey = Keys.hmacShaKeyFor(jwtConfig.getRefreshKey().getBytes(StandardCharsets.UTF_8));
        return generateToken(user, expirationTime, secretKey);
    }

    private String generateToken(User user, long expirationTime, SecretKey secretKey) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);
        return Jwts.builder()
                .header().type("JWT")
                .and()
                .issuer(jwtConfig.getIssuer())
                .issuedAt(now)
                .expiration(expiryDate)
                .subject(user.getId().toString())
                .signWith(secretKey)
                .compact();
    }

    public Jws<Claims> parse(String token, SecretKey secretKey) {
        return Jwts.parser()
                .requireIssuer(jwtConfig.getIssuer())
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
    }

    public SecretKey getAccessKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getAccessKey().getBytes(StandardCharsets.UTF_8));
    }

    public SecretKey getRefreshKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getRefreshKey().getBytes(StandardCharsets.UTF_8));
    }

}