package com.limvik.econome;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.limvik.econome.domain.budgetplan.entity.BudgetPlan;
import com.limvik.econome.domain.category.entity.Category;
import com.limvik.econome.domain.category.enums.BudgetCategory;
import com.limvik.econome.domain.expense.entity.Expense;
import com.limvik.econome.domain.user.entity.User;
import com.limvik.econome.global.config.JwtConfig;
import com.limvik.econome.global.security.jwt.provider.JwtProvider;
import com.limvik.econome.infrastructure.budgetplan.BudgetPlanRepository;
import com.limvik.econome.infrastructure.category.CategoryRepository;
import com.limvik.econome.infrastructure.expense.ExpenseRepository;
import com.limvik.econome.infrastructure.user.UserRepository;
import com.limvik.econome.web.budgetplan.dto.BudgetPlanListRequest;
import com.limvik.econome.web.budgetplan.dto.BudgetPlanRequest;
import com.limvik.econome.web.expense.dto.ExpenseRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
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
	BudgetPlanRepository budgetPlanRepository;

	@Autowired
	ExpenseRepository expenseRepository;

	@Autowired
	TestRestTemplate restTemplate;

	User user;
	String accessToken;
	String refreshToken;

	int budgetYear = 2023;
	int budgetMonth = 1;

	@BeforeAll
	void setup() {
		user = User.builder().id(1L)
				.username("test")
				.email("test@test.com")
				.password("$2a$12$jxQoUurwE37F9VBEqtXEtuIfCeJ2aKvY6LkicQ5KFF5.9CZLFeNN6")
				.minimumDailyExpense(10000)
				.agreeAlarm(true)
				.build();
		accessToken = jwtProvider.generateAccessToken(user);
		refreshToken = jwtProvider.generateRefreshToken(user);
		user.setRefreshToken(refreshToken);
		userRepository.save(user);

		List<BudgetPlan> budgetPlans = new ArrayList<>();
		for (long i = 1; i <= BudgetCategory.values().length; i++) {
			budgetPlans.add(BudgetPlan.builder()
					.user(user)
					.category(Category.builder().id(i).build())
					.amount(i * 1000)
					.date(LocalDate.of(budgetYear, budgetMonth, 1))
					.build()
			);
		}

		budgetPlanRepository.saveAll(budgetPlans);

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

	@Test
	@DisplayName("인증된 사용자의 예산 데이터 조회")
	void shouldGetBudgetPlansIfValidUser() {
		var headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer " + accessToken);
		String url = "/api/v1/budget-plans?year=%d&month=%d".formatted(budgetYear, budgetMonth);

		HttpEntity<String> request = new HttpEntity<>(null, headers);
		ResponseEntity<String> response = restTemplate.exchange(
				url, HttpMethod.GET, request, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		DocumentContext documentContext = JsonPath.parse(response.getBody());
		for (int i = 0; i < BudgetCategory.values().length; i++) {
			assertThat(documentContext.read("$.budgetPlans[%d].categoryId".formatted(i), Long.class))
					.isEqualTo(i + 1);
			assertThat(documentContext.read("$.budgetPlans[%d].categoryName".formatted(i), String.class))
					.isEqualTo(BudgetCategory.values()[i].getCategory());
			assertThat(documentContext.read("$.budgetPlans[%d].amount".formatted(i), Long.class))
					.isEqualTo((i + 1) * 1000L);
		}
	}

	@Test
	@DisplayName("인증된 사용자의 예산 데이터 수정")
	void shouldUpdateBudgetPlanIfValidUser() {
		var headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer " + accessToken);

		var newBudget = 20000L;
		var requests = new ArrayList<BudgetPlanRequest>();
		for (int i = 0; i < BudgetCategory.values().length; i++) {
			requests.add(new BudgetPlanRequest(i + 1, newBudget));
		}
		var requestList = new BudgetPlanListRequest(requests);

		String url = "/api/v1/budget-plans?year=%d&month=%d".formatted(budgetYear, budgetMonth);
		HttpEntity<BudgetPlanListRequest> request = new HttpEntity<>(requestList, headers);
		ResponseEntity<String> response = restTemplate.exchange(
				url, HttpMethod.PATCH, request, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		var budgetList = budgetPlanRepository.findAllByUserAndDate(user, LocalDate.of(budgetYear, budgetMonth, 1));
		budgetList.forEach(budgetPlan -> {
			assertThat(budgetList.size()).isEqualTo(BudgetCategory.values().length);
			assertThat(budgetPlan.getAmount()).isEqualTo(newBudget);
		});

	}

	@Test
	@DisplayName("인증된 사용자의 예산 추천 요청")
	void shouldReturnRecommendedBudgetPlansIfValidUser() {
		for (long i = 2; i <= 5; i++) {
			var user = User.builder().id(i)
					.username("test" + i)
					.email("test%d@test.com".formatted(i))
					.password("$2a$12$jxQoUurwE37F9VBEqtXEtuIfCeJ2aKvY6LkicQ5KFF5.9CZLFeNN6")
					.minimumDailyExpense(10000)
					.agreeAlarm(true)
					.build();
			userRepository.save(user);
		}

		for (long i = 1; i <= 5; i++) {
			List<BudgetPlan> budgetPlans = new ArrayList<>();
			for (long j = 1; j <= BudgetCategory.values().length; j++) {
				budgetPlans.add(BudgetPlan.builder()
						.user(userRepository.findById(i).get())
						.category(Category.builder().id(j).build())
						.amount(j * 1000)
						.date(LocalDate.of(budgetYear, budgetMonth, 1))
						.build()
				);
			}
			budgetPlanRepository.saveAll(budgetPlans);
		}

		var headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer " + accessToken);
		String url = "/api/v1/budget-plans/recommendations?amount=%d".formatted(1000000);
		HttpEntity<String> request = new HttpEntity<>(null, headers);
		ResponseEntity<String> response = restTemplate.exchange(
				url, HttpMethod.GET, request, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());

		for (int i = 1; i < BudgetCategory.values().length; i++) {
			assertThat(documentContext.read("$.budgetPlans[%d].categoryId".formatted(i), Long.class))
					.isEqualTo(i + 1);
			assertThat(documentContext.read("$.budgetPlans[%d].categoryName".formatted(i), String.class))
					.isEqualTo(BudgetCategory.values()[i].getCategory());
			assertThat(documentContext.read("$.budgetPlans[%d].amount".formatted(i), Long.class))
					.isGreaterThan(documentContext.read("$.budgetPlans[%d].amount".formatted(i-1), Long.class));
		}
	}

	@Test
	@DisplayName("인증된 사용자의 정상적인 지출 기록 생성 요청")
	void shouldCreateExpenseIfValidUser() {
		var headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer " + accessToken);

		String datetime = "2023-12-31T09:30:00Z";
		long categoryId = 1L;
		long amount = 100000;
		String memo = "memo";
		boolean excluded = false;

		var request = new ExpenseRequest(Instant.parse(datetime), categoryId, amount, memo, excluded);

		String url = "/api/v1/expenses";
		HttpEntity<ExpenseRequest> entity = new HttpEntity<>(request, headers);
		ResponseEntity<String> response = restTemplate.exchange(
				url, HttpMethod.POST, entity, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getHeaders().getLocation()).isEqualTo(URI.create(url + "/" + expenseRepository.count()));
	}

	@Test
	@DisplayName("인증된 사용자의 정상적인 지출 기록 상세조회 요청")
	void shouldGetExpenseIfValidUser() {
		String datetime = "2023-12-31T09:30:00Z";
		long categoryId = 1L;
		long amount = 100000;
		String memo = "memo";
		boolean excluded = false;
		var expense = Expense.builder().datetime(Instant.parse(datetime))
				.category(Category.builder().id(categoryId).build())
				.amount(amount)
				.memo(memo)
				.excluded(excluded)
				.user(user)
				.build();
		expense = expenseRepository.save(expense);


		var headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer " + accessToken);
		String url = "/api/v1/expenses/" + expense.getId();
		HttpEntity<String> request = new HttpEntity<>(null, headers);
		ResponseEntity<String> response = restTemplate.exchange(
				url, HttpMethod.GET, request, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		DocumentContext documentContext = JsonPath.parse(response.getBody());
		assertThat(documentContext.read("$.datetime", String.class)).isEqualTo(datetime);
		assertThat(documentContext.read("$.categoryId", Long.class)).isEqualTo(categoryId);
		assertThat(documentContext.read("$.amount", Long.class)).isEqualTo(amount);
		assertThat(documentContext.read("$.memo", String.class)).isEqualTo(memo);
		assertThat(documentContext.read("$.excluded", Boolean.class)).isEqualTo(excluded);
		assertThat(documentContext.read("$.id", Long.class)).isEqualTo(expense.getId());

	}

	@Test
	@DisplayName("인증된 사용자의 정상적인 지출기록 수정 요청")
	void shouldUpdateExpenseIfValidUser() {
		var datetime = "2023-12-31T09:30:00Z";
		var categoryId = 1L;
		var amount = 100000L;
		var memo = "memo";
		var excluded = false;
		var expense = Expense.builder().datetime(Instant.parse(datetime))
				.category(Category.builder().id(categoryId).build())
				.amount(amount)
				.memo(memo)
				.excluded(excluded)
				.user(user)
				.build();
		expense = expenseRepository.save(expense);

		var updatedDatetime = "2023-12-31T13:30:00Z";
		var updatedCategoryId = 2L;
		var updatedAmount = 200000L;
		var updatedMemo = "updated memo";
		boolean updatedExcluded = true;
		var updateExpenseRequest = new ExpenseRequest(
				Instant.parse(updatedDatetime), updatedCategoryId, updatedAmount, updatedMemo, updatedExcluded);

		var headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer " + accessToken);
		String url = "/api/v1/expenses/" + expense.getId();
		HttpEntity<ExpenseRequest> request = new HttpEntity<>(updateExpenseRequest, headers);
		ResponseEntity<String> response = restTemplate.exchange(
				url, HttpMethod.PATCH, request, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		var updatedExpense = expenseRepository.findById(expense.getId());
		assertThat(updatedExpense.get().getDatetime()).isEqualTo(Instant.parse(updatedDatetime));
		assertThat(updatedExpense.get().getAmount()).isEqualTo(updatedAmount);
		assertThat(updatedExpense.get().getMemo()).isEqualTo(updatedMemo);
		assertThat(updatedExpense.get().isExcluded()).isEqualTo(updatedExcluded);

	}

}
