package sad.storereg.dto.auth;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import sad.storereg.models.auth.Role;

@Data
@Builder
public class RegisterRequest {

	//@NotBlank
	private String mobileNo;
	
	//@NotBlank(message="Username/email is required")
	private String username;
	
	@NotBlank
	private String name;
	
	@NotBlank
	private String designation;
	
	@NotBlank
	private String department;
	
	//@NotNull(message = "Role is required")
	private Role role;
	
	private String email;
	
	//@NotBlank(message = "Password (password) is required")
	@Size(min = 8, message = "Password should be at least 8 characters long.")
	private String password;
	
	// @NotBlank(message="Captcha (captcha) is required")
	private String captcha;

	// @NotNull(message="UUID (uuid) is required")
	private UUID captchaToken;
	
	private UUID otpToken;
	
	private String otp;
}
