package com.sit.campusbackend.complaint.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Saves uploaded photos to disk under random names, accepting only real JPEG, PNG or WebP files. */
@Service
public class ImageStorageService {

    static final long MAX_BYTES = 5L * 1024 * 1024;

    private final Path root;

    public ImageStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create upload directory " + root, e);
        }
    }

    /** @return the public path of the stored file, e.g. "/uploads/3f2a....jpg" */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A photo is required.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("Image is too large (maximum 5 MB).");
        }
        try (InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(12);
            String extension = detectExtension(head);
            if (extension == null) {
                throw new IllegalArgumentException("Only JPEG, PNG or WebP images are allowed.");
            }
            String name = UUID.randomUUID() + "." + extension;
            try (InputStream rest = file.getInputStream()) {
                Files.copy(rest, root.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/" + name;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store the uploaded image", e);
        }
    }

    /** Decides the type from the file's own bytes, not from the name or the content type the client claims. */
    static String detectExtension(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) return "png";
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        return null;
    }
}
