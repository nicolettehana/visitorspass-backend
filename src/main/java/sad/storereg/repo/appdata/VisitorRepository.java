package sad.storereg.repo.appdata;

import org.springframework.data.jpa.repository.JpaRepository;

import sad.storereg.models.appdata.Visitor;

public interface VisitorRepository extends JpaRepository<Visitor, Long>{

}
