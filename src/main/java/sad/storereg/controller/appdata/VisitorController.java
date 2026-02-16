package sad.storereg.controller.appdata;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lowagie.text.DocumentException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import lombok.RequiredArgsConstructor;

import sad.storereg.annotations.Auditable;
import sad.storereg.dto.appdata.PhotoData;
import sad.storereg.dto.appdata.VisitorRequestDto;
import sad.storereg.models.appdata.Visitor;
import sad.storereg.models.auth.User;
import sad.storereg.repo.appdata.VisitorRepository;
import sad.storereg.services.appdata.PassService;
import sad.storereg.services.appdata.ReportService;
import sad.storereg.services.appdata.ReportServiceExcel;
import sad.storereg.services.appdata.VisitorPhotoService;
import sad.storereg.services.appdata.VisitorService;

@RestController
@RequestMapping("/visitor")
@RequiredArgsConstructor
public class VisitorController {
	
	private final VisitorService visitorService;
	private final VisitorPhotoService visitorPhotoService;
	private final PassService passService;
	private final ReportService reportService;
	private final ReportServiceExcel reportServiceExcel;
	
	@Auditable
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<byte[]> createVisitor(
	        @RequestPart("visitor") String visitorJson,
	        @RequestPart("photo") MultipartFile photo,
	        @AuthenticationPrincipal User user
	) throws JsonMappingException, JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
	    VisitorRequestDto visitorDto =
	            mapper.readValue(visitorJson, VisitorRequestDto.class);
	    Visitor visitor =visitorService.createVisitor(visitorDto, photo,user.getOfficeCode());
	    return ResponseEntity.ok()
	            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + visitor.getVPassNo() + ".pdf")
	            .contentType(MediaType.APPLICATION_PDF)
	            .body(passService.generateVisitorPassPdf(visitor, photo));
	    //return ResponseEntity.ok("Registered");
	}
	
	@GetMapping
    public Page<Visitor> getVisitorsByDateRange(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
 	        @RequestParam(defaultValue = "10") int size,
 	        @RequestParam(defaultValue = "") String search,
 	        @RequestParam(required=false) Integer officeCode,
 	       @AuthenticationPrincipal User user
    ) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        
        Integer offCode;
        if(officeCode==null)
        	offCode = user.getOfficeCode();
        else
        	offCode=officeCode;

        return visitorService.getVisitorsBetweenDates(startDate,endDate,search, offCode, pageable);
    }
	
	@GetMapping("/{visitorCode}/photo")
    public ResponseEntity<byte[]> getVisitorPhoto(
            @PathVariable Long visitorCode
    ) {
        PhotoData photoData = visitorPhotoService.getVisitorPhoto(visitorCode);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(MediaType.parseMediaType(photoData.contentType()))
                .body(photoData.data());
    }
    
    @GetMapping("/{visitorCode}/pass")
    public ResponseEntity<byte[]> getVisitorPass(@PathVariable Long visitorCode) {
        PhotoData pdfData = passService.getVisitorPassPdf(visitorCode);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(pdfData.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=visitor-pass.pdf")
                .body(pdfData.data());
    }
    
    @GetMapping("/export")
    public ResponseEntity<byte[]> generateReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String format, @RequestParam Integer withPhoto, @RequestParam(required=false) Integer officeCode, @AuthenticationPrincipal User user) throws Exception {

        byte[] fileBytes;
        String fileName;
        MediaType mediaType;

        Integer offCode;
        if(user.getRole().name().equals("SAD"))
        	offCode = user.getOfficeCode();
        else
        	offCode=officeCode;
        
        if ("PDF".equalsIgnoreCase(format)) {
            fileBytes = reportService.generateVisitorReport(startDate, endDate, offCode, withPhoto);
            fileName = "visitor_report.pdf";
            mediaType = MediaType.APPLICATION_PDF;

        } else if ("EXCEL".equalsIgnoreCase(format)) {
            fileBytes = reportServiceExcel.generateVisitorReportExcel(startDate, endDate, offCode, withPhoto);
            fileName = "visitor_report.xlsx";
            mediaType = MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        } else {
            throw new IllegalArgumentException("Invalid format. Use PDF or EXCEL");
        }

        if (fileBytes == null || fileBytes.length < 100) {
            throw new RuntimeException("Generated file is empty");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", fileName);
        headers.setContentType(mediaType);

        return ResponseEntity.ok()
                .headers(headers)
                .body(fileBytes);
    }
    
    @GetMapping(path = "/get-info", params = { "mobileNo" })
	public Map<String, Object> getInfo2(
			@Valid @RequestParam("mobileNo") @Pattern(regexp = "\\d{10}", message = "Mobile number must be 10 digits") final String mobileNo) throws Exception {
    	
		try {
			if (mobileNo.matches(".*\\D.*")) {
				throw new BadRequestException("Invalid Mobile Number");
			}
			if (mobileNo.length() != 10 || mobileNo == null)
				throw new BadRequestException("Invalid Mobile number");
			
			Optional<Visitor> visitor = visitorService.getData(mobileNo);
			if(visitor.isEmpty())
				return null;

			Map<String, Object> data = new HashMap<>();
			data.put("name", visitor.get().getName());
			data.put("state", visitor.get().getState());
			data.put("address", visitor.get().getAddress());			
			data.put("email", visitor.get().getEmail());
			
			return data;
		} catch (Exception e) {
			throw e;
		}
	}
    
    @GetMapping(path = "/stats")
	public Map<String, Object> getStats(
			@RequestParam final Integer month, @RequestParam final Integer year, @RequestParam(required=false) final String purpose,
			@RequestParam(required=false) final Integer officeCode) throws Exception {
    	
		try {
			
			LocalDate today = LocalDate.now();
		    YearMonth requestedYearMonth = YearMonth.of(year, month);
		    YearMonth currentYearMonth = YearMonth.from(today);

		    int maxDay;

		    if (requestedYearMonth.equals(currentYearMonth)) {
		        maxDay = today.getDayOfMonth(); // current month → until today
		    } else {
		        maxDay = requestedYearMonth.lengthOfMonth(); // full month
		    }
		    
			Map<String, Object> data = new HashMap<>();
			List<Map<String, Object>> result = new ArrayList<>();
			
			for (int day = 1; day <= maxDay; day++) {

		        LocalDate currentDate = LocalDate.of(year, month, day);

		        Map<String, Object> dailyData = new HashMap<>();

		        dailyData.put("date", currentDate);
		        dailyData.put("dayOfWeek",
		                currentDate.getDayOfWeek()
		                        .getDisplayName(TextStyle.FULL, Locale.ENGLISH)
		        );

		        // 🔽 Replace these with actual DB counts
		        long total = 1;
		        long purpose1 = 1;
		        long purpose2 = 1;
		        long purpose3 = 1;
		        long purpose4 = 1;

		        dailyData.put("totalNoOfVisitors", total);
		        dailyData.put("noOfVisitorsPurpose1", purpose1);
		        dailyData.put("noOfVisitorsPurpose2", purpose2);
		        dailyData.put("noOfVisitorsPurpose3", purpose3);
		        dailyData.put("noOfVisitorsPurpose4", purpose4);

		        result.add(dailyData);
		    }
			
			data.put("details", result);
			data.put("noOfVisitors", 1);
			data.put("purpose1", 1);
			data.put("purpose2", 1);			
			data.put("purpose3", 1);
			
			return data;
		} catch (Exception e) {
			throw e;
		}
	}
}
