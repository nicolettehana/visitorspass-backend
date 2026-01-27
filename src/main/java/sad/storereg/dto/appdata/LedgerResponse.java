package sad.storereg.dto.appdata;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LedgerResponse {
	
	private Long itemId;
    private String itemName;
    private String category;
    private String categoryCode;

    private List<LedgerUnitResponse> units = new ArrayList<>();
    private List<LedgerSubItemResponse> subItems = new ArrayList<>();
}