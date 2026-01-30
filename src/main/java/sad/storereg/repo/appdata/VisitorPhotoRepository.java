package sad.storereg.repo.appdata;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import sad.storereg.models.appdata.VisitorPhoto;

public interface VisitorPhotoRepository extends JpaRepository<VisitorPhoto, Long>{
	
	Optional<VisitorPhoto> findFirstByVisitor_Id(Long visitorId);

}
