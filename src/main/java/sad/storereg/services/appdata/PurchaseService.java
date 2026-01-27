package sad.storereg.services.appdata;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import sad.storereg.dto.appdata.ItemPurchaseDTO;
import sad.storereg.dto.appdata.PurchaseCreateDTO;
import sad.storereg.dto.appdata.PurchaseReceiptDTO;
import sad.storereg.dto.appdata.PurchaseReceiptItems;
import sad.storereg.dto.appdata.PurchaseResponseDTO;
import sad.storereg.dto.appdata.SubItemPurchaseDTO;
import sad.storereg.exception.ObjectNotFoundException;
import sad.storereg.models.appdata.Purchase;
import sad.storereg.models.appdata.PurchaseItems;
import sad.storereg.models.appdata.PurchaseNonStock;
import sad.storereg.models.appdata.PurchaseItemNonStock;
import sad.storereg.models.master.Category;
import sad.storereg.models.master.Firm;
import sad.storereg.models.master.SubItems;
import sad.storereg.models.master.YearRange;
import sad.storereg.repo.appdata.PurchaseNonStockRepository;
import sad.storereg.repo.appdata.PurchaseRepository;
import sad.storereg.repo.master.CategoryRepository;
import sad.storereg.repo.master.FirmsRepository;
import sad.storereg.repo.master.ItemRepository;
import sad.storereg.repo.master.SubItemRepository;
import sad.storereg.repo.master.UnitRepository;
import sad.storereg.repo.master.YearRangeRepository;
import sad.storereg.services.master.RateService;

@RequiredArgsConstructor
@Service
public class PurchaseService {
	
	private final PurchaseRepository purchaseRepository;
	
	private final PurchaseNonStockRepository purchaseNonStockRepository;
	
	private final FirmsRepository firmRepository;
	
	private final UnitRepository unitRepository;
	
	private final ItemRepository itemRepository;
	
	private final SubItemRepository subItemRepository;
	
	private final RateService rateService;
	
	private final YearRangeRepository yearRangeRepository;
	
	private final ExcelServices excelService;
	
	private final CategoryRepository categoryRepository;
	
	public Page<PurchaseResponseDTO> searchPurchases(
            LocalDate startDate,
            LocalDate endDate,
            String category,
            String searchValue,
            String status,
            String type,
            Pageable pageable) {

		Page<Purchase> page;
		if(type.equals("PO"))
				page = purchaseRepository.searchPurchases(
                startDate, endDate, category, searchValue, status, pageable);
		else
			page = purchaseRepository.searchPurchaseReceipts(
	                startDate, endDate, category, searchValue, status, pageable);

        return page.map(this::convertToDTO);
    }

	private PurchaseResponseDTO convertToDTO(Purchase p) {
		
	    PurchaseResponseDTO dto = new PurchaseResponseDTO();
	    
	    dto.setPurchaseId(p.getId());
	    dto.setFirmName(p.getFirm().getFirm());
	    dto.setRemarks(p.getRemarks());
	    dto.setTotalCost(p.getTotalCost());
	    dto.setDate(p.getDate());
	    dto.setFileNo(p.getFileNo());
	    dto.setBillNo(p.getBillNo());
	    dto.setBillDate(p.getBillDate());
	    dto.setGstPercentage(p.getGstPercentage());
	    //dto.setGst(p.getGstPercentage()!=null? (p.getGstPercentage()*p.getTotalCost())/100 : null);
	    dto.setGst(
	    	    p.getGstPercentage() != null
	    	        ? BigDecimal.valueOf(p.getTotalCost())
	    	            .multiply(BigDecimal.valueOf(p.getGstPercentage()))
	    	            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
	    	            .doubleValue()
	    	        : null
	    	);

	    
	    // Group items by item name
	    Map<String, List<PurchaseItems>> itemGroup = p.getItems()
	            .stream()
	            .collect(Collectors.groupingBy(pi -> pi.getItem().getName()));

	    List<ItemPurchaseDTO> itemDTOs = new ArrayList<>();

	    for (var entry : itemGroup.entrySet()) {

	        ItemPurchaseDTO itemDTO = new ItemPurchaseDTO();
	        itemDTO.setItemName(entry.getKey());

	        // Set category (all grouped items have the same category)
	        String category = entry.getValue().get(0).getItem().getCategory().getName();
	        
	        itemDTO.setCategory(category);
	        itemDTO.setCategoryCode( entry.getValue().get(0).getItem().getCategory().getCode());
	        itemDTO.setId(entry.getValue().get(0).getId());
	        itemDTO.setItemId(entry.getValue().get(0).getItem().getId());
	        itemDTO.setUnitId(entry.getValue().get(0).getUnit().getId());;
	        
	        List<SubItemPurchaseDTO> subItems = entry.getValue()
	                .stream()
	                .map(pi -> {
	                    if (pi.getSubItem() == null) {
	                        itemDTO.setQuantity(pi.getQuantity());
	                        itemDTO.setRate(pi.getRate());
	                        itemDTO.setAmount(pi.getAmount());
	                        itemDTO.setUnit(pi.getUnit().getUnit());
	                        itemDTO.setGstPercentage(pi.getGstPercentage());
	                        itemDTO.setCgst(pi.getCgst());
	                        itemDTO.setSgst(pi.getSgst());
	                        
	                        return null;
	                    }

	                    SubItemPurchaseDTO sd = new SubItemPurchaseDTO();
	                    sd.setSubItemName(pi.getSubItem().getName());
	                    sd.setQuantity(pi.getQuantity());
	                    sd.setRate(pi.getRate());
	                    sd.setAmount(pi.getAmount());
	                    sd.setUnit(pi.getUnit().getUnit());
	                    sd.setSubItemId(pi.getSubItem().getId());
	                    sd.setUnitId(pi.getUnit().getId());
	                    sd.setGstPercentage(pi.getGstPercentage());
	                    sd.setCgst(pi.getCgst());
	                    sd.setSgst(pi.getSgst());
	                    
	                    return sd;
	                })
	                .filter(Objects::nonNull)
	                .collect(Collectors.toList());

	        itemDTO.setSubItems(subItems);
	        itemDTOs.add(itemDTO);
	    }
	    Double totalAmount = p.getItems()
	            .stream()
	            .mapToDouble(PurchaseItems::getAmount)
	            .sum();

	    dto.setItems(itemDTOs);
	    dto.setTotalCost(totalAmount);
	    return dto;
	}
	
	public String savePurchase(PurchaseCreateDTO dto) {
		
        // 1. Fetch Firm
        Firm firm = firmRepository.findById(dto.getFirmId())
                .orElseThrow(() -> new RuntimeException("Firm not found"));

        // 2. Create Purchase entity
        Purchase purchase = new Purchase();
        purchase.setDate(dto.getPurchaseDate());   // Already LocalDate
        purchase.setFirm(firm);
        purchase.setEntryDate(LocalDateTime.now());
        purchase.setRemarks(dto.getRemarks());
        purchase.setFileNo(dto.getFileNo());
        //purchase.setTotalCost(dto.getTotalCost());
        
     // Convert items
        List<PurchaseItems> items = dto.getItems().stream().map(itemDTO -> {

            PurchaseItems item = new PurchaseItems();
            item.setPurchase(purchase);

            // Item
            item.setItem(itemRepository.findById(itemDTO.getItemId())
                    .orElseThrow(() -> new RuntimeException("Item not found")));

            // Sub Item (nullable)
            if (itemDTO.getSubItemId() != null) {
                item.setSubItem(subItemRepository.findById(itemDTO.getSubItemId())
                        .orElseThrow(() -> new RuntimeException("SubItem not found")));
            }

            // Unit
            item.setUnit(unitRepository.findById(itemDTO.getUnitId())
                    .orElseThrow(() -> new RuntimeException("Unit not found")));

            int year = dto.getPurchaseDate().getYear();
            YearRange yearRange = yearRangeRepository
                    .findByStartYearLessThanEqualAndEndYearGreaterThanEqual(year, year)
                    .orElseThrow(() -> new ObjectNotFoundException("Year not found"));

            item.setQuantity(itemDTO.getQuantity());
            Double rate = rateService.getRate(
                    itemDTO.getUnitId(),
                    itemDTO.getItemId(),
                    itemDTO.getSubItemId(),
                    yearRange.getId()
            );

            item.setRate(rate);
            item.setAmount(rate * itemDTO.getQuantity());

            return item;

        }).toList();

        // Calculate total cost AFTER mapping
        Double totalCost = items.stream()
                .mapToDouble(PurchaseItems::getAmount)
                .sum();

        purchase.setItems(items);
        purchase.setTotalCost(totalCost);

        // 4. Save (cascade saves items)
         purchaseRepository.save(purchase);
         return "Purchase added";
    }
	
	public String savePurchaseNS(PurchaseCreateDTO dto) {
		
        PurchaseNonStock purchase = new PurchaseNonStock();
        purchase.setFileDate(dto.getPurchaseDate());
        purchase.setReceivedFrom(dto.getReceivedFrom());
        purchase.setEntrydate(LocalDateTime.now());
        purchase.setRemarks(dto.getRemarks());
        purchase.setFileNo(dto.getFileNo());
        purchase.setIssueTo(dto.getIssueTo());
        
        // Convert items
        List<PurchaseItemNonStock> items = dto.getItems().stream().map(itemDTO -> {

            PurchaseItemNonStock item = new PurchaseItemNonStock();
            item.setPurchase(purchase);

            // Item
            item.setItem(itemDTO.getItem());
            item.setCategory(itemDTO.getCategoryCode());
            item.setUnit(itemDTO.getUnit());
            item.setQuantity(itemDTO.getQuantity());
            
            return item;

        }).toList();

        purchase.setItems(items);

        // 4. Save (cascade saves items)
         purchaseNonStockRepository.save(purchase);
         return "Purchase added";
    }
	
	@Transactional
	public String savePurchaseReceipt(PurchaseReceiptDTO dto) {
        // 1. Fetch Firm
        Firm firm = firmRepository.findById(dto.getFirmId())
                .orElseThrow(() -> new RuntimeException("Firm not found"));

        Purchase purchase = purchaseRepository.findById(dto.getPurchaseId()).orElseThrow(() -> new RuntimeException("Purchase Order not found"));
        // 2. Create Purchase entity
        purchase.setBillDate(dto.getBillDate());
        purchase.setFirm(firm);
        purchase.setBillNo(dto.getBillNo());
        purchase.setReceiptEntryDate(LocalDateTime.now());    
        purchase.setTotalCost(dto.getTotalCost());
        
        // Fetch existing items
        List<PurchaseItems> existingItems = purchase.getItems();

        Map<Long, PurchaseItems> itemMap = existingItems.stream()
                .collect(Collectors.toMap(PurchaseItems::getId, Function.identity()));

        // Update items from DTO
        for (PurchaseReceiptItems itemDTO : dto.getItems()) {

            PurchaseItems item = itemMap.get(itemDTO.getId());

            if (item == null) {
                throw new RuntimeException(
                        "PurchaseItem not found with id: " + itemDTO.getId()
                );
            }
            if (item.getSubItem() == null) {
                // 🔹 No sub-item → tax belongs to PurchaseItems
                item.setGstPercentage(itemDTO.getGstPercentage());
                item.setCgst(itemDTO.getCgst());
                item.setSgst(itemDTO.getSgst());
                item.setRate(itemDTO.getRate());
	            item.setAmount(itemDTO.getAmount());

            } else {
                // 🔹 Sub-item exists → update SubItems tax
                SubItems subItem = item.getSubItem();
                

                item.setGstPercentage(itemDTO.getSubItems().get(0).getGstPercentage());
                item.setCgst(itemDTO.getSubItems().get(0).getCgst());
                item.setSgst(itemDTO.getSubItems().get(0).getSgst());
                item.setRate(itemDTO.getSubItems().get(0).getRate());
                item.setAmount(itemDTO.getSubItems().get(0).getAmount());
            }
        }

        // No explicit save of items needed
        purchaseRepository.save(purchase);

        return "Purchase Receipt updated successfully";
    }
	
	@Transactional
	public String savePurchaseReceiptNS(PurchaseReceiptDTO dto) {
        

        PurchaseNonStock purchase = purchaseNonStockRepository.findById(dto.getPurchaseId()).orElseThrow(() -> new RuntimeException("Purchase Order not found"));
        // 2. Create Purchase entity
        purchase.setBillDate(dto.getBillDate());
        purchase.setReceivedFrom(dto.getReceivedFrom());;
        purchase.setBillNo(dto.getBillNo());
        purchase.setReceiptDate(LocalDate.now()); 
        purchase.setTotal(dto.getTotalCost());
        purchase.setIssueTo(dto.getIssuedTo());;
        
        // Fetch existing items
        List<PurchaseItemNonStock> existingItems = purchase.getItems();

        Map<Object, PurchaseItemNonStock> itemMap =
                existingItems.stream()
                    .collect(Collectors.toMap(
                        item -> item.getItem().trim().toLowerCase(),
                        Function.identity()
                    ));


        for (PurchaseReceiptItems itemDTO : dto.getItems()) {

            if (itemDTO.getItemName() == null) {
            	System.out.println("Item: "+itemDTO.getItemName());
                throw new RuntimeException("Item name missing");
            }

            PurchaseItemNonStock item =
                itemMap.get(itemDTO.getItemName().trim().toLowerCase());

            if (item == null) {
                throw new RuntimeException(
                    "PurchaseItem not found with name: " + itemDTO.getItemName()
                );
            }

            // ✅ Update GST & amounts
            item.setRate(itemDTO.getRate());
            item.setAmount(itemDTO.getAmount());
            item.setGstPercentage(itemDTO.getGstPercentage());
            item.setCgst(itemDTO.getCgst());
            item.setSgst(itemDTO.getSgst());
        }


        // No explicit save of items needed
        purchaseNonStockRepository.save(purchase);

        return "Purchase Receipt updated successfully";
    }
	
	public int getAvailableBalance(Long itemId, Long subItemId, Integer unitId, LocalDate date) {
		Integer availableStock = purchaseRepository.getAvailableStock(itemId, subItemId, unitId, date);
		//System.out.println("ItemID: "+itemId+" subItemId: "+subItemId+" unitId: "+unitId+" date: "+date+" available stock: "+availableStock);
	    return availableStock;
	}
	
	public String getAvailableBalanceAllUnits(Long itemId, Long subItemId, LocalDate date) {
		List<Object[]> availableStock = purchaseRepository.getAvailableStockForAllUnits(itemId, subItemId, date);
		
		String totalStock = availableStock.stream()
		        .map(r -> ((Number) r[2]).intValue() + " " + r[1])   // balance + unitName
		        .collect(Collectors.joining(", "));

		//System.out.println("ItemID: "+itemId+" subItemId: "+subItemId+" unitId: "+unitId+" date: "+date+" available stock: "+availableStock);
	    return totalStock;
	}
	
	public Map<String, Object> getFinancialYearReport(int year) {

	    LocalDate fromDate = LocalDate.of(year, 4, 1);
	    LocalDate toDate   = LocalDate.of(year + 1, 3, 31);

	    List<Object[]> results = purchaseRepository.getCategoryTotals(fromDate, toDate);

	    double total = 0;
	    List<Map<String, Object>> categories = new ArrayList<>();

	    for (Object[] row : results) {
	        String category      = (String) row[0];
	        String categoryCode  = (String) row[1];
	        Double amount        = (Double) row[2];

	        total += amount;

	        Map<String, Object> map = new HashMap<>();
	        map.put("category", category);
	        map.put("categoryCode", categoryCode);
	        map.put("value", amount);

	        categories.add(map);
	    }

	    Map<String, Object> response = new HashMap<>();
	    response.put("total", total);
	    response.put("categories", categories);

	    return response;
	}
	
	public boolean purchaseExist(Firm firm, YearRange yearRange, Category category) {
		LocalDate startDate = LocalDate.of(yearRange.getStartYear(), 1, 1);
		LocalDate endDate   = LocalDate.of(yearRange.getEndYear(), 12, 31);

		List<Purchase> purchases =
		        purchaseRepository.findPurchasesByFirmDateRangeAndCategory(
		                firm.getId(),
		                startDate,
		                endDate,
		                category.getCode()
		        );
		return (purchases.size()>0?true:false);
	}
	
	public byte[] exportPurchase(LocalDate startDate, LocalDate endDate, String categoryCode, String status) {
		
		List<Purchase> purchases = purchaseRepository.getPurchases(
                startDate, endDate, categoryCode, status
        );
		
        List<PurchaseResponseDTO> dtoList = purchases.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

               Sheet sheet = workbook.createSheet("Purchases");
               Map<String, CellStyle> styles = excelService.createStyles(workbook);

               int rowIdx = 0;

               // =====================
               // TITLE
               // =====================
               rowIdx = excelService.createTitleRow(
                       workbook,
                       sheet,
                       rowIdx,
                       "Purchase Orders",
                       0,
                       7
               );

               rowIdx++;

               // =====================
               // METADATA
               // =====================
               rowIdx = excelService.createLabelValueRow(
                       sheet,
                       rowIdx,
                       "File Date:",
                       startDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                               + " to "
                               + endDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                       styles.get("bold")
               );

               String categoryName = (categoryCode != null && !categoryCode.isEmpty())
                       ? categoryRepository.findByCode(categoryCode).get().getName()
                       : "All";

               rowIdx = excelService.createLabelValueRow(
                       sheet,
                       rowIdx,
                       "Category:",
                       categoryName,
                       styles.get("bold")
               );
               
               rowIdx = excelService.createLabelValueRow(
                       sheet,
                       rowIdx,
                       "Status:",
                       status.equals("P")?"Pending":(status.equals("R")?"Received":"All"),
                       styles.get("bold")
               );
               
               rowIdx++;

               // =====================
               // TABLE HEADER
               // =====================
               String[] headers = {
                       "Sl No.",
                       "File No. & Date",
                       "Firm",
                       "Category",
                       "Particulars",
                       "",
                       "Quantity",
                       "Status"
               };

               rowIdx = excelService.createTableHeaderRow(
                       sheet,
                       rowIdx,
                       headers,
                       styles.get("headerBorder")
               );
               
               int headerRowIndex = rowIdx - 1;
               
            // merge columns 2 and 3
      	     sheet.addMergedRegion(new CellRangeAddress(headerRowIndex, headerRowIndex, 4, 5));

      	     // ensure border style applies to merged cells
      	     Row headerRow1 = sheet.getRow(headerRowIndex);
      	     for (int col = 4; col <= 5; col++) {
      	         headerRow1.getCell(col).setCellStyle(styles.get("headerBorder"));
      	     }
               // =====================
               // TABLE DATA
               // =====================
               int slNo = 1;

               for (PurchaseResponseDTO purchase : dtoList) {

                   int purchaseStartRow = rowIdx;

                   for (ItemPurchaseDTO item : purchase.getItems()) {

                       // =====================
                       // ITEM WITHOUT SUB-ITEMS
                       // =====================
                       if (item.getSubItems() == null || item.getSubItems().isEmpty()) {
                    	   
                    	   int itemStartRow = rowIdx;

                           Row row = sheet.createRow(rowIdx++);

                           row.createCell(0).setCellValue(slNo);
                           row.createCell(1).setCellValue(purchase.getFileNo()+" Dtd. "+purchase.getDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                           row.createCell(2).setCellValue(purchase.getFirmName());
                           row.createCell(3).setCellValue(item.getCategory());
                           row.createCell(4).setCellValue(item.getItemName());
                           row.createCell(6).setCellValue(item.getQuantity());
                           row.createCell(7).setCellValue(purchase.getBillNo()==null?"Pending":"Received");

                        // styles + wrap
                           for (int col = 0; col <= 7; col++) {
                               Cell cell = row.getCell(col);
                               if (cell == null) cell = row.createCell(col);

                               cell.setCellStyle(
                            		   (col == 2 || col == 4)
                                               ? styles.get("wrapBorder")
                                               : styles.get("border")
                               );
                           }

                           row.setHeight((short) -1);
                           
                           int itemEndRow = rowIdx - 1;

          	             // merge Item + SubItem horizontally
          	             sheet.addMergedRegion(
          	                     new CellRangeAddress(itemStartRow, itemEndRow, 4, 5)
          	             );
          	             excelService.applyBorder(
          	                     new CellRangeAddress(itemStartRow, itemEndRow, 4, 5),
          	                     sheet
          	             );
                       }

                       // =====================
                       // ITEM WITH SUB-ITEMS
                       // =====================
                       else {
                    	   int itemStartRow = rowIdx;
                    	   
                           for (SubItemPurchaseDTO sub : item.getSubItems()) {

                               Row row = sheet.createRow(rowIdx++);

                               row.createCell(0).setCellValue(slNo);
                               row.createCell(1).setCellValue(purchase.getFileNo()+" Dtd. "+purchase.getDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                               row.createCell(2).setCellValue(purchase.getFirmName());
                               row.createCell(3).setCellValue(item.getCategory());
                               row.createCell(4).setCellValue(
                                       item.getItemName()
                               );
                               row.createCell(5).setCellValue(sub.getSubItemName());
                               row.createCell(6).setCellValue(sub.getQuantity());
                               row.createCell(7).setCellValue(purchase.getBillNo()==null?"Pending":"Received");

                               for (int col = 0; col <= 7; col++) {
                                   Cell cell = row.getCell(col);
                                   if (cell == null) cell = row.createCell(col);

                                   cell.setCellStyle(
                                           (col == 2 || col == 4 || col == 5)
                                                   ? styles.get("wrapBorder")
                                                   : styles.get("border")
                                   );
                               }

                               row.setHeight((short) -1);
                           }
                           int itemEndRow = rowIdx - 1;

                           // merge Item name vertically
                           excelService.mergeVertically(sheet, itemStartRow, itemEndRow, 4);
                           excelService.mergeVertically(sheet, itemStartRow, itemEndRow, 3);
                       }
                   }

                   int purchaseEndRow = rowIdx - 1;

                   // =====================
                   // MERGES PER PURCHASE
                   // =====================
                   excelService.mergeVertically(sheet, purchaseStartRow, purchaseEndRow, 0); // Sl No
                   excelService.mergeVertically(sheet, purchaseStartRow, purchaseEndRow, 1); // Date
                   excelService.mergeVertically(sheet, purchaseStartRow, purchaseEndRow, 2); // Firm

                   slNo++;
               }

               // =====================
               // AUTO SIZE
               // =====================
               for (int i = 0; i < headers.length; i++) {
                   sheet.autoSizeColumn(i);
               }

               workbook.write(out);
               return out.toByteArray();

           } catch (IOException e) {
               throw new RuntimeException("Failed to export Purchases Excel", e);
           }
	}
	
	public byte[] exportPurchaseReceipts(LocalDate startDate, LocalDate endDate, String categoryCode) {
		
		List<Purchase> purchases = purchaseRepository.getPurchaseReceipts(
                startDate, endDate, categoryCode, "R"
        );
		
        List<PurchaseResponseDTO> dtoList = purchases.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

               Sheet sheet = workbook.createSheet("Purchases");
               Map<String, CellStyle> styles = excelService.createStyles(workbook);

               int rowIdx = 0;

               // =====================
               // TITLE
               // =====================
               rowIdx = excelService.createTitleRow(
                       workbook,
                       sheet,
                       rowIdx,
                       "Purchase Receipts",
                       0,
                       10
               );

               rowIdx++;

               // =====================
               // METADATA
               // =====================
               rowIdx = excelService.createLabelValueRow(
                       sheet,
                       rowIdx,
                       "Bill Date:",
                       startDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                               + " to "
                               + endDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                       styles.get("bold")
               );

               String categoryName = (categoryCode != null && !categoryCode.isEmpty())
                       ? categoryRepository.findByCode(categoryCode).get().getName()
                       : "All";

               rowIdx = excelService.createLabelValueRow(
                       sheet,
                       rowIdx,
                       "Category:",
                       categoryName,
                       styles.get("bold")
               );
               

               rowIdx++;

               // =====================
               // TABLE HEADER
               // =====================
               String[] headers = {
                       "Sl No.",
                       "Bill No. & Date",
                       "Firm",
                       "Category",
                       "Particulars",
                       "",
                       "Quantity",
                       "Rate (₹)",
                       "Amount (₹)",
                       "Total",
                       "Remarks"
               };

               rowIdx = excelService.createTableHeaderRow(
                       sheet,
                       rowIdx,
                       headers,
                       styles.get("headerBorder")
               );
               
               int headerRowIndex = rowIdx - 1;
               
            // merge columns 2 and 3
      	     sheet.addMergedRegion(new CellRangeAddress(headerRowIndex, headerRowIndex, 4, 5));

      	     // ensure border style applies to merged cells
      	     Row headerRow1 = sheet.getRow(headerRowIndex);
      	     for (int col = 4; col <= 5; col++) {
      	         headerRow1.getCell(col).setCellStyle(styles.get("headerBorder"));
      	     }
               // =====================
               // TABLE DATA
               // =====================
               int slNo = 1;

               for (PurchaseResponseDTO purchase : dtoList) {

                   int purchaseStartRow = rowIdx;

                   for (ItemPurchaseDTO item : purchase.getItems()) {

                       // =====================
                       // ITEM WITHOUT SUB-ITEMS
                       // =====================
                       if (item.getSubItems() == null || item.getSubItems().isEmpty()) {
                    	   
                    	   int itemStartRow = rowIdx;

                           Row row = sheet.createRow(rowIdx++);

                           row.createCell(0).setCellValue(slNo);
                           row.createCell(1).setCellValue(purchase.getBillNo()+" Dtd. "+purchase.getBillDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                           row.createCell(2).setCellValue(purchase.getFirmName());
                           row.createCell(3).setCellValue(item.getCategory());
                           row.createCell(4).setCellValue(item.getItemName());
                           row.createCell(6).setCellValue(item.getQuantity());
                           row.createCell(7).setCellValue(item.getRate()+" "+item.getUnit());
                           row.createCell(8).setCellValue(item.getAmount());
                           row.createCell(9).setCellValue(purchase.getTotalCost());
                           row.createCell(10).setCellValue(purchase.getRemarks());

                        // styles + wrap
                           for (int col = 0; col <= 10; col++) {
                               Cell cell = row.getCell(col);
                               if (cell == null) cell = row.createCell(col);

                               cell.setCellStyle(
                            		   (col == 2 || col == 4 || col == 9 || col == 10)
                                               ? styles.get("wrapBorder")
                                               : styles.get("border")
                               );
                           }

                           row.setHeight((short) -1);
                           
                           int itemEndRow = rowIdx - 1;

          	             // merge Item + SubItem horizontally
          	             sheet.addMergedRegion(
          	                     new CellRangeAddress(itemStartRow, itemEndRow, 4, 5)
          	             );
          	             excelService.applyBorder(
          	                     new CellRangeAddress(itemStartRow, itemEndRow, 4, 5),
          	                     sheet
          	             );
                       }

                       // =====================
                       // ITEM WITH SUB-ITEMS
                       // =====================
                       else {
                    	   int itemStartRow = rowIdx;
                    	   
                           for (SubItemPurchaseDTO sub : item.getSubItems()) {

                               Row row = sheet.createRow(rowIdx++);

                               row.createCell(0).setCellValue(slNo);
                               row.createCell(1).setCellValue(purchase.getBillNo()+" Dtd. "+purchase.getBillDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                               row.createCell(2).setCellValue(purchase.getFirmName());
                               row.createCell(3).setCellValue(item.getCategory());
                               row.createCell(4).setCellValue(
                                       item.getItemName()
                               );
                               row.createCell(5).setCellValue(sub.getSubItemName());
                               row.createCell(6).setCellValue(sub.getQuantity());
                               row.createCell(7).setCellValue(sub.getRate()+" "+sub.getUnit());
                               row.createCell(8).setCellValue(sub.getAmount());
                               row.createCell(9).setCellValue(purchase.getTotalCost());
                               row.createCell(10).setCellValue(purchase.getRemarks());

                               for (int col = 0; col <= 10; col++) {
                                   Cell cell = row.getCell(col);
                                   if (cell == null) cell = row.createCell(col);

                                   cell.setCellStyle(
                                           (col == 2 || col == 4 || col == 5 || col == 9 || col == 10)
                                                   ? styles.get("wrapBorder")
                                                   : styles.get("border")
                                   );
                               }

                               row.setHeight((short) -1);
                           }
                           int itemEndRow = rowIdx - 1;

                           // merge Item name vertically
                           excelService.mergeVertically(sheet, itemStartRow, itemEndRow, 4);
                           excelService.mergeVertically(sheet, itemStartRow, itemEndRow, 3);
                       }
                   }

                   int purchaseEndRow = rowIdx - 1;

                   // =====================
                   // MERGES PER PURCHASE
                   // =====================
                   excelService.mergeVertically(sheet, purchaseStartRow, purchaseEndRow, 0); // Sl No
                   excelService.mergeVertically(sheet, purchaseStartRow, purchaseEndRow, 1); // Date
                   excelService.mergeVertically(sheet, purchaseStartRow, purchaseEndRow, 2); // Firm
                   excelService.mergeVertically(sheet, purchaseStartRow, purchaseEndRow, 9); // Total
                   excelService.mergeVertically(sheet, purchaseStartRow, purchaseEndRow, 10); // Remarks

                   slNo++;
               }

               // =====================
               // AUTO SIZE
               // =====================
               for (int i = 0; i < headers.length; i++) {
                   sheet.autoSizeColumn(i);
               }

               workbook.write(out);
               return out.toByteArray();

           } catch (IOException e) {
               throw new RuntimeException("Failed to export Purchases Excel", e);
           }
		
	}

	public byte[] exportPurchaseOrdersNS(LocalDate startDate, LocalDate endDate, String categoryCode, String status) {
	
	List<PurchaseNonStock> purchases = purchaseNonStockRepository.getPurchasesNonStock(
            startDate, endDate, categoryCode, status
    );
	
    List<PurchaseResponseDTO> dtoList = purchases.stream()
            .map(this::convertToDTONonStock)
            .collect(Collectors.toList());
    
    try (Workbook workbook = new XSSFWorkbook();
            ByteArrayOutputStream out = new ByteArrayOutputStream()) {

           Sheet sheet = workbook.createSheet("Purchases");
           Map<String, CellStyle> styles = excelService.createStyles(workbook);

           int rowIdx = 0;

           // =====================
           // TITLE
           // =====================
           rowIdx = excelService.createTitleRow(
                   workbook,
                   sheet,
                   rowIdx,
                   "Purchases Orders (Non-Stock)",
                   0,
                   7
           );

           rowIdx++;

           // =====================
           // METADATA
           // =====================
           rowIdx = excelService.createLabelValueRow(
                   sheet,
                   rowIdx,
                   "File Date:",
                   startDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                           + " to "
                           + endDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                   styles.get("bold")
           );

           String categoryName = (categoryCode != null && !categoryCode.isEmpty())
                   ? categoryRepository.findByCode(categoryCode).get().getName()
                   : "All";

           rowIdx = excelService.createLabelValueRow(
                   sheet,
                   rowIdx,
                   "Category:",
                   categoryName,
                   styles.get("bold")
           );
           
           rowIdx = excelService.createLabelValueRow(
                   sheet,
                   rowIdx,
                   "Status:",
                   status.equals("P")?"Pending":(status.equals("R")?"Received":"All"),
                   styles.get("bold")
           );

           rowIdx++;

           // =====================
           // TABLE HEADER
           // =====================
           String[] headers = {
                   "Sl No.",
                   "Date of Purchase",
                   "Firm",
                   "Category",
                   "Particulars",
                   "",
                   "Quantity",
                   "Status"
           };

           rowIdx = excelService.createTableHeaderRow(
                   sheet,
                   rowIdx,
                   headers,
                   styles.get("headerBorder")
           );
           
           int headerRowIndex = rowIdx - 1;
           
        // merge columns 2 and 3
  	     sheet.addMergedRegion(new CellRangeAddress(headerRowIndex, headerRowIndex, 4, 5));

  	     // ensure border style applies to merged cells
  	     Row headerRow1 = sheet.getRow(headerRowIndex);
  	     for (int col = 4; col <= 5; col++) {
  	         headerRow1.getCell(col).setCellStyle(styles.get("headerBorder"));
  	     }
           // =====================
           // TABLE DATA
           // =====================
           int slNo = 1;

           for (PurchaseResponseDTO purchase : dtoList) {

               int purchaseStartRow = rowIdx;

               for (ItemPurchaseDTO item : purchase.getItems()) {

                   // =====================
                   // ITEM WITHOUT SUB-ITEMS
                   // =====================
                   if (item.getSubItems() == null || item.getSubItems().isEmpty()) {
                	   
                	   int itemStartRow = rowIdx;

                       Row row = sheet.createRow(rowIdx++);

                       row.createCell(0).setCellValue(slNo);
                       row.createCell(1).setCellValue(purchase.getDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                       row.createCell(2).setCellValue(purchase.getFirmName());
                       row.createCell(3).setCellValue(item.getCategory());
                       row.createCell(4).setCellValue(item.getItemName());
                       row.createCell(6).setCellValue(item.getQuantity());
                       row.createCell(7).setCellValue(purchase.getBillNo()==null?"Pending":"Received");
                     

                    // styles + wrap
                       for (int col = 0; col <= 7; col++) {
                           Cell cell = row.getCell(col);
                           if (cell == null) cell = row.createCell(col);

                           cell.setCellStyle(
                        		   (col == 2 || col == 4)
                                           ? styles.get("wrapBorder")
                                           : styles.get("border")
                           );
                       }

                       row.setHeight((short) -1);
                       
                       int itemEndRow = rowIdx - 1;

      	             // merge Item + SubItem horizontally
      	             sheet.addMergedRegion(
      	                     new CellRangeAddress(itemStartRow, itemEndRow, 4, 5)
      	             );
      	             excelService.applyBorder(
      	                     new CellRangeAddress(itemStartRow, itemEndRow, 4, 5),
      	                     sheet
      	             );
                   }

                   // =====================
                   // ITEM WITH SUB-ITEMS
                   // =====================
                   else {
                	   int itemStartRow = rowIdx;
                	   
                       for (SubItemPurchaseDTO sub : item.getSubItems()) {

                           Row row = sheet.createRow(rowIdx++);

                           row.createCell(0).setCellValue(slNo);
                           row.createCell(1).setCellValue(purchase.getDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                           row.createCell(2).setCellValue(purchase.getFirmName());
                           row.createCell(3).setCellValue(item.getCategory());
                           row.createCell(4).setCellValue(
                                   item.getItemName()
                           );
                           row.createCell(5).setCellValue(sub.getSubItemName());
                           row.createCell(6).setCellValue(sub.getQuantity());
                           row.createCell(7).setCellValue(purchase.getBillNo()==null?"Pending":"Received");

                           for (int col = 0; col <= 7; col++) {
                               Cell cell = row.getCell(col);
                               if (cell == null) cell = row.createCell(col);

                               cell.setCellStyle(
                                       (col == 2 || col == 4 || col == 5)
                                               ? styles.get("wrapBorder")
                                               : styles.get("border")
                               );
                           }

                           row.setHeight((short) -1);
                       }
                       int itemEndRow = rowIdx - 1;

                       // merge Item name vertically
                       excelService.mergeVertically(sheet, itemStartRow, itemEndRow, 4);
                       excelService.mergeVertically(sheet, itemStartRow, itemEndRow, 3);
                   }
               }

               int purchaseEndRow = rowIdx - 1;

               // =====================
               // MERGES PER PURCHASE
               // =====================
               excelService.mergeVertically(sheet, purchaseStartRow, purchaseEndRow, 0); // Sl No
               excelService.mergeVertically(sheet, purchaseStartRow, purchaseEndRow, 1); // Date
               excelService.mergeVertically(sheet, purchaseStartRow, purchaseEndRow, 2); // Firm

               slNo++;
           }

           // =====================
           // AUTO SIZE
           // =====================
           for (int i = 0; i < headers.length; i++) {
               sheet.autoSizeColumn(i);
           }

           workbook.write(out);
           return out.toByteArray();

       } catch (IOException e) {
           throw new RuntimeException("Failed to export Purchases Excel", e);
       }
	
	}

	public byte[] exportPurchaseReceiptsNS(LocalDate startDate, LocalDate endDate, String categoryCode) {
	
	List<PurchaseNonStock> purchases = purchaseNonStockRepository.getPurchaseReceiptsNonStock(
            startDate, endDate, categoryCode, "R"
    );
	
    List<PurchaseResponseDTO> dtoList = purchases.stream()
            .map(this::convertToDTONonStock)
            .collect(Collectors.toList());
    
    try (Workbook workbook = new XSSFWorkbook();
            ByteArrayOutputStream out = new ByteArrayOutputStream()) {

           Sheet sheet = workbook.createSheet("Purchase Receipts (Non-Stock)");
           Map<String, CellStyle> styles = excelService.createStyles(workbook);

           int rowIdx = 0;

           // =====================
           // TITLE
           // =====================
           rowIdx = excelService.createTitleRow(
                   workbook,
                   sheet,
                   rowIdx,
                   "Purchases Receipts (Non-Stock)",
                   0,
                   10
           );

           rowIdx++;

           // =====================
           // METADATA
           // =====================
           rowIdx = excelService.createLabelValueRow(
                   sheet,
                   rowIdx,
                   "Bill Date:",
                   startDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                           + " to "
                           + endDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                   styles.get("bold")
           );

           String categoryName = (categoryCode != null && !categoryCode.isEmpty())
                   ? categoryRepository.findByCode(categoryCode).get().getName()
                   : "All";

           rowIdx = excelService.createLabelValueRow(
                   sheet,
                   rowIdx,
                   "Category:",
                   categoryName,
                   styles.get("bold")
           );

           rowIdx++;

           // =====================
           // TABLE HEADER
           // =====================
           String[] headers = {
                   "Sl No.",
                   "Bill no. & Date",
                   "Firm",
                   "Issue to",
                   "Category",
                   "Particulars",
                   "Quantity",
                   "Rate (₹)",
                   "Amount (₹)",
                   "Total",
                   "Remarks"
           };

           rowIdx = excelService.createTableHeaderRow(
                   sheet,
                   rowIdx,
                   headers,
                   styles.get("headerBorder")
           );
           
           int headerRowIndex = rowIdx - 1;
           
        // merge columns 2 and 3
  	     //sheet.addMergedRegion(new CellRangeAddress(headerRowIndex, headerRowIndex, 4, 5));

  	     // ensure border style applies to merged cells
  	     Row headerRow1 = sheet.getRow(headerRowIndex);
  	     for (int col = 4; col <= 5; col++) {
  	         headerRow1.getCell(col).setCellStyle(styles.get("headerBorder"));
  	     }
           // =====================
           // TABLE DATA
           // =====================
           int slNo = 1;

           for (PurchaseResponseDTO purchase : dtoList) {

               int purchaseStartRow = rowIdx;

               for (ItemPurchaseDTO item : purchase.getItems()) {

                   // =====================
                   // ITEM WITHOUT SUB-ITEMS
                   // =====================
                   if (item.getSubItems() == null || item.getSubItems().isEmpty()) {
                	   
                	   int itemStartRow = rowIdx;

                       Row row = sheet.createRow(rowIdx++);

                       row.createCell(0).setCellValue(slNo);
                       row.createCell(1).setCellValue(purchase.getBillNo()+" Dtd. "+purchase.getBillDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                       row.createCell(2).setCellValue(purchase.getFirmName());
                       row.createCell(3).setCellValue(purchase.getIssuedTo());
                       row.createCell(4).setCellValue(item.getCategory());
                       row.createCell(5).setCellValue(item.getItemName());
                       row.createCell(6).setCellValue(item.getQuantity());
                       row.createCell(7).setCellValue(item.getRate()+" "+item.getUnit());
                       row.createCell(8).setCellValue(item.getAmount());
                       row.createCell(9).setCellValue(purchase.getTotalCost());
                       row.createCell(10).setCellValue(purchase.getRemarks());

                    // styles + wrap
                       for (int col = 0; col <= 10; col++) {
                           Cell cell = row.getCell(col);
                           if (cell == null) cell = row.createCell(col);

                           cell.setCellStyle(
                        		   (col == 2 || col == 4 || col == 9 || col == 10)
                                           ? styles.get("wrapBorder")
                                           : styles.get("border")
                           );
                       }

                       row.setHeight((short) -1);
                       
                       int itemEndRow = rowIdx - 1;

      	             // merge Item + SubItem horizontally
//      	             sheet.addMergedRegion(
//      	                     new CellRangeAddress(itemStartRow, itemEndRow, 4, 5)
//      	             );
//      	             excelService.applyBorder(
//      	                     new CellRangeAddress(itemStartRow, itemEndRow, 4, 5),
//      	                     sheet
//      	             );
                   }

                   // =====================
                   // ITEM WITH SUB-ITEMS
                   // =====================
                   else {
                	   int itemStartRow = rowIdx;
                	   
                       for (SubItemPurchaseDTO sub : item.getSubItems()) {

                           Row row = sheet.createRow(rowIdx++);

                           row.createCell(0).setCellValue(slNo);
                           row.createCell(1).setCellValue(purchase.getBillNo()+" Dtd. "+purchase.getBillDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                           row.createCell(2).setCellValue(purchase.getFirmName());
                           row.createCell(3).setCellValue(purchase.getIssuedTo());
                           row.createCell(4).setCellValue(item.getCategory());
                           row.createCell(5).setCellValue(
                                   item.getItemName()
                           );
                           row.createCell(6).setCellValue(sub.getQuantity());
                           row.createCell(7).setCellValue(sub.getRate()+" "+sub.getUnit());
                           row.createCell(8).setCellValue(sub.getAmount());
                           row.createCell(9).setCellValue(purchase.getTotalCost());
                           row.createCell(10).setCellValue(purchase.getRemarks());

                           for (int col = 0; col <= 10; col++) {
                               Cell cell = row.getCell(col);
                               if (cell == null) cell = row.createCell(col);

                               cell.setCellStyle(
                                       (col == 2 || col == 4 || col == 5 || col == 9 || col == 10)
                                               ? styles.get("wrapBorder")
                                               : styles.get("border")
                               );
                           }

                           row.setHeight((short) -1);
                       }
                       int itemEndRow = rowIdx - 1;

                       // merge Item name vertically
                       excelService.mergeVertically(sheet, itemStartRow, itemEndRow, 4);
                       excelService.mergeVertically(sheet, itemStartRow, itemEndRow, 3);
                   }
               }

               int purchaseEndRow = rowIdx - 1;

               // =====================
               // MERGES PER PURCHASE
               // =====================
               excelService.mergeVertically(sheet, purchaseStartRow, purchaseEndRow, 0); // Sl No
               excelService.mergeVertically(sheet, purchaseStartRow, purchaseEndRow, 1); // Date
               excelService.mergeVertically(sheet, purchaseStartRow, purchaseEndRow, 2); // Firm
               excelService.mergeVertically(sheet, purchaseStartRow, purchaseEndRow, 9); // Total
               excelService.mergeVertically(sheet, purchaseStartRow, purchaseEndRow, 10); // Remarks

               slNo++;
           }

           // =====================
           // AUTO SIZE
           // =====================
           for (int i = 0; i < headers.length; i++) {
               sheet.autoSizeColumn(i);
           }

           workbook.write(out);
           return out.toByteArray();

       } catch (IOException e) {
           throw new RuntimeException("Failed to export Purchases Excel", e);
       }
	
	}

	public Page<PurchaseResponseDTO> searchNonStockPurchases(
            LocalDate startDate,
            LocalDate endDate,
            String category,
            String searchValue,
            String status,
            String type,
            Pageable pageable) {

        Page<PurchaseNonStock> page;
        if(type.equals("PO"))
        	page= purchaseNonStockRepository.searchPurchaseNonStock(startDate, endDate, category, searchValue, status, pageable);
        else
        	page = purchaseNonStockRepository.searchPurchaseReceiptsNonStock(startDate, endDate, category, searchValue, status, pageable);

        return page.map(this::convertToDTONonStock);
    }
	
	private PurchaseResponseDTO convertToDTONonStock(PurchaseNonStock p) {

	    PurchaseResponseDTO dto = new PurchaseResponseDTO();

	    dto.setPurchaseId(p.getId());
	    dto.setFirmName(p.getReceivedFrom());
	    dto.setRemarks(p.getRemarks());
	    dto.setFileNo(p.getFileNo());
	    dto.setBillNo(p.getBillNo());
	    dto.setBillDate(p.getBillDate());
	    dto.setDate(p.getFileDate());
	    dto.setTotalCost(p.getTotal());
	    dto.setIssuedTo(p.getIssueTo());
	    
	    List<ItemPurchaseDTO> itemDTOs = p.getItems()
	            .stream()
	            .map(pi -> {

	                ItemPurchaseDTO itemDTO = new ItemPurchaseDTO();

	                itemDTO.setItemName(pi.getItem());
	                itemDTO.setCategoryCode(pi.getCategory());
	                itemDTO.setCategory(categoryRepository.findByCode(pi.getCategory()).orElse(null).getName());
	                itemDTO.setUnit(pi.getUnit());
	                itemDTO.setQuantity(pi.getQuantity());
	                
	                if(pi.getRate()!=null) {
	                	itemDTO.setRate(pi.getRate());

	                	Double amount = pi.getRate() * pi.getQuantity();
	                	itemDTO.setAmount(amount);

	                	itemDTO.setGstPercentage(pi.getGstPercentage());
	                	itemDTO.setCgst(pi.getCgst());
	                	itemDTO.setSgst(pi.getSgst());
	                }

	                return itemDTO;
	            })
	            .collect(Collectors.toList());

	    dto.setItems(itemDTOs);

	    // Calculate total safely from items
	    
	    if(dto.getBillNo()!=null) {
		    Double totalAmount = itemDTOs.stream()
		            .mapToDouble(ItemPurchaseDTO::getAmount)
		            .sum();
	
		    dto.setTotalCost(totalAmount);
	    }

	    return dto;
	}

}
