package com.limvik.econome.domain.category.entity;

import com.limvik.econome.domain.budgetplan.entity.BudgetPlan;
import com.limvik.econome.domain.category.enums.BudgetCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BudgetCategory name;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    private List<BudgetPlan> budgetPlans;
}
