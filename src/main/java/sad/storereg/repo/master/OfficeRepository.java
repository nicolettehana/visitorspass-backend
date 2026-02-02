package sad.storereg.repo.master;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import sad.storereg.models.master.Office;

public interface OfficeRepository extends JpaRepository<Office, Integer>{
	
	Optional<Office> findByOfficeCode(Integer officeCode);

}
