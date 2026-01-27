package sad.storereg.logs;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Data;

@Builder
@Table(name = "login", schema = "logs")
@Data
@Entity
public class Login {
	
	@Id
	@GeneratedValue(strategy= GenerationType.AUTO)
	private long id;
	
	@Column(name="endpoint")
	private String uri;

	@Column(name="http_method")
	private String httpMethod;

	@Column(name="username")
	private String username;

	@Column(name="timestamp")
	private Date ts;

	@Column(name="http_response_status")
	private Integer httpStatus;

}
