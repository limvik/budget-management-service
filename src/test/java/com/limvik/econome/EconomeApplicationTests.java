package com.limvik.econome;

import com.limvik.econome.domain.user.entity.User;
import com.limvik.econome.global.config.JwtConfig;
import com.limvik.econome.global.security.jwt.provider.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EconomeApplicationTests {

	@Autowired
	JwtConfig jwtConfig;

	@Autowired
	JwtProvider jwtProvider;

	@Test
	void contextLoads() {
	}

	@Test
	@DisplayName("JWT 설정 기본값 정상 로딩")
	void jwtConfigDataLoads() {
		assertThat(jwtConfig.getIssuer()).isNotNull();
		assertThat(jwtConfig.getKey()).isNotNull();
		assertThat(jwtConfig.getAccessTokenExpirationMinutes()).isNotNull();
		assertThat(jwtConfig.getRefreshTokenExpirationDays()).isNotNull();
	}

	@Test
	@DisplayName("token 정상 발급")
	void getTokens() {
		var user = User.builder().id(1L).build();
		String accessToken = jwtProvider.generateAccessToken(user);
		String refreshToken = jwtProvider.generateRefreshToken(user);

		assertThat(accessToken).isNotNull();
		assertThat(refreshToken).isNotNull();

		assertThat(jwtProvider.parse(accessToken).getPayload().getSubject()).isEqualTo("1");
		assertThat(jwtProvider.parse(refreshToken).getPayload().getSubject()).isEqualTo("1");
	}

}
