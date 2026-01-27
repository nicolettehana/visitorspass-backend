package sad.storereg.controller.master;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import sad.storereg.annotations.Auditable;
import sad.storereg.dto.master.UnitRateDTO;
import sad.storereg.dto.master.UnitRequestDTO;
import sad.storereg.exception.InternalServerError;
import sad.storereg.exception.UnauthorizedException;
import sad.storereg.models.master.Unit;
import sad.storereg.models.master.YearRange;
import sad.storereg.services.master.MasterDataServices;

@RestController
@RequestMapping("/unit")
@RequiredArgsConstructor
public class UnitController {

	private final MasterDataServices masterDataServices;
	
	@GetMapping
	public List<Unit> getUnits() throws IOException {
		try {
			return masterDataServices.getUnits();
		} catch (UnauthorizedException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerError("Unable to fetch units", ex);
		}
	}
	
	@Auditable
	@PostMapping("/rates")
	public List<UnitRateDTO> getUnitsRates(@RequestBody UnitRequestDTO request) throws IOException {
		try {
			return masterDataServices.getUnitsRates(request);
		} catch (UnauthorizedException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerError("Unable to fetch units", ex);
		}
	}
	
	@GetMapping("/rates")
	public List<UnitRateDTO> getUnitsRatess(@RequestParam LocalDate purchaseDate) throws IOException {
		try {
			return masterDataServices.getUnitsRatesByDate(purchaseDate);
		} catch (UnauthorizedException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerError("Unable to fetch units", ex);
		}
	}
	
	@GetMapping("/balance")
	public List<UnitRateDTO> getUnitsBalance(@RequestParam(required = false) LocalDate issueDate) throws IOException {
		try {
			return masterDataServices.getUnitsBalance(issueDate);
		} catch (UnauthorizedException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerError("Unable to fetch units", ex);
		}
	}
	
	@Auditable
	@PostMapping
    public ResponseEntity<?> createUnit(@RequestBody Unit request) {
        return ResponseEntity.ok(masterDataServices.createUnit(request));
		//return ResponseEntity.ok("ok");
    }
}
