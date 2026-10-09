package com.jobportal.ui;

import com.jobportal.dao.JobDAO;
import com.jobportal.model.JobPosting;
import com.jobportal.model.User;
import com.jobportal.util.Async;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Create or edit a job posting (existing == null creates a new one). */
public class JobFormDialog extends JDialog {
    private static final String[] CATEGORIES = {"Software & IT", "Design & Creative", "Marketing & Sales", "Finance & Accounting",
            "Education", "Healthcare", "Engineering", "Tourism & Hospitality", "Customer Support", "Administration", "Other"};
    private static final String[] EXPERIENCE = {"Fresher / Entry level", "1-3 years", "3-5 years", "5+ years"};

    private final JTextField title = UIStyle.field(), company = UIStyle.field(), location = UIStyle.field(),
            salary = UIStyle.field(), deadline = UIStyle.field();
    private final JComboBox<String> type = UIStyle.combo(UIStyle.JOB_TYPES), experience = UIStyle.combo(EXPERIENCE),
            category = UIStyle.combo(CATEGORIES);
    private final JTextArea description = UIStyle.area(9);

    public JobFormDialog(Frame parent, User employer, JobPosting existing, String defaultCompany, Runnable onSaved) {
        super(parent, existing == null ? "Post a new job" : "Edit job", true);
        category.setEditable(true);
        if (existing != null) {
            title.setText(existing.title);
            company.setText(existing.company);
            location.setText(existing.location);
            salary.setText(UIStyle.nz(existing.salary));
            deadline.setText(existing.deadline == null ? "" : existing.deadline.toString());
            type.setSelectedItem(UIStyle.pretty(existing.jobType));
            experience.setSelectedItem(UIStyle.nz(existing.experience));
            category.setSelectedItem(UIStyle.nz(existing.category));
            description.setText(existing.description);
            description.setCaretPosition(0);
        } else {
            company.setText(defaultCompany);
        }

        UIStyle.Form f = new UIStyle.Form(2);
        f.addWide("Job title *", title);
        f.add("Company *", company);
        f.add("Location *", location);
        f.add("Job type", type);
        f.add("Experience required", experience);
        f.add("Category", category);
        f.add("Salary (e.g. NPR 40,000 - 60,000)", salary);
        f.add("Application deadline (YYYY-MM-DD, optional)", deadline);
        f.addWide("Job description * (responsibilities, requirements, benefits)", UIStyle.areaPane(description));

        JButton cancel = UIStyle.ghost("Cancel");
        JButton save = UIStyle.primary(existing == null ? "Publish job" : "Save changes");
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> save(employer, existing, onSaved));

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(22, 26, 18, 26));
        root.add(UIStyle.h2(existing == null ? "Post a new job" : "Edit job"), BorderLayout.NORTH);
        root.add(f, BorderLayout.CENTER);
        root.add(UIStyle.buttonRow(FlowLayout.RIGHT, cancel, save), BorderLayout.SOUTH);
        setContentPane(root);
        setSize(700, 660);
        setLocationRelativeTo(parent);
        getRootPane().setDefaultButton(save);
    }

    private void save(User employer, JobPosting existing, Runnable onSaved) {
        String problem = null;
        Date dl = null;
        if (title.getText().trim().length() < 3) problem = "Please enter a job title (at least 3 characters).";
        else if (title.getText().length() > 150) problem = "Job title is too long (max 150).";
        else if (company.getText().trim().isEmpty() || company.getText().length() > 100) problem = "Company name is required (max 100 characters).";
        else if (location.getText().trim().isEmpty() || location.getText().length() > 100) problem = "Location is required (max 100 characters).";
        else if (salary.getText().length() > 50) problem = "Salary text is too long (max 50 characters).";
        else if (description.getText().trim().length() < 30) problem = "Please write a description of at least 30 characters.";
        else if (!deadline.getText().isBlank()) {
            try {
                LocalDate d = LocalDate.parse(deadline.getText().trim());
                if (d.isBefore(LocalDate.now())) problem = "The deadline cannot be in the past.";
                else dl = Date.valueOf(d);
            } catch (DateTimeParseException ex) {
                problem = "Deadline must look like 2026-12-31.";
            }
        }
        if (problem != null) { UIStyle.warn(this, problem); return; }

        JobPosting j = new JobPosting();
        j.id = existing == null ? 0 : existing.id;
        j.employerId = employer.id;
        j.title = title.getText().trim();
        j.company = company.getText().trim();
        j.location = location.getText().trim();
        j.salary = salary.getText().trim();
        j.description = description.getText().trim();
        j.jobType = UIStyle.raw((String) type.getSelectedItem());
        j.experience = (String) experience.getSelectedItem();
        String cat = String.valueOf(category.getEditor().getItem()).trim();
        j.category = cat.length() > 60 ? cat.substring(0, 60) : cat;
        j.deadline = dl;
        JobDAO dao = new JobDAO();
        Async.run(this, () -> {
            if (existing == null) { dao.create(j); return true; }
            return dao.update(j);
        }, ok -> {
            if (!ok) { UIStyle.warn(this, "This job could not be updated."); return; }
            onSaved.run();
            dispose();
        });
    }
}
