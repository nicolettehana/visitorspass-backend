package sad.storereg.dto.appdata;

import lombok.Data;

@Data
public class SubItemPurchaseDTO {
	
	private String subItemName;
    private Integer quantity;
    private Double rate;
    private Double amount;
    private String unit;
    private Integer unitId;
    private Long subItemId;
    private Double gstPercentage;
    private Double cgst;
    private Double sgst;

}
