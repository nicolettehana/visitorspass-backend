package sad.storereg.services.appdata;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sad.storereg.dto.appdata.PhotoData;
import sad.storereg.models.appdata.Visitor;
import sad.storereg.models.master.Office;
import sad.storereg.repo.appdata.VisitorRepository;
import sad.storereg.repo.master.OfficeRepository;

@Service
@RequiredArgsConstructor
public class ReportServiceExcel {

	private final VisitorRepository visitorRepository;
	private final OfficeRepository officeRepository;
	private final VisitorPhotoService visitorPhotoService;
	
	public byte[] generateVisitorReportExcel(LocalDate startDate, LocalDate endDate, Integer officeCode, Integer withPhoto) throws Exception {

		boolean includePhoto = Integer.valueOf(1).equals(withPhoto);
		
	    LocalDateTime startDateTime = startDate.atStartOfDay();
	    LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

	    DateTimeFormatter dateOnly = DateTimeFormatter.ofPattern("dd-MM-yyyy");
	    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");
	    
	    Optional<Office> office = officeRepository.findByOfficeCode(officeCode);
        String building = office.isEmpty()?"":office.get().getOfficeName();

        List<Visitor> visitors;
        if(officeCode==null || !officeCode.equals(""))
        	visitors = visitorRepository.findByVisitDateTimeBetween(startDateTime, endDateTime);
        else
        	visitors = visitorRepository.findByVisitDateTimeBetweenAndOfficeCodeEquals(startDateTime, endDateTime, officeCode);


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
	    //sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 7));
	    int totalColumns = includePhoto ? 9 : 8;
	    sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, totalColumns - 1));
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
//	    String[] headers = {
//	            "S.No", "Visitor Pass No.", "Visitor Name", "Mobile Number",
//	            "Purpose", "Purpose Details/Name", "Date & Time of Visit", "Address"
//	    };
//
//	    Row headerRow = sheet.createRow(rowNum++);
//	    for (int i = 0; i < headers.length; i++) {
//	        Cell cell = headerRow.createCell(i);
//	        cell.setCellValue(headers[i]);
//	        cell.setCellStyle(headerStyle);
//	    }
	    
	    List<String> headerList = new ArrayList<>();
	    headerList.add("S.No");

	    if (includePhoto) {
	        headerList.add("Photo");
	    }

	    headerList.addAll(List.of(
	            "Visitor Pass No.",
	            "Visitor Name",
	            "Mobile Number",
	            "Purpose",
	            "Purpose Details/Name",
	            "Date & Time of Visit",
	            "Address"
	    ));

	    Row headerRow = sheet.createRow(rowNum++);
	    for (int i = 0; i < headerList.size(); i++) {
	        Cell cell = headerRow.createCell(i);
	        cell.setCellValue(headerList.get(i));
	        cell.setCellStyle(headerStyle);
	    }

	    // ---------------- Data Rows ----------------
	    Drawing<?> drawing = sheet.createDrawingPatriarch();
//	    int serial = 1;
//	    for (Visitor v : visitors) {
//	        Row row = sheet.createRow(rowNum++);
//
//	        row.createCell(0).setCellValue(serial++);
//	        row.createCell(1).setCellValue(v.getVPassNo());
//	        row.createCell(2).setCellValue(v.getName());
//	        row.createCell(3).setCellValue(v.getMobileNo());
//	        row.createCell(4).setCellValue(v.getPurpose());
//	        row.createCell(5).setCellValue(v.getPurposeDetails());
//	        row.createCell(6).setCellValue(v.getVisitDateTime().format(dateTimeFormatter));
//	        row.createCell(7).setCellValue(v.getAddress()+", "+v.getState());
//
//	        for (int i = 0; i < 8; i++) {
//	            row.getCell(i).setCellStyle(dataStyle);
//	        }
//	    }

	    int serial = 1;

	    for (Visitor v : visitors) {

	        Row row = sheet.createRow(rowNum);
	        int col = 0;

	        row.createCell(col++).setCellValue(serial++);

	        if (includePhoto) {
	            try {
	                PhotoData photoData = visitorPhotoService.getVisitorPhoto(v.getId());

	                int pictureIndex = workbook.addPicture(
	                        photoData.data(),
	                        Workbook.PICTURE_TYPE_JPEG // works for jpg/jpeg/png
	                );

	                CreationHelper helper = workbook.getCreationHelper();
	                ClientAnchor anchor = helper.createClientAnchor();

	                anchor.setCol1(col);
	                anchor.setRow1(rowNum);
	                anchor.setCol2(col + 1);
	                anchor.setRow2(rowNum + 1);

	                Picture pict = drawing.createPicture(anchor, pictureIndex);
	                pict.resize(1, 1);

	                row.setHeightInPoints(60);
	                sheet.setColumnWidth(col, 20 * 256);

	            } catch (Exception e) {
	                row.createCell(col).setCellValue("No Photo");
	            }

	            col++;
	        }

	        row.createCell(col++).setCellValue(v.getVPassNo());
	        row.createCell(col++).setCellValue(v.getName());
	        row.createCell(col++).setCellValue(v.getMobileNo());
	        row.createCell(col++).setCellValue(v.getPurpose());
	        row.createCell(col++).setCellValue(v.getPurposeDetails());
	        row.createCell(col++).setCellValue(v.getVisitDateTime().format(dateTimeFormatter));
	        row.createCell(col++).setCellValue(v.getAddress() + ", " + v.getState());

	        // Apply border style
	        for (int i = 0; i < col; i++) {
	            if (row.getCell(i) != null) {
	                row.getCell(i).setCellStyle(dataStyle);
	            }
	        }

	        rowNum++;
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
