package com.jobportal.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

/** File checks for uploads: size, extension, and real content type (not just the name). */
public final class FileUtil {
    public static final long MAX_DOC_BYTES = 5L * 1024 * 1024;   // 5 MB
    public static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024; // source photo limit, it is shrunk before saving
    public static final List<String> DOC_EXT = List.of("pdf", "doc", "docx", "txt", "png", "jpg", "jpeg");
    public static final List<String> IMAGE_EXT = List.of("png", "jpg", "jpeg");

    private FileUtil() {}

    public static String ext(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    /** Reads a file after checking extension and size. Throws IllegalArgumentException with a readable message. */
    public static byte[] readChecked(File f, List<String> allowedExt, long maxBytes) throws IOException {
        String ext = ext(f.getName());
        if (!allowedExt.contains(ext)) throw new IllegalArgumentException("Allowed file types: " + String.join(", ", allowedExt).toUpperCase());
        if (f.length() == 0) throw new IllegalArgumentException("The selected file is empty.");
        if (f.length() > maxBytes) throw new IllegalArgumentException("File is too large. Maximum size is " + maxBytes / (1024 * 1024) + " MB.");
        byte[] data = Files.readAllBytes(f.toPath());
        if (!signatureMatches(ext, data)) throw new IllegalArgumentException("The file content does not match its ." + ext + " extension.");
        return data;
    }

    private static boolean startsWith(byte[] d, int... sig) {
        if (d.length < sig.length) return false;
        for (int i = 0; i < sig.length; i++) if ((d[i] & 0xFF) != sig[i]) return false;
        return true;
    }

    private static boolean signatureMatches(String ext, byte[] d) {
        return switch (ext) {
            case "pdf" -> startsWith(d, 0x25, 0x50, 0x44, 0x46);
            case "png" -> startsWith(d, 0x89, 0x50, 0x4E, 0x47);
            case "jpg", "jpeg" -> startsWith(d, 0xFF, 0xD8, 0xFF);
            case "docx" -> startsWith(d, 0x50, 0x4B, 0x03, 0x04);
            case "doc" -> startsWith(d, 0xD0, 0xCF, 0x11, 0xE0);
            case "txt" -> noBinary(d);
            default -> false;
        };
    }

    private static boolean noBinary(byte[] d) {
        for (int i = 0; i < Math.min(d.length, 1024); i++) if (d[i] == 0) return false;
        return true;
    }

    public static String mime(String name) {
        return switch (ext(name)) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            default -> "text/plain";
        };
    }

    public static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024));
    }

    /** Shrinks a photo/logo so that it is at most maxDim px and stores it as JPEG (small DB footprint). */
    public static byte[] scaleToJpeg(byte[] source, int maxDim) throws IOException {
        BufferedImage in = ImageIO.read(new ByteArrayInputStream(source));
        if (in == null) throw new IllegalArgumentException("This image could not be read.");
        double scale = Math.min(1.0, (double) maxDim / Math.max(in.getWidth(), in.getHeight()));
        int w = Math.max(1, (int) (in.getWidth() * scale)), h = Math.max(1, (int) (in.getHeight() * scale));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.drawImage(in, 0, 0, w, h, null);
        g.dispose();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(out, "jpg", bos);
        return bos.toByteArray();
    }
}
