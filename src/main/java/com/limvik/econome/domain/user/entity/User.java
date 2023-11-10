package com.limvik.econome.domain.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column
    private String refreshToken;

}
