package com.limvik.econome.infrastructure.user;

import com.limvik.econome.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
