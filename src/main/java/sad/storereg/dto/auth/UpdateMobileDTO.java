package sad.storereg.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateMobileDTO {

	@NotBlank
	private String mobileNo;
	
	@NotBlank
	private String otp;
	
	@NotBlank
	private String otpToken;
}
