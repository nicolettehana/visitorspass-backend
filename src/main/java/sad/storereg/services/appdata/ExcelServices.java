package sad.storereg.services.appdata;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;

import sad.storereg.models.master.Item;
import sad.storereg.models.master.SubItems;

import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;

@Service
public class ExcelServices {
	
	public Map<String, CellStyle> createStyles(Workbook workbook) {

	    Map<String, CellStyle> styles = new HashMap<>();

	    // ===== TITLE =====
	    Font titleFont = workbook.createFont();
	    titleFont.setBold(true);
	    titleFont.setFontHeightInPoints((short) 16);

	    CellStyle title = workbook.createCellStyle();
	    title.setFont(titleFont);
	    title.setAlignment(HorizontalAlignment.LEFT);
	    styles.put("title", title);

	    // ===== BOLD =====
	    Font boldFont = workbook.createFont();
	    boldFont.setBold(true);

	    CellStyle bold = workbook.createCellStyle();
	    bold.setFont(boldFont);
	    styles.put("bold", bold);

	    // ===== HEADER =====
	    Font headerFont = workbook.createFont();
	    headerFont.setBold(true);

	    CellStyle header = workbook.createCellStyle();
	    header.setFont(headerFont);
	    styles.put("header", header);

	    // ===== BORDER =====
	    CellStyle border = workbook.createCellStyle();
	    border.setBorderTop(BorderStyle.THIN);
	    border.setBorderBottom(BorderStyle.THIN);
	    border.setBorderLeft(BorderStyle.THIN);
	    border.setBorderRight(BorderStyle.THIN);
	    border.setVerticalAlignment(VerticalAlignment.CENTER);
	    styles.put("border", border);

	    // ===== HEADER + BORDER =====
	    CellStyle headerBorder = workbook.createCellStyle();
	    headerBorder.cloneStyleFrom(border);
	    headerBorder.setFont(headerFont);
	    styles.put("headerBorder", headerBorder);

	    // ===== WRAP + BORDER =====
	    CellStyle wrapBorder = workbook.createCellStyle();
	    wrapBorder.cloneStyleFrom(border);
	    wrapBorder.setWrapText(true);
	    wrapBorder.setVerticalAlignment(VerticalAlignment.TOP);
	    styles.put("wrapBorder", wrapBorder);

	    return styles;
	}

	
	public void createCell(Row row, int col, Object value, Map<String, CellStyle> styles) {
	    Cell cell = row.createCell(col);

	    if (value instanceof Number) {
	        cell.setCellValue(((Number) value).doubleValue());
	    } else {
	        cell.setCellValue(value != null ? value.toString() : "");
	    }

	    cell.setCellStyle(styles.get("wrapBorder"));
	}
	
	public void applyBorder(CellRangeAddress region, Sheet sheet) {
	    RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
	    RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
	    RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
	    RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
	}
	
	public void applyBorder(Row row, Map<String, CellStyle> styles, int lastCol) {
	    for (int col = 0; col <= lastCol; col++) {
	        Cell cell = row.getCell(col);
	        if (cell == null) cell = row.createCell(col);
	        cell.setCellStyle(styles.get("border"));
	    }
	}

	
	public static Map<String, CellStyle> create(Workbook workbook) {
        Map<String, CellStyle> styles = new HashMap<>();

        CellStyle tableBorder = workbook.createCellStyle();
        tableBorder.setBorderTop(BorderStyle.THIN);
        tableBorder.setBorderBottom(BorderStyle.THIN);
        tableBorder.setBorderLeft(BorderStyle.THIN);
        tableBorder.setBorderRight(BorderStyle.THIN);
        styles.put("tableBorder", tableBorder);

        CellStyle headerBorder = workbook.createCellStyle();
        headerBorder.cloneStyleFrom(tableBorder);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerBorder.setFont(headerFont);
        styles.put("headerBorder", headerBorder);

        CellStyle bold = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        bold.setFont(boldFont);
        styles.put("bold", bold);

        return styles;
    }
	
	public void createExcelContentItems(
	        Sheet sheet,
	        List<Item> items,
	        String category,
	        String categoryName,
	        Map<String, CellStyle> styles, Workbook workbook
	) {

	    int rowNum = 0;

	    rowNum = createTitleRow(
                workbook,
                sheet,
                rowNum,
                "Items",
                0,
                4
        );

	    rowNum++;

	    // ===== CATEGORY =====
	    rowNum = createLabelValueRow(
                sheet,
                rowNum,
                "Category:",
                categoryName,
                styles.get("bold")
        );

	    // ===== DATE =====
	    rowNum = createLabelValueRow(
                sheet,
                rowNum,
                "Date:",
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                styles.get("bold")
        );

	    rowNum++;

	    // ===== TABLE HEADER =====
	    String[] headers = {"Sl. No.", "Item", "", "Category", "Balance"};

        rowNum = createTableHeaderRow(
                sheet,
                rowNum,
                headers,
                styles.get("headerBorder")
        );
        int headerRowIndex = rowNum - 1;

       // merge columns 2 and 3
       sheet.addMergedRegion(new CellRangeAddress(headerRowIndex, headerRowIndex, 1, 2));

       // ensure border style applies to merged cells
       Row headerRow1 = sheet.getRow(headerRowIndex);
       for (int col = 1; col <= 2; col++) {
           headerRow1.getCell(col).setCellStyle(styles.get("headerBorder"));
       }
       
	    int slNo = 1;

	    for (Item item : items) {

	        int startRow = rowNum;
	        int rowsCreated = 0;

	        boolean hasSubItems =
	                item.getSubItems() != null && !item.getSubItems().isEmpty();

	        if (hasSubItems) {

	            // ===== One row per sub-item =====
	            for (SubItems subItem : item.getSubItems()) {
	                Row row = sheet.createRow(rowNum++);

	                // Sub Item
	                createCell(
	                        row, 2, subItem.getName(), styles);

	                // Balance
	                createCell(
	                        row, 4, safeBalance(subItem.getBalance()), styles);

	                rowsCreated++;
	            }

	        } else {

	            // ===== Single row =====
	            Row row = sheet.createRow(rowNum++);

	            // Balance
	            createCell(
	                    row, 4, safeBalance(item.getBalance()), styles);

	            rowsCreated = 1;
	        }

	        int endRow = startRow + rowsCreated - 1;
	        Row firstRow = sheet.getRow(startRow);

	        // ===== Sl No =====
	        createCell(firstRow, 0, slNo, styles);

	        // ===== Category =====
	        createCell(
	                firstRow, 3, item.getCategory().getName(), styles);

	        if (hasSubItems) {

	            // ===== Item only in Item column =====
	            createCell(
	                    firstRow, 1, item.getName(), styles);

	            // ===== Vertical merge =====
	            merge(sheet, startRow, endRow, 0); // Sl No
	            merge(sheet, startRow, endRow, 1); // Item
	            merge(sheet, startRow, endRow, 3); // Category

	        } else {

	            // ===== Merge Item + Sub Item horizontally =====
	            createCell(
	                    firstRow, 1, item.getName(), styles);

	            mergeHorizontal(sheet, startRow, 1, 2);
	        }

	        slNo++;
	    }



	    // ===== AUTO SIZE =====
	    for (int i = 0; i < 4; i++) {
	        sheet.autoSizeColumn(i);
	    }
	}

	private void merge(Sheet sheet, int startRow, int endRow, int col) {
		if (startRow >= endRow) {
	        return;
	    }
	    CellRangeAddress region =
	            new CellRangeAddress(startRow, endRow, col, col);
	    sheet.addMergedRegion(region);

	    RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
	    RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
	    RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
	    RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
	}
	
	private void mergeHorizontal(Sheet sheet, int row, int colStart, int colEnd) {

	    CellRangeAddress region =
	            new CellRangeAddress(row, row, colStart, colEnd);

	    sheet.addMergedRegion(region);

	    RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
	    RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
	    RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
	    RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
	}

	private String safeBalance(Object balance) {
	    if (balance == null) {
	        return "0";
	    }
	    if (balance instanceof Number) {
	        return String.valueOf(balance);
	    }
	    String val = balance.toString().trim();
	    return val.isEmpty() ? "0" : val;
	}
	
	public int createTitleRow(
	        Workbook workbook,
	        Sheet sheet,
	        int rowIndex,
	        String titleText,
	        int mergeFromCol,
	        int mergeToCol
	) {
	    Row titleRow = sheet.createRow(rowIndex);
	    Cell titleCell = titleRow.createCell(mergeFromCol);
	    titleCell.setCellValue(titleText);

	    // Style
	    CellStyle titleStyle = workbook.createCellStyle();
	    Font titleFont = workbook.createFont();
	    titleFont.setBold(true);
	    titleFont.setFontHeightInPoints((short) 16);
	    titleStyle.setFont(titleFont);
	    titleStyle.setAlignment(HorizontalAlignment.CENTER);
	    titleCell.setCellStyle(titleStyle);

	    // Merge cells
	    sheet.addMergedRegion(
	            new CellRangeAddress(rowIndex, rowIndex, mergeFromCol, mergeToCol)
	    );

	    return rowIndex + 1; // return next row index
	}

	public int createLabelValueRow(
	        Sheet sheet,
	        int rowIndex,
	        String label,
	        String value,
	        CellStyle labelStyle
	) {
	    Row row = sheet.createRow(rowIndex);

	    Cell labelCell = row.createCell(0);
	    labelCell.setCellValue(label);
	    if (labelStyle != null) {
	        labelCell.setCellStyle(labelStyle);
	    }

	    row.createCell(1).setCellValue(value);

	    return rowIndex + 1;
	}

	public int createTableHeaderRow(
	        Sheet sheet,
	        int rowIndex,
	        String[] headers,
	        CellStyle headerStyle
	) {
	    Row headerRow = sheet.createRow(rowIndex);

	    for (int i = 0; i < headers.length; i++) {
	        Cell cell = headerRow.createCell(i);
	        cell.setCellValue(headers[i]);
	        if (headerStyle != null) {
	            cell.setCellStyle(headerStyle);
	        }
	    }

	    return rowIndex + 1;
	}

	public void mergeVertically(
	        Sheet sheet,
	        int startRow,
	        int endRow,
	        int col
	) {
	    if (startRow < endRow) {
	        sheet.addMergedRegion(
	                new CellRangeAddress(startRow, endRow, col, col)
	        );
	    }
	}


}
