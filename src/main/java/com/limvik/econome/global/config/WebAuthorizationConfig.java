package com.limvik.econome.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;


/**
 * 이 설정 클래스는 어플리케이션의 web endpoint 접근 제어를 수행합니다.
 */
@EnableWebSecurity
@Configuration
public class WebAuthorizationConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        String commonApiPath = "/api/v1/";
        String signupPath = commonApiPath + "users/signup";
        String signinPath = commonApiPath + "users/signin";
        String allApiPath = commonApiPath + "**";

        return http
                .securityMatcher(allApiPath)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, signupPath, signinPath)
                            .permitAll()
                        .requestMatchers(allApiPath)
                            .authenticated()
                        .anyRequest().denyAll())
                .csrf(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .anonymous(AbstractHttpConfigurer::disable)
                .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }
}
