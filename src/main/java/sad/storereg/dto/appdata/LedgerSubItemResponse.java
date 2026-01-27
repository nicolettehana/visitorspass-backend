package sad.storereg.dto.appdata;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LedgerSubItemResponse {
	private Long subItemId;
    private String subItemName;

    private List<LedgerUnitResponse> units = new ArrayList<>();
}