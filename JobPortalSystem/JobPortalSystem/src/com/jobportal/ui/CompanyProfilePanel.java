package com.jobportal.ui;

import com.jobportal.dao.ProfileDAO;
import com.jobportal.dao.UserDAO;
import com.jobportal.model.CompanyProfile;
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

/** Employer: edit company details and logo. */
public class CompanyProfilePanel extends JPanel {
    private final User user;
    private final Consumer<byte[]> identityChanged;
    private final ProfileDAO profileDAO = new ProfileDAO();
    private final UserDAO userDAO = new UserDAO();
    private final UIStyle.Avatar logoView = new UIStyle.Avatar(88);
    private byte[] logo;

    private final JTextField company = UIStyle.field(), website = UIStyle.field(), location = UIStyle.field(),
            contactName = UIStyle.field(), contactPhone = UIStyle.field();
    private final JComboBox<String> industry = UIStyle.combo("", "Information Technology", "Banking & Finance", "Education",
            "Healthcare", "Manufacturing", "Tourism & Hospitality", "Retail & E-commerce", "Marketing & Media", "NGO / Non-profit", "Other");
    private final JComboBox<String> size = UIStyle.combo("", "1-10", "11-50", "51-200", "201-500", "500+");
    private final JTextArea about = UIStyle.area(6);

    public CompanyProfilePanel(User user, Consumer<byte[]> identityChanged) {
        this.user = user;
        this.identityChanged = identityChanged;
        setOpaque(false);
        setLayout(new BorderLayout());
        JPanel page = UIStyle.page("Company Profile", "Job seekers see this information, so make a good first impression.");

        UIStyle.Card logoCard = UIStyle.card(new BorderLayout(20, 0));
        JButton change = UIStyle.outline("Upload logo");
        JButton remove = UIStyle.ghost("Remove");
        change.addActionListener(e -> chooseLogo());
        remove.addActionListener(e -> saveLogo(null));
        JPanel t = new JPanel(new GridLayout(2, 1, 0, 4));
        t.setOpaque(false);
        t.add(UIStyle.h2("Company logo"));
        t.add(UIStyle.muted("JPG or PNG. It is resized automatically."));
        JPanel pb = new JPanel(new BorderLayout(0, 10));
        pb.setOpaque(false);
        pb.add(t, BorderLayout.NORTH);
        pb.add(UIStyle.buttonRow(FlowLayout.LEFT, change, remove), BorderLayout.CENTER);
        logoCard.add(logoView, BorderLayout.WEST);
        logoCard.add(pb, BorderLayout.CENTER);

        UIStyle.Form f = new UIStyle.Form(2);
        f.addWide("Company name *", company);
        f.add("Industry", industry);
        f.add("Company size (employees)", size);
        f.add("Website", website);
        f.add("Location", location);
        f.addWide("About the company", UIStyle.areaPane(about));
        UIStyle.Form c = new UIStyle.Form(2);
        c.add("Contact person *", contactName);
        c.add("Contact phone", contactPhone);

        JPanel content = UIStyle.stack(logoCard, section("Company details", f), section("Contact", c));

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
        Async.run(this, () -> profileDAO.getCompany(user.id), p -> {
            company.setText(UIStyle.nz(p.companyName));
            website.setText(UIStyle.nz(p.website));
            location.setText(UIStyle.nz(p.location));
            about.setText(UIStyle.nz(p.about));
            industry.setSelectedItem(UIStyle.nz(p.industry));
            size.setSelectedItem(UIStyle.nz(p.size));
            contactName.setText(user.name);
            contactPhone.setText(UIStyle.nz(user.phone));
            logo = p.logo;
            logoView.set(UIStyle.nz(p.companyName).isBlank() ? user.name : p.companyName, logo);
        });
    }

    private void save() {
        String problem = null;
        if (company.getText().trim().length() < 2) problem = "Company name is required.";
        else if (company.getText().length() > 100 || location.getText().length() > 100) problem = "Company name or location is too long (max 100).";
        else if (contactName.getText().trim().length() < 2) problem = "Contact person is required.";
        else if (!Validator.phone(contactPhone.getText())) problem = "Phone number is not valid.";
        else if (!Validator.url(website.getText()) || website.getText().length() > 200) problem = "Website must start with http:// or https://";
        if (problem != null) { UIStyle.warn(this, problem); return; }

        CompanyProfile p = new CompanyProfile();
        p.userId = user.id;
        p.companyName = company.getText().trim();
        p.industry = (String) industry.getSelectedItem();
        p.size = (String) size.getSelectedItem();
        p.website = website.getText().trim();
        p.location = location.getText().trim();
        p.about = about.getText().trim();
        String name = contactName.getText().trim(), phone = contactPhone.getText().trim();
        Async.exec(this, () -> {
            userDAO.updateBasic(user.id, name, phone);
            profileDAO.saveCompany(p);
        }, () -> {
            user.name = name;
            user.phone = phone;
            identityChanged.accept(logo);
            UIStyle.info(this, "Company profile saved.");
        });
    }

    private void chooseLogo() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Pictures (PNG, JPG)", "png", "jpg", "jpeg"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        try {
            saveLogo(FileUtil.scaleToJpeg(FileUtil.readChecked(f, FileUtil.IMAGE_EXT, FileUtil.MAX_IMAGE_BYTES), 400));
        } catch (IllegalArgumentException ex) {
            UIStyle.warn(this, ex.getMessage());
        } catch (Exception ex) {
            UIStyle.error(this, "Could not read the picture: " + ex.getMessage());
        }
    }

    private void saveLogo(byte[] jpeg) {
        Async.exec(this, () -> profileDAO.saveLogo(user.id, jpeg), () -> {
            logo = jpeg;
            logoView.set(company.getText().isBlank() ? user.name : company.getText(), logo);
            identityChanged.accept(logo);
        });
    }
}
