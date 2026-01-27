package sad.storereg.dto.master;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
//@NoArgsConstructor
@RequiredArgsConstructor
public class UnitRateDTO {

	private Integer unitId;
	
	private String unitName;
	
	private Double rate;
	
	private Long itemId;
	
	private Long subItemId;
	
	private String unit;
	
	private Integer balance;
	
	// ✅ Custom lightweight constructor
    public UnitRateDTO(Integer unitId, String unit, String unitName, Double rate) {
        this.unitId = unitId;
        this.unit = unit;
        this.unitName = unitName;
        this.rate = rate;
    }
}
