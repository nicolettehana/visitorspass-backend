package sad.storereg.controller.appdata;

import org.springframework.http.ResponseEntity;
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

import jakarta.validation.Valid;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import lombok.RequiredArgsConstructor;
import sad.storereg.annotations.Auditable;
import sad.storereg.dto.appdata.VisitorRequestDto;
import sad.storereg.models.appdata.Visitor;
import sad.storereg.repo.appdata.VisitorRepository;
import sad.storereg.services.appdata.PassService;
import sad.storereg.services.appdata.VisitorPhotoService;
import sad.storereg.services.appdata.VisitorService;

@RestController
@RequestMapping("/visitor")
@RequiredArgsConstructor
public class VisitorController {
	
	private final VisitorService visitorService;
	private final VisitorPhotoService visitorPhotoService;
	private final PassService passService;
	
	@Auditable
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<byte[]> createVisitor(
	        @RequestPart("visitor") String visitorJson,
	        @RequestPart("photo") MultipartFile photo
	) throws JsonMappingException, JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
	    VisitorRequestDto visitorDto =
	            mapper.readValue(visitorJson, VisitorRequestDto.class);
	    Visitor visitor =visitorService.createVisitor(visitorDto, photo);
	    //return passService.generateVisitorPassPdf(visitor, photo);
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
 	        @RequestParam(defaultValue = "") String search
    ) {

        Pageable pageable = PageRequest.of(page, size);

        return visitorService.getVisitorsBetweenDates(startDate,endDate,search, pageable);
    }
	
	@GetMapping("/{visitorCode}/photo")
    public ResponseEntity<byte[]> getVisitorPhoto(
            @PathVariable Long visitorCode
    ) {
        byte[] imageBytes = visitorPhotoService.getVisitorPhoto(visitorCode);
        String contentType = visitorPhotoService.getVisitorPhotoContentType(visitorCode);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(MediaType.parseMediaType(contentType))
                .body(imageBytes);
    }

}
