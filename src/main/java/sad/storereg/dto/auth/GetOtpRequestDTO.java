package sad.storereg.dto.auth;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GetOtpRequestDTO {
	
	@NotBlank(message = "Mobile no. (mobileno) is required")
	private String mobileno;

	// @NotBlank(message="Captcha (captcha) is required")
	private String captcha;

	// @NotNull(message="UUID (uuid) is required")
	private UUID uuid;
	
	private Integer isSignUp;

}
