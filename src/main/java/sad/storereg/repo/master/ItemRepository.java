package sad.storereg.repo.master;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import sad.storereg.dto.appdata.CategoryCountDTO;
import sad.storereg.models.master.Item;

public interface ItemRepository extends JpaRepository<Item, Long>{
	
	Page<Item> findAll(Pageable pageable);
	
	Page<Item> findAllByCategory_Code(String category, Pageable pageable);
	
	List<Item> findAllByCategory_Code(String category);
	
	@Query("""
		    SELECT new sad.storereg.dto.appdata.CategoryCountDTO(
		        i.category.name,
		        i.category.code,
		        SUM(
		            CASE 
		                WHEN SIZE(i.subItems) = 0 THEN 1
		                ELSE SIZE(i.subItems)
		            END
		        )
		    )
		    FROM Item i
		    GROUP BY i.category.name, i.category.code
		    ORDER BY i.category.name ASC
		""")
		List<CategoryCountDTO> getCategoryCounts();


	
	@Query("""
		    SELECT SUM(
		        CASE 
		            WHEN size(i.subItems) = 0 THEN 1
		            ELSE size(i.subItems)
		        END
		    )
		    FROM Item i
		""")
		Long getAbsoluteTotal();

	@Query("""
		    select distinct r.id
		    from Item r
		    where 
		      (:categoryCode is null or r.category.code = :categoryCode)
		    order by r.id
		""")
		Page<Long> findDistinctItemIds(
		    @Param("categoryCode") String categoryCode,
		    Pageable pageable
		);
	
	@Query("""
	        SELECT DISTINCT i
	        FROM Item i
	        LEFT JOIN i.subItems s
	        WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%'))
	           OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))
	    """)
	    Page<Item> searchByItemOrSubItemName(
	            @Param("search") String search,
	            Pageable pageable
	    );
}
