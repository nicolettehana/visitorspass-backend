package sad.storereg.dto.appdata;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubItemStockDTO {
    private String subItem;
    private Integer stock;
}