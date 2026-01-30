package sad.storereg.services.appdata;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PhotoStorageService {

	@Value("${photos.dir}")
    private String baseDir;

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png");

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png");

    public String storeVisitorPhoto(MultipartFile file, Long visitorId) {

        validate(file);

        try {
            BufferedImage originalImage = ImageIO.read(file.getInputStream());

            if (originalImage == null) {
                throw new RuntimeException("File is not a valid image");
            }

            /* Resize (optional but recommended) */
            BufferedImage resized = resize(originalImage, 400, 400);

            String extension = getExtension(file.getOriginalFilename());
            String filename = "visitor_" + System.currentTimeMillis() + "." + extension;

            Path visitorDir = Paths.get(baseDir, String.valueOf(visitorId));
            Files.createDirectories(visitorDir);

            Path outputPath = visitorDir.resolve(filename);

            ImageIO.write(resized, extension.equals("png") ? "png" : "jpg",
                    outputPath.toFile());

            return outputPath.toString();

        } catch (IOException e) {
            throw new RuntimeException("Failed to store image", e);
        }
    }

    /* ================= VALIDATION ================= */

    private void validate(MultipartFile file) {

        if (file.isEmpty()) {
            throw new RuntimeException("Uploaded file is empty");
        }

        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new RuntimeException("Only JPG and PNG images are allowed");
        }

        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new RuntimeException("Invalid image content type");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    /* ================= RESIZE ================= */

    private BufferedImage resize(BufferedImage original, int width, int height) {

        Image scaled = original.getScaledInstance(width, height, Image.SCALE_SMOOTH);

        BufferedImage resized = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(scaled, 0, 0, null);
        g2d.dispose();

        return resized;
    }
}