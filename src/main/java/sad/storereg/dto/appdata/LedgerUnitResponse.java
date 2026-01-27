package sad.storereg.dto.appdata;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LedgerUnitResponse {

    private Integer unitId;
    private String unitName;

    private Integer openingBalance;
    private Integer noOfPurchases;
    private Integer noOfIssues;
    private Integer closingBalance;
}
