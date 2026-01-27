package sad.storereg.dto.appdata;

import lombok.Data;

@Data
public class ItemCreatePurchaseDTO {

	private String categoryCode;
    private Long itemId;
    private String item;
    private Long subItemId;
    private String unit;
    private Integer unitId;
    private Integer quantity;
    private Double rate;
    private Double amount;
    private Double gstPercentage;
    private Double cgst;
    private Double sgst;
    private Long id;
}
