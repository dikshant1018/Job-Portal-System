package com.jobportal.ui;

import com.jobportal.dao.ProfileDAO;
import com.jobportal.dao.UserDAO;
import com.jobportal.model.SeekerProfile;
import com.jobportal.model.User;
import com.jobportal.util.Async;
import com.jobportal.util.FileUtil;
import com.jobportal.util.Validator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.function.Consumer;

/** The job seeker can edit every detail of their profile here. */
public class SeekerProfilePanel extends JPanel {
    private final User user;
    private final Consumer<byte[]> identityChanged;
    private final ProfileDAO profileDAO = new ProfileDAO();
    private final UserDAO userDAO = new UserDAO();
    private final UIStyle.Avatar avatar = new UIStyle.Avatar(88);
    private byte[] photo;

    private final JTextField name = UIStyle.field(), phone = UIStyle.field(), location = UIStyle.field(),
            headline = UIStyle.field(), expected = UIStyle.field(), skills = UIStyle.field(),
            linkedin = UIStyle.field(), portfolio = UIStyle.field();
    private final JTextArea summary = UIStyle.area(4), education = UIStyle.area(3), experience = UIStyle.area(4);

    public SeekerProfilePanel(User user, Consumer<byte[]> identityChanged) {
        this.user = user;
        this.identityChanged = identityChanged;
        setOpaque(false);
        setLayout(new BorderLayout());
        JPanel page = UIStyle.page("My Profile", "Employers see these details when you apply. Keep them complete and up to date.");

        // photo card
        UIStyle.Card photoCard = UIStyle.card(new BorderLayout(20, 0));
        JButton change = UIStyle.outline("Upload photo");
        JButton remove = UIStyle.ghost("Remove");
        change.addActionListener(e -> choosePhoto());
        remove.addActionListener(e -> savePhoto(null));
        JPanel pt = new JPanel(new GridLayout(2, 1, 0, 4));
        pt.setOpaque(false);
        pt.add(UIStyle.h2("Profile photo"));
        pt.add(UIStyle.muted("JPG or PNG. Big pictures are resized automatically."));
        JPanel pb = new JPanel(new BorderLayout(0, 10));
        pb.setOpaque(false);
        pb.add(pt, BorderLayout.NORTH);
        pb.add(UIStyle.buttonRow(FlowLayout.LEFT, change, remove), BorderLayout.CENTER);
        photoCard.add(avatar, BorderLayout.WEST);
        photoCard.add(pb, BorderLayout.CENTER);

        // personal details
        UIStyle.Form personal = new UIStyle.Form(2);
        personal.add("Full name *", name);
        personal.add("Phone", phone);
        personal.add("Location (city, country)", location);
        personal.add("Expected salary", expected);
        personal.addWide("Professional headline (e.g. Junior Java Developer)", headline);

        // professional
        UIStyle.Form pro = new UIStyle.Form(2);
        pro.addWide("About me", UIStyle.areaPane(summary));
        pro.addWide("Skills (separate with commas)", skills);
        pro.addWide("Education", UIStyle.areaPane(education));
        pro.addWide("Work experience / projects", UIStyle.areaPane(experience));
        pro.add("LinkedIn URL", linkedin);
        pro.add("Portfolio / GitHub URL", portfolio);

        JPanel content = UIStyle.stack(photoCard, section("Personal details", personal), section("Professional information", pro));

        JButton save = UIStyle.primary("Save changes");
        save.addActionListener(e -> save());
        JPanel bar = UIStyle.buttonRow(FlowLayout.RIGHT, save);
        bar.setBorder(new EmptyBorder(12, 0, 0, 0));

        page.add(UIStyle.pageScroll(content), BorderLayout.CENTER);
        page.add(bar, BorderLayout.SOUTH);
        add(page);
    }

    private static JPanel section(String title, JComponent body) {
        UIStyle.Card c = UIStyle.card(new BorderLayout(0, 14));
        c.add(UIStyle.h2(title), BorderLayout.NORTH);
        c.add(body, BorderLayout.CENTER);
        return c;
    }

    public void load() {
        Async.run(this, () -> profileDAO.getSeeker(user.id), p -> {
            name.setText(user.name);
            phone.setText(UIStyle.nz(user.phone));
            location.setText(UIStyle.nz(p.location));
            expected.setText(UIStyle.nz(p.expectedSalary));
            headline.setText(UIStyle.nz(p.headline));
            summary.setText(UIStyle.nz(p.summary));
            skills.setText(UIStyle.nz(p.skills));
            education.setText(UIStyle.nz(p.education));
            experience.setText(UIStyle.nz(p.experience));
            linkedin.setText(UIStyle.nz(p.linkedin));
            portfolio.setText(UIStyle.nz(p.portfolio));
            photo = p.photo;
            avatar.set(user.name, photo);
        });
    }

    private void save() {
        String problem = null;
        if (name.getText().trim().length() < 2) problem = "Full name is required.";
        else if (!Validator.phone(phone.getText())) problem = "Phone number is not valid.";
        else if (!Validator.url(linkedin.getText()) || !Validator.url(portfolio.getText()))
            problem = "Links must start with http:// or https://";
        else if (headline.getText().length() > 150) problem = "Headline is too long (max 150 characters).";
        else if (location.getText().length() > 100 || expected.getText().length() > 60) problem = "Location or expected salary is too long.";
        else if (linkedin.getText().length() > 200 || portfolio.getText().length() > 200) problem = "A link is too long (max 200 characters).";
        if (problem != null) { UIStyle.warn(this, problem); return; }

        SeekerProfile p = new SeekerProfile();
        p.userId = user.id;
        p.headline = headline.getText().trim();
        p.location = location.getText().trim();
        p.summary = summary.getText().trim();
        p.skills = skills.getText().trim();
        p.education = education.getText().trim();
        p.experience = experience.getText().trim();
        p.linkedin = linkedin.getText().trim();
        p.portfolio = portfolio.getText().trim();
        p.expectedSalary = expected.getText().trim();
        String newName = name.getText().trim(), newPhone = phone.getText().trim();
        Async.exec(this, () -> {
            userDAO.updateBasic(user.id, newName, newPhone);
            profileDAO.saveSeeker(p);
        }, () -> {
            user.name = newName;
            user.phone = newPhone;
            identityChanged.accept(photo);
            UIStyle.info(this, "Your profile was saved.");
        });
    }

    private void choosePhoto() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Pictures (PNG, JPG)", "png", "jpg", "jpeg"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        try {
            byte[] jpeg = FileUtil.scaleToJpeg(FileUtil.readChecked(f, FileUtil.IMAGE_EXT, FileUtil.MAX_IMAGE_BYTES), 400);
            savePhoto(jpeg);
        } catch (IllegalArgumentException ex) {
            UIStyle.warn(this, ex.getMessage());
        } catch (Exception ex) {
            UIStyle.error(this, "Could not read the picture: " + ex.getMessage());
        }
    }

    private void savePhoto(byte[] jpeg) {
        Async.exec(this, () -> profileDAO.savePhoto(user.id, jpeg), () -> {
            photo = jpeg;
            avatar.set(user.name, photo);
            identityChanged.accept(photo);
        });
    }
}
