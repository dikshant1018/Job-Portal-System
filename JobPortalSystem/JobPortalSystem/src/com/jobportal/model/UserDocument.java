package com.jobportal.model;

import java.sql.Timestamp;

public class UserDocument {
    public int id, userId;
    public String type, fileName, mimeType;   // type: RESUME, CERTIFICATE, PORTFOLIO, OTHER
    public long size;
    public Timestamp uploadedAt;
    public byte[] data;                        // only filled when a single document is downloaded
}
