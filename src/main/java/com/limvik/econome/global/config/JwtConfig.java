package com.limvik.econome.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties("jwt")
public class JwtConfig {

    private String issuer;
    private String accessKey;
    private String refreshKey;
    private Long accessTokenExpirationMinutes;
    private Long refreshTokenExpirationDays;

}
