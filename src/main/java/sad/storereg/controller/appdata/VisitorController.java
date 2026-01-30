package sad.storereg.controller.appdata;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;

import lombok.RequiredArgsConstructor;
import sad.storereg.annotations.Auditable;
import sad.storereg.dto.appdata.VisitorRequestDto;
import sad.storereg.repo.appdata.VisitorRepository;
import sad.storereg.services.appdata.VisitorService;

@RestController
@RequestMapping("/visitor")
@RequiredArgsConstructor
public class VisitorController {
	
	private final VisitorService visitorService;
	
	//@Auditable
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> createVisitor(
	        @Valid @RequestPart("visitor") String visitorJson,
	        @RequestPart("photo") MultipartFile photo
	) throws JsonMappingException, JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
	    VisitorRequestDto visitorDto =
	            mapper.readValue(visitorJson, VisitorRequestDto.class);
	    String message =visitorService.createVisitor(visitorDto, photo);
	    return ResponseEntity.ok(message);
	}

}
