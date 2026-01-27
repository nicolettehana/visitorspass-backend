package sad.storereg.repo.master;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import sad.storereg.models.master.Unit;

public interface UnitRepository extends JpaRepository<Unit, Integer>{
	
	Optional<Unit> findByUnitOrName(String unit, String name);

}
