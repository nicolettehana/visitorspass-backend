package sad.storereg.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class GetOtpResponseDTO {
	
	@JsonProperty("otp")
	private String otp;

	@JsonProperty("expiry")
	private String expiry;
	
	@JsonProperty("otpToken")
	private String otpToken;

}
