package sad.storereg.models.auth;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@Table(name = "password_reset_token", schema = "auth")
@Entity
public class PasswordResetToken {
	
	private static final int EXPIRATION = 10;
	
	@JsonProperty(access = Access.WRITE_ONLY)
	@Id
	@GeneratedValue
	public Integer id;
	
	@Column(name = "user_id")
	public Integer user;
	
	public String token;
	
	public Timestamp expiry;

}
