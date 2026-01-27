package sad.storereg.dto.master;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemRateCreateDTO {
	
    private Long itemId;
    private Long subItemId;
    private String categoryCode;
    private Integer yearRangeId;

    private Integer unitId;     // only for items with no subItems
    private Double rate;     // only for items with no subItems

}
