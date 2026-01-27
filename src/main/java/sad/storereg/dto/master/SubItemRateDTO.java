package sad.storereg.dto.master;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubItemRateDTO {
	
	private Long id;
    private String name;
    private String unit;
    private Double rate;
    
    private List<UnitRateDTO> rates = new ArrayList<>();

}
