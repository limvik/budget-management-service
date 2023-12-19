package com.limvik.econome.domain.expense.entity;

import com.limvik.econome.domain.category.entity.Category;
import com.limvik.econome.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column
    private LocalDateTime datetime;

    @Column
    private Long amount;

    @Column(length = 60)
    private String memo;

    @Column(name = "exclude_in_total")
    private Boolean excluded;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    public void update(Expense updateExpense) {
        if (updateExpense.getDatetime() != null) {
            this.datetime = updateExpense.getDatetime();
        }
        if (updateExpense.getAmount() != null && updateExpense.getAmount() >= 0) {
            this.amount = updateExpense.getAmount();
        }
        if (updateExpense.getMemo() != null) {
            this.memo = updateExpense.getMemo();
        }
        if (updateExpense.getExcluded() != null) {
            this.excluded = updateExpense.getExcluded();
        }
        if (updateExpense.getCategory() != null) {
            this.category = updateExpense.getCategory();
        }
    }

}
