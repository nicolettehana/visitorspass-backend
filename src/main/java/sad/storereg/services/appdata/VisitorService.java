package sad.storereg.services.appdata;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import sad.storereg.dto.appdata.DailyVisitorReportDTO;
import sad.storereg.dto.appdata.PurposeStatsDto;
import sad.storereg.dto.appdata.VisitorReportResponse;
import sad.storereg.dto.appdata.VisitorRequestDto;
import sad.storereg.models.appdata.Visitor;
import sad.storereg.repo.appdata.VisitorRepository;

@Service
@RequiredArgsConstructor
public class VisitorService {
	
	private final VisitorRepository visitorRepository;
	private final PhotoStorageService photoStorageService;
	
	@Transactional
    public Visitor createVisitor(
            VisitorRequestDto dto,
            MultipartFile photo,
            Integer officeCode
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
                .officeCode(officeCode)
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
            Integer officeCode,
            Pageable pageable
    ) {
    	LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        return visitorRepository.searchVisitorsBetweenDates(
                startDateTime,
                endDateTime,
                search,
                officeCode,
                pageable
        );
    }
    
    public Optional<Visitor> getData(String mobileNo){
    	return visitorRepository.findTopByMobileNo(mobileNo);
    }
    
    public List<PurposeStatsDto> getVisitorsByPurpose(int year, int month, int officeCode) {
        return visitorRepository.countVisitorsByPurpose(year, month, officeCode);
    }
    
    public VisitorReportResponse getVisitorReport(
            Integer officeCode,
            LocalDate startDate,
            LocalDate endDate,
            String purpose) {

        // Handle "All"
        if (purpose != null && purpose.equalsIgnoreCase("All")) {
            purpose = null;
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        List<Object[]> results =
                visitorRepository.getVisitorsGrouped(officeCode, start, end, purpose);

        Map<LocalDate, DailyVisitorReportDTO> dailyMap = new LinkedHashMap<>();

        int ministerTotal = 0;
        int csTotal = 0;
        int meetingTotal = 0;
        int officerTotal = 0;
        int departmentTotal = 0;
        int grandTotal = 0;

        // 🔹 Process DB results
        for (Object[] row : results) {

            // Safe cast (PostgreSQL returns java.sql.Date here)
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();

            String dbPurpose = row[1] != null ? row[1].toString().trim().toLowerCase() : "";
            int count = ((Number) row[2]).intValue();

            dailyMap.putIfAbsent(date,
                    DailyVisitorReportDTO.builder()
                            .date(date.toString())
                            .dayOfWeek(date.getDayOfWeek().toString())
                            .minister(0)
                            .cs(0)
                            .meeting(0)
                            .officer(0)
                            .department(0)
                            .totalNoOfVisitors(0)
                            .build()
            );

            DailyVisitorReportDTO dto = dailyMap.get(date);

            switch (dbPurpose) {
                case "to meet minister" -> {
                    dto.setMinister(dto.getMinister() + count);
                    ministerTotal += count;
                }
                case "to meet chief secretary" -> {
                    dto.setCs(dto.getCs() + count);
                    csTotal += count;
                }
                case "to attend meeting/function" -> {
                    dto.setMeeting(dto.getMeeting() + count);
                    meetingTotal += count;
                }
                case "to meet officers" -> {
                    dto.setOfficer(dto.getOfficer() + count);
                    officerTotal += count;
                }
                case "to visit department" -> {
                    dto.setDepartment(dto.getDepartment() + count);
                    departmentTotal += count;
                }
            }

            dto.setTotalNoOfVisitors(dto.getTotalNoOfVisitors() + count);
            grandTotal += count;
        }

        // 🔹 Fill missing dates (important for bar chart)
        LocalDate today = LocalDate.now();
        LocalDate lastDate = endDate.isAfter(today) ? today : endDate;

        for (LocalDate date = startDate; !date.isAfter(lastDate); date = date.plusDays(1)) {
            dailyMap.putIfAbsent(date,
                    DailyVisitorReportDTO.builder()
                            .date(date.toString())
                            .dayOfWeek(date.getDayOfWeek().toString())
                            .minister(0)
                            .cs(0)
                            .meeting(0)
                            .officer(0)
                            .department(0)
                            .totalNoOfVisitors(0)
                            .build()
            );
        }

        return VisitorReportResponse.builder()
                .minister(ministerTotal)
                .cs(csTotal)
                .meeting(meetingTotal)
                .officer(officerTotal)
                .department(departmentTotal)
                .noOfVisitors(grandTotal)
                .details(new ArrayList<>(dailyMap.values()))
                .build();
    }
}
