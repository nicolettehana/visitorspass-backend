package sad.storereg.dto.appdata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DailyVisitorReportDTO {

    private String date;
    private String dayOfWeek;

    private int minister;
    private int cs;
    private int meeting;
    private int officer;
    private int department;

    private int totalNoOfVisitors;
}
