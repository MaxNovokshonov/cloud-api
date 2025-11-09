package ru.netology.cloud_api.dto;

public class FileListItem {
    private String filename;
    private long size;

    public FileListItem() {}
    public FileListItem(String filename, long size) {
        this.filename = filename;
        this.size = size;
    }
    public String getFilename() { return filename; }
    public long getSize() { return size; }
    public void setFilename(String filename) { this.filename = filename; }
    public void setSize(long size) { this.size = size; }
}
