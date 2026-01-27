package sad.storereg.dto.appdata;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryStockResponse {
    private String category;
    private String categoryCode;
    private List<ItemStockDTO> items;
}
