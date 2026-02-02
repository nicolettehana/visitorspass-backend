package sad.storereg.services.appdata;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sad.storereg.models.appdata.Visitor;
import sad.storereg.models.master.Office;
import sad.storereg.repo.appdata.VisitorRepository;
import sad.storereg.repo.master.OfficeRepository;

@Service
@RequiredArgsConstructor
public class ReportServiceExcel {

	private final VisitorRepository visitorRepository;
	private final OfficeRepository officeRepository;
	
	public byte[] generateVisitorReportExcel(LocalDate startDate, LocalDate endDate, Integer officeCode) throws Exception {

	    LocalDateTime startDateTime = startDate.atStartOfDay();
	    LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

	    DateTimeFormatter dateOnly = DateTimeFormatter.ofPattern("dd-MM-yyyy");
	    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");
	    
	    Optional<Office> office = officeRepository.findByOfficeCode(officeCode);
        String building = office.isEmpty()?"":office.get().getOfficeName();

	    List<Visitor> visitors =
	            visitorRepository.findByVisitDateTimeBetweenAndOfficeCodeEquals(startDateTime, endDateTime, officeCode);

	    Workbook workbook = new XSSFWorkbook();
	    Sheet sheet = workbook.createSheet("Visitor Report");

	    int rowNum = 0;

	    // ---------------- Styles ----------------
	    Font titleFont = workbook.createFont();
	    titleFont.setBold(true);
	    titleFont.setFontHeightInPoints((short) 16);

	    CellStyle titleStyle = workbook.createCellStyle();
	    titleStyle.setFont(titleFont);
	    titleStyle.setAlignment(HorizontalAlignment.CENTER);

	    Font boldFont = workbook.createFont();
	    boldFont.setBold(true);

	    CellStyle boldStyle = workbook.createCellStyle();
	    boldStyle.setFont(boldFont);

	    CellStyle headerStyle = workbook.createCellStyle();
	    headerStyle.setFont(boldFont);
	    headerStyle.setAlignment(HorizontalAlignment.CENTER);
	    headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
	    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	    headerStyle.setBorderBottom(BorderStyle.THIN);
	    headerStyle.setBorderTop(BorderStyle.THIN);
	    headerStyle.setBorderLeft(BorderStyle.THIN);
	    headerStyle.setBorderRight(BorderStyle.THIN);

	    CellStyle dataStyle = workbook.createCellStyle();
	    dataStyle.setBorderBottom(BorderStyle.THIN);
	    dataStyle.setBorderTop(BorderStyle.THIN);
	    dataStyle.setBorderLeft(BorderStyle.THIN);
	    dataStyle.setBorderRight(BorderStyle.THIN);

	    // ---------------- Title ----------------
	    Row titleRow = sheet.createRow(rowNum++);
	    Cell titleCell = titleRow.createCell(0);
	    titleCell.setCellValue("Government of Meghalaya \n"+building+" \nVisitors");
	    titleCell.setCellStyle(titleStyle);
	    titleStyle.setWrapText(true); 
	    sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 7));
	    titleRow.setHeightInPoints((5 * sheet.getDefaultRowHeightInPoints())); 

	    rowNum++;

	    // ---------------- Meta Info ----------------
	    Row dateRow = sheet.createRow(rowNum++);
	    dateRow.createCell(0).setCellValue("Date:");
	    dateRow.getCell(0).setCellStyle(boldStyle);
	    dateRow.createCell(1).setCellValue(
	            startDate.format(dateOnly) + " to " + endDate.format(dateOnly));

	    Row countRow = sheet.createRow(rowNum++);
	    countRow.createCell(0).setCellValue("No. of Visitors:");
	    countRow.getCell(0).setCellStyle(boldStyle);
	    countRow.createCell(1).setCellValue(visitors.size());

	    Row genRow = sheet.createRow(rowNum++);
	    genRow.createCell(0).setCellValue("Generated on:");
	    genRow.getCell(0).setCellStyle(boldStyle);
	    genRow.createCell(1).setCellValue(LocalDateTime.now().format(dateTimeFormatter));

	    rowNum++;

	    // ---------------- Table Header ----------------
	    String[] headers = {
	            "S.No", "Visitor Pass No.", "Visitor Name", "Mobile Number",
	            "Purpose", "Purpose Details/Name", "Date & Time of Visit", "Address"
	    };

	    Row headerRow = sheet.createRow(rowNum++);
	    for (int i = 0; i < headers.length; i++) {
	        Cell cell = headerRow.createCell(i);
	        cell.setCellValue(headers[i]);
	        cell.setCellStyle(headerStyle);
	    }

	    // ---------------- Data Rows ----------------
	    int serial = 1;
	    for (Visitor v : visitors) {
	        Row row = sheet.createRow(rowNum++);

	        row.createCell(0).setCellValue(serial++);
	        row.createCell(1).setCellValue(v.getVPassNo());
	        row.createCell(2).setCellValue(v.getName());
	        row.createCell(3).setCellValue(v.getMobileNo());
	        row.createCell(4).setCellValue(v.getPurpose());
	        row.createCell(5).setCellValue(v.getPurposeDetails());
	        row.createCell(6).setCellValue(v.getVisitDateTime().format(dateTimeFormatter));
	        row.createCell(7).setCellValue(v.getAddress()+", "+v.getState());

	        for (int i = 0; i < 8; i++) {
	            row.getCell(i).setCellStyle(dataStyle);
	        }
	    }

	    // ---------------- Auto-size Columns ----------------
	    for (int i = 0; i < 8; i++) {
	        sheet.autoSizeColumn(i);
	    }

	    // ---------------- Export ----------------
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    workbook.write(out);
	    workbook.close();

	    return out.toByteArray();
	}
}
