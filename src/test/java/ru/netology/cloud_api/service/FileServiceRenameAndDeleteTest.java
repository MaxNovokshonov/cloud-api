package ru.netology.cloud_api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.netology.cloud_api.domain.FileMeta;
import ru.netology.cloud_api.domain.User;
import ru.netology.cloud_api.exception.BadRequest400Exception;
import ru.netology.cloud_api.repository.FileBlobRepository;
import ru.netology.cloud_api.repository.FileRepository;
import ru.netology.cloud_api.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FileServiceRenameAndDeleteTest {

    private FileRepository fileRepository;
    private FileBlobRepository blobRepository;
    private UserRepository userRepository;
    private FileService fileService;

    private final UUID userId = UUID.randomUUID();
    private User user;

    @BeforeEach
    void setUp() {
        fileRepository = mock(FileRepository.class);
        blobRepository = mock(FileBlobRepository.class);
        userRepository = mock(UserRepository.class);
        fileService = new FileService(fileRepository, blobRepository, userRepository);

        user = new User();
        user.setId(userId);
        user.setUsername("user1");
        user.setPasswordHash("noop");
    }


    @Test
    void rename_success_updatesFilename() {
        UUID fileId = UUID.randomUUID();
        FileMeta meta = new FileMeta();
        meta.setId(fileId);
        meta.setUser(user);
        meta.setFilename("old.txt");

        when(fileRepository.findByUser_IdAndFilename(userId, "old.txt")).thenReturn(Optional.of(meta));
        when(fileRepository.existsByUser_IdAndFilename(userId, "new.txt")).thenReturn(false);
        when(fileRepository.save(any(FileMeta.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> fileService.rename(userId, "old.txt", "new.txt"));

        verify(fileRepository, times(1)).save(any(FileMeta.class));
    }

    @Test
    void rename_conflict_throws400() {
        when(fileRepository.existsByUser_IdAndFilename(userId, "new.txt")).thenReturn(true);

        assertThrows(BadRequest400Exception.class,
                () -> fileService.rename(userId, "old.txt", "new.txt"));

        verify(fileRepository, never()).save(any());
    }

    @Test
    void rename_notFound_throws400() {
        when(fileRepository.findByUser_IdAndFilename(userId, "missing.txt")).thenReturn(Optional.empty());

        assertThrows(BadRequest400Exception.class,
                () -> fileService.rename(userId, "missing.txt", "new.txt"));

        verify(fileRepository, never()).save(any());
    }


    @Test
    void delete_success_doesNotThrow() {
        UUID fileId = UUID.randomUUID();
        FileMeta meta = new FileMeta();
        meta.setId(fileId);
        meta.setUser(user);
        meta.setFilename("doc.txt");

        when(fileRepository.findByUser_IdAndFilename(userId, "doc.txt")).thenReturn(Optional.of(meta));

        doNothing().when(fileRepository).delete(any(FileMeta.class));
        doNothing().when(fileRepository).deleteById(any());
        doNothing().when(blobRepository).deleteById(any());

        assertDoesNotThrow(() -> fileService.delete(userId, "doc.txt"));
    }

    @Test
    void delete_notFound_throws400() {
        when(fileRepository.findByUser_IdAndFilename(userId, "nope.txt")).thenReturn(Optional.empty());

        assertThrows(BadRequest400Exception.class,
                () -> fileService.delete(userId, "nope.txt"));
    }
}
