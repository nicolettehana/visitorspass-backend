package sad.storereg.dto.master;

import java.util.List;

import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

import lombok.Data;

@Data
public class CreateFirmDTO {
	
	@Required
	private String firmName;
	
	@Required
    private List<String> categories; 

}
