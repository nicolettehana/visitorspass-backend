package sad.storereg.services.appdata;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import sad.storereg.dto.appdata.LedgerResponse;
import sad.storereg.dto.appdata.LedgerSubItemResponse;
import sad.storereg.dto.appdata.LedgerUnitResponse;
import sad.storereg.models.appdata.StockBalance;
import sad.storereg.models.master.Item;
import sad.storereg.models.master.Rate;
import sad.storereg.models.master.SubItems;
import sad.storereg.models.master.YearRange;
import sad.storereg.repo.appdata.IssueRepository;
import sad.storereg.repo.appdata.PurchaseRepository;
import sad.storereg.repo.appdata.StockBalanceRepository;
import sad.storereg.repo.master.CategoryRepository;
import sad.storereg.repo.master.ItemRepository;
import sad.storereg.repo.master.RateRepository;

@Service
@RequiredArgsConstructor
public class LedgerService {
	
	private final ItemRepository itemRepo;
    private final PurchaseRepository purchaseRepo;
    private final IssueRepository issueRepo;
    private final RateRepository rateRepo;
    private final ExcelServices excelService;
    private final CategoryRepository categoryRepo;

    @Transactional(readOnly = true)
    public Page<LedgerResponse> getLedger(LocalDate startDate,
                                          LocalDate endDate,
                                          String categoryCode,
                                          Pageable pageable) {
    	Integer yearRangeId=1;
    	Page<Item> itemsPage = itemRepo.findAll(pageable);
    	
    	if (categoryCode != null && categoryCode.length()>0) {
            itemsPage = itemRepo.findAllByCategory_Code(categoryCode, pageable);
        } if(categoryCode==null || categoryCode.equals("")){
            itemsPage = itemRepo.findAll(pageable);
        }

        List<LedgerResponse> dtoList = new ArrayList<>(itemsPage.getContent().size());

        for (Item item : itemsPage.getContent()) {
            LedgerResponse ledger = new LedgerResponse();
            ledger.setItemId(item.getId());
            ledger.setItemName(item.getName());
            ledger.setCategory(item.getCategory() != null ? item.getCategory().getName() : null);
            ledger.setCategoryCode(item.getCategory() != null ? item.getCategory().getCode() : null);

            boolean hasSubItems = item.getSubItems() != null && !item.getSubItems().isEmpty();

            if (!hasSubItems) {
                // get all rates (units) for item (subItem = null)
                List<Rate> rates = rateRepo.findRatesByItemAndOptionalSubItem(item.getId(), null, yearRangeId);
                Set<Integer> unitIdsAdded = new HashSet<>();

                for (Rate r : rates) {
                    if (r.getUnit() == null) continue;
                    Integer unitId = r.getUnit().getId();
                    // avoid duplicates if multiple rate rows exist for same unit
                    if (unitIdsAdded.contains(unitId)) continue;
                    unitIdsAdded.add(unitId);

                    LedgerUnitResponse u = computeUnitLedger(item.getId(), null,
                            unitId, r.getUnit().getName(), startDate, endDate);

                    ledger.getUnits().add(u);
                }

                // If there were no rates found, optionally we could still compute units
                // by looking at actual transactions' unit ids. (Edge case)
                if (ledger.getUnits().isEmpty()) {
                    // discover units from transactions for this item (optional step)
                    // Skipped here for brevity; recommended to add if needed.
                }

            } else {
                // for each subitem, gather rates (units) and compute per unit
                for (SubItems si : item.getSubItems()) {
                    LedgerSubItemResponse subDto = new LedgerSubItemResponse();
                    subDto.setSubItemId(si.getId());
                    subDto.setSubItemName(si.getName());

                    List<Rate> rates = rateRepo.findRatesByItemAndOptionalSubItem(item.getId(), si.getId(), yearRangeId);
                    Set<Integer> unitIdsAdded = new HashSet<>();

                    for (Rate r : rates) {
                        if (r.getUnit() == null) continue;
                        Integer unitId = r.getUnit().getId();
                        if (unitIdsAdded.contains(unitId)) continue;
                        unitIdsAdded.add(unitId);

                        LedgerUnitResponse u = computeUnitLedger(item.getId(), si.getId(),
                                unitId, r.getUnit().getName(), startDate, endDate);

                        subDto.getUnits().add(u);
                    }

                    // if no rates found, optional fallback to units seen in transactions

                    ledger.getSubItems().add(subDto);
                }
            }

            dtoList.add(ledger);
        }

        return new PageImpl<>(dtoList, pageable, itemsPage.getTotalElements());
    }

    // Helper: compute opening (before startDate), purchases and issues between start & end
    private LedgerUnitResponse computeUnitLedger(Long itemId, Long subItemId,
                                                 Integer unitId, String unitName,
                                                 LocalDate startDate, LocalDate endDate) {

        // opening = purchases_until(startDate-1) - issues_until(startDate-1)
        LocalDate openingEnd = startDate.minusDays(1);

        Integer purchasesBefore = purchaseRepo.sumQtyByItemSubItemUnitUntil(itemId, subItemId, unitId, openingEnd);
        Integer issuesBefore = issueRepo.sumQtyByItemSubItemUnitUntil(itemId, subItemId, unitId, openingEnd);

        int opening = (purchasesBefore == null ? 0 : purchasesBefore) - (issuesBefore == null ? 0 : issuesBefore);

        // purchases in range
        Integer purchasesInRange = purchaseRepo.sumQtyByItemSubItemUnitBetween(itemId, subItemId, unitId, startDate, endDate);
        Integer issuesInRange = issueRepo.sumQtyByItemSubItemUnitBetween(itemId, subItemId, unitId, startDate, endDate);

        int purchased = purchasesInRange == null ? 0 : purchasesInRange;
        int issued = issuesInRange == null ? 0 : issuesInRange;

        int closing = opening + purchased - issued;

        LedgerUnitResponse u = new LedgerUnitResponse();
        u.setUnitId(unitId);
        u.setUnitName(unitName);
        u.setOpeningBalance(opening);
        u.setNoOfPurchases(purchased);
        u.setNoOfIssues(issued);
        u.setClosingBalance(closing);

        return u;
    }
    
    public byte[] exportLedger(LocalDate startDate, LocalDate endDate, String categoryCode) {

    	List<Item> items = null;
    	
    	if (categoryCode != null && categoryCode.length()>0) {
            items = itemRepo.findAllByCategory_Code(categoryCode);
        } if(categoryCode==null || categoryCode.equals("")){
            items = itemRepo.findAll();
        }

        List<LedgerResponse> dtoList = new ArrayList<>(items.size());

        for (Item item : items) {
            LedgerResponse ledger = new LedgerResponse();
            ledger.setItemId(item.getId());
            ledger.setItemName(item.getName());
            ledger.setCategory(item.getCategory() != null ? item.getCategory().getName() : null);
            ledger.setCategoryCode(item.getCategory() != null ? item.getCategory().getCode() : null);

            boolean hasSubItems = item.getSubItems() != null && !item.getSubItems().isEmpty();

            if (!hasSubItems) {
                // get all rates (units) for item (subItem = null)
                //List<Rate> rates = rateRepo.findRatesByItemAndOptionalSubItem(item.getId(), null, yearRangeId);
            	List<Rate> rates = rateRepo.findRatesByItemAndOptionalSubItemm(item.getId(), null);
                Set<Integer> unitIdsAdded = new HashSet<>();

                for (Rate r : rates) {
                    if (r.getUnit() == null) continue;
                    Integer unitId = r.getUnit().getId();
                    // avoid duplicates if multiple rate rows exist for same unit
                    if (unitIdsAdded.contains(unitId)) continue;
                    unitIdsAdded.add(unitId);

                    LedgerUnitResponse u = computeUnitLedger(item.getId(), null,
                            unitId, r.getUnit().getName(), startDate, endDate);

                    ledger.getUnits().add(u);
                }

                // If there were no rates found, optionally we could still compute units
                // by looking at actual transactions' unit ids. (Edge case)
                if (ledger.getUnits().isEmpty()) {
                    // discover units from transactions for this item (optional step)
                    // Skipped here for brevity; recommended to add if needed.
                }

            } else {
                // for each subitem, gather rates (units) and compute per unit
                for (SubItems si : item.getSubItems()) {
                    LedgerSubItemResponse subDto = new LedgerSubItemResponse();
                    subDto.setSubItemId(si.getId());
                    subDto.setSubItemName(si.getName());

                    List<Rate> rates = rateRepo.findRatesByItemAndOptionalSubItemm(item.getId(), si.getId());
                    Set<Integer> unitIdsAdded = new HashSet<>();

                    for (Rate r : rates) {
                        if (r.getUnit() == null) continue;
                        Integer unitId = r.getUnit().getId();
                        if (unitIdsAdded.contains(unitId)) continue;
                        unitIdsAdded.add(unitId);

                        LedgerUnitResponse u = computeUnitLedger(item.getId(), si.getId(),
                                unitId, r.getUnit().getName(), startDate, endDate);

                        subDto.getUnits().add(u);
                    }

                    // if no rates found, optional fallback to units seen in transactions

                    ledger.getSubItems().add(subDto);
                }
            }

            dtoList.add(ledger);
        }

	    try (Workbook workbook = new XSSFWorkbook();
	         ByteArrayOutputStream out = new ByteArrayOutputStream()) {

	        Sheet sheet = workbook.createSheet("Ledger");
	        Map<String, CellStyle> styles = excelService.createStyles(workbook);


	        int rowIdx = 0;
	        
	        rowIdx = excelService.createTitleRow(
	                workbook,
	                sheet,
	                rowIdx,
	                "Ledger",
	                0,
	                8
	        );

	        // =====================
	        // METADATA
	        // =====================
	        rowIdx++;
	        
	        rowIdx = excelService.createLabelValueRow(
	                sheet,
	                rowIdx,
	                "Date:",
	                startDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))+" to "+endDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
	                styles.get("bold")
	        );
	        
	        String categoryName = (categoryCode!=null && categoryCode.length()>0)? categoryRepo.findByCode(categoryCode).get().getName():"All";
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

	        String[] headers = { "Sl No.", "Category", "Particulars","", "Unit", "Opening Balance as on "+startDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")), "No. of Purchases", "No. of Issues", "Closing Blanace as on "+endDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) };

	        rowIdx = excelService.createTableHeaderRow(
	                sheet,
	                rowIdx,
	                headers,
	                styles.get("headerBorder")
	        );
	        int headerRowIndex = rowIdx - 1;

	     // merge columns 2 and 3
	     sheet.addMergedRegion(new CellRangeAddress(headerRowIndex, headerRowIndex, 2, 3));

	     // ensure border style applies to merged cells
	     Row headerRow1 = sheet.getRow(headerRowIndex);
	     for (int col = 2; col <= 3; col++) {
	         headerRow1.getCell(col).setCellStyle(styles.get("headerBorder"));
	     }

	        // =====================
	        // TABLE DATA
	        // =====================
	     int slNo = 1;

	     for (LedgerResponse ledger : dtoList) {

	         // =====================
	         // ITEM WITHOUT SUB-ITEMS
	         // =====================
	         if (ledger.getSubItems() == null || ledger.getSubItems().isEmpty()) {

	             int itemStartRow = rowIdx;

	             for (LedgerUnitResponse unit : ledger.getUnits()) {

	                 Row row = sheet.createRow(rowIdx++);

	                 row.createCell(0).setCellValue(slNo);
	                 row.createCell(1).setCellValue(ledger.getCategory());
	                 row.createCell(2).setCellValue(ledger.getItemName());
	                 row.createCell(3); // empty for merge
	                 row.createCell(4).setCellValue(unit.getUnitName());
	                 row.createCell(5).setCellValue(unit.getOpeningBalance());
	                 row.createCell(6).setCellValue(unit.getNoOfPurchases());
	                 row.createCell(7).setCellValue(unit.getNoOfIssues());
	                 row.createCell(8).setCellValue(unit.getClosingBalance());

	                 // styles
	                 for (int col = 0; col <= 8; col++) {
	                     Cell cell = row.getCell(col);
	                     if (cell == null) cell = row.createCell(col);

	                     cell.setCellStyle(
	                             (col == 2 || col == 3)
	                                     ? styles.get("wrapBorder")
	                                     : styles.get("border")
	                     );
	                 }
	             }

	             int itemEndRow = rowIdx - 1;

	             if (itemEndRow >= itemStartRow) {
	                 CellRangeAddress range =
	                     new CellRangeAddress(itemStartRow, itemEndRow, 2, 3);

	                 sheet.addMergedRegion(range);
	                 excelService.applyBorder(range, sheet);
	             }


	             // merge Item + SubItem horizontally
//	             sheet.addMergedRegion(
//	                     new CellRangeAddress(itemStartRow, itemEndRow, 2, 3)
//	             );
//	             excelService.applyBorder(
//	                     new CellRangeAddress(itemStartRow, itemEndRow, 2, 3),
//	                     sheet
//	             );

	             // vertical merges
	             excelService.mergeVertically(sheet, itemStartRow, itemEndRow, 0); // Sl No
	             excelService.mergeVertically(sheet, itemStartRow, itemEndRow, 1); // Category
	             excelService.mergeVertically(sheet, itemStartRow, itemEndRow, 2); // Item

	             slNo++;
	             continue;
	         }

	         // =====================
	         // ITEM WITH SUB-ITEMS
	         // =====================
	         int itemStartRow = rowIdx;
	         int slNoForItem = slNo++;

	         for (LedgerSubItemResponse sub : ledger.getSubItems()) {

	             for (LedgerUnitResponse unit : sub.getUnits()) {

	                 Row row = sheet.createRow(rowIdx++);

	                 row.createCell(0).setCellValue(slNoForItem);
	                 row.createCell(1).setCellValue(ledger.getCategory());
	                 row.createCell(2).setCellValue(ledger.getItemName());
	                 row.createCell(3).setCellValue(sub.getSubItemName());
	                 row.createCell(4).setCellValue(unit.getUnitName());
	                 row.createCell(5).setCellValue(unit.getOpeningBalance());
	                 row.createCell(6).setCellValue(unit.getNoOfPurchases());
	                 row.createCell(7).setCellValue(unit.getNoOfIssues());
	                 row.createCell(8).setCellValue(unit.getClosingBalance());

	                 for (int col = 0; col <= 8; col++) {
	                     Cell cell = row.getCell(col);
	                     if (cell == null) cell = row.createCell(col);

	                     cell.setCellStyle(
	                             (col == 2 || col == 3)
	                                     ? styles.get("wrapBorder")
	                                     : styles.get("border")
	                     );
	                 }
	             }
	         }

	         int itemEndRow = rowIdx - 1;

	         // vertical merges
	         excelService.mergeVertically(sheet, itemStartRow, itemEndRow, 0); // Sl No
	         excelService.mergeVertically(sheet, itemStartRow, itemEndRow, 1); // Category
	         excelService.mergeVertically(sheet, itemStartRow, itemEndRow, 2); // Item
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
	        throw new RuntimeException("Failed to export Excel", e);
	    }
	}
}
