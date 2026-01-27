package sad.storereg.dto.appdata;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class PurchaseCreateDTO {
	
	private String remarks;
    private LocalDate purchaseDate;
    private Long firmId;
    private String receivedFrom;
    private Double totalCost;
    private LocalDate issueDate;
    private String issueTo;
    private String fileNo;
    private LocalDate billDate;
    private String billNo;

    private List<ItemCreatePurchaseDTO> items;

}
