package sad.storereg.dto.appdata;

import java.time.LocalDate;

import lombok.Data;

@Data
public class MyFilter {
	
	private LocalDate fromDate;
	
	private LocalDate toDate;
	
	private String search;

}
