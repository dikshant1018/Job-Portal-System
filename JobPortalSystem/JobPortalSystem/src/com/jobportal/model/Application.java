package com.jobportal.model;

import java.sql.Timestamp;

public class Application {
    public int id, jobId, seekerId;
    public Integer documentId;
    public String coverLetter, status, jobTitle, company, seekerName, seekerEmail, seekerPhone, documentName;
    public Timestamp appliedAt;
}
