package sad.storereg.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {
	
	@JsonProperty("oldPassword")
	// @NotBlank(message="Old Password (oldPassword) is required")
	private String oldPassword;

	@JsonProperty("newPassword")
	@Size(min = 8, message = "Password should be at least 8 characters long.")
	@NotBlank(message = "New Password (newPassword) is required")
	private String newPassword;

	//private UUID userUuid;
	
	private String username;

}
