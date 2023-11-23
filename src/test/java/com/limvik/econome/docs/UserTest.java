package com.limvik.econome.docs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.limvik.econome.global.config.JwtConfig;
import com.limvik.econome.global.exception.ErrorCode;
import com.limvik.econome.global.security.jwt.provider.JwtProvider;
import com.limvik.econome.web.user.dto.SigninRequest;
import com.limvik.econome.web.user.dto.SignupRequest;
import io.jsonwebtoken.Jwts;
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
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Date;

import static org.hamcrest.Matchers.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyUris;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.restassured.RestAssuredRestDocumentation.document;
import static org.springframework.restdocs.restassured.RestAssuredRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.snippet.Attributes.key;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@ExtendWith(RestDocumentationExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserTest {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JwtProvider jwtProvider;

    @Autowired
    JwtConfig jwtConfig;

    @LocalServerPort
    int port;

    RequestSpecification spec;

    String username = "restdocs";
    String password = "restdocs";

    String refreshToken;

    @BeforeEach
    public void setUp(RestDocumentationContextProvider restDocumentation) {
        this.spec = new RequestSpecBuilder()
                .addFilter(documentationConfiguration(restDocumentation).operationPreprocessors()
                        .withRequestDefaults(modifyUris().scheme("http").host("econome.com").removePort(), prettyPrint())
                        .withResponseDefaults(prettyPrint()))
                .setPort(this.port)
                .build();
    }

    @Test
    @Order(1)
    @DisplayName("회원가입 성공")
    void shouldSignupWithReturn201IfValidSignupInfo() throws JsonProcessingException {
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("users-signup", getSignupRequestFields()))
                    .body(objectMapper.writeValueAsString(getSignupRequestBody()))
                .when()
                    .post("/api/v1/users/signup")
                .then()
                    .statusCode(is(HttpStatus.CREATED.value()));
    }

    @Test
    @Order(2)
    @DisplayName("회원가입 실패 - 사용자 이름 중복")
    void shouldNotSignupWithReturn409IfDuplicatedUsername() throws JsonProcessingException {
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("users-signup-conflict"))
                    .body(objectMapper.writeValueAsString(getSignupRequestBody()))
                .when()
                    .post("/api/v1/users/signup")
                .then()
                    .statusCode(is(ErrorCode.DUPLICATED_USERNAME.getHttpStatus().value()))
                    .body("errorCode", is(ErrorCode.DUPLICATED_USERNAME.name()))
                    .body("errorReason", is(ErrorCode.DUPLICATED_USERNAME.getMessage()));
    }

    @Test
    @Order(3)
    @DisplayName("회원가입 실패 - 잘못된 이메일 형식")
    void shouldNotSignupWithReturn422IfInvalidEmail() throws JsonProcessingException {
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("users-signup-unprocessable"))
                    .body(objectMapper.writeValueAsString(getUnprocessableSignupRequestBody()))
                .when()
                    .post("/api/v1/users/signup")
                .then()
                    .statusCode(is(ErrorCode.UNPROCESSABLE_USERINFO.getHttpStatus().value()));
    }

    private RequestFieldsSnippet getSignupRequestFields() {
        return requestFields(
                fieldWithPath("username")
                        .type(JsonFieldType.STRING)
                        .description("사용자 이름")
                        .attributes(key("constraints").value("최대 20글자까지 입력 가능합니다.")),
                fieldWithPath("email")
                        .type(JsonFieldType.STRING)
                        .description("사용자 이메일")
                        .attributes(key("constraints").value("이메일 형식을 만족해야 합니다.")),
                fieldWithPath("password")
                        .type(JsonFieldType.STRING)
                        .description("사용자 비밀번호")
                        .attributes(key("constraints").value("최소 8글자, 최대 64글자까지 입력 가능합니다.")),
                fieldWithPath("minimumDailyExpense")
                        .type(JsonFieldType.NUMBER)
                        .description("예산 추천 시 일일 소비 최소금액")
                        .attributes(key("constraints").value("양수만 입력할 수 있습니다.")),
                fieldWithPath("agreeAlarm")
                        .type(JsonFieldType.BOOLEAN)
                        .description("오늘 추천 지출(오전 8시) 및 오늘 지출(오후 8시) 알람 동의 여부 / 선택적(Optional)\n기본값 : false")
                        .attributes(key("constraints").value(""))
                        .optional());
    }

    private SignupRequest getSignupRequestBody() {
        return new SignupRequest(
                username,
                "restdocs@restdocs.com",
                password,
                10000L,
                false);
    }

    private SignupRequest getUnprocessableSignupRequestBody() {
        return new SignupRequest(
                username + 2,
                "XXXXXXXXXXXXXXXXXXXXX",
                password,
                10000L,
                false);
    }

    @Test
    @Order(4)
    @DisplayName("로그인 성공")
    void shouldSigninWithReturn200AndTokensIfValidLoginInfo() throws JsonProcessingException {
        refreshToken = RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("users-signin", getSigninRequestFields(), getSigninResponseFields()))
                    .body(objectMapper.writeValueAsString(getSigninRequestBody()))
                .when()
                    .post("/api/v1/users/signin")
                .then()
                    .statusCode(is(HttpStatus.OK.value()))
                    .body(containsString("accessToken"))
                    .body(containsString("refreshToken"))
                    .extract().body().jsonPath().get("refreshToken");
    }

    @Test
    @Order(5)
    @DisplayName("로그인 실패 - 일치하는 사용자 정보 없음")
    void shouldNotSigninWithReturn401IfNoUser() throws JsonProcessingException {
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("users-signin-unauthorized"))
                    .body(objectMapper.writeValueAsString(getNotExistUserSigninRequestBody()))
                .when()
                    .post("/api/v1/users/signin")
                .then()
                    .statusCode(is(ErrorCode.NOT_EXIST_USER.getHttpStatus().value()))
                    .body("errorCode", is(ErrorCode.NOT_EXIST_USER.name()))
                    .body("errorReason", is(ErrorCode.NOT_EXIST_USER.getMessage()));
    }

    @Test
    @Order(6)
    @DisplayName("로그인 실패 - 잘못된 사용자 이름 형식")
    void shouldNotSigninWithReturn422IfInvalidUsername() throws JsonProcessingException {
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("users-signin-unprocessable"))
                    .body(objectMapper.writeValueAsString(getUnprocessableSigninRequestBody()))
                .when()
                    .post("/api/v1/users/signin")
                .then()
                    .statusCode(is(ErrorCode.UNPROCESSABLE_USERINFO.getHttpStatus().value()))
                    .body("errorCode", is(ErrorCode.UNPROCESSABLE_USERINFO.name()))
                    .body("errorReason", is(ErrorCode.UNPROCESSABLE_USERINFO.getMessage()));
    }

    private RequestFieldsSnippet getSigninRequestFields() {
        return requestFields(
                fieldWithPath("username")
                        .type(JsonFieldType.STRING)
                        .description("사용자 이름")
                        .attributes(key("constraints").value("최대 20글자까지 입력 가능합니다.")),
                fieldWithPath("password")
                        .type(JsonFieldType.STRING)
                        .description("사용자 비밀번호")
                        .attributes(key("constraints").value("최소 8글자, 최대 64글자까지 입력 가능합니다.")));
    }

    private ResponseFieldsSnippet getSigninResponseFields() {
        return responseFields(
                fieldWithPath("accessToken")
                        .type(JsonFieldType.STRING)
                        .description("인증이 필요한 엔드포인트 요청을 위한 Token"),
                fieldWithPath("refreshToken")
                        .type(JsonFieldType.STRING)
                        .description("사용자 로그인 상태 유지를 위한 Token"));
    }

    private SigninRequest getSigninRequestBody() {
        return new SigninRequest(
                username,
                password);
    }

    private SigninRequest getNotExistUserSigninRequestBody() {
        return new SigninRequest(
                username + 999,
                password);
    }

    private SigninRequest getUnprocessableSigninRequestBody() {
        return new SigninRequest(
                username + 99999999999999999L,
                password);
    }

    @Test
    @Order(7)
    @DisplayName("Access Token 갱신 성공")
    void shouldRefreshAccessTokenWithReturn200AndNewAccessToken() throws JsonProcessingException {
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("users-token", getRefreshAccessTokenRequestHeader(), getRefreshAccessTokenResponseFields()))
                    .header("Authorization", "Bearer " + refreshToken)
                .when()
                    .post("/api/v1/users/token")
                .then()
                    .statusCode(is(HttpStatus.OK.value()))
                    .body(containsString("accessToken"));
    }

    @Test
    @Order(8)
    @DisplayName("Access Token 갱신 실패 - 데이터베이스와 다른 Refresh Token")
    void shouldNotRefreshAccessTokenWithReturn401IfInvalidToken() {
        RestAssured
                .given(this.spec)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .filter(document("users-token-invalid", getRefreshAccessTokenRequestHeader()))
                    .header("Authorization", "Bearer " + getFakeToken())
                .when()
                    .post("/api/v1/users/token")
                .then()
                    .statusCode(is(ErrorCode.INVALID_TOKEN.getHttpStatus().value()))
                    .body("errorCode", is(ErrorCode.INVALID_TOKEN.name()))
                    .body("errorReason", is(ErrorCode.INVALID_TOKEN.getMessage()));;
    }

    private RequestHeadersSnippet getRefreshAccessTokenRequestHeader() {
        return requestHeaders(
                headerWithName("Authorization")
                        .description("Bearer JWT(Refresh Token)"));
    }

    private ResponseFieldsSnippet getRefreshAccessTokenResponseFields() {
        return responseFields(
                fieldWithPath("accessToken")
                        .type(JsonFieldType.STRING)
                        .description("Refresh Token 에 의해 갱신된 Token"));
    }

    private String getFakeToken() {
        return Jwts.builder()
                .header().type("JWT")
                .and()
                .issuer(jwtConfig.getIssuer())
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + Duration.ofDays(1).toMillis()))
                .subject("1")
                .signWith(jwtProvider.getRefreshKey())
                .compact();
    }
}
