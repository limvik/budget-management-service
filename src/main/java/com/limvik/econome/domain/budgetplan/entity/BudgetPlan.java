package com.limvik.econome.domain.budgetplan.entity;

import com.limvik.econome.domain.category.entity.Category;
import com.limvik.econome.domain.category.enums.BudgetCategory;
import com.limvik.econome.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "budget_plans")
public class BudgetPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private long amount;

    @Setter
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Setter
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 예산 추천 시에 사용되는 생성자입니다.
     * @param categoryId 카테고리 식별자
     * @param budgetCategory 카테고리
     * @param averageAmount 전체 평균
     */
    public BudgetPlan(long categoryId, BudgetCategory budgetCategory, double averageAmount) {
        this.category = Category.builder().id(categoryId).name(budgetCategory).build();
        this.amount = ((long) averageAmount) / 10 * 10; // 1원 단위 절삭
    }

    public void addAmount(long amount) {
        this.amount += amount;
    }

}
