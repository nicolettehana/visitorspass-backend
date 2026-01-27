package sad.storereg.repo.master;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import sad.storereg.dto.appdata.CategoryCountDTO;
import sad.storereg.models.master.FirmCategory;

public interface FirmCategoryRepository extends JpaRepository<FirmCategory, Long>{

	Page<FirmCategory> findByCategory_Code(String categoryCode, Pageable pageable);
	
	long count();
	
	@Query("""
		    SELECT new sad.storereg.dto.appdata.CategoryCountDTO(
		        fc.category.name,
		        fc.category.code,
		        COUNT(fc.firm)
		    )
		    FROM FirmCategory fc
		    GROUP BY fc.category.name, fc.category.code
		    ORDER BY fc.category.name ASC
		""")
		List<CategoryCountDTO> countFirmsPerCategory();

}
