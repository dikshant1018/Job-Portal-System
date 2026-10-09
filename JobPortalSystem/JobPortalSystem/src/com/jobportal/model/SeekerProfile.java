package com.jobportal.model;

import com.jobportal.util.Validator;

public class SeekerProfile {
    public int userId;
    public String headline, location, summary, skills, education, experience, linkedin, portfolio, expectedSalary;
    public byte[] photo;

    /** Profile completeness in percent (8 items, resume is passed in because it lives in another table). */
    public int completeness(boolean hasResume) {
        boolean[] parts = {!Validator.blank(headline), !Validator.blank(location), !Validator.blank(summary),
                !Validator.blank(skills), !Validator.blank(education), !Validator.blank(experience), photo != null, hasResume};
        int n = 0;
        for (boolean b : parts) if (b) n++;
        return n * 100 / parts.length;
    }
}
