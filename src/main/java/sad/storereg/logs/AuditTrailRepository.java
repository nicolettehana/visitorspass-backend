package sad.storereg.logs;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditTrailRepository extends JpaRepository<AuditTrail, Long>{
	
	Page<AuditTrail> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
	
	Optional<AuditTrail> findTopByUsernameAndActionAndIpAddressNotOrderByTimestampDesc(String username, String action, String ipAddress);

}
