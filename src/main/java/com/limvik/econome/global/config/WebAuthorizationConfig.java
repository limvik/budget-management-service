package com.limvik.econome.global.config;

import com.limvik.econome.domain.user.service.UserService;
import com.limvik.econome.global.security.filter.UsernamePasswordAuthenticationFilter;
import com.limvik.econome.infrastructure.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.RequestCacheAwareFilter;


/**
 * 이 설정 클래스는 어플리케이션의 web endpoint 접근 제어를 수행합니다.
 */
@RequiredArgsConstructor
@EnableWebSecurity
@Configuration
public class WebAuthorizationConfig {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
                .addFilterAfter(new UsernamePasswordAuthenticationFilter(getDaoProviderManager()),
                        RequestCacheAwareFilter.class)
                .build();
    }

    /**
     * 사용자 이름과 비밀번호 로그인 시 인증 작업에 사용되는 ProviderManager를 반환합니다.
     */
    private ProviderManager getDaoProviderManager() {
        return new ProviderManager(getDaoAuthenticationProvider());
    }

    /**
     * 사용자 이름과 비밀번호 로그인 시 인증 작업에 사용할 구체적인 Provider를 반환합니다.
     */
    private DaoAuthenticationProvider getDaoAuthenticationProvider() {
        var daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(new UserService(userRepository));
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);
        return daoAuthenticationProvider;
    }
}
