package com.limvik.econome.docs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.limvik.econome.domain.budgetplan.entity.BudgetPlan;
import com.limvik.econome.domain.category.entity.Category;
import com.limvik.econome.domain.category.enums.BudgetCategory;
import com.limvik.econome.domain.expense.entity.Expense;
import com.limvik.econome.domain.user.entity.User;
import com.limvik.econome.global.config.JwtConfig;
import com.limvik.econome.global.exception.ErrorCode;
import com.limvik.econome.global.security.jwt.provider.JwtProvider;
import com.limvik.econome.infrastructure.budgetplan.BudgetPlanRepository;
import com.limvik.econome.infrastructure.expense.ExpenseRepository;
import com.limvik.econome.infrastructure.user.UserRepository;
import com.limvik.econome.web.expense.dto.*;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.headers.RequestHeadersSnippet;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.payload.RequestFieldsSnippet;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.restdocs.request.PathParametersSnippet;
import org.springframework.restdocs.request.QueryParametersSnippet;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.hamcrest.Matchers.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyUris;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.restassured.RestAssuredRestDocumentation.document;
import static org.springframework.restdocs.restassured.RestAssuredRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.snippet.Attributes.key;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@ExtendWith(RestDocumentationExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ExpenseTest {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JwtProvider jwtProvider;

    @Autowired
    JwtConfig jwtConfig;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ExpenseRepository expenseRepository;

    @Autowired
    BudgetPlanRepository budgetPlanRepository;

    @LocalServerPort
    int port;

    RequestSpecification spec;

    String accessToken;

    User user;

    final int COUNT_CATEGORY = 6;

    final String EXPENSES_URL = "/api/v1/expenses";
    final String EXPENSES_LIST_QUERY_PARAMS = "?startDate=%s&endDate=%s&categoryId=%d&minAmount=%d&maxAmount=%d";
    long createdExpenseId;

    @BeforeEach
    public void setUp(RestDocumentationContextProvider restDocumentation) {
        this.spec = new RequestSpecBuilder()
                .addFilter(documentationConfiguration(restDocumentation).operationPreprocessors()
                        .withRequestDefaults(modifyUris().scheme("http").host("econome.com").removePort(), prettyPrint())
                        .withResponseDefaults(prettyPrint()))
                .setPort(this.port)
                .build();
    }

    @BeforeAll
    public void init() {
        user = User.builder()
                .username("expensetest")
                .email("expensetest@expensetest.com")
                .password("expensetest")
                .minimumDailyExpense(10000L)
                .agreeAlarm(false)
                .build();
        user = userRepository.save(user);

        this.accessToken = jwtProvider.generateAccessToken(user);
    }

    @AfterEach
    public void tearDown() {
        expenseRepository.deleteAllInBatch();
        expenseRepository.flush();
        budgetPlanRepository.deleteAllInBatch();
        budgetPlanRepository.flush();
    }

    @AfterAll
    public void destroy() {
        userRepository.deleteAllInBatch();
        userRepository.flush();
    }

    @Test
    @DisplayName("지출 생성 성공")
    void shouldCreateExpenseWithReturn201IfValidToken() throws JsonProcessingException {
        String createdExpensePath = RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("expense-create", getAccessTokenRequestHeaderSnippet(), getExpenseRequestFields()))
                    .header("Authorization", "Bearer " + accessToken)
                    .body(objectMapper.writeValueAsString(getExpenseRequest()))
                .when()
                    .post(EXPENSES_URL)
                .then()
                    .statusCode(is(HttpStatus.CREATED.value()))
                    .body(emptyString())
                    .header("Location", startsWith(EXPENSES_URL))
                .extract().header("Location");
        createdExpenseId = Long.parseLong(createdExpensePath.substring(createdExpensePath.lastIndexOf("/") + 1));
    }

    @Test
    @DisplayName("지출 생성 실패 - 유효하지 않은 토큰")
    void shouldNotCreateExpenseWithReturn401IfInvalidToken() throws JsonProcessingException {
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("expense-create-unauthorized"))
                    .header("Authorization", "Bearer " + "expense.invalid.accessToken")
                    .body(objectMapper.writeValueAsString(getExpenseRequest()))
                .when()
                    .post(EXPENSES_URL)
                .then()
                    .statusCode(is(ErrorCode.INVALID_TOKEN.getHttpStatus().value()))
                    .body("errorCode", is(ErrorCode.INVALID_TOKEN.name()))
                    .body("errorReason", is(ErrorCode.INVALID_TOKEN.getMessage()));
    }

    private ExpenseRequest getExpenseRequest() {
        return new ExpenseRequest(
                LocalDateTime.now(),
                1L,
                10000L,
                "memo",
                false
        );
    }

    @Test
    @DisplayName("지출 수정 성공")
    void shouldUpdateExpenseWithReturn200IfValidToken() throws JsonProcessingException {
        shouldCreateExpenseWithReturn201IfValidToken();
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("expense-update", getAccessTokenRequestHeaderSnippet(), getExpenseRequestFields(), getExpensePathParametersSnippet()))
                    .header("Authorization", "Bearer " + accessToken)
                    .body(objectMapper.writeValueAsString(getExpenseUpdateRequest()))
                .when()
                    .patch(EXPENSES_URL + "/{id}", createdExpenseId)
                .then()
                    .statusCode(is(HttpStatus.OK.value()))
                    .body(emptyString());
    }

    @Test
    @DisplayName("지출 수정 실패 - 유효하지 않은 토큰")
    void shouldNotUpdateExpenseWithReturn401IfInvalidToken() throws JsonProcessingException {
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("expense-update-unauthorized"))
                    .header("Authorization", "Bearer " + "expense.invalid.accessToken")
                    .body(objectMapper.writeValueAsString(getExpenseUpdateRequest()))
                .when()
                    .patch(EXPENSES_URL + "/{id}", createdExpenseId)
                .then()
                    .statusCode(is(ErrorCode.INVALID_TOKEN.getHttpStatus().value()))
                    .body("errorCode", is(ErrorCode.INVALID_TOKEN.name()))
                    .body("errorReason", is(ErrorCode.INVALID_TOKEN.getMessage()));;
    }

    @Test
    @DisplayName("지출 수정 실패 - 존재하지 않는 지출")
    void shouldNotUpdateExpenseWithReturn404IfNotExistExpense() throws JsonProcessingException {
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("expense-update-not-found"))
                    .header("Authorization", "Bearer " + accessToken)
                    .body(objectMapper.writeValueAsString(getExpenseUpdateRequest()))
                .when()
                    .patch(EXPENSES_URL + "/{id}", 999999)
                .then()
                    .statusCode(is(ErrorCode.NOT_EXIST_EXPENSE.getHttpStatus().value()))
                    .body("errorCode", is(ErrorCode.NOT_EXIST_EXPENSE.name()))
                    .body("errorReason", is(ErrorCode.NOT_EXIST_EXPENSE.getMessage()));
    }

    private ExpenseRequest getExpenseUpdateRequest() {
        return new ExpenseRequest(
                LocalDateTime.now(),
                2L,
                20000L,
                "22222",
                true
        );
    }

    private RequestFieldsSnippet getExpenseRequestFields() {
        return requestFields(
                fieldWithPath("datetime")
                        .type(JsonFieldType.STRING)
                        .description("지출 일시")
                        .attributes(key("constraints").value("Nullable")),
                fieldWithPath("categoryId")
                        .type(JsonFieldType.NUMBER)
                        .description("예산 카테고리 id")
                        .attributes(key("constraints").value("Not Null\n카테고리에 해당하는 id만 사용 가능. 예산 카테고리 목록 조회 엔드포인트 참고.")),
                fieldWithPath("amount")
                        .type(JsonFieldType.NUMBER)
                        .description("지출 금액")
                        .attributes(key("constraints").value("Nullable\n최소 0 이상")),
                fieldWithPath("memo")
                        .type(JsonFieldType.STRING)
                        .description("지출 메모")
                        .attributes(key("constraints").value("Nullable\n최대 60 글자")),
                fieldWithPath("excluded")
                        .type(JsonFieldType.BOOLEAN)
                        .description("지출 총액 제외 여부")
                        .attributes(key("constraints").value("Nullable")));
    }

    @Test
    @DisplayName("지출 조회 성공")
    void shouldGetExpenseWithReturn200IfValidToken() throws JsonProcessingException {
        shouldCreateExpenseWithReturn201IfValidToken();
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("expense-get", getAccessTokenRequestHeaderSnippet(), getExpenseResponseFields(), getExpensePathParametersSnippet()))
                    .header("Authorization", "Bearer " + accessToken)
                .when()
                    .get(EXPENSES_URL + "/{id}", createdExpenseId)
                .then()
                    .statusCode(is(HttpStatus.OK.value()))
                    .body(is(objectMapper.writeValueAsString(getExpenseResponse())));
    }

    private ResponseFieldsSnippet getExpenseResponseFields() {
        return responseFields(
                fieldWithPath("id")
                        .type(JsonFieldType.NUMBER)
                        .description("지출 id"),
                fieldWithPath("datetime")
                        .type(JsonFieldType.STRING)
                        .description("지출 일시"),
                fieldWithPath("categoryId")
                        .type(JsonFieldType.NUMBER)
                        .description("예산 카테고리 id"),
                fieldWithPath("categoryName")
                        .type(JsonFieldType.STRING)
                        .description("예산 카테고리 이름"),
                fieldWithPath("amount")
                        .type(JsonFieldType.NUMBER)
                        .description("지출 금액"),
                fieldWithPath("memo")
                        .type(JsonFieldType.STRING)
                        .description("지출 메모"),
                fieldWithPath("excluded")
                        .type(JsonFieldType.BOOLEAN)
                        .description("지출 총액 제외 여부"));
    }

    private ExpenseResponse getExpenseResponse() {
        var expenseRequest = getExpenseRequest();
        return new ExpenseResponse(
                createdExpenseId,
                expenseRepository.findById(createdExpenseId).get().getDatetime(),
                expenseRequest.categoryId(),
                BudgetCategory.values()[(int)(expenseRequest.categoryId() - 1)].getCategory(),
                expenseRequest.amount(),
                expenseRequest.memo(),
                expenseRequest.excluded()
        );
    }

    @Test
    @DisplayName("지출 조회 실패 - 유효하지 않은 토큰")
    void shouldNotGetExpenseWithReturn401IfInvalidToken() {
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("expense-get-unauthorized"))
                    .header("Authorization", "Bearer " + "expense.invalid.accessToken")
                .when()
                    .get(EXPENSES_URL + "/{id}", 999999)
                .then()
                    .statusCode(is(ErrorCode.INVALID_TOKEN.getHttpStatus().value()))
                    .body("errorCode", is(ErrorCode.INVALID_TOKEN.name()))
                    .body("errorReason", is(ErrorCode.INVALID_TOKEN.getMessage()));;
    }

    @Test
    @DisplayName("지출 조회 실패 - 존재하지 않는 지출")
    void shouldNotGetExpenseWithReturn404IfNotExistExpense() {
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("expense-get-not-found"))
                    .header("Authorization", "Bearer " + accessToken)
                .when()
                    .get(EXPENSES_URL + "/{id}", 999999)
                .then()
                    .statusCode(is(ErrorCode.NOT_EXIST_EXPENSE.getHttpStatus().value()))
                    .body("errorCode", is(ErrorCode.NOT_EXIST_EXPENSE.name()))
                    .body("errorReason", is(ErrorCode.NOT_EXIST_EXPENSE.getMessage()));;
    }

    @Test
    @DisplayName("지출 삭제 성공")
    void shouldDeleteExpenseWithReturn204IfValidToken() throws JsonProcessingException {
        shouldCreateExpenseWithReturn201IfValidToken();
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("expense-delete", getAccessTokenRequestHeaderSnippet(), getExpensePathParametersSnippet()))
                    .header("Authorization", "Bearer " + accessToken)
                .when()
                    .delete(EXPENSES_URL + "/{id}", createdExpenseId)
                .then()
                    .statusCode(is(HttpStatus.NO_CONTENT.value()))
                    .body(emptyString());
    }

    @Test
    @DisplayName("지출 삭제 실패 - 유효하지 않은 토큰")
    void shouldNotDeleteExpenseWithReturn401IfInvalidToken() {
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("expense-delete-unauthorized"))
                    .header("Authorization", "Bearer " + "expense.invalid.accessToken")
                .when()
                    .delete(EXPENSES_URL + "/{id}", 999999)
                .then()
                    .statusCode(is(ErrorCode.INVALID_TOKEN.getHttpStatus().value()))
                    .body("errorCode", is(ErrorCode.INVALID_TOKEN.name()))
                    .body("errorReason", is(ErrorCode.INVALID_TOKEN.getMessage()));;
    }

    @Test
    @DisplayName("지출 삭제 실패 - 존재하지 않는 지출")
    void shouldNotDeleteExpenseWithReturn404IfNotExistExpense() {
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("expense-delete-not-found"))
                    .header("Authorization", "Bearer " + accessToken)
                .when()
                    .delete(EXPENSES_URL + "/{id}", 999999)
                .then()
                    .statusCode(is(ErrorCode.NOT_EXIST_EXPENSE.getHttpStatus().value()))
                    .body("errorCode", is(ErrorCode.NOT_EXIST_EXPENSE.name()))
                    .body("errorReason", is(ErrorCode.NOT_EXIST_EXPENSE.getMessage()));;
    }

    private PathParametersSnippet getExpensePathParametersSnippet() {
        return pathParameters(
                parameterWithName("id")
                        .description("지출 id"));
    }

    @Test
    @DisplayName("지출 목록 조회 성공")
    void shouldGetExpenseListWithReturn200IfValidToken() throws JsonProcessingException {
        shouldCreateExpenseWithReturn201IfValidToken();
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("expense-list", getAccessTokenRequestHeaderSnippet(), getExpenseListQueryParametersSnippet(), getExpenseListResponseFields()))
                    .header("Authorization", "Bearer " + accessToken)
                .when()
                    .get(EXPENSES_URL + EXPENSES_LIST_QUERY_PARAMS.formatted(LocalDate.now().toString(), LocalDate.now().toString(), getExpenseRequest().categoryId(), 0, 99999))
                .then()
                    .statusCode(is(HttpStatus.OK.value()))
                    .body(is(objectMapper.writeValueAsString(getExpenseListResponse())));
    }

    private QueryParametersSnippet getExpenseListQueryParametersSnippet() {
        return queryParameters(
                parameterWithName("startDate")
                        .description("조회 범위 시작 연월일"),
                parameterWithName("endDate")
                        .description("조회 범위 마지막 연월일"),
                parameterWithName("categoryId")
                        .description("조회 대상 카테고리 id"),
                parameterWithName("minAmount")
                        .description("조회 대상 지출 최소 금액"),
                parameterWithName("maxAmount")
                        .description("조회 대상 지출 최대 금액"));
    }

    private ResponseFieldsSnippet getExpenseListResponseFields() {
        return responseFields(
                fieldWithPath("expenses[].id")
                        .type(JsonFieldType.NUMBER)
                        .description("지출 id"),
                fieldWithPath("expenses[].datetime")
                        .type(JsonFieldType.STRING)
                        .description("지출 일시"),
                fieldWithPath("expenses[].categoryId")
                        .type(JsonFieldType.NUMBER)
                        .description("예산 카테고리 id"),
                fieldWithPath("expenses[].categoryName")
                        .type(JsonFieldType.STRING)
                        .description("예산 카테고리 이름"),
                fieldWithPath("expenses[].amount")
                        .type(JsonFieldType.NUMBER)
                        .description("지출 금액"),
                fieldWithPath("expenses[].memo")
                        .type(JsonFieldType.STRING)
                        .description("지출 메모"),
                fieldWithPath("expenses[].excluded")
                        .type(JsonFieldType.BOOLEAN)
                        .description("지출 총액 제외 여부"),
                fieldWithPath("totalAmount")
                        .type(JsonFieldType.NUMBER)
                        .description("기간 내 지출 총액"),
                fieldWithPath("totalAmountForCategory")
                        .type(JsonFieldType.NUMBER)
                        .description("조회한 카테고리의 지출 총액"));
    }

    private ExpenseListResponse getExpenseListResponse() {
        var expenseResponse = getExpenseResponse();
        return new ExpenseListResponse(
                List.of(expenseResponse),
                expenseResponse.amount(),
                expenseResponse.amount()
        );
    }

    @Test
    @DisplayName("지출 목록 조회 실패 - 유효하지 않은 토큰")
    void shouldNotGetExpenseListWithReturn401IfInvalidToken() {
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("expense-list-unauthorized"))
                    .header("Authorization", "Bearer " + "expense.invalid.accessToken")
                .when()
                    .get(EXPENSES_URL + EXPENSES_LIST_QUERY_PARAMS.formatted(LocalDate.now().toString(), LocalDate.now().toString(), getExpenseRequest().categoryId(), 0, 99999))
                .then()
                    .statusCode(is(ErrorCode.INVALID_TOKEN.getHttpStatus().value()))
                    .body("errorCode", is(ErrorCode.INVALID_TOKEN.name()))
                    .body("errorReason", is(ErrorCode.INVALID_TOKEN.getMessage()));;
    }

    @Test
    @DisplayName("오늘 지출 추천 성공")
    void shouldGetExpenseRecommendationsWithReturn200IfValidToken() throws JsonProcessingException {
        createData(user, LocalDate.now().getDayOfMonth() - 1);
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("expense-recommendations", getAccessTokenRequestHeaderSnippet(), getExpenseRecommendationsListResponseFields()))
                    .header("Authorization", "Bearer " + accessToken)
                .when()
                    .get(EXPENSES_URL + "/recommendations")
                .then()
                    .statusCode(is(HttpStatus.OK.value()))
                    .body(is(objectMapper.writeValueAsString(getExpenseRecommendationListResponse())));
    }

    private ResponseFieldsSnippet getExpenseRecommendationsListResponseFields() {
        return responseFields(
                fieldWithPath("recommendedTodayTotalAmount")
                        .type(JsonFieldType.NUMBER)
                        .description("추천하는 오늘 총 금액"),
                fieldWithPath("message")
                        .type(JsonFieldType.STRING)
                        .description("사용자 응원 메시지"),
                fieldWithPath("recommendations[].categoryId")
                        .type(JsonFieldType.NUMBER)
                        .description("예산 카테고리 id"),
                fieldWithPath("recommendations[].categoryName")
                        .type(JsonFieldType.STRING)
                        .description("예산 카테고리 이름"),
                fieldWithPath("recommendations[].amount")
                        .type(JsonFieldType.NUMBER)
                        .description("지출 금액"));
    }

    private RecommendationExpenseListResponse getExpenseRecommendationListResponse() {
        return new RecommendationExpenseListResponse(
                10000L * COUNT_CATEGORY,
                "오늘도 합리적인 소비 생활 화이팅!",
                getExpenseRecommendationsResponse()
        );
    }

    private List<RecommendationExpenseResponse> getExpenseRecommendationsResponse() {
        List<RecommendationExpenseResponse> recommendations = new ArrayList<>();
        for (long categoryId = 1; categoryId <= COUNT_CATEGORY; categoryId++) {
            recommendations.add(new RecommendationExpenseResponse(
                    categoryId,
                    BudgetCategory.values()[(int) (categoryId-1)].getCategory(),
                    10000L));
        }
        return recommendations;
    }

    @Test
    @DisplayName("오늘 지출 추천 실패 - 유효하지 않은 토큰")
    void shouldNotGetExpenseRecommendationsWithReturn401IfInvalidToken() {
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("expense-recommendations-unauthorized"))
                    .header("Authorization", "Bearer " + "expense.invalid.accessToken")
                .when()
                    .get(EXPENSES_URL + "/recommendations")
                .then()
                    .statusCode(is(ErrorCode.INVALID_TOKEN.getHttpStatus().value()))
                    .body("errorCode", is(ErrorCode.INVALID_TOKEN.name()))
                    .body("errorReason", is(ErrorCode.INVALID_TOKEN.getMessage()));
    }

    @Test
    @DisplayName("오늘의 지출 안내 성공")
    void shouldGetTodayExpenseWithReturn200IfValidToken() throws JsonProcessingException {
        createData(user, LocalDate.now().getDayOfMonth() - 1);
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("expense-today", getAccessTokenRequestHeaderSnippet(), getTodayExpenseResponseFields()))
                    .header("Authorization", "Bearer " + accessToken)
                .when()
                    .get(EXPENSES_URL + "/today")
                .then()
                    .statusCode(is(HttpStatus.OK.value()))
                    .body(is(objectMapper.writeValueAsString(getTodayExpenseListResponse())));
    }

    private ResponseFieldsSnippet getTodayExpenseResponseFields() {
        return responseFields(
                fieldWithPath("spentTotalAmount")
                        .type(JsonFieldType.NUMBER)
                        .description("오늘의 총 지출금액"),
                fieldWithPath("details[].categoryId")
                        .type(JsonFieldType.NUMBER)
                        .description("예산 카테고리 id"),
                fieldWithPath("details[].categoryName")
                        .type(JsonFieldType.STRING)
                        .description("예산 카테고리 이름"),
                fieldWithPath("details[].recommendedAmount")
                        .type(JsonFieldType.NUMBER)
                        .description("오늘 추천 금액"),
                fieldWithPath("details[].spentAmount")
                        .type(JsonFieldType.NUMBER)
                        .description("오늘 소비 금액"),
                fieldWithPath("details[].risk")
                        .type(JsonFieldType.STRING)
                        .description("리스크 - 오늘 추천 금액 대비 소비 금액 백분율"));
    }

    private TodayExpenseListResponse getTodayExpenseListResponse() {
        return new TodayExpenseListResponse(
                10000L * COUNT_CATEGORY,
                getTodayExpenseResponses()
        );
    }

    private List<TodayExpenseResponse> getTodayExpenseResponses() {
        List<TodayExpenseResponse> todayExpenseResponses = new ArrayList<>();
        for (long categoryId = 1; categoryId <= COUNT_CATEGORY; categoryId++) {
            todayExpenseResponses.add(new TodayExpenseResponse(
                    categoryId,
                    BudgetCategory.values()[(int) (categoryId-1)].getCategory(),
                    10000L,
                    10000L,
                    "100%"
            ));
        }
        return todayExpenseResponses;
    }

    @Test
    @DisplayName("오늘의 지출 안내 실패 - 유효하지 않은 토큰")
    void shouldNotGetTodayExpenseWithReturn401IfInvalidToken() {
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("expense-today-unauthorized"))
                    .header("Authorization", "Bearer " + "expense.invalid.accessToken")
                .when()
                    .get(EXPENSES_URL + "/today")
                .then()
                    .statusCode(is(ErrorCode.INVALID_TOKEN.getHttpStatus().value()))
                    .body("errorCode", is(ErrorCode.INVALID_TOKEN.name()))
                    .body("errorReason", is(ErrorCode.INVALID_TOKEN.getMessage()));;
    }

    @Test
    @DisplayName("지출 통계 조회 성공")
    void shouldGetExpenseStatisticsWithReturn200IfValidToken() throws JsonProcessingException {
        createData(user, LocalDate.now().getDayOfMonth() + LocalDate.now().minusMonths(1).lengthOfMonth() - 1);
        createData(createNewUser(), LocalDate.now().getDayOfMonth() - 1);
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("expense-statistics", getAccessTokenRequestHeaderSnippet(), getExpenseStatisticsResponseFields()))
                    .header("Authorization", "Bearer " + accessToken)
                .when()
                    .get(EXPENSES_URL + "/stat")
                .then()
                    .statusCode(is(HttpStatus.OK.value()))
                    .body(is(objectMapper.writeValueAsString(getExpenseStatisticsResponse("100%"))));
    }

    private ResponseFieldsSnippet getExpenseStatisticsResponseFields() {
        return responseFields(
                fieldWithPath("againstLastMonth.totalExpenseRate")
                        .type(JsonFieldType.STRING)
                        .description("지난 달 같은 날까지 소비 대비 이번 달 소비 비율"),
                fieldWithPath("againstLastMonth.details[].categoryId")
                        .type(JsonFieldType.NUMBER)
                        .description("예산 카테고리 id"),
                fieldWithPath("againstLastMonth.details[].categoryName")
                        .type(JsonFieldType.STRING)
                        .description("예산 카테고리 이름"),
                fieldWithPath("againstLastMonth.details[].expenseRate")
                        .type(JsonFieldType.STRING)
                        .description("카테고리의 지난 달 같은 날까지 소비 대비 이번 달 소비 비율"),
                fieldWithPath("againstLastDayOfWeek.totalExpenseRate")
                        .type(JsonFieldType.STRING)
                        .description("지난 주 같은 날 소비 대비 오늘 소비 비율"),
                fieldWithPath("againstLastDayOfWeek.details[].categoryId")
                        .type(JsonFieldType.NUMBER)
                        .description("예산 카테고리 id"),
                fieldWithPath("againstLastDayOfWeek.details[].categoryName")
                        .type(JsonFieldType.STRING)
                        .description("예산 카테고리 이름"),
                fieldWithPath("againstLastDayOfWeek.details[].expenseRate")
                        .type(JsonFieldType.STRING)
                        .description("카테고리의 지난 주 같은 날 소비 대비 오늘 소비 비율"),
                fieldWithPath("againstOtherUsers.relativeExpenseRate")
                        .type(JsonFieldType.STRING)
                        .description("이번 달 다른 유저의 예산 대비 소비 비율 대비 나의 예산 대비 소비 비율"));
    }

    @Test
    @DisplayName("지출 통계 조회 성공 - 사용자가 오늘 지출했지만, 지난 소비 데이터와 다른 사용자 소비 데이터가 없는 경우")
    void shouldGetExpenseStatisticsWithReturn200IfValidTokenButNoLastAndOtherUserData() throws JsonProcessingException {
        createData(user, 0);
        RestAssured
                .given(this.spec)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(EXPENSES_URL + "/stat")
                .then()
                .statusCode(is(HttpStatus.OK.value()))
                .body(is(objectMapper.writeValueAsString(getExpenseStatisticsResponse("N/A"))));
    }

    private ExpenseStatResponse getExpenseStatisticsResponse(String expectedRate) {
        return new ExpenseStatResponse(
                new ExpenseStatCalendarResponse(expectedRate, getAgainstLast(expectedRate)),
                new ExpenseStatCalendarResponse(expectedRate, getAgainstLast(expectedRate)),
                new ExpenseStatUserResponse(expectedRate)
        );
    }

    private List<ExpenseStatCalendarCategoryResponse> getAgainstLast(String expectedRate) {
        List<ExpenseStatCalendarCategoryResponse> expenseStatCalendarCategoryResponses = new ArrayList<>();
        for (long categoryId = 1; categoryId <= COUNT_CATEGORY; categoryId++) {
            expenseStatCalendarCategoryResponses.add(new ExpenseStatCalendarCategoryResponse(
                    categoryId,
                    BudgetCategory.values()[(int) (categoryId-1)].getCategory(),
                    expectedRate
            ));
        }
        return expenseStatCalendarCategoryResponses;
    }

    private void createData(User user, int minusDay) {
        createBudgetPlan(user);
        createExpensesFromToday(user, minusDay);
    }

    private User createNewUser() {
        var newUser = User.builder()
                .username("expensetest" + new Random().nextInt() % 100)
                .email("expensetest%d@expensetest.com" + new Random().nextInt())
                .password("expensetest")
                .minimumDailyExpense(10000L)
                .agreeAlarm(false)
                .build();
        return userRepository.save(newUser);
    }

    private void createBudgetPlan(User user) {
        // 예산이 없는 카테고리의 반환 여부 테스트를 위해 절반의 카테고리만 예산 설정
        List<BudgetPlan> budgetPlans = new ArrayList<>(COUNT_CATEGORY);
        for (long categoryId = 1; categoryId <= COUNT_CATEGORY; categoryId++) {
            BudgetPlan budgetPlan = BudgetPlan.builder()
                    .user(user)
                    .date(LocalDate.of(LocalDate.now().getYear(), LocalDate.now().getMonth(), 1))
                    .amount(LocalDate.now().lengthOfMonth() * 10000L)
                    .category(Category.builder().id(categoryId).build())
                    .build();
            budgetPlans.add(budgetPlan);
        }
        budgetPlanRepository.saveAllAndFlush(budgetPlans);
    }

    private void createExpensesFromToday(User user, int minusDay) {
        List<Expense> expenses = new ArrayList<>((LocalDate.now().getDayOfMonth() - 1) * COUNT_CATEGORY);
        for (int day = 0; day <= minusDay; day++) {
            for (long categoryId = 1; categoryId <= COUNT_CATEGORY; categoryId++) {
                expenses.add(Expense.builder()
                        .user(user)
                        .datetime(LocalDateTime.now().minusDays(day))
                        .category(Category.builder().id(categoryId).build())
                        .amount(10000L)
                        .memo("오늘 지출")
                        .build());
            }
        }
        expenseRepository.saveAllAndFlush(expenses);
    }

    @Test
    @DisplayName("지출 통계 조회 실패 - 유효하지 않은 토큰")
    void shouldNotGetExpenseStatisticsWithReturn401IfInvalidToken() {
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("expense-statistics-unauthorized"))
                    .header("Authorization", "Bearer " + "expense.invalid.accessToken")
                .when()
                    .get(EXPENSES_URL + "/stat")
                .then()
                    .statusCode(is(ErrorCode.INVALID_TOKEN.getHttpStatus().value()))
                    .body("errorCode", is(ErrorCode.INVALID_TOKEN.name()))
                    .body("errorReason", is(ErrorCode.INVALID_TOKEN.getMessage()));;
    }

    private RequestHeadersSnippet getAccessTokenRequestHeaderSnippet() {
        return requestHeaders(
                headerWithName("Authorization")
                        .description("Bearer JWT(Access Token)"));
    }

}
