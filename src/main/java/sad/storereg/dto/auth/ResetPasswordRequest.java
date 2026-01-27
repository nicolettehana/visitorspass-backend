package sad.storereg.dto.auth;

import lombok.Data;

@Data
public class ResetPasswordRequest {
	
	//private UUID user_id;
	
	private String username;

	//@NotBlank(message = "Token is required")
	private String token;
	
	private String password;
	
	private String otpToken;
	
	private String mobileNo;
	
	private String otp;

}
