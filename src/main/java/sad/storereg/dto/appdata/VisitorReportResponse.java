package sad.storereg.dto.appdata;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VisitorReportResponse {

    private int minister;
    private int cs;
    private int meeting;
    private int officer;
    private int department;

    private int noOfVisitors;

    private List<DailyVisitorReportDTO> details;
}