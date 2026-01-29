package sad.storereg.services.appdata;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import sad.storereg.dto.appdata.VisitorRequestDto;
import sad.storereg.models.appdata.Visitor;
import sad.storereg.models.appdata.VisitorPhoto;
import sad.storereg.repo.appdata.VisitorPhotoRepository;
import sad.storereg.repo.appdata.VisitorRepository;

@Service
@RequiredArgsConstructor
public class VisitorService {
	
	private final VisitorRepository visitorRepository;
	private final VisitorPhotoRepository visitorPhotoRepository;
	
	@Transactional
    public String createVisitor(
            VisitorRequestDto dto,
            MultipartFile photo
    ) {

        /* 1️⃣ Save Visitor */
        Visitor visitor = Visitor.builder()
                .name(dto.getName())
                .noOfVisitors(dto.getNoOfVisitors())
                .state(dto.getState())
                .address(dto.getAddress())
                .purpose(dto.getPurpose())
                .purposeDetails(dto.getPurposeDetails())
                .mobileNo(dto.getMobileNo())
                .email(dto.getEmail())
                .visitDateTime(dto.getVisitDateTime())
                .entrydate(LocalDateTime.now())
                .build();

        visitor = visitorRepository.save(visitor);

        /* 2️⃣ Store photo on disk */
        String storedPath = storePhoto(photo, visitor.getId());

        /* 3️⃣ Save photo metadata */
        VisitorPhoto visitorPhoto = VisitorPhoto.builder()
                .path(storedPath)
                .extension(getExtension(photo.getOriginalFilename()))
                .visitor(visitor)
                .build();

        visitorPhotoRepository.save(visitorPhoto);

        /* 4️⃣ Build response */
        return "Uploaded successfully";
    }

    /* ================= HELPER METHODS ================= */

    private String storePhoto(MultipartFile photo, Long visitorId) {

        if (photo.isEmpty()) {
            throw new RuntimeException("Photo file is empty");
        }

        try {
            String uploadDir = "uploads/visitors/" + visitorId;
            Files.createDirectories(Paths.get(uploadDir));

            String filename = System.currentTimeMillis() + "_" + photo.getOriginalFilename();
            Path filePath = Paths.get(uploadDir, filename);

            Files.copy(photo.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return filePath.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to store photo", e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

}
