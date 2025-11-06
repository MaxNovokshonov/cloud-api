package ru.netology.cloud_api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.cloud_api.domain.FileBlob;
import ru.netology.cloud_api.domain.FileMeta;
import ru.netology.cloud_api.domain.User;
import ru.netology.cloud_api.exception.BadRequest400Exception;
import ru.netology.cloud_api.exception.Unauthorized401Exception;
import ru.netology.cloud_api.repository.FileBlobRepository;
import ru.netology.cloud_api.repository.FileRepository;
import ru.netology.cloud_api.repository.UserRepository;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class FileService {

    private final FileRepository files;
    private final FileBlobRepository blobs;
    private final UserRepository users;

    public FileService(FileRepository files, FileBlobRepository blobs, UserRepository users) {
        this.files = files;
        this.blobs = blobs;
        this.users = users;
    }

    private static void validateFilename(String filename) {
        if (filename == null || filename.isBlank())
            throw new BadRequest400Exception("filename is required");
        if (filename.length() > 255)
            throw new BadRequest400Exception("filename too long");
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\"))
            throw new BadRequest400Exception("filename is invalid");
    }

    private static byte[] sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(data);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] decodeOptionalHash(String hash) {
        if (hash == null || hash.isBlank()) return null;
        // Принимаем либо hex, либо base64url
        String s = hash.trim();
        try {
            if (s.matches("^[0-9a-fA-F]{64}$")) {
                byte[] out = new byte[32];
                for (int i = 0; i < 32; i++) {
                    out[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
                }
                return out;
            }
            return Base64.getUrlDecoder().decode(s);
        } catch (Exception e) {
            throw new BadRequest400Exception("hash format is invalid");
        }
    }

    @Transactional
    public void upload(UUID userId, String filename, MultipartFile partFile, String optHash) {
        validateFilename(filename);
        if (partFile == null || partFile.isEmpty())
            throw new BadRequest400Exception("file part is required");

        if (files.existsByUser_IdAndFilename(userId, filename))
            throw new BadRequest400Exception("file with this name already exists");

        byte[] data;
        try {
            data = partFile.getBytes();
        } catch (Exception e) {
            throw new BadRequest400Exception("cannot read file");
        }
        byte[] checksum = sha256(data);
        byte[] provided = decodeOptionalHash(optHash);
        if (provided != null && !java.util.Arrays.equals(provided, checksum))
            throw new BadRequest400Exception("hash mismatch");

        User user = users.findById(userId).orElseThrow(() -> new Unauthorized401Exception("Unauthorized"));

        FileMeta meta = new FileMeta();
        meta.setUser(user);
        meta.setFilename(filename);
        meta.setSizeBytes(data.length);
        meta.setContentType(partFile.getContentType() != null ? partFile.getContentType() : "application/octet-stream");
        meta.setChecksumSha256(checksum);
        meta.setUploadedAt(Instant.now());
        meta.setUpdatedAt(Instant.now());
        meta = files.save(meta);

        blobs.save(new FileBlob(meta.getId(), data));
    }

    @Transactional(readOnly = true)
    public DownloadResult download(UUID userId, String filename) {
        validateFilename(filename);
        FileMeta meta = files.findByUser_IdAndFilename(userId, filename)
                .orElseThrow(() -> new BadRequest400Exception("file not found"));
        byte[] data = blobs.findByFileId(meta.getId())
                .map(FileBlob::getData)
                .orElseThrow(() -> new BadRequest400Exception("file content missing"));

        return new DownloadResult(meta.getFilename(), meta.getContentType(), data, meta.getChecksumSha256());
    }

    @Transactional
    public void rename(UUID userId, String oldName, String newName) {
        validateFilename(oldName);
        validateFilename(newName);
        if (oldName.equals(newName)) return;

        FileMeta meta = files.findByUser_IdAndFilename(userId, oldName)
                .orElseThrow(() -> new BadRequest400Exception("file not found"));
        if (files.existsByUser_IdAndFilename(userId, newName))
            throw new BadRequest400Exception("file with new name already exists");

        meta.setFilename(newName);
        meta.setUpdatedAt(Instant.now());
        files.save(meta);
    }

    @Transactional
    public void delete(UUID userId, String filename) {
        validateFilename(filename);
        FileMeta meta = files.findByUser_IdAndFilename(userId, filename)
                .orElseThrow(() -> new BadRequest400Exception("file not found"));
        blobs.deleteByFileId(meta.getId());
        files.delete(meta);
    }

    public static class DownloadResult {
        public final String filename;
        public final String contentType;
        public final byte[] data;
        public final byte[] sha256;

        public DownloadResult(String filename, String contentType, byte[] data, byte[] sha256) {
            this.filename = filename;
            this.contentType = contentType;
            this.data = data;
            this.sha256 = sha256;
        }
    }

    public static String toHex(byte[] bytes) {
        if (bytes == null) return null;
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
