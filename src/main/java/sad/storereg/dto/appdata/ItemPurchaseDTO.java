package sad.storereg.dto.appdata;

import java.util.List;

import lombok.Data;

@Data
public class ItemPurchaseDTO {
	
	private String itemName;
    private List<SubItemPurchaseDTO> subItems;
    private Integer quantity;
    private Double rate;
    private Double amount;
    private String unit;
    private Integer unitId;
    private String category;
    private String categoryCode;
    private Long id;
    private Long itemId;
    private Double gstPercentage;
    private Double cgst;
    private Double sgst;
}
