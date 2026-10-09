package com.jobportal.model;

import java.sql.Timestamp;

public class User {
    public static final String ADMIN = "ADMIN", EMPLOYER = "EMPLOYER", SEEKER = "JOB_SEEKER";

    public int id;
    public String name, email, phone, role;
    public boolean active = true;
    public Timestamp createdAt;
}
