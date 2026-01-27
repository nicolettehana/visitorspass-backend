package sad.storereg.dto.master;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FirmApproveDTO {
	
	private Long firmId;
    private Integer isChecked;
    private Integer yearRangeId;
    private String categoryCode; 

}
