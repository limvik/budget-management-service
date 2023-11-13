package com.limvik.econome.infrastructure.user;

import com.limvik.econome.domain.user.entity.User;
import com.limvik.econome.global.config.JpaAuditConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditConfig.class)
@ActiveProfiles("integration")
public class UserSignupRepositoryTest {

    @Autowired
    UserRepository userRepository;

    @Test
    @DisplayName("중복되지 않은 username과 email 테스트")
    public void shouldReturnFalseIfUsernameOrEmailNotExists() {

        boolean isDuplicatedEmail = userRepository.existsByEmail("test999@test.com");
        boolean isDuplicatedUsername = userRepository.existsByUsername("test999");

        assertThat(isDuplicatedEmail).isFalse();
        assertThat(isDuplicatedUsername).isFalse();

    }

    @Test
    @DisplayName("중복된 username과 email 테스트")
    public void shouldReturnTrueIfUsernameOrEmailNotExists() {

        String username = "test999";
        String email = "test999@test.com";
        String encodedPassword = "$2a$12$dNSsw8M8NoAghJ6KJKzQM.fc8p9ysnufwYGhqjbasIWfjqt6axLMW";
        long minimumDailyExpense = 10000;
        boolean agreeAlarm = true;
        var user = User.builder()
                .username(username)
                .email(email)
                .password(encodedPassword)
                .minimumDailyExpense(minimumDailyExpense)
                .agreeAlarm(agreeAlarm).build();

        userRepository.save(user);

        boolean isDuplicatedEmail = userRepository.existsByEmail("test999@test.com");
        boolean isDuplicatedUsername = userRepository.existsByUsername("test999");

        assertThat(isDuplicatedEmail).isTrue();
        assertThat(isDuplicatedUsername).isTrue();

    }

}
