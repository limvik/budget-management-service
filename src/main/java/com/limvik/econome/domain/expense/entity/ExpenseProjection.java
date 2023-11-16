package com.limvik.econome.domain.expense.entity;

public class ExpenseProjection {

    public interface SumCategory {
        Long getCategoryId();
        Long getAmount();
    }

}
