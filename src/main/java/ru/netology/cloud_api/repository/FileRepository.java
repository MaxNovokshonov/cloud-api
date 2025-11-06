package ru.netology.cloud_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.netology.cloud_api.domain.FileMeta;

import java.util.Optional;
import java.util.UUID;

public interface FileRepository extends JpaRepository<FileMeta, UUID> {
    Optional<FileMeta> findByUser_IdAndFilename(UUID userId, String filename);

    boolean existsByUser_IdAndFilename(UUID userId, String filename);
}
