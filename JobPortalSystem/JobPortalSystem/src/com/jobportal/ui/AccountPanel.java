package com.jobportal.ui;

import com.jobportal.dao.UserDAO;
import com.jobportal.model.User;
import com.jobportal.util.Async;
import com.jobportal.util.Validator;

import javax.swing.*;
import java.awt.*;

/** Change email / password, delete account. Used by all three roles. */
public class AccountPanel extends JPanel {
    private final UserDAO dao = new UserDAO();

    public AccountPanel(User user, Runnable onAccountGone) {
        setOpaque(false);
        setLayout(new BorderLayout());
        JPanel page = UIStyle.page("Account Settings", "Manage how you sign in.");

        // --- email
        JTextField newEmail = UIStyle.field();
        JPasswordField emailPw = UIStyle.password();
        UIStyle.Form f1 = new UIStyle.Form(1);
        f1.add("Current email", UIStyle.label(user.email));
        f1.add("New email", newEmail);
        f1.add("Current password (to confirm)", emailPw);
        JButton b1 = UIStyle.primary("Update email");
        b1.addActionListener(e -> {
            String email = newEmail.getText().trim();
            if (!Validator.email(email)) { UIStyle.warn(this, "Please enter a valid email address."); return; }
            String pw = new String(emailPw.getPassword());
            Async.run(this, () -> dao.changeEmail(user.id, email, pw), err -> {
                if (err != null) { UIStyle.warn(this, err); return; }
                user.email = email.toLowerCase();
                newEmail.setText("");
                emailPw.setText("");
                UIStyle.info(this, "Email updated. Use the new email the next time you sign in.");
            });
        });

        // --- password
        JPasswordField oldPw = UIStyle.password(), newPw = UIStyle.password(), newPw2 = UIStyle.password();
        UIStyle.Form f2 = new UIStyle.Form(1);
        f2.add("Current password", oldPw);
        f2.add("New password (min 8 characters, letters + numbers)", newPw);
        f2.add("Confirm new password", newPw2);
        JButton b2 = UIStyle.primary("Change password");
        b2.addActionListener(e -> {
            String o = new String(oldPw.getPassword()), n = new String(newPw.getPassword());
            String problem = Validator.passwordProblem(n);
            if (problem == null && !n.equals(new String(newPw2.getPassword()))) problem = "New passwords do not match.";
            if (problem != null) { UIStyle.warn(this, problem); return; }
            Async.run(this, () -> dao.changePassword(user.id, o, n), err -> {
                if (err != null) { UIStyle.warn(this, err); return; }
                oldPw.setText(""); newPw.setText(""); newPw2.setText("");
                UIStyle.info(this, "Password changed.");
            });
        });

        JPanel col;
        if (User.ADMIN.equals(user.role)) {
            col = UIStyle.stack(section("Sign-in email", f1, b1), section("Password", f2, b2));
        } else {
            JButton b3 = UIStyle.danger("Delete my account");
            b3.addActionListener(e -> deleteAccount(user, onAccountGone));
            UIStyle.Card c = UIStyle.card(new BorderLayout(0, 12));
            c.add(UIStyle.h2("Delete account"), BorderLayout.NORTH);
            c.add(UIStyle.muted("<html>This permanently removes your profile, documents" +
                    (User.EMPLOYER.equals(user.role) ? ", job postings and received applications." : " and applications.") + "</html>"), BorderLayout.CENTER);
            c.add(UIStyle.buttonRow(FlowLayout.LEFT, b3), BorderLayout.SOUTH);
            col = UIStyle.stack(section("Sign-in email", f1, b1), section("Password", f2, b2), c);
        }
        col.setPreferredSize(new Dimension(620, User.ADMIN.equals(user.role) ? 560 : 740));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(col);
        page.add(UIStyle.pageScroll(left), BorderLayout.CENTER);
        add(page);
    }

    private static JPanel section(String title, JComponent form, JButton action) {
        UIStyle.Card c = UIStyle.card(new BorderLayout(0, 14));
        c.add(UIStyle.h2(title), BorderLayout.NORTH);
        c.add(form, BorderLayout.CENTER);
        c.add(UIStyle.buttonRow(FlowLayout.LEFT, action), BorderLayout.SOUTH);
        return c;
    }

    private void deleteAccount(User user, Runnable onGone) {
        JPasswordField pw = UIStyle.password();
        UIStyle.Form f = new UIStyle.Form(1);
        f.add("Type your password to confirm", pw);
        f.setPreferredSize(new Dimension(320, 70));
        if (JOptionPane.showConfirmDialog(this, f, "Delete account", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) return;
        String password = new String(pw.getPassword());
        Async.run(this, () -> dao.deleteOwnAccount(user.id, password), ok -> {
            if (!ok) { UIStyle.warn(this, "Wrong password. Your account was not deleted."); return; }
            UIStyle.info(this, "Your account was deleted.");
            onGone.run();
        });
    }
}
