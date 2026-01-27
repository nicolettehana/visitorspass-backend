package sad.storereg.dto.appdata;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class PurchaseReceiptDTO {
	
	private Long purchaseId;
	private LocalDate billDate;
	private String billNo;
	private Double totalCost;
	private Long firmId;
	private String receivedFrom;
	private String issuedTo;
	private List<PurchaseReceiptItems> items;

}
