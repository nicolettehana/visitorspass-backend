package sad.storereg.services.appdata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import lombok.RequiredArgsConstructor;
import sad.storereg.dto.appdata.PhotoData;
import sad.storereg.models.appdata.Visitor;
import sad.storereg.models.master.Office;
import sad.storereg.repo.appdata.VisitorRepository;
import sad.storereg.repo.master.OfficeRepository;

@Service
@RequiredArgsConstructor
public class ReportService {
	
	private final VisitorRepository visitorRepository;
	private final OfficeRepository officeRepository;
	private final VisitorPhotoService visitorPhotoService;

	public byte[] generateVisitorReport(LocalDate startDate, LocalDate endDate, Integer officeCode, Integer withPhoto) throws Exception {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");
        
        Optional<Office> office = officeRepository.findByOfficeCode(officeCode);
        String building = office.isEmpty()?"":office.get().getOfficeName();
        System.out.println("Office Code: "+building);

        List<Visitor> visitors;
        if(officeCode==null || !officeCode.equals(""))
        	visitors = visitorRepository.findByVisitDateTimeBetween(startDateTime, endDateTime);
        else
        	visitors = visitorRepository.findByVisitDateTimeBetweenAndOfficeCodeEquals(startDateTime, endDateTime, officeCode);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Initialize PDF writer and document
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4.rotate());
        document.setMargins(20, 20, 20, 20);

        // Title
        Paragraph title = new Paragraph("Government of Meghalaya \n "+building+" \n Visitors")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);
        
        
     // ← Add this block: Date range info
        DateTimeFormatter dateOnly = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        Paragraph dateRange = new Paragraph().setBold()
            .add(new Paragraph("Date: " + startDate.format(dateOnly)+" ")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(12))
            .add(new Paragraph(" to " + endDate.format(dateOnly))
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(12))
            .setTextAlignment(TextAlignment.LEFT)
            .setMarginBottom(0)
            .setMarginTop(5);
        
        document.add(dateRange);
        
        Paragraph noOfVisitors= new Paragraph("No. of Visitors: " + visitors.size())
        		.setBold()
                .setFontSize(12)
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginTop(0);
        document.add(noOfVisitors);
        
        Paragraph text= new Paragraph("Generated on: " + LocalDateTime.now().format(formatter))
                .setFontSize(10)
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginTop(3);
        document.add(text);

        // Table with 8 columns
//        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 3, 3, 3, 3, 3, 3, 3}))
//                .useAllAvailableWidth();
        Table table;

        if (withPhoto != null && withPhoto == 1) {
            table = new Table(UnitValue.createPercentArray(
                    new float[]{1, 2, 3, 3, 3, 3, 3, 3, 3}))
                    .useAllAvailableWidth();
        } else {
            table = new Table(UnitValue.createPercentArray(
                    new float[]{1, 3, 3, 3, 3, 3, 3, 3}))
                    .useAllAvailableWidth();
        }

        // Header
//        String[] headers = {"S.No", "Visitor Pass No.", "Visitor Name", "Mobile Number", "Purpose", "Purpose Details/Name", "Date & Time of Visit", "Address"};
//        for (String h : headers) {
//            table.addHeaderCell(new Cell()
//                    .add(new Paragraph(h))
//                    .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
//                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
//                    .setTextAlignment(TextAlignment.CENTER));
//        }
        List<String> headers = new ArrayList<>();

        headers.add("S.No");

        if (withPhoto != null && withPhoto == 1) {
            headers.add("Photo");
        }

        headers.addAll(List.of(
                "Visitor Pass No.",
                "Visitor Name",
                "Mobile Number",
                "Purpose",
                "Purpose Details/Name",
                "Date & Time of Visit",
                "Address"
        ));

        for (String h : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h))
                    .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER));
        }

        // Table rows
        int serial = 1;
        for (Visitor v : visitors) {
            table.addCell(new Cell().add(new Paragraph(String.valueOf(serial++))).setTextAlignment(TextAlignment.CENTER));
            
            if (withPhoto != null && withPhoto == 1) {
                try {
                    PhotoData photoData = visitorPhotoService.getVisitorPhoto(v.getId());

                    ImageData imageData = ImageDataFactory.create(photoData.data());
                    Image img = new Image(imageData);

                    img.scaleToFit(60, 60); // control image size

                    table.addCell(new Cell()
                            .add(img)
                            .setTextAlignment(TextAlignment.CENTER));
                } catch (Exception e) {
                    // If photo not found, keep empty cell instead of breaking PDF
                    table.addCell(new Cell()
                            .add(new Paragraph("No Photo"))
                            .setTextAlignment(TextAlignment.CENTER));
                }
            }

            
            table.addCell(new Cell().add(new Paragraph(v.getVPassNo())));
            table.addCell(new Cell().add(new Paragraph(v.getName())));
            table.addCell(new Cell().add(new Paragraph(v.getMobileNo())));
            table.addCell(new Cell().add(new Paragraph(v.getPurpose())));
            table.addCell(new Cell().add(new Paragraph(v.getPurposeDetails())));
            table.addCell(new Cell().add(new Paragraph(v.getVisitDateTime().format(formatter))));
            table.addCell(new Cell().add(new Paragraph(v.getAddress()+", "+v.getState())));
        }

        document.add(table);

        // Footer
        Paragraph footer = new Paragraph("This report is generated by e-Pass System on " + LocalDateTime.now().format(formatter))
                .setFontSize(8)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(10);
        document.add(footer);

        document.close();
        pdf.close();
        //writer.close(); 
        byte[] bytes = out.toByteArray();
        //return new ByteArrayInputStream(out.toByteArray());
        return bytes;
    }
	
	
}
