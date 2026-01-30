package sad.storereg.services.appdata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sad.storereg.models.appdata.VisitorPhoto;
import sad.storereg.repo.appdata.VisitorPhotoRepository;

@Service
@RequiredArgsConstructor
public class VisitorPhotoService {
	
	private final VisitorPhotoRepository visitorPhotoRepository;
	
	public byte[] getVisitorPhoto(Long visitorId) {
        VisitorPhoto photo = visitorPhotoRepository
                .findFirstByVisitor_Id(visitorId)
                .orElseThrow(() -> new RuntimeException("Visitor photo not found"));

        try {
            return Files.readAllBytes(Path.of(photo.getPath()));
        } catch (IOException e) {
            throw new RuntimeException("Unable to read visitor photo", e);
        }
    }
	
    public String getVisitorPhotoContentType(Long visitorId) {
        VisitorPhoto photo = visitorPhotoRepository
                .findFirstByVisitor_Id(visitorId)
                .orElseThrow(() -> new RuntimeException("Visitor photo not found"));

        return switch (photo.getExtension().toLowerCase()) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            default -> "application/octet-stream";
        };
    }

}
