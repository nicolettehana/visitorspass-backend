package sad.storereg.controller.master;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import sad.storereg.annotations.Auditable;
import sad.storereg.dto.master.CreateFirmDTO;
import sad.storereg.dto.master.FirmApproveDTO;
import sad.storereg.dto.master.FirmCheckDTO;
import sad.storereg.dto.master.FirmYearDTO;
import sad.storereg.dto.master.FirmsDTO;
import sad.storereg.models.master.Firm;
import sad.storereg.services.master.FirmService;

@RestController
@RequestMapping("/firms")
@RequiredArgsConstructor
public class FirmController {
	
	private final FirmService firmService;

    @GetMapping({ "", "/{category}" })
    public Page<FirmsDTO> getPaginatedFirms(
    		 @PathVariable(required = false) String category,
    	        @RequestParam(defaultValue = "0") int page,
    	        @RequestParam(defaultValue = "10") int size,
    	        @RequestParam(defaultValue = "") String search,
    	        @RequestParam(defaultValue = "") Integer yearRangeId
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "firm"));
        if(search!=null && search.length()>0){
        	return firmService.searchFirms(pageable, search, yearRangeId);
        }
        else if(yearRangeId!=null)
        	return firmService.getFirms(yearRangeId, category, pageable);

        return firmService.getFirms(pageable, search, category);
    }
    
    @Auditable
    @PostMapping
    public ResponseEntity<?> createFirm(@RequestBody CreateFirmDTO request) {
        
        return ResponseEntity.ok(firmService.createFirm(request));
    }
    
    @GetMapping({ "/listt" })
    public List<FirmsDTO> getListFirmss(
    ) {
    	
    	return firmService.getFirmsList();
    }
    
    @GetMapping({ "/list" })
    public List<FirmsDTO> getListFirms(@RequestParam(defaultValue = "") LocalDate date
    ) {
    	System.out.println("Date: "+date);
    	if(date!=null) 
    	
    		return firmService.getFirmsListByDate(date);
    	else
    		return firmService.getFirmsList();
    }
    
    @Auditable
    @PostMapping("/add-approved")
    public ResponseEntity<?> createFirmYear(@RequestBody FirmYearDTO request) {
    	
        return ResponseEntity.ok(firmService.createFirmYear(request));
    }
    
    @GetMapping({"/all/{category}" })
    public Page<FirmCheckDTO> getPaginatedAllFirms(
    		 @PathVariable(required = true) String category,
    	        @RequestParam(defaultValue = "0") int page,
    	        @RequestParam(defaultValue = "10") int size,
    	        @RequestParam(defaultValue = "") String search,
    	        @RequestParam(required= true, defaultValue = "") Integer yearRangeId
    ) {
    	
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "firm"));
        
        	return firmService.getAllFirms(yearRangeId, category, search, pageable);

    }
    
    @Auditable
    @PostMapping("/approve")
    public ResponseEntity<?> addRemoveApprovedFirm(@RequestBody FirmApproveDTO request) {
    	try {
    		return ResponseEntity.ok(firmService.updateFirmYear(request));
    	}catch(Exception ex) {
    		throw ex;
    	}
    }
    
    @Auditable
    @PostMapping("/update")
    public ResponseEntity<?> updateFirm(@RequestBody FirmsDTO request) {
        
        return ResponseEntity.ok(firmService.updateFirm(request));
    }
    
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportFirms(
            @RequestParam(required = false) Integer yearRangeId,
            @RequestParam(required = false) String category
    ) throws IOException {

    	if(yearRangeId!=null) {
	    	byte[] excelData = firmService.exportApprovedFirms(yearRangeId, category);
	
	        return ResponseEntity.ok()
	                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=approved_firms.xlsx")
	                .contentType(MediaType.APPLICATION_OCTET_STREAM)
	                .body(excelData);
    	}
    	else {
	    	byte[] excelData = firmService.exportAllFirms();
	
	        return ResponseEntity.ok()
	                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=all_firms.xlsx")
	                .contentType(MediaType.APPLICATION_OCTET_STREAM)
	                .body(excelData);
    	}
    }

}
