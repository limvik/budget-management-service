package com.limvik.econome.domain.user.entity;

import com.limvik.econome.domain.budgetplan.entity.BudgetPlan;
import com.limvik.econome.domain.expense.entity.Expense;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(unique = true, length = 20)
    private String username;

    @Column(unique = true)
    private String email;

    @Column(nullable = false, length = 64)
    private String password;

    @Column
    private long minimumDailyExpense;

    @Column
    private boolean agreeAlarm;

    @Column
    @CreationTimestamp
    private Instant createTime;

    @Setter
    @Column
    private String refreshToken;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<BudgetPlan> budgetPlans;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Expense> expenses;

}
