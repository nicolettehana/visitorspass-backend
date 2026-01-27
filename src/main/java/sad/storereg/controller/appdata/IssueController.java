package sad.storereg.controller.appdata;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import sad.storereg.annotations.Auditable;
import sad.storereg.dto.appdata.PurchaseCreateDTO;
import sad.storereg.dto.appdata.PurchaseResponseDTO;
import sad.storereg.services.appdata.IssueService;

@RestController
@RequestMapping("/issue")
@RequiredArgsConstructor
public class IssueController {
	
	private final IssueService issueService;
	
	@GetMapping({ "", "/{category}" })
    public Page<PurchaseResponseDTO> getPaginatedFirms(
    		 @PathVariable(required = false) String category,
    	        @RequestParam(defaultValue = "0") int page,
    	        @RequestParam(defaultValue = "10") int size,
    	        @RequestParam(defaultValue = "") String search,
    	        @RequestParam(defaultValue = "") LocalDate startDate,
    	        @RequestParam(defaultValue = "") LocalDate endDate
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return issueService.searchIssues(startDate, endDate, category, search, pageable);
    }
	
	@Auditable
	@PostMapping("/create")
    public ResponseEntity<String> saveIssue(@RequestBody PurchaseCreateDTO purchaseDTO) {

        return ResponseEntity.ok(issueService.saveIssue(purchaseDTO));

    }
	
	@GetMapping({ "/export", "/export/{categoryCode}" })
	 public ResponseEntity<byte[]> exportPurchase(
	 		 @PathVariable(required = false) String categoryCode,
	  	        @RequestParam(defaultValue = "") LocalDate startDate,
	  	        @RequestParam(defaultValue = "") LocalDate endDate
	 ) {
		 System.out.println("Data: "+categoryCode+" "+startDate+" "+endDate);
	  	byte[] excelData = issueService.exportIssues(startDate, endDate, categoryCode);
	  	
	    return ResponseEntity.ok()
	         .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=issue_"+categoryCode+"_"+startDate+"-"+endDate+".xlsx")
	         .contentType(MediaType.APPLICATION_OCTET_STREAM)
	         .body(excelData);
	 }

}
