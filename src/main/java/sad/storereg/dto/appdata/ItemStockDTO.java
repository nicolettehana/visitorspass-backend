package sad.storereg.dto.appdata;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ItemStockDTO {
    private String itemName;
    private Integer stock;
    private List<SubItemStockDTO> subItems;
}