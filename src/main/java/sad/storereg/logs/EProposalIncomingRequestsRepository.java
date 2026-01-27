package sad.storereg.logs;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EProposalIncomingRequestsRepository extends JpaRepository<EProposalIncomingRequests, Long>{

	List<EProposalIncomingRequests> findByRequestIdOrderByTimestampAsc(Long requestId);
}
