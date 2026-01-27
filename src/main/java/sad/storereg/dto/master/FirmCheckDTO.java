package sad.storereg.dto.master;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
public class FirmCheckDTO {
	
	private Long firmId;
    private String firmName;
    private Integer isChecked;

}
