package sad.storereg.controller.master;

import java.io.IOException;

import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import sad.storereg.services.master.RateService;
import sad.storereg.annotations.Auditable;
import sad.storereg.dto.master.ItemRateCreateDTO;
import sad.storereg.dto.master.ItemRateDTO;

@RestController
@RequestMapping("/rates")
@RequiredArgsConstructor
public class RatesController {

	private final RateService rateService;
	
	@GetMapping({ "", "/{category}" })
	public Page<ItemRateDTO> filterRates(@PathVariable(required = false) String category,
	@RequestParam(required = false) Integer yearRange,
	@RequestParam int page,
	@RequestParam int size,
	@RequestParam(defaultValue = "") String search) {

		return rateService.getRates(category, yearRange, search, PageRequest.of(page, size));
		

	}
	
	@Auditable
	@PostMapping
    public ResponseEntity<?> createRate(@RequestBody ItemRateCreateDTO request) {
        return ResponseEntity.ok(rateService.createRate(request));
		//return ResponseEntity.ok("ok");
    }
	
	@Auditable
	@PostMapping("/add")
    public ResponseEntity<?> addRate(@RequestBody ItemRateCreateDTO request) throws BadRequestException {
		if(request.getItemId()==null || request.getRate()==null || request.getUnitId()==null || request.getYearRangeId()==null)
			throw new BadRequestException("Invalid input");
        return ResponseEntity.ok(rateService.addRate(request));
		//return ResponseEntity.ok("ok");
    }
	
	@GetMapping({ "/export", "/export/{category}" })
    public ResponseEntity<byte[]> exportFirms(
    		@PathVariable(required = false) String category,
    		@RequestParam(required = false) Integer yearRange
    ) throws IOException {

	    	byte[] excelData = rateService.exportRates(category, yearRange);
	
	        return ResponseEntity.ok()
	                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=approved_firms.xlsx")
	                .contentType(MediaType.APPLICATION_OCTET_STREAM)
	                .body(excelData);

    	
    }
}
