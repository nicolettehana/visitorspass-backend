package sad.storereg.dto.appdata;

import java.util.List;

import lombok.Data;

@Data
public class PurchaseReceiptItems {
	
	private Long id;
	private Double gstPercentage;
	private Double cgst;
	private Double sgst;
	private Double rate;
	private Double amount;
	private String itemName;
	private List<PurchaseReceiptSubItems> subItems;

}
