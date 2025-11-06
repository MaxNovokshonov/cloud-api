package ru.netology.cloud_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.netology.cloud_api.domain.FileBlob;

import java.util.Optional;
import java.util.UUID;

public interface FileBlobRepository extends JpaRepository<FileBlob, UUID> {
    Optional<FileBlob> findByFileId(UUID fileId);

    void deleteByFileId(UUID fileId);
}
