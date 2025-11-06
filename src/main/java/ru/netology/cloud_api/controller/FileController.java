package ru.netology.cloud_api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.cloud_api.dto.FileRenameRequest;
import ru.netology.cloud_api.service.FileService;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
public class FileController {

    private final FileService files;

    public FileController(FileService files) {
        this.files = files;
    }

    private static UUID currentUserId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        Object p = a != null ? a.getPrincipal() : null;
        if (p instanceof UUID u) return u;

        return UUID.fromString(String.valueOf(p));
    }

    @PostMapping(path = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> upload(@RequestParam(value = "filename", required = false) String filename,
                                       @RequestPart(value = "file", required = false) MultipartFile file,
                                       @RequestPart(value = "hash", required = false) String hash) {
        files.upload(currentUserId(), filename, file, hash);

        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "/file")
    public ResponseEntity<byte[]> download(@RequestParam(value = "filename", required = false) String filename) {
        FileService.DownloadResult dr = files.download(currentUserId(), filename);

        String boundary = "cloudBoundary-" + java.util.UUID.randomUUID();
        byte[] partFileHeader = (
                "--" + boundary + "\r\n" +
                        "Content-Disposition: form-data; name=\"file\"; filename=\"" + escape(dr.filename) + "\"\r\n" +
                        "Content-Type: " + safe(dr.contentType) + "\r\n\r\n"
        ).getBytes(StandardCharsets.UTF_8);

        String hashHex = FileService.toHex(dr.sha256);
        byte[] partHash = ("\r\n--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"hash\"\r\n\r\n" +
                (hashHex != null ? hashHex : "")).getBytes(StandardCharsets.UTF_8);

        byte[] closing = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

        byte[] body = concat(partFileHeader, dr.data, partHash, closing);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("multipart/form-data; boundary=" + boundary));
        headers.setContentLength(body.length);

        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }

    @PutMapping(path = "/file", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> rename(@RequestParam(value = "filename", required = false) String filename,
                                       @Valid @RequestBody FileRenameRequest req) {
        files.rename(currentUserId(), filename, req.getName());

        return ResponseEntity.ok().build();
    }

    @DeleteMapping(path = "/file")
    public ResponseEntity<Void> delete(@RequestParam("filename") String filename) {
        files.delete(currentUserId(), filename);

        return ResponseEntity.ok().build();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "%22");
    }

    private static String safe(String s) {
        return (StringUtils.hasText(s) ? s : "application/octet-stream");
    }

    private static byte[] concat(byte[]... chunks) {
        int len = 0;
        for (byte[] c : chunks) len += c.length;
        byte[] out = new byte[len];
        int pos = 0;
        for (byte[] c : chunks) {
            System.arraycopy(c, 0, out, pos, c.length);
            pos += c.length;
        }
        return out;
    }
}
