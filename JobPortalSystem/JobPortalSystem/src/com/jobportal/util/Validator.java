package com.jobportal.util;

import java.util.regex.Pattern;

/** Input validation helpers. Methods returning String give an error message, or null when valid. */
public final class Validator {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9 \\-]{7,15}$");

    private Validator() {}

    public static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static boolean email(String s) {
        return s != null && s.length() <= 100 && EMAIL.matcher(s.trim()).matches();
    }

    /** Phone is optional: blank is valid. */
    public static boolean phone(String s) {
        return blank(s) || PHONE.matcher(s.trim()).matches();
    }

    /** Optional URL: blank is valid. */
    public static boolean url(String s) {
        return blank(s) || s.trim().matches("^https?://\\S+\\.\\S+$");
    }

    public static String passwordProblem(String pw) {
        if (pw == null || pw.length() < 8) return "Password must be at least 8 characters.";
        if (!pw.matches(".*[A-Za-z].*") || !pw.matches(".*[0-9].*"))
            return "Password must contain at least one letter and one number.";
        return null;
    }
}
