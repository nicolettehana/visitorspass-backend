package sad.storereg.dto.auth;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequestDTO {

	@NotBlank(message = "OTP (otp) is required")
	private String otp;

	@NotBlank(message = "Mobile no. (mobileno) is required")
	private String mobileno;

	// @NotBlank(message="Captcha (captcha) is required")
	private String captcha;

	// @NotNull(message="UUID (uuid) is required")
	private UUID uuid;
}
