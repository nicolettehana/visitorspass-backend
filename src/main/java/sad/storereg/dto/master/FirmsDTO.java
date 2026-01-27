package sad.storereg.dto.master;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sad.storereg.models.master.Category;
import sad.storereg.models.master.YearRange;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FirmsDTO {
	
	private Long id;
    private String firm;
    private List<Category> categories;
    private List<YearRange> yearRanges;

}
