package sad.storereg.repo.master;

import org.springframework.data.jpa.repository.JpaRepository;

import sad.storereg.models.master.SubItems;

public interface SubItemRepository extends JpaRepository<SubItems, Long>{

}
