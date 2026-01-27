package sad.storereg.controller.appdata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import sad.storereg.dto.appdata.CategoryStockResponse;
import sad.storereg.services.appdata.StockBalanceService;

@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {
	
	 private final StockBalanceService stockService;

	    @GetMapping("/{level}")
	    public ResponseEntity<List<CategoryStockResponse>> getStock(
	    		@PathVariable Integer level
	    ) {
	        List<CategoryStockResponse> response = stockService.getStockFiltered(level);
	        return ResponseEntity.ok(response);
	    }
	    
	    @GetMapping("/stats")
	    public Map<String, Object> getStock(
	    ) {
	    	
	    	Map<String, Object> response = new HashMap<>();
	        response.put("total", 1);
	        response.put("byCategory", 1);
	        return response;

	    }

}
