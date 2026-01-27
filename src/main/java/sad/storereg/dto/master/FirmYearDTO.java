package sad.storereg.dto.master;

import java.util.List;

import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

import lombok.Data;

@Data
public class FirmYearDTO {
	
	@Required
	private Long firmId;
	
	@Required
	private Integer yearRangeId;
	
	@Required
	private List<String> categories;

}
