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
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled("Spring REST Docs 용 테스트로 대체하였습니다.")
public class EconomeRecommendationTests {

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

    long monthlyBudgetPerCategory;
    int countCreatedBudget;
    int countCreatedExpense;
    int dayOfMonth;
    long dailyExpensePerCategory;

    @BeforeAll
    void setup() {
        // 기본 사용자 테스트 데이터
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

        // 예산 데이터 생성 - 예산이 없는 카테고리의 반환 여부 테스트를 위해 절반의 카테고리만 예산 설정
        monthlyBudgetPerCategory = 500000L;
        countCreatedBudget = 6;
        for (long categoryId = 1; categoryId <= countCreatedBudget; categoryId++) {
            BudgetPlan budgetPlan = BudgetPlan.builder()
                    .user(user)
                    .date(LocalDate.of(LocalDate.now().getYear(), LocalDate.now().getMonth(), 1))
                    .amount(monthlyBudgetPerCategory)
                    .category(Category.builder().id(categoryId).build())
                    .build();
            budgetPlanRepository.save(budgetPlan);
        }

        // 지출 데이터 생성 - 예산이 없는 카테고리의 지출도 반영되도록 예산 없는 카테고리 지출 기록 추가
        dayOfMonth = LocalDate.now().getDayOfMonth();
        dailyExpensePerCategory = 10000L;
        countCreatedExpense = countCreatedBudget + 1;
        var excluded = false;
        for (int days = 0; days < dayOfMonth; days++) {
            var datetime = LocalDateTime.now().minusDays(days);
            for (long categoryId = 1L; categoryId <= countCreatedExpense; categoryId++) {
                var expense = Expense.builder().datetime(datetime)
                        .category(Category.builder().id(categoryId).build())
                        .amount(dailyExpensePerCategory)
                        .memo(null)
                        .excluded(excluded)
                        .user(user)
                        .build();
                expenseRepository.save(expense);
            }
        }

    }

    @AfterAll
    void tearDown() {
        budgetPlanRepository.deleteAllInBatch();
        expenseRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("인증된 사용자의 오늘의 추천 지출 금액 조회 요청 - 예산 내 지출")
    void shouldGetRecommendedExpenseAmountIfValidUser() {

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + accessToken);
        String url = "/api/v1/expenses/recommendations";
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(response.getBody());

        // 일자별 메시지 확인
        if (dayOfMonth == 1) {
            assertThat(documentContext.read("$.message", String.class))
                    .isEqualTo("1일 이네요! 이번달도 새로운 마음으로 체계적인 지출 도전!");
        } else {
            assertThat(documentContext.read("$.message", String.class))
                    .isEqualTo("오늘도 합리적인 소비 생활 화이팅!");
        }
        // 오늘의 추천 총 지출 확인
        int lengthOfMonth = LocalDate.now().lengthOfMonth();
        int restDaysOfMonthFromToday = lengthOfMonth - dayOfMonth + 1;
        long expenseAmountPerCategory = dailyExpensePerCategory * (dayOfMonth - 1);
        long penaltyAmountPerCategory = dailyExpensePerCategory * (dayOfMonth - 1) / countCreatedBudget;
        long RecommendedTodayTotalAmountPerCategory = (monthlyBudgetPerCategory - expenseAmountPerCategory - penaltyAmountPerCategory) / restDaysOfMonthFromToday;
        long recommendedTodayTotalAmount = (RecommendedTodayTotalAmountPerCategory * countCreatedBudget) / 1000 * 1000;
        assertThat(documentContext.read("$.recommendedTodayTotalAmount", Long.class))
                .isEqualTo(recommendedTodayTotalAmount);

        // 카테고리별 추천 지출 확인 - 예산을 설정한 카테고리만 추천하는지 확인
        assertThat(documentContext.read("$.recommendations.length()", Integer.class)).isEqualTo(countCreatedBudget);
        for (int i = 0; i < countCreatedBudget; i++) {
            assertThat(documentContext.read("$.recommendations[%d].categoryId".formatted(i), Long.class))
                    .isEqualTo(i+1L);
            assertThat(documentContext.read("$.recommendations[%d].categoryName".formatted(i), String.class))
                    .isEqualTo(BudgetCategory.values()[i].getCategory());
            assertThat(documentContext.read("$.recommendations[%d].amount".formatted(i), Long.class))
                    .isEqualTo(RecommendedTodayTotalAmountPerCategory);
        }

    }

    @Test
    @DisplayName("인증된 사용자의 오늘의 지출 금액 조회 요청")
    void shouldGetExpenseAmountIfValidUser() {

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + accessToken);
        String url = "/api/v1/expenses/today";
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(response.getBody());

        long spentTotalAmount = dailyExpensePerCategory * countCreatedExpense;
        assertThat(documentContext.read("$.spentTotalAmount", Long.class))
                .isEqualTo(spentTotalAmount);

        int lengthOfMonth = LocalDate.now().lengthOfMonth();
        int restDaysOfMonthFromToday = lengthOfMonth - dayOfMonth + 1;
        long expenseAmount = dailyExpensePerCategory * (dayOfMonth - 1) * countCreatedExpense;
        long recommendedTodayTotalAmount = (monthlyBudgetPerCategory * countCreatedBudget - expenseAmount) / restDaysOfMonthFromToday / 1000 * 1000;
        long recommendedPerCategory = recommendedTodayTotalAmount / countCreatedBudget;

        for (int i = 0; i < countCreatedExpense; i++) {
            assertThat(documentContext.read("$.details[%d].categoryId".formatted(i), Long.class))
                    .isEqualTo(i+1L);
            assertThat(documentContext.read("$.details[%d].categoryName".formatted(i), String.class))
                    .isEqualTo(BudgetCategory.values()[i].getCategory());
            assertThat(documentContext.read("$.details[%d].recommendedAmount".formatted(i), Long.class))
                    .isEqualTo(i == countCreatedExpense - 1 ? 0 : recommendedPerCategory / 1000 * 1000);
            assertThat(documentContext.read("$.details[%d].spentAmount".formatted(i), Long.class))
                    .isEqualTo(dailyExpensePerCategory);

            String risk = i == countCreatedExpense - 1 ? "0%" : Math.round((double)dailyExpensePerCategory / recommendedPerCategory * 100) + "%";
            assertThat(documentContext.read("$.details[%d].risk".formatted(i), String.class))
                    .isEqualTo(risk);
        }
    }

    @Test
    @DisplayName("인증된 사용자의 월간/주간/상대적 지출 통계정보 조회 요청")
    void shouldGetStatisticsIfValidUser() {
        addOtherUserData();
        addLastMonthData();

        accessToken = jwtProvider.generateAccessToken(user);

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + accessToken);
        String url = "/api/v1/expenses/stat";
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(response.getBody());

        assertThat(documentContext.read("$.againstLastMonth.totalExpenseRate", String.class))
                .isEqualTo("100%");
        assertThat(documentContext.read("$.againstLastDayOfWeek.totalExpenseRate", String.class))
                .isEqualTo("100%");
        for (int i = 0; i < countCreatedExpense; i++) {
            assertThat(documentContext.read("$.againstLastMonth.details[%d].categoryId".formatted(i), Long.class))
                    .isEqualTo(i+1L);
            assertThat(documentContext.read("$.againstLastMonth.details[%d].categoryName".formatted(i), String.class))
                    .isEqualTo(BudgetCategory.values()[i].getCategory());
            assertThat(documentContext.read("$.againstLastMonth.details[%d].expenseRate".formatted(i), String.class))
                    .isEqualTo("100%");

            assertThat(documentContext.read("$.againstLastDayOfWeek.details[%d].categoryId".formatted(i), Long.class))
                    .isEqualTo(i+1L);
            assertThat(documentContext.read("$.againstLastDayOfWeek.details[%d].categoryName".formatted(i), String.class))
                    .isEqualTo(BudgetCategory.values()[i].getCategory());
            assertThat(documentContext.read("$.againstLastDayOfWeek.details[%d].expenseRate".formatted(i), String.class))
                    .isEqualTo("100%");
        }
        assertThat(documentContext.read("$.againstOtherUsers.relativeExpenseRate", String.class))
                .isEqualTo("100%");
    }

    private void addOtherUserData() {
        // 상대적 지출 통계를 위한 사용자 추가
        var user2 = User.builder().id(2L)
                .username("test2")
                .email("test2@test.com")
                .password("$2a$12$jxQoUurwE37F9VBEqtXEtuIfCeJ2aKvY6LkicQ5KFF5.9CZLFeNN6")
                .minimumDailyExpense(10000)
                .agreeAlarm(true)
                .build();
        user2.setRefreshToken(jwtProvider.generateRefreshToken(user2));
        userRepository.save(user2);

        // 상대적 지출 통계를 위한 다른 유저 이번달 예산 추가
        for (long categoryId = 1; categoryId <= countCreatedBudget; categoryId++) {
            BudgetPlan budgetPlan = BudgetPlan.builder()
                    .user(user2)
                    .date(LocalDate.of(LocalDate.now().getYear(), LocalDate.now().getMonth(), 1))
                    .amount(monthlyBudgetPerCategory)
                    .category(Category.builder().id(categoryId).build())
                    .build();
            budgetPlanRepository.save(budgetPlan);
        }

        // 상대적 지출 통계를 위한 다른 유저 이번달 지출 기록 추가
        for (int days = 0; days < dayOfMonth; days++) {
            var datetime = LocalDateTime.now().minusDays(days);
            for (long categoryId = 1L; categoryId <= countCreatedExpense; categoryId++) {
                var expense = Expense.builder().datetime(datetime)
                        .category(Category.builder().id(categoryId).build())
                        .amount(dailyExpensePerCategory)
                        .memo(null)
                        .excluded(false)
                        .user(user2)
                        .build();
                expenseRepository.save(expense);
            }
        }
    }

    private void addLastMonthData() {
        int lastDaysOfMonth = LocalDate.now().minusMonths(1).lengthOfMonth();
        // 지난 달 지출 기록 추가
        for (int days = dayOfMonth; days < dayOfMonth + lastDaysOfMonth; days++) {
            var datetime = LocalDateTime.now().minusDays(days);
            for (long categoryId = 1L; categoryId <= countCreatedExpense; categoryId++) {
                var expense = Expense.builder().datetime(datetime)
                        .category(Category.builder().id(categoryId).build())
                        .amount(dailyExpensePerCategory)
                        .memo(null)
                        .excluded(false)
                        .user(user)
                        .build();
                expenseRepository.save(expense);
            }
        }
    }
}
