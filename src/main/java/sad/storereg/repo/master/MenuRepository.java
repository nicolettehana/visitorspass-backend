package sad.storereg.repo.master;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import sad.storereg.models.master.Menu;

public interface MenuRepository extends JpaRepository<Menu, Integer>{
	
	//@Query(value="select * from master.menu m join master.menu_roles r",nativeQuery = true)
	//@Query("SELECT m FROM Menu m LEFT JOIN FETCH m.roles")
	@Query("SELECT m FROM Menu m JOIN m.roles r WHERE r.role = :role ORDER BY m.sortOrder")
	List<Menu> findByRole(@Param("role") String role);
	
}
