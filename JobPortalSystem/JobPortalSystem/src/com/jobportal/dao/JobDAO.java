package com.jobportal.dao;

import com.jobportal.model.JobPosting;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JobDAO extends BaseDAO {
    private static final String APPLICANTS =
            "(SELECT COUNT(*) FROM applications a WHERE a.job_id = j.id AND a.status <> 'WITHDRAWN') AS applicants";

    private static JobPosting map(ResultSet rs) throws SQLException {
        JobPosting j = new JobPosting();
        j.id = rs.getInt("id");
        j.employerId = rs.getInt("employer_id");
        j.title = rs.getString("title");
        j.company = rs.getString("company");
        j.location = rs.getString("location");
        j.description = rs.getString("description");
        j.salary = rs.getString("salary");
        j.jobType = rs.getString("job_type");
        j.category = rs.getString("category");
        j.experience = rs.getString("experience");
        j.deadline = rs.getDate("deadline");
        j.status = rs.getString("status");
        j.postedAt = rs.getTimestamp("posted_at");
        j.applicants = rs.getInt("applicants");
        j.saved = rs.getBoolean("saved");
        return j;
    }

    public void create(JobPosting j) {
        run("INSERT INTO jobs (employer_id, title, company, location, description, salary, job_type, category, experience, deadline) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?)",
                j.employerId, j.title, j.company, j.location, j.description, j.salary, j.jobType, j.category, j.experience, j.deadline);
    }

    /** Only the owner (employer_id) can edit a job. */
    public boolean update(JobPosting j) {
        return run("UPDATE jobs SET title=?, company=?, location=?, description=?, salary=?, job_type=?, category=?, "
                        + "experience=?, deadline=? WHERE id=? AND employer_id=?",
                j.title, j.company, j.location, j.description, j.salary, j.jobType, j.category, j.experience, j.deadline,
                j.id, j.employerId) > 0;
    }

    /** ownerId = employer id; pass 0 for an administrator (may act on any job). */
    public boolean setStatus(int jobId, int ownerId, String status) {
        return run("UPDATE jobs SET status = ? WHERE id = ? AND (? = 0 OR employer_id = ?)", status, jobId, ownerId, ownerId) > 0;
    }

    /** ownerId = employer id; pass 0 for an administrator. */
    public boolean delete(int jobId, int ownerId) {
        return run("DELETE FROM jobs WHERE id = ? AND (? = 0 OR employer_id = ?)", jobId, ownerId, ownerId) > 0;
    }

    /** Open, not-expired jobs for seekers with optional filters. */
    public List<JobPosting> search(int seekerId, String keyword, String location, String jobType, boolean savedOnly) {
        StringBuilder sql = new StringBuilder("SELECT j.*, " + APPLICANTS + ", (s.seeker_id IS NOT NULL) AS saved FROM jobs j "
                + "LEFT JOIN saved_jobs s ON s.job_id = j.id AND s.seeker_id = ? "
                + "WHERE j.status = 'OPEN' AND (j.deadline IS NULL OR j.deadline >= CURDATE())");
        List<Object> p = new ArrayList<>();
        p.add(seekerId);
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (j.title LIKE ? OR j.company LIKE ? OR j.description LIKE ? OR j.category LIKE ?)");
            String k = like(keyword);
            p.add(k); p.add(k); p.add(k); p.add(k);
        }
        if (location != null && !location.isBlank()) { sql.append(" AND j.location LIKE ?"); p.add(like(location)); }
        if (jobType != null && !jobType.isBlank()) { sql.append(" AND j.job_type = ?"); p.add(jobType); }
        if (savedOnly) sql.append(" AND s.seeker_id IS NOT NULL");
        sql.append(" ORDER BY j.posted_at DESC");
        return query(sql.toString(), p.toArray());
    }

    public List<JobPosting> byEmployer(int employerId) {
        return query("SELECT j.*, " + APPLICANTS + ", 0 AS saved FROM jobs j WHERE j.employer_id = ? ORDER BY j.posted_at DESC", employerId);
    }

    public List<JobPosting> all() {
        return query("SELECT j.*, " + APPLICANTS + ", 0 AS saved FROM jobs j ORDER BY j.posted_at DESC");
    }

    /** @return true if the job is now saved, false if it was un-saved. */
    public boolean toggleSaved(int seekerId, int jobId) {
        if (run("DELETE FROM saved_jobs WHERE seeker_id = ? AND job_id = ?", seekerId, jobId) > 0) return false;
        run("INSERT IGNORE INTO saved_jobs (seeker_id, job_id) VALUES (?, ?)", seekerId, jobId);
        return true;
    }

    public java.util.Map<String, Integer> employerStats(int employerId) {
        return counts("SELECT "
                + "(SELECT COUNT(*) FROM jobs WHERE employer_id = ? AND status = 'OPEN') AS openJobs, "
                + "(SELECT COUNT(*) FROM applications a JOIN jobs j ON a.job_id = j.id WHERE j.employer_id = ? AND a.status <> 'WITHDRAWN') AS applicants, "
                + "(SELECT COUNT(*) FROM applications a JOIN jobs j ON a.job_id = j.id WHERE j.employer_id = ? AND a.status = 'PENDING') AS pending, "
                + "(SELECT COUNT(*) FROM applications a JOIN jobs j ON a.job_id = j.id WHERE j.employer_id = ? AND a.status = 'SHORTLISTED') AS shortlisted",
                employerId, employerId, employerId, employerId);
    }

    private static String like(String s) {
        return "%" + s.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    private List<JobPosting> query(String sql, Object... params) {
        List<JobPosting> out = new ArrayList<>();
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
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
