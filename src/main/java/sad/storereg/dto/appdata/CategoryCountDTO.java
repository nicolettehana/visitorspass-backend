package sad.storereg.dto.appdata;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CategoryCountDTO {
	
	private String category;
	
	private String categoryCode;
	
    private Long totalFirms;

}
