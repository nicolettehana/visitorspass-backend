package sad.storereg.dto.appdata;

import lombok.Data;

@Data
public class PurchaseReceiptSubItems {
	
	private Double gstPercentage;
	private Double cgst;
	private Double sgst;
	private Double amount;
	private Double rate;

}
