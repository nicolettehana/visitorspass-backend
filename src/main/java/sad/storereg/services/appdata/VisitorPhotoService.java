package sad.storereg.services.appdata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sad.storereg.dto.appdata.PhotoData;
import sad.storereg.models.appdata.Visitor;
import sad.storereg.models.appdata.VisitorPhoto;
import sad.storereg.repo.appdata.VisitorPhotoRepository;
import sad.storereg.repo.appdata.VisitorRepository;

@Service
@RequiredArgsConstructor
public class VisitorPhotoService {
	
	@Value("${photos.dir}")
    private String baseDir;
	
	private final VisitorPhotoRepository visitorPhotoRepository;
	private final VisitorRepository visitorRepository;
	
	public PhotoData getVisitorPhoto(Long visitorCode) {
	    Visitor visitor = visitorRepository.findById(visitorCode)
	            .orElseThrow(() -> new RuntimeException("Visitor not found"));

	    String[] extensions = {"jpg", "jpeg", "png"};

	    for (String ext : extensions) {
	        Path photoPath = Paths.get(baseDir, visitor.getVPassNo() + "." + ext);

	        if (Files.exists(photoPath)) {
	            try {
	                byte[] bytes = Files.readAllBytes(photoPath);
	                String contentType = switch (ext) {
	                    case "png" -> "image/png";
	                    case "jpg", "jpeg" -> "image/jpeg";
	                    default -> "application/octet-stream";
	                };

	                return new PhotoData(bytes, contentType);

	            } catch (IOException e) {
	                throw new RuntimeException("Unable to read visitor photo", e);
	            }
	        }
	    }

	    throw new RuntimeException(
	            "Visitor photo not found for pass no: " + visitor.getVPassNo()
	    );
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
