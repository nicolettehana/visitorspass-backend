package sad.storereg.controller.master;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
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

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import sad.storereg.annotations.Auditable;
import sad.storereg.dto.appdata.CategoryCountDTO;
import sad.storereg.dto.master.ItemDTO;
import sad.storereg.models.master.Item;
import sad.storereg.services.appdata.ExcelServices;
import sad.storereg.services.master.ItemService;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

	private final ItemService itemService;

    @GetMapping({ "", "/{category}" })
    public Page<Item> getPaginatedItems(
    		 @PathVariable(required = false) String category,
    	        @RequestParam(defaultValue = "0") int page,
    	        @RequestParam(defaultValue = "10") int size,
    	        @RequestParam(defaultValue = "") String search
    ) {
        Pageable pageable = PageRequest.of(page, size);

        return itemService.getItems(pageable, search, category);
    }
    
    @GetMapping({ "/list/{category}" })
    public List<Item> getListItems(
    		 @PathVariable(required = false) String category,
    	        @RequestParam(defaultValue = "") String search
    ) {

        return itemService.getItemsList(search, category);
    }

    @Auditable
    @PostMapping
    public ResponseEntity<?> createItem(@RequestBody ItemDTO request) {
      
        return ResponseEntity.ok(itemService.createItem(request));
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCategoryCounts() {
    	 Map<String, Object> response = new HashMap<>();
         response.put("total", itemService.getTotalItems());
         response.put("byCategory", itemService.getCategoryCounts());

         //return response;
        //List<CategoryCountDTO> counts = itemService.getCategoryCounts();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportItemsToExcel(
            @RequestParam(required = false) String category,
            HttpServletResponse response
    ) throws IOException {

    	byte[] excelData = itemService.getItems(category);
    	
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=itemsS.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelData);
    }

}
