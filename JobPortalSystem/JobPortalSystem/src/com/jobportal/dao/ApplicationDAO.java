package com.jobportal.dao;

import com.jobportal.model.Application;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ApplicationDAO extends BaseDAO {
    private static final Set<String> EMPLOYER_STATUSES = Set.of("PENDING", "SHORTLISTED", "ACCEPTED", "REJECTED");

    private static final String SELECT = "SELECT a.*, j.title, j.company, u.name AS seeker_name, u.email AS seeker_email, "
            + "u.phone AS seeker_phone, d.file_name FROM applications a JOIN jobs j ON a.job_id = j.id "
            + "JOIN users u ON a.seeker_id = u.id LEFT JOIN documents d ON a.document_id = d.id ";

    /** @return an error message, or null when the application was submitted. */
    public String apply(int jobId, int seekerId, Integer documentId, String coverLetter) {
        try (Connection c = conn()) {
            String existing = null;
            try (PreparedStatement ps = c.prepareStatement("SELECT status FROM applications WHERE job_id = ? AND seeker_id = ?")) {
                bind(ps, jobId, seekerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) existing = rs.getString(1);
                }
            }
            if (existing != null && !existing.equals("WITHDRAWN")) return "You have already applied for this job.";

            // the document is attached only if it really belongs to this seeker
            String docExpr = "(SELECT id FROM documents WHERE id = ? AND user_id = ?)";
            if (existing != null) {   // re-apply after withdrawing
                try (PreparedStatement ps = c.prepareStatement("UPDATE applications a JOIN jobs j ON a.job_id = j.id "
                        + "SET a.status = 'PENDING', a.resume_note = ?, a.document_id = " + docExpr + ", a.applied_at = NOW() "
                        + "WHERE a.job_id = ? AND a.seeker_id = ? AND j.status = 'OPEN' AND (j.deadline IS NULL OR j.deadline >= CURDATE())")) {
                    bind(ps, coverLetter, documentId, seekerId, jobId, seekerId);
                    return ps.executeUpdate() > 0 ? null : "This job is closed or has expired.";
                }
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO applications (job_id, seeker_id, resume_note, document_id) "
                    + "SELECT j.id, ?, ?, " + docExpr + " FROM jobs j "
                    + "WHERE j.id = ? AND j.status = 'OPEN' AND (j.deadline IS NULL OR j.deadline >= CURDATE())")) {
                bind(ps, seekerId, coverLetter, documentId, seekerId, jobId);
                return ps.executeUpdate() > 0 ? null : "This job is closed or has expired.";
            }
        } catch (SQLException e) {
            if (isDuplicate(e)) return "You have already applied for this job.";
            throw fail(e);
        }
    }

    public List<Application> bySeeker(int seekerId) {
        return query(SELECT + "WHERE a.seeker_id = ? ORDER BY a.applied_at DESC", seekerId);
    }

    public List<Application> byEmployer(int employerId) {
        return query(SELECT + "WHERE j.employer_id = ? AND a.status <> 'WITHDRAWN' ORDER BY a.applied_at DESC", employerId);
    }

    /** Only the employer who owns the job can change the status. */
    public boolean updateStatus(int appId, int employerId, String status) {
        if (!EMPLOYER_STATUSES.contains(status)) throw new DaoException("Invalid status.", null);
        return run("UPDATE applications a JOIN jobs j ON a.job_id = j.id SET a.status = ? "
                + "WHERE a.id = ? AND j.employer_id = ? AND a.status <> 'WITHDRAWN'", status, appId, employerId) > 0;
    }

    public boolean withdraw(int appId, int seekerId) {
        return run("UPDATE applications SET status = 'WITHDRAWN' WHERE id = ? AND seeker_id = ? AND status IN ('PENDING','SHORTLISTED')",
                appId, seekerId) > 0;
    }

    public Map<String, Integer> seekerStats(int seekerId) {
        return counts("SELECT "
                + "(SELECT COUNT(*) FROM applications WHERE seeker_id = ? AND status <> 'WITHDRAWN') AS total, "
                + "(SELECT COUNT(*) FROM applications WHERE seeker_id = ? AND status = 'SHORTLISTED') AS shortlisted, "
                + "(SELECT COUNT(*) FROM applications WHERE seeker_id = ? AND status = 'ACCEPTED') AS accepted, "
                + "(SELECT COUNT(*) FROM saved_jobs WHERE seeker_id = ?) AS saved", seekerId, seekerId, seekerId, seekerId);
    }

    private List<Application> query(String sql, Object... params) {
        List<Application> out = new ArrayList<>();
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Application a = new Application();
                    a.id = rs.getInt("id");
                    a.jobId = rs.getInt("job_id");
                    a.seekerId = rs.getInt("seeker_id");
                    int doc = rs.getInt("document_id");
                    a.documentId = rs.wasNull() ? null : doc;
                    a.coverLetter = rs.getString("resume_note");
                    a.status = rs.getString("status");
                    a.appliedAt = rs.getTimestamp("applied_at");
                    a.jobTitle = rs.getString("title");
                    a.company = rs.getString("company");
                    a.seekerName = rs.getString("seeker_name");
                    a.seekerEmail = rs.getString("seeker_email");
                    a.seekerPhone = rs.getString("seeker_phone");
                    a.documentName = rs.getString("file_name");
                    out.add(a);
                }
            }
        } catch (SQLException e) {
            throw fail(e);
        }
        return out;
    }

    private int run(String sql, Object... params) {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw fail(e);
        }
    }
}
