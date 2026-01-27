package sad.storereg.repo.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import sad.storereg.models.auth.Invalidate;

public interface InvalidateRepository extends JpaRepository<Invalidate, Integer>{
	
	Optional<Invalidate> findByUsername(String username);
	
	Optional<Invalidate> findByUsernameAndIpAddressEquals(String username, String ipAddress);

}
