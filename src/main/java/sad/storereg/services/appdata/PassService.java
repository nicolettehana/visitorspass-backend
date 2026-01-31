package sad.storereg.services.appdata;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import lombok.RequiredArgsConstructor;
import sad.storereg.dto.appdata.PhotoData;
import sad.storereg.models.appdata.Visitor;
import sad.storereg.repo.appdata.VisitorRepository;

import org.springframework.core.io.ClassPathResource;


@Service
@RequiredArgsConstructor
public class PassService {
	
	@Value("${passes.dir}")
    private String baseDir;
	private final VisitorPhotoService visitorPhotoService;
	private final VisitorRepository visitorRepository;
	
	@Value("${passes.dir}")
	private String passesDir;
	
	public byte[] generateQrCode(Visitor visitor) throws Exception {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");
		String text ="Name:"+visitor.getName()+"\n"+"Visit Date:"+visitor.getVisitDateTime().format(formatter)+"\n"+"Purpose:"+visitor.getPurpose()+"\n"+"e-Pass no:"+visitor.getVPassNo();
	    BitMatrix matrix = new QRCodeWriter()
	            .encode(text, BarcodeFormat.QR_CODE, 150, 150);

	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    MatrixToImageWriter.writeToStream(matrix, "PNG", out);
	    return out.toByteArray();
	}
	
	public byte[] generateVisitorPassPdf(
	        Visitor visitor, MultipartFile photo1
	) {
		try {
			Path dirPath = Paths.get(passesDir);
	        Files.createDirectories(dirPath);
	        
	        String fileName = visitor.getVPassNo() + ".pdf";
	        Path filePath = dirPath.resolve(fileName);
	        
	        PdfWriter writer = new PdfWriter(Files.newOutputStream(filePath));
	        PdfDocument pdf = new PdfDocument(writer);
	        Document document = new Document(pdf, PageSize.A4);
	        
	        document.setMargins(30, 40, 30, 40);
	        
	        byte[] logoBytes = loadLogo();

	        Image logo = new Image(ImageDataFactory.create(logoBytes))
	                .setWidth(70)
	                .setHorizontalAlignment(HorizontalAlignment.CENTER);

	        document.add(logo);
	        
	        document.add(new Paragraph("GOVERNMENT OF MEGHALAYA")
	                .setBold()
	                .setFontSize(14)
	                .setTextAlignment(TextAlignment.CENTER)
	                .setMarginTop(0)
	                .setMarginBottom(2));

	        document.add(new Paragraph("SECRETARIAT ADMINISTRATION DEPARTMENT")
	                .setFontSize(11)
	                .setTextAlignment(TextAlignment.CENTER)
	                .setMarginTop(0)
	                .setMarginBottom(2));

	        document.add(new Paragraph("e-VISITOR PASS No : " + visitor.getVPassNo())
	                .setBold()
	                .setFontSize(11)
	                .setMarginTop(0)
	                .setMarginBottom(5)
	                .setTextAlignment(TextAlignment.CENTER));
	        
	        Table mainTable = new Table(new float[]{3, 1}); 
	        mainTable.setWidth(UnitValue.createPercentValue(100)); 
	        mainTable.setMarginTop(20); Cell left = new Cell() .setBorder(Border.NO_BORDER) .setPaddingTop(0) .setPaddingBottom(0) .setPaddingLeft(10) .setPaddingRight(10); 
	        
	        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a"); 
	        
	        left.add(new Paragraph("Applicant's Name : " + visitor.getName())); 
	        left.add(new Paragraph("Address : " + visitor.getAddress())); left.add(new Paragraph("Visit Date & Time : " + visitor.getVisitDateTime().format(formatter))); 
	        left.add(new Paragraph("Purpose of Visit : " + visitor.getPurpose())); mainTable.addCell(left); 
	        
	        Image photoImg = new Image(ImageDataFactory.create(visitorPhotoService.getVisitorPhoto(visitor.getId()).data())) .setWidth(100)  .setAutoScale(true); 
	        
	        Cell right = new Cell() .setBorder(Border.NO_BORDER) .setTextAlignment(TextAlignment.CENTER) .setVerticalAlignment(VerticalAlignment.TOP) .add(photoImg); 
	        mainTable.addCell(right); document.add(mainTable); 
	        
	        Image qrImg = new Image(ImageDataFactory.create(generateQrCode(visitor))) .setWidth(120) .setHorizontalAlignment(HorizontalAlignment.RIGHT) .setMarginTop(20); 
	        document.add(qrImg);

	        document.add(new Paragraph(
	                "This visitor pass is valid for a period of 4 hours from the date & time of visit.\n"
	              + "Any QR code scanning application can scan the above QR Code. The URL on the QR Code will allow\r\n"
	              + "access to the digital version of this certificate.")
	                .setFontSize(9)
	                .setTextAlignment(TextAlignment.CENTER)
	                .setMarginTop(10));

	        
	        document.close();

	        return Files.readAllBytes(filePath);

	    } catch (Exception e) {
	        throw new RuntimeException("Error generating visitor pass PDF", e);
	    }
	}

	
	private byte[] loadLogo() {
	    try {
	        ClassPathResource resource = new ClassPathResource("meglogo.png");
	        return resource.getInputStream().readAllBytes();
	    } catch (Exception e) {
	        throw new RuntimeException("Unable to load logo image", e);
	    }
	}

	
	public PhotoData getVisitorPassPdf(Long visitorCode) {
	    Visitor visitor = visitorRepository.findById(visitorCode)
	            .orElseThrow(() -> new RuntimeException("Visitor not found"));
	    Path pdfPath = Paths.get(baseDir, visitor.getVPassNo() + ".pdf");

	    if (!Files.exists(pdfPath)) {
	        throw new RuntimeException(
	                "Visitor pass PDF not found for pass no: " + visitor.getVPassNo()
	        );
	    }

	    try {
	        byte[] bytes = Files.readAllBytes(pdfPath);
	        return new PhotoData(bytes, "application/pdf");

	    } catch (IOException e) {
	        throw new RuntimeException("Unable to read visitor pass PDF", e);
	    }
	}


}
