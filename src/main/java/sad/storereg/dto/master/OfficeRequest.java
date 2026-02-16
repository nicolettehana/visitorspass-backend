package sad.storereg.dto.master;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OfficeRequest {
	
	private String officeName;
	private Integer officeCode;

}
