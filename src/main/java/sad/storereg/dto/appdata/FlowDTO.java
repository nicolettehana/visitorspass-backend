package sad.storereg.dto.appdata;

import java.util.Date;

import lombok.Data;

@Data
public class FlowDTO {
	
	public String role;
	
	public Long userCode;
	
	public String action;
	
	public String remarks;
	
	public Date timstamp;
	
	public String appNo;

}
