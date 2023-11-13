package com.limvik.econome;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.limvik.econome.domain.category.entity.Category;
import com.limvik.econome.domain.category.enums.BudgetCategory;
import com.limvik.econome.domain.user.entity.User;
import com.limvik.econome.global.config.JwtConfig;
import com.limvik.econome.global.security.jwt.provider.JwtProvider;
import com.limvik.econome.infrastructure.category.CategoryRepository;
import com.limvik.econome.infrastructure.user.UserRepository;
import com.limvik.econome.web.budgetplan.dto.BudgetPlanListRequest;
import com.limvik.econome.web.budgetplan.dto.BudgetPlanRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
	UserRepository userRepository;

	@Autowired
	CategoryRepository categoryRepository;

	@Autowired
	TestRestTemplate restTemplate;

	User user;
	String accessToken;
	String refreshToken;

	@BeforeAll
	void setup() {
		user = User.builder().id(1L)
				.username("test")
				.email("test@test.com")
				.password("password")
				.minimumDailyExpense(10000)
				.agreeAlarm(true)
				.build();
		accessToken = jwtProvider.generateAccessToken(user);
		refreshToken = jwtProvider.generateRefreshToken(user);
		user.setRefreshToken(refreshToken);
		userRepository.save(user);
	}

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
		String invalidAccessToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9." +
				"eyJpc3MiOiJsaW12aWtfZWNvbm9tZSIsImlhdCI6MTY5OTY3NDk5NSwiZXhwIjoxNjk5Njc1NTk1LCJzdWIiOiI4In0." +
				"6uvQXPz8WwXcXoNYBylmS1QWvyfdnjRSbNOg_54aP5g3jWJu7OfVugfuGb14UVJU1umMMj5Nn2KMQn4ASTiYsg";

		var headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer " + invalidAccessToken);

		HttpEntity<String> request = new HttpEntity<>(null, headers);
		ResponseEntity<String> response = restTemplate.exchange(
				"/api/v1/test", HttpMethod.POST, request, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	@DisplayName("refresh token으로 access token 재발급")
	void shouldReturnAccessTokenIfValidRefreshToken() {

		var headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer " + refreshToken);

		HttpEntity<String> request = new HttpEntity<>(null, headers);
		ResponseEntity<String> response = restTemplate.exchange(
				"/api/v1/users/token", HttpMethod.POST, request, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());

		String accessToken = documentContext.read("$.accessToken");
		assertThat(accessToken).isNotNull();
	}

	@Test
	@DisplayName("카테고리 목록 저장 여부 확인")
	void shouldSaveAllCategoriesInEnum() {
		List<Category> categories = categoryRepository.findAll();
		categories.sort(Comparator.comparing(Category::getId));
		BudgetCategory[] budgetCategories = BudgetCategory.values();
		assertThat(categories.size()).isEqualTo(budgetCategories.length);
		for (int i = 0; i < budgetCategories.length; i++) {
			assertThat(categories.get(i).getName()).isEqualTo(budgetCategories[i]);
		}
	}

	@Test
	@DisplayName("인증된 사용자의 정상적인 예산 설정")
	void shouldCreateBudgetPlanIfValidUser() {
		List<BudgetPlanRequest> requests = new ArrayList<>();
		for (long i = 1; i <= BudgetCategory.values().length; i++) {
			requests.add(new BudgetPlanRequest(i, 1000 * i));
		}
		var requestList = new BudgetPlanListRequest(requests);

		var year = 2023;
		var month = 11;

		var headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer " + accessToken);
		String url = "/api/v1/budget-plans?year=%d&month=%d".formatted(year, month);

		HttpEntity<BudgetPlanListRequest> request = new HttpEntity<>(requestList, headers);
		ResponseEntity<String> response = restTemplate.exchange(
				url, HttpMethod.POST, request, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getHeaders().getLocation()).isEqualTo(URI.create(url));
	}

}
