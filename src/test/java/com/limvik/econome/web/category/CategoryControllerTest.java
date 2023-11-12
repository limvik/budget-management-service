package com.limvik.econome.web.category;

import com.limvik.econome.domain.category.service.CategoryService;
import com.limvik.econome.global.config.WebAuthorizationConfig;
import com.limvik.econome.global.exception.ErrorCode;
import com.limvik.econome.global.security.jwt.provider.JwtProvider;
import com.limvik.econome.infrastructure.user.UserRepository;
import com.limvik.econome.web.category.controller.CategoryController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import(WebAuthorizationConfig.class)
public class CategoryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    CategoryService categoryService;

    @MockBean
    PasswordEncoder passwordEncoder;

    @MockBean
    JwtProvider jwtProvider;

    @MockBean
    UserRepository userRepository;

    @Test
    @DisplayName("인증된 사용자의 카테고리 목록 요청")
    @WithMockUser(username="testuser")
    void shouldReturnAllCategories() throws Exception {

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['categories'][0]['id']").value(1))
                .andExpect(jsonPath("$.['categories'][0]['name']").value("식료품/비주류음료"))
                .andExpect(jsonPath("$.['categories'][11]['id']").value(12))
                .andExpect(jsonPath("$.['categories'][11]['name']").value("기타 상품/서비스"))
                .andExpect(jsonPath("$.['categories'][12]['id']").doesNotExist());

    }

    @Test
    @DisplayName("인증되지 않은 사용자의 카테고리 목록 요청")
    void shouldReturn401Unauthorized() throws Exception {

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.['errorCode']").value(ErrorCode.INVALID_TOKEN.name()))
                .andExpect(jsonPath("$.['errorReason']").value(ErrorCode.INVALID_TOKEN.getMessage()));

    }

}
