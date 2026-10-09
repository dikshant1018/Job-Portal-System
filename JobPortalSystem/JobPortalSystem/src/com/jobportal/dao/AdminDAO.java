package com.jobportal.dao;

import java.util.Map;

public class AdminDAO extends BaseDAO {
    public Map<String, Integer> stats() {
        return counts("SELECT "
                + "(SELECT COUNT(*) FROM users WHERE role = 'JOB_SEEKER') AS seekers, "
                + "(SELECT COUNT(*) FROM users WHERE role = 'EMPLOYER') AS employers, "
                + "(SELECT COUNT(*) FROM jobs WHERE status = 'OPEN') AS openJobs, "
                + "(SELECT COUNT(*) FROM applications WHERE status <> 'WITHDRAWN') AS applications");
    }
}
