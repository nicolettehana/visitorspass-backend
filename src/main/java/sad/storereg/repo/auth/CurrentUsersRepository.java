package sad.storereg.repo.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import sad.storereg.models.auth.CurrentUsers;

public interface CurrentUsersRepository extends JpaRepository<CurrentUsers, Integer>{

}
