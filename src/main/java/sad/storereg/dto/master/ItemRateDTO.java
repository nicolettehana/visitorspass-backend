package sad.storereg.dto.master;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.ArrayList;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemRateDTO {
	
	private Long id;
    private String name;
    private String category;
    private String categoryCode;
    private Integer startYear;
    private Integer endYear;

    private String unit;     // only for items with no subItems
    private Double rate;     // only for items with no subItems

    private List<UnitRateDTO> rates = new ArrayList<>();
    
    private List<SubItemRateDTO> subItems = new ArrayList<>();

}
