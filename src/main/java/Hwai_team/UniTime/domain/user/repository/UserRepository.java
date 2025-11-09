// src/main/java/Hwai_team/UniTime/domain/user/repository/UserRepository.java
package Hwai_team.UniTime.domain.user.repository;

import Hwai_team.UniTime.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
}