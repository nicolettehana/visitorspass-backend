package sad.storereg.repo.master;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import sad.storereg.dto.appdata.CategoryCountDTO;
import sad.storereg.models.master.Category;

public interface CategoryRepository extends JpaRepository<Category, String>{
	
	Optional<Category> findByCodeOrName(String code, String name);
	
	Optional<Category> findByCode(String code);
	
	List<Category> findAllByStockType(String stockType);
	
//	@Query("""
//	        SELECT new com.example.dto.CategoryCountDTO(c.name, COUNT(f))
//	        FROM Category c
//	        LEFT JOIN c.firms f
//	        GROUP BY c.name
//	    """)
//	    List<CategoryCountDTO> countFirmsPerCategory();

}
