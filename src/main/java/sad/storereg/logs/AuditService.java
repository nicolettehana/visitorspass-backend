package sad.storereg.logs;

import java.util.Date;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditService {
	
	private final AuditTrailRepository auditTrailRepo;
	
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void saveAuditTrail(String action, String username, String uri, String description, String status, 
			String ipAddress, Integer httpStatus) {
		AuditTrail auditLog = AuditTrail.builder().action(action)
        		.username(username)
        		.uri(uri).description(description)
        		.timestamp(new Date())
        		.status(status)
        		.ipAddress(ipAddress)
        		.httpStatus(httpStatus)
        		.build();
        auditTrailRepo.save(auditLog);
    }
	
	public Optional<AuditTrail> getUserNotIP(String username, String ipAddress){
		return auditTrailRepo.findTopByUsernameAndActionAndIpAddressNotOrderByTimestampDesc(username,  "authenticate2", ipAddress);
	}

}
