package sad.storereg.models.auth;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "otp", schema = "auth")
public class Otp {

	@JsonProperty(access = Access.WRITE_ONLY)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Id
	private Long id;

	private String username;

	private String otp;

	@Column(name="generated_at")
	private Timestamp generatedAt;

	private Integer count;
	
	@Column(name="forgot_password")
	private Integer forgotPassword;
	
	@Column(name="is_signup")
	private Integer isSignUp;
	
	@Column(name="token")
	public String otpToken;
}
