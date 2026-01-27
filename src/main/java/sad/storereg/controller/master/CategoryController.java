package sad.storereg.controller.master;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import sad.storereg.annotations.Auditable;
import sad.storereg.exception.InternalServerError;
import sad.storereg.exception.UnauthorizedException;
import sad.storereg.models.auth.User;
import sad.storereg.models.master.Category;
import sad.storereg.models.master.YearRange;
import sad.storereg.repo.master.CategoryRepository;
import sad.storereg.repo.master.FirmCategoryRepository;
import sad.storereg.repo.master.FirmYearRepository;
import sad.storereg.repo.master.FirmsRepository;
import sad.storereg.services.master.MasterDataServices;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
public class CategoryController {
	
	private final MasterDataServices masterDataServices;
	private final FirmsRepository firmRepo;
	private final FirmYearRepository firmYearRepo;

	@GetMapping({ "", "/{stockType}" })
	public List<Category> getCatagory(@PathVariable(required = false) String stockType,HttpServletRequest request, HttpServletResponse response , @AuthenticationPrincipal User user) throws IOException {
		try {
			return masterDataServices.getCategories(stockType);
		} catch (UnauthorizedException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerError("Unable to fetch categories", ex);
		}
	}
	
	@GetMapping("/stats")
    public Map<String, Object> getStats(@RequestParam(required= false) Long yearRangeId) {

        Map<String, Object> response = new HashMap<>();
        response.put("total", firmRepo.count());
        //response.put("byCategory", firmCategoryRepo.countFirmsPerCategory());
        if(yearRangeId!=null)
        	response.put("byCategory", firmYearRepo.findCategoryCountsByYearRange(yearRangeId));

        return response;
    }
	
	@Auditable
	@PostMapping
    public ResponseEntity<?> createCategory(@RequestBody Category request) {
		try {
			return ResponseEntity.ok(masterDataServices.createCategory(request));
			//return ResponseEntity.ok("ok");
		} catch (UnauthorizedException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerError("Unable to add category", ex);
		}
        
    }
	
	@Auditable
	@PostMapping("/update")
    public ResponseEntity<?> updateCategory(@RequestBody Category request) {
		try {
			return ResponseEntity.ok(masterDataServices.updateCategory(request));
			//return ResponseEntity.ok("ok");
		} catch (UnauthorizedException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerError("Unable to update category", ex);
		}
        
    }

}
