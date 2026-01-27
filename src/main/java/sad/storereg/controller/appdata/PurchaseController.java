package sad.storereg.controller.appdata;

import java.time.LocalDate;
import java.util.Map;

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
import sad.storereg.dto.appdata.PurchaseReceiptDTO;
import sad.storereg.dto.appdata.PurchaseResponseDTO;
import sad.storereg.services.appdata.PurchaseService;

@RestController
@RequestMapping("/purchase")
@RequiredArgsConstructor
public class PurchaseController {
	
	private final PurchaseService purchaseService;
	
	 @GetMapping({ "", "/{category}" })
	 public Page<PurchaseResponseDTO> getPaginatedFirms(
	 	 @PathVariable(required = false) String category,
	 	        @RequestParam(defaultValue = "0") int page,
	 	        @RequestParam(defaultValue = "10") int size,
	 	        @RequestParam(defaultValue = "") String search,
	 	        @RequestParam(defaultValue = "") LocalDate startDate,
	 	        @RequestParam(defaultValue = "") LocalDate endDate,
	 	       @RequestParam(defaultValue = "") String status,
	 	       @RequestParam(defaultValue="PO") String type
	 ) {
	     Pageable pageable = PageRequest.of(page, size);
	     return purchaseService.searchPurchases(startDate, endDate, category, search, status, type, pageable);
	 }
	 
	 @GetMapping({ "/ns", "/ns/{category}" })
	 public Page<PurchaseResponseDTO> getPaginatedNonStockPurchases(
	 	 @PathVariable(required = false) String category,
	 	        @RequestParam(defaultValue = "0") int page,
	 	        @RequestParam(defaultValue = "10") int size,
	 	        @RequestParam(defaultValue = "") String search,
	 	        @RequestParam(defaultValue = "") LocalDate startDate,
	 	        @RequestParam(defaultValue = "") LocalDate endDate,
	 	       @RequestParam(defaultValue = "") String status,
	 	      @RequestParam(defaultValue="PO") String type
	 ) {
	     Pageable pageable = PageRequest.of(page, size);
	     return purchaseService.searchNonStockPurchases(startDate, endDate, category, search, status, type, pageable);
	 }
	 
	 @Auditable
	 @PostMapping("/create")
	 public ResponseEntity<String> savePurchase(@RequestBody PurchaseCreateDTO purchaseDTO) {

	     return ResponseEntity.ok(purchaseService.savePurchase(purchaseDTO));
	 }
	 
	 @Auditable
	 @PostMapping("/create-ns")
	 public ResponseEntity<String> savePurchaseNS(@RequestBody PurchaseCreateDTO purchaseDTO) {

	     return ResponseEntity.ok(purchaseService.savePurchaseNS(purchaseDTO));
	 }
	 
	 @Auditable
	 @PostMapping("/receipt")
	 public ResponseEntity<String> savePurchaseReceipt(@RequestBody PurchaseReceiptDTO purchaseDTO) {
		 //System.out.println("Data: "+purchaseDTO);
	     return ResponseEntity.ok(purchaseService.savePurchaseReceipt(purchaseDTO));
		//return ResponseEntity.ok(null);
	 }
	 
	 @Auditable
	 @PostMapping("/receipt-ns")
	 public ResponseEntity<String> savePurchaseReceiptNS(@RequestBody PurchaseReceiptDTO purchaseDTO) {
		 //System.out.println("Data: "+purchaseDTO);
	     return ResponseEntity.ok(purchaseService.savePurchaseReceiptNS(purchaseDTO));
		//return ResponseEntity.ok(null);
	 }
	 
	 @GetMapping("/year/{year}")
	    public ResponseEntity<Map<String, Object>> getFinancialYearReport(
	            @PathVariable int year) {

	        Map<String, Object> response = purchaseService.getFinancialYearReport(year);
	        return ResponseEntity.ok(response);
	    }
	 
	 @GetMapping({ "/export", "/export/{categoryCode}" })
	 public ResponseEntity<byte[]> exportPurchaseOrders(
	 		 @PathVariable(required = false) String categoryCode,
	  	        @RequestParam(defaultValue = "") LocalDate startDate,
	  	        @RequestParam(defaultValue = "") LocalDate endDate,
	  	      @RequestParam(defaultValue = "A") String status
	 ) {
	  	byte[] excelData = purchaseService.exportPurchase(startDate, endDate, categoryCode, status);
	  	
	    return ResponseEntity.ok()
	         .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=purchase_orders_"+categoryCode+"_"+startDate+"-"+endDate+".xlsx")
	         .contentType(MediaType.APPLICATION_OCTET_STREAM)
	         .body(excelData);
	 }
	 
	 @GetMapping({ "/export/receipts", "/export/receipts/{categoryCode}" })
	 public ResponseEntity<byte[]> exportPurchaseReceipts(
	 		 @PathVariable(required = false) String categoryCode,
	  	        @RequestParam(defaultValue = "") LocalDate startDate,
	  	        @RequestParam(defaultValue = "") LocalDate endDate
	 ) {
	  	byte[] excelData = purchaseService.exportPurchaseReceipts(startDate, endDate, categoryCode);
	  	
	    return ResponseEntity.ok()
	         .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=purchase_receipts_"+categoryCode+"_"+startDate+"-"+endDate+".xlsx")
	         .contentType(MediaType.APPLICATION_OCTET_STREAM)
	         .body(excelData);
	 }
	 
	 @GetMapping({ "/export-ns", "/export-ns/{categoryCode}" })
	 public ResponseEntity<byte[]> exportPurchaseOrdersNonStock(
	 		 @PathVariable(required = false) String categoryCode,
	  	        @RequestParam(defaultValue = "") LocalDate startDate,
	  	        @RequestParam(defaultValue = "") LocalDate endDate,
	  	      @RequestParam(defaultValue = "A") String status
	 ) {
	  	byte[] excelData = purchaseService.exportPurchaseOrdersNS(startDate, endDate, categoryCode, status);
	  	
	    return ResponseEntity.ok()
	         .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=purchase_orders_ns_"+categoryCode+"_"+startDate+"-"+endDate+".xlsx")
	         .contentType(MediaType.APPLICATION_OCTET_STREAM)
	         .body(excelData);
	 }
	 
	 @GetMapping({ "/export/receipts-ns", "/export/receipts-ns/{categoryCode}" })
	 public ResponseEntity<byte[]> exportPurchaseReceiptsNS(
	 		 @PathVariable(required = false) String categoryCode,
	  	        @RequestParam(defaultValue = "") LocalDate startDate,
	  	        @RequestParam(defaultValue = "") LocalDate endDate
	 ) {
	  	byte[] excelData = purchaseService.exportPurchaseReceiptsNS(startDate, endDate, categoryCode);
	  	
	    return ResponseEntity.ok()
	         .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=purchase_receipts_ns_"+categoryCode+"_"+startDate+"-"+endDate+".xlsx")
	         .contentType(MediaType.APPLICATION_OCTET_STREAM)
	         .body(excelData);
	 }
	 
}
