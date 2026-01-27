 package sad.storereg.dto.auth;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthenticationRequest {
	
	@NotBlank(message = "Username (username) is required")
	private String username;

	@NotBlank(message = "Password (password) is required")
	@Size(min = 8, message = "Password must be at least 8 characters long")
	@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=]).*$", message = "Password must contain at least one lowercase letter, one uppercase letter, one digit, and one special character")
	String password;

	// @NotBlank(message="Captcha (captcha) is required")
	private String captcha;

	// @NotNull(message="UUID (uuid) is required")
	private UUID captchaToken;

}
