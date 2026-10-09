package com.jobportal.model;

import java.sql.Date;
import java.sql.Timestamp;

public class JobPosting {
    public int id, employerId, applicants;
    public String title, company, location, description, salary, jobType, category, experience, status;
    public Date deadline;
    public Timestamp postedAt;
    public boolean saved;   // saved by the current job seeker
}
