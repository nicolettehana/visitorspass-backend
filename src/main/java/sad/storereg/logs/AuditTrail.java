package sad.storereg.logs;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "audit_logs", schema = "logs")
public class AuditTrail {
	
	@Id
	@JsonProperty(access = Access.WRITE_ONLY)
	@GeneratedValue
	private long id;

	private String username;

	@Column(nullable = false)
	private String uri;

	//@Lob
	//@Basic(fetch = FetchType.EAGER)
	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false)
	private String action;

	@Column(nullable = false)
	private String status;

	@Column(name = "ip_addr")
	private String ipAddress;

	@Column(nullable = false)
	private Date timestamp;
	
	@Column(name = "http_status")
	private Integer httpStatus;

}
