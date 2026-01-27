package sad.storereg.services.appdata;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sad.storereg.dto.appdata.ItemPurchaseDTO;
import sad.storereg.dto.appdata.PurchaseCreateDTO;
import sad.storereg.dto.appdata.PurchaseResponseDTO;
import sad.storereg.dto.appdata.SubItemPurchaseDTO;
import sad.storereg.models.appdata.Issue;
import sad.storereg.models.appdata.IssueItem;
import sad.storereg.models.appdata.Purchase;
import sad.storereg.repo.appdata.IssueRepository;
import sad.storereg.repo.master.CategoryRepository;
import sad.storereg.repo.master.ItemRepository;
import sad.storereg.repo.master.SubItemRepository;
import sad.storereg.repo.master.UnitRepository;

@Service
@RequiredArgsConstructor
public class IssueService {
	
	private final IssueRepository issueRepository;
	private final ItemRepository itemRepository;
	private final SubItemRepository subItemRepository;
	private final UnitRepository unitRepository;
	private final ExcelServices excelService;
	private final CategoryRepository categoryRepository;
	
	public Page<PurchaseResponseDTO> searchIssues(
            LocalDate startDate,
            LocalDate endDate,
            String category,
            String searchValue,
            Pageable pageable) {

		
      Page<Issue> page = issueRepository.searchIssues(
      startDate, endDate, category, searchValue, pageable);
      
      page.forEach(issue ->
      issue.getItems().removeIf(item ->
              category != null && !category.equals(item.getCategoryCode())
      )
      );

        return page.map(this::convertToDTO);
    }

	private PurchaseResponseDTO convertToDTO(Issue p) {

	    PurchaseResponseDTO dto = new PurchaseResponseDTO();

	    dto.setPurchaseId(p.getId());
	    dto.setIssuedTo(p.getIssueTo());
	    dto.setRemarks(p.getRemarks());
	    dto.setDate(p.getDate());
	    
	    Map<String, List<IssueItem>> itemGroup = p.getItems()
	            .stream()
	            .collect(Collectors.groupingBy(pi -> pi.getItem().getName()));

	    List<ItemPurchaseDTO> itemDTOs = new ArrayList<>();

	    for (var entry : itemGroup.entrySet()) {
	    	
	    	ItemPurchaseDTO itemDTO = new ItemPurchaseDTO();
	        itemDTO.setItemName(entry.getKey());
	        
	        // Set category (all grouped items have the same category)
	        String category = entry.getValue().get(0).getItem().getCategory().getName();
	        itemDTO.setCategory(category);
	        itemDTO.setCategoryCode(entry.getValue().get(0).getItem().getCategory().getCode());

	        List<SubItemPurchaseDTO> subItems = entry.getValue()
	                .stream()
	                .map(pi -> {
	                    if (pi.getSubItem() == null) {
	                        itemDTO.setQuantity(pi.getQuantity());
	                        itemDTO.setUnit(pi.getUnit().getUnit());
	                        return null;
	                    }

	                    SubItemPurchaseDTO sd = new SubItemPurchaseDTO();
	                    sd.setSubItemName(pi.getSubItem().getName());
	                    sd.setQuantity(pi.getQuantity());
	                    sd.setUnit(pi.getUnit().getUnit());
	                    return sd;
	                })
	                .filter(Objects::nonNull)
	                .collect(Collectors.toList());

	        itemDTO.setSubItems(subItems);
	        itemDTOs.add(itemDTO);
	    }

	    dto.setItems(itemDTOs);
	    return dto;
	}
	
	public String saveIssue(PurchaseCreateDTO dto) {
		System.out.println("DTO: "+dto
				);        
		Issue issue = new Issue();
        issue.setDate(dto.getIssueDate());
        issue.setEntrydate(LocalDateTime.now());
        issue.setRemarks(dto.getRemarks());
        issue.setIssueTo(dto.getIssueTo());
     
        // Convert items
        List<IssueItem> items = dto.getItems().stream().map(itemDTO -> {

            IssueItem item = new IssueItem();
            item.setIssue(issue);

            // Item
            item.setItem(itemRepository.findById(itemDTO.getItemId())
                    .orElseThrow(() -> new RuntimeException("Item not found")));

            if (itemDTO.getSubItemId() != null) {
                item.setSubItem(subItemRepository.findById(itemDTO.getSubItemId())
                        .orElseThrow(() -> new RuntimeException("SubItem not found")));
            }

            // Unit
            item.setUnit(unitRepository.findById(itemDTO.getUnitId())
                    .orElseThrow(() -> new RuntimeException("Unit not found")));

            item.setQuantity(itemDTO.getQuantity());
            item.setCategoryCode(itemDTO.getCategoryCode());

            return item;

        }).toList();


        issue.setItems(items);

        // 4. Save (cascade saves items)
         issueRepository.save(issue);
         return "Issue added";
    }
	
	public byte[] exportIssues(LocalDate startDate, LocalDate endDate, String categoryCode) {
		
		List<Issue> issues = issueRepository.getIssues(
                startDate, endDate, categoryCode
        );
		
        List<PurchaseResponseDTO> dtoList = issues.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

               Sheet sheet = workbook.createSheet("Issues");
               Map<String, CellStyle> styles = excelService.createStyles(workbook);

               int rowIdx = 0;

               // =====================
               // TITLE
               // =====================
               rowIdx = excelService.createTitleRow(
                       workbook,
                       sheet,
                       rowIdx,
                       "Issues",
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
                       "Date:",
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
                       "Date of Issue",
                       "Issued to",
                       "Category",
                       "Particulars",
                       "",
                       "Quantity",
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

               for (PurchaseResponseDTO issue : dtoList) {

                   int issueStartRow = rowIdx;

                   for (ItemPurchaseDTO item : issue.getItems()) {

                       // =====================
                       // ITEM WITHOUT SUB-ITEMS
                       // =====================
                       if (item.getSubItems() == null || item.getSubItems().isEmpty()) {
                    	   
                    	   int itemStartRow = rowIdx;

                           Row row = sheet.createRow(rowIdx++);

                           row.createCell(0).setCellValue(slNo);
                           row.createCell(1).setCellValue(issue.getDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                           row.createCell(2).setCellValue(issue.getIssuedTo());
                           row.createCell(3).setCellValue(item.getCategory());
                           row.createCell(4).setCellValue(item.getItemName());
                           row.createCell(6).setCellValue(item.getQuantity());
                           row.createCell(7).setCellValue(issue.getRemarks());

                        // styles + wrap
                           for (int col = 0; col <= 7; col++) {
                               Cell cell = row.getCell(col);
                               if (cell == null) cell = row.createCell(col);

                               cell.setCellStyle(
                            		   (col == 2 || col == 4 || col == 7)
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
                               row.createCell(1).setCellValue(issue.getDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                               row.createCell(2).setCellValue(issue.getIssuedTo());
                               row.createCell(3).setCellValue(item.getCategory());
                               row.createCell(4).setCellValue(
                                       item.getItemName()
                               );
                               row.createCell(5).setCellValue(sub.getSubItemName());
                               row.createCell(6).setCellValue(sub.getQuantity());
                               row.createCell(7).setCellValue(issue.getRemarks());

                               for (int col = 0; col <= 7; col++) {
                                   Cell cell = row.getCell(col);
                                   if (cell == null) cell = row.createCell(col);

                                   cell.setCellStyle(
                                           (col == 2 || col == 4 || col == 5 || col == 7)
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
                   excelService.mergeVertically(sheet, issueStartRow, purchaseEndRow, 0); // Sl No
                   excelService.mergeVertically(sheet, issueStartRow, purchaseEndRow, 1); // Date
                   excelService.mergeVertically(sheet, issueStartRow, purchaseEndRow, 2); // Firm
                   excelService.mergeVertically(sheet, issueStartRow, purchaseEndRow, 7); // Remarks

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


}
