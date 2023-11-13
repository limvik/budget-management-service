package com.limvik.econome.web.expense.controller;

import com.limvik.econome.domain.category.entity.Category;
import com.limvik.econome.domain.expense.entity.Expense;
import com.limvik.econome.domain.expense.service.ExpenseService;
import com.limvik.econome.domain.user.entity.User;
import com.limvik.econome.global.security.authentication.JwtAuthenticationToken;
import com.limvik.econome.web.expense.dto.ExpenseRequest;
import com.limvik.econome.web.expense.dto.ExpenseResponse;
import com.limvik.econome.web.util.UserUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
        Expense createdExpense = expenseService.createExpense(mapRequestToEntity(expenseRequest, userId, null));
        return ResponseEntity.created(URI.create("/api/v1/expenses/" + createdExpense.getId())).build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Expense> updateExpense(@Valid @PathVariable(name = "id") @Min(1) Long expenseId,
                                                @Valid @RequestBody ExpenseRequest expenseRequest,
                                                Authentication authentication) {
        long userId = UserUtil.getUserIdFromJwt((JwtAuthenticationToken) authentication);
        expenseService.updateExpense(mapRequestToEntity(expenseRequest, userId, expenseId));
        return ResponseEntity.ok().build();
    }

    private Expense mapRequestToEntity(ExpenseRequest expenseRequest, Long userId, Long expenseId) {
        return Expense.builder()
                .id(expenseId)
                .user(User.builder().id(userId).build())
                .category(Category.builder().id(expenseRequest.categoryId()).build())
                .amount(expenseRequest.amount())
                .memo(expenseRequest.memo())
                .datetime(expenseRequest.datetime())
                .excluded(expenseRequest.excluded())
                .build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponse> getExpense(@Valid @PathVariable(name = "id") @Min(1) long expenseId,
                                                      Authentication authentication) {
        long userId = UserUtil.getUserIdFromJwt((JwtAuthenticationToken) authentication);
        Expense expense = expenseService.getExpense(userId, expenseId);
        ExpenseResponse expenseResponse = mapEntityToResponse(expense);
        return ResponseEntity.ok(expenseResponse);
    }

    private ExpenseResponse mapEntityToResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getDatetime(),
                expense.getCategory().getId(),
                expense.getAmount(),
                expense.getMemo(),
                expense.isExcluded()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteExpense(@Valid @PathVariable(name = "id") @Min(1) long expenseId,
                                                Authentication authentication) {
        long userId = UserUtil.getUserIdFromJwt((JwtAuthenticationToken) authentication);
        expenseService.deleteExpense(userId, expenseId);
        return ResponseEntity.noContent().build();
    }
}
