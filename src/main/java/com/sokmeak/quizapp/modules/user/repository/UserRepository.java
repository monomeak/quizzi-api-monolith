package com.sokmeak.quizapp.modules.user.repository;

import com.sokmeak.quizapp.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String email);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}
