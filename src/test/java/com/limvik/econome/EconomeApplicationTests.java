package com.limvik.econome;

import com.limvik.econome.domain.user.entity.User;
import com.limvik.econome.global.config.JwtConfig;
import com.limvik.econome.global.security.jwt.provider.JwtProvider;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
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

	@Autowired
	TestRestTemplate restTemplate;

	@Test
	void contextLoads() {
	}

	@Test
	@DisplayName("JWT 설정 기본값 정상 로딩")
	void jwtConfigDataLoads() {
		assertThat(jwtConfig.getIssuer()).isNotNull();
		assertThat(jwtConfig.getAccessKey()).isNotNull();
		assertThat(jwtConfig.getRefreshKey()).isNotNull();
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

		assertThat(jwtProvider.parse(accessToken, jwtProvider.getAccessKey()).getPayload().getSubject()).isEqualTo("1");
		assertThat(jwtProvider.parse(refreshToken, jwtProvider.getRefreshKey()).getPayload().getSubject()).isEqualTo("1");
	}

	@Test
	@DisplayName("유효한 access token으로 엔드포인트 요청")
	void shouldReturn200OkIfValidToken() {
		var user = User.builder().id(1L).build();
		String accessToken = jwtProvider.generateAccessToken(user);

		var headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer " + accessToken);

		HttpEntity<String> request = new HttpEntity<>(null, headers);
		ResponseEntity<String> response = restTemplate.exchange(
				"/api/v1/test", HttpMethod.POST, request, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	@DisplayName("유효하지 않은 access token으로 엔드포인트 요청")
	void shouldReturn401UnauthorizedIfNotValidToken() {
		String accessToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9." +
				"eyJpc3MiOiJsaW12aWtfZWNvbm9tZSIsImlhdCI6MTY5OTY3NDk5NSwiZXhwIjoxNjk5Njc1NTk1LCJzdWIiOiI4In0." +
				"6uvQXPz8WwXcXoNYBylmS1QWvyfdnjRSbNOg_54aP5g3jWJu7OfVugfuGb14UVJU1umMMj5Nn2KMQn4ASTiYsg";

		var headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer " + accessToken);

		HttpEntity<String> request = new HttpEntity<>(null, headers);
		ResponseEntity<String> response = restTemplate.exchange(
				"/api/v1/test", HttpMethod.POST, request, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

}
