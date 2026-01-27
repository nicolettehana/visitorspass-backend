package sad.storereg.repo.auth;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import sad.storereg.models.auth.User;

public interface UserRepository extends JpaRepository<User, Long>{
	
	Optional<User> findByUsername(String username);
	
	Optional<User> findByMobileNo(String mobileNo);
	
	Optional<User> findByEmail(String email);
	
	Optional<User> findByOfficeCode(Integer officeCode);
	
	List<User> findAllByMobileNoAndUsernameNot(String mobileNo, String username);

}
