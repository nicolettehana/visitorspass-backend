package sad.storereg.controller.appdata;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import lombok.RequiredArgsConstructor;
import sad.storereg.dto.appdata.VisitorRequestDto;
import sad.storereg.repo.appdata.VisitorRepository;
import sad.storereg.services.appdata.VisitorService;

@RestController
@RequestMapping("/visitor")
@RequiredArgsConstructor
public class VisitorController {
	
	private final VisitorService visitorService;
	
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> createVisitor(
	        @RequestPart("visitor") VisitorRequestDto visitorDto,
	        @RequestPart("photo") MultipartFile photo
	) {
	    visitorService.createVisitor(visitorDto, photo);
	    return ResponseEntity.ok("Visitor created");
	}

}
