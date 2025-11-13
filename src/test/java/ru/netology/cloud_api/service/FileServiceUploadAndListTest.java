package ru.netology.cloud_api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.cloud_api.domain.FileBlob;
import ru.netology.cloud_api.domain.FileMeta;
import ru.netology.cloud_api.domain.User;
import ru.netology.cloud_api.exception.BadRequest400Exception;
import ru.netology.cloud_api.exception.Unauthorized401Exception;
import ru.netology.cloud_api.repository.FileBlobRepository;
import ru.netology.cloud_api.repository.FileRepository;
import ru.netology.cloud_api.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FileServiceUploadAndListTest {

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
    void upload_success_savesMetaAndBlob() throws Exception {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(fileRepository.existsByUser_IdAndFilename(userId, "test.txt")).thenReturn(false);
        when(fileRepository.save(any(FileMeta.class))).thenAnswer(inv -> inv.getArgument(0));
        when(blobRepository.save(any(FileBlob.class))).thenAnswer(inv -> inv.getArgument(0));

        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        MultipartFile mf = mock(MultipartFile.class);
        when(mf.isEmpty()).thenReturn(false);
        when(mf.getBytes()).thenReturn(data);
        when(mf.getContentType()).thenReturn("text/plain");

        assertDoesNotThrow(() -> fileService.upload(userId, "test.txt", mf, null));

        verify(fileRepository, times(1)).save(any(FileMeta.class));
        verify(blobRepository, times(1)).save(any(FileBlob.class));
    }

    @Test
    void upload_duplicateName_throws400() throws Exception {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(fileRepository.existsByUser_IdAndFilename(userId, "test.txt")).thenReturn(true);

        MultipartFile mf = mock(MultipartFile.class);
        when(mf.isEmpty()).thenReturn(false);

        when(mf.getBytes()).thenReturn("x".getBytes(StandardCharsets.UTF_8));

        assertThrows(BadRequest400Exception.class,
                () -> fileService.upload(userId, "test.txt", mf, null));

        verify(blobRepository, never()).save(any());
        verify(fileRepository, never()).save(any(FileMeta.class));
    }

    @Test
    void upload_userNotFound_throws401() throws Exception {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        MultipartFile mf = mock(MultipartFile.class);
        when(mf.isEmpty()).thenReturn(false);
        when(mf.getBytes()).thenReturn("x".getBytes(StandardCharsets.UTF_8));

        assertThrows(Unauthorized401Exception.class,
                () -> fileService.upload(userId, "a.txt", mf, null));

        verify(fileRepository, never()).save(any(FileMeta.class));
        verify(blobRepository, never()).save(any(FileBlob.class));
    }

    @Test
    void list_basic_returnsEmptyPage() {
        when(fileRepository.findByUser_IdOrderByUpdatedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(Page.empty());

        assertDoesNotThrow(() -> fileService.list(userId, 10));

        verify(fileRepository, times(1))
                .findByUser_IdOrderByUpdatedAtDesc(eq(userId), any(Pageable.class));
    }
}
