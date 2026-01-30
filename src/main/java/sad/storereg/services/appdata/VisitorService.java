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
	private final PhotoStorageService photoStorageService;
	
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
        //String storedPath = storePhoto(photo, visitor.getId());
        String storedPath = null;
        try {
            storedPath = photoStorageService.storeVisitorPhoto(photo, visitor.getId());

            VisitorPhoto visitorPhoto = VisitorPhoto.builder()
                    .path(storedPath)
                    .extension(getExtension(photo.getOriginalFilename()))
                    .visitor(visitor)
                    .build();

            visitorPhotoRepository.save(visitorPhoto);

            return "Uploaded successfully";

        } catch (Exception e) {
            if (storedPath != null) {
                try {
                    Files.deleteIfExists(Paths.get(storedPath));
                } catch (Exception ignored) {}
            }
            throw e;
        }

    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

}
