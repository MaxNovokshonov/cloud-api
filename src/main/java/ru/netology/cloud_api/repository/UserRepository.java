package ru.netology.cloud_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.netology.cloud_api.domain.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
}
