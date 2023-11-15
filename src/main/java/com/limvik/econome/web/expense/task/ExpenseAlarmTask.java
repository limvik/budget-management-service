package com.limvik.econome.web.expense.task;

import com.limvik.econome.domain.user.entity.User;
import com.limvik.econome.infrastructure.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Component
public class ExpenseAlarmTask {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Scheduled(cron = "0 08 00 * * ?", zone = "Asia/Seoul")
    public void recommendExpenses() {
        List<User> users = userRepository.findAllByAgreeAlarm(true);
        users.forEach(user -> {
            // TODO: 오늘 추천 지출 알림
        });
    }

    @Scheduled(cron = "0 20 00 * * ?", zone = "Asia/Seoul")
    public void notifyTodayExpenses() {
        List<User> users = userRepository.findAllByAgreeAlarm(true);
        users.forEach(user -> {
            // TODO: 오늘 지출 내역 알림
        });
    }

}
