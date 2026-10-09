package com.jobportal.ui;

import com.jobportal.dao.UserDAO;
import com.jobportal.model.User;
import com.jobportal.util.Async;
import com.jobportal.util.Validator;

import javax.swing.*;
import java.awt.*;

public class RegisterFrame extends JFrame {
    private final UserDAO userDAO = new UserDAO();
    private final JTextField txtName = UIStyle.field();
    private final JTextField txtEmail = UIStyle.field();
    private final JTextField txtPhone = UIStyle.field();
    private final JPasswordField txtPass = UIStyle.password();
    private final JPasswordField txtConfirm = UIStyle.password();
    private final JRadioButton rbSeeker = new JRadioButton("I am looking for a job", true);
    private final JRadioButton rbEmployer = new JRadioButton("I want to hire people");
    private final JLabel lblError = new JLabel(" ");
    private final JButton btnCreate = UIStyle.primary("Create account");

    public RegisterFrame() {
        setTitle("JobPortal - Create account");
        setSize(920, 700);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridLayout(1, 2));
        add(LoginFrame.brandPanel());
        add(buildForm());
        getRootPane().setDefaultButton(btnCreate);
    }

    private JPanel buildForm() {
        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(Color.WHITE);
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setPreferredSize(new Dimension(350, 590));

        JLabel title = new JLabel("Create your account");
        title.setFont(UIStyle.F_TITLE);
        title.setForeground(UIStyle.TEXT);

        ButtonGroup g = new ButtonGroup();
        JPanel roles = new JPanel(new GridLayout(2, 1));
        roles.setOpaque(false);
        for (JRadioButton r : new JRadioButton[]{rbSeeker, rbEmployer}) {
            r.setOpaque(false);
            r.setFont(UIStyle.F_BODY);
            g.add(r);
            roles.add(r);
        }

        UIStyle.Form form = new UIStyle.Form(1);
        form.add("Full name", txtName);
        form.add("Email", txtEmail);
        form.add("Phone (optional)", txtPhone);
        form.add("Password (min 8 characters, letters + numbers)", txtPass);
        form.add("Confirm password", txtConfirm);
        form.add("I am joining as", roles);

        lblError.setFont(UIStyle.F_SMALL);
        lblError.setForeground(UIStyle.DANGER);
        btnCreate.addActionListener(e -> submit());

        JButton back = LoginFrame.link("Sign in");
        back.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });
        JPanel bk = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bk.setOpaque(false);
        bk.add(UIStyle.muted("Already have an account?  "));
        bk.add(back);

        for (JComponent c : new JComponent[]{title, form, lblError, btnCreate, bk}) c.setAlignmentX(0f);
        col.add(title);
        col.add(Box.createVerticalStrut(20));
        col.add(form);
        col.add(lblError);
        col.add(Box.createVerticalStrut(8));
        col.add(btnCreate);
        col.add(Box.createVerticalStrut(14));
        col.add(bk);
        right.add(col);
        return right;
    }

    private void submit() {
        String name = txtName.getText().trim(), email = txtEmail.getText().trim(), phone = txtPhone.getText().trim();
        String pass = new String(txtPass.getPassword()), confirm = new String(txtConfirm.getPassword());
        String problem = null;
        if (name.length() < 2) problem = "Please enter your full name.";
        else if (!Validator.email(email)) problem = "Please enter a valid email address.";
        else if (!Validator.phone(phone)) problem = "Phone number is not valid.";
        else if (Validator.passwordProblem(pass) != null) problem = Validator.passwordProblem(pass);
        else if (!pass.equals(confirm)) problem = "Passwords do not match.";
        if (problem != null) {
            lblError.setText(problem);
            return;
        }
        User u = new User();
        u.name = name;
        u.email = email;
        u.phone = phone;
        u.role = rbEmployer.isSelected() ? User.EMPLOYER : User.SEEKER;
        lblError.setText(" ");
        btnCreate.setEnabled(false);
        Async.run(this, () -> userDAO.register(u, pass), created -> {
            btnCreate.setEnabled(true);
            if (created) {
                UIStyle.info(this, "Account created! You can sign in now.");
                new LoginFrame().setVisible(true);
                dispose();
            } else {
                lblError.setText("An account with this email already exists.");
            }
        }, () -> btnCreate.setEnabled(true));
    }
}
