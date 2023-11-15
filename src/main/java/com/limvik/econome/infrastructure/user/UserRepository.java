package com.limvik.econome.infrastructure.user;

import com.limvik.econome.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByIdAndRefreshToken(Long id, String refreshToken);

    @Query("SELECT u.minimumDailyExpense FROM User u WHERE u.id = ?1")
    long findMinimumDailyExpenseById(Long id);

    List<User> findAllByAgreeAlarm(boolean agreeAlarm);
}
