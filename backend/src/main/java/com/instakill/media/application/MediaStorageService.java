package com.instakill.media.application;

import com.instakill.common.error.BadRequestException;
import com.instakill.common.id.IdGenerator;
import com.instakill.imageprocessing.ImageProcessor;
import com.instakill.infrastructure.config.MediaProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MediaStorageService {

    /** Formats the image processor can read and write; anything else (videos, gif, webp) is stored as-is. */
    private static final Set<String> PROCESSED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    private final Path baseDir;
    private final Set<String> allowedExtensions;
    private final IdGenerator idGenerator;
    private final ImageProcessor imageProcessor;

    public MediaStorageService(MediaProperties properties, IdGenerator idGenerator, ImageProcessor imageProcessor) {
        this.baseDir = Path.of(properties.basePath()).toAbsolutePath().normalize();
        this.allowedExtensions = properties.allowedExtensions().stream()
                .map(ext -> ext.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        this.idGenerator = idGenerator;
        this.imageProcessor = imageProcessor;
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create media directory " + baseDir, e);
        }
    }

    /**
     * Stores the file under the media base path and returns its public filename.
     * JPEG and PNG images are scaled down and watermarked before saving.
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File must not be empty");
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (extension == null || !allowedExtensions.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("File type not allowed. Allowed extensions: " + allowedExtensions);
        }
        String normalizedExtension = extension.toLowerCase(Locale.ROOT);
        String filename = idGenerator.generate() + "." + normalizedExtension;
        Path target = baseDir.resolve(filename);
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(baseDir);
            if (PROCESSED_IMAGE_EXTENSIONS.contains(normalizedExtension)) {
                Files.write(target, imageProcessor.process(in, normalizedExtension));
            } else {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store file", e);
        }
        return filename;
    }
}
