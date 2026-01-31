package sad.storereg.services.appdata;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
	private final PhotoStorageService photoStorageService;
	
	@Transactional
    public Visitor createVisitor(
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
        
     // 2️⃣ Generate Visitor Pass No
        String vPassNo = "VPASS-" +
                LocalDate.now().getYear() +"-"+
                String.format("%05d", visitor.getId());

        // 3️⃣ Update and save again
        visitor.setVPassNo(vPassNo);
        visitorRepository.save(visitor);

        /* 2️⃣ Store photo on disk */
        //String storedPath = storePhoto(photo, visitor.getId());
        String storedPath = null;
        try {
            storedPath = photoStorageService.storeVisitorPhoto(photo, visitor.getVPassNo());

//            VisitorPhoto visitorPhoto = VisitorPhoto.builder()
//                    .path(storedPath)
//                    .extension(getExtension(photo.getOriginalFilename()))
//                    .visitor(visitor)
//                    .build();
//
//            visitorPhotoRepository.save(visitorPhoto);

            return visitor;

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
    
    public Page<Visitor> getVisitorsBetweenDates(
            LocalDate startDate,
            LocalDate endDate,
            String search,
            Pageable pageable
    ) {
    	LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        return visitorRepository.searchVisitorsBetweenDates(
                startDateTime,
                endDateTime,
                search,
                pageable
        );
    }

}
