package ru.netology.cloud_api.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "file_blobs")
public class FileBlob {
    @Id
    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "data", nullable = false, columnDefinition = "bytea")
    private byte[] data;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", referencedColumnName = "id", insertable = false, updatable = false)
    private FileMeta file;

    public FileBlob() {
    }

    public FileBlob(UUID fileId, byte[] data) {
        this.fileId = fileId;
        this.data = data;
    }

    public UUID getFileId() {
        return fileId;
    }

    public byte[] getData() {
        return data;
    }

    public FileMeta getFile() {
        return file;
    }

    public void setFileId(UUID fileId) {
        this.fileId = fileId;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setFile(FileMeta file) {
        this.file = file;
    }
}
