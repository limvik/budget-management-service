package com.limvik.econome.web.expense.controller;

import com.limvik.econome.domain.category.entity.Category;
import com.limvik.econome.domain.expense.entity.Expense;
import com.limvik.econome.domain.expense.service.ExpenseService;
import com.limvik.econome.domain.user.entity.User;
import com.limvik.econome.global.security.authentication.JwtAuthenticationToken;
import com.limvik.econome.web.expense.dto.ExpenseRequest;
import com.limvik.econome.web.util.UserUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<String> createExpense(@Valid @RequestBody ExpenseRequest expenseRequest,
                                                Authentication authentication) {
        long userId = UserUtil.getUserIdFromJwt((JwtAuthenticationToken) authentication);
        Expense createdExpense = expenseService.createExpense(mapRequestToEntity(expenseRequest, userId));
        return ResponseEntity.created(URI.create("/api/v1/expenses/" + createdExpense.getId())).build();
    }

    private Expense mapRequestToEntity(ExpenseRequest expenseRequest, long userId) {
        return Expense.builder()
                .user(User.builder().id(userId).build())
                .category(Category.builder().id(expenseRequest.categoryId()).build())
                .amount(expenseRequest.amount())
                .memo(expenseRequest.memo())
                .datetime(expenseRequest.datetime())
                .build();
    }
}
