package sad.storereg.models.auth;

import java.sql.Timestamp;

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
import sad.storereg.models.auth.Invalidate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "invalidate", schema = "auth")
public class Invalidate {
	
	@Id
	@GeneratedValue(strategy= GenerationType.AUTO)
	private Integer id;
	
	public String username;

	@Column(name="invalidated_at")
	public Timestamp invalidatedAt;
	
	@Column(name="ip_address")
	public String ipAddress;

}
