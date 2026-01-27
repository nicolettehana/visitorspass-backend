package sad.storereg.repo.master;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import sad.storereg.dto.master.FirmCheckDTO;
import sad.storereg.models.master.Firm;

public interface FirmsRepository extends JpaRepository<Firm, Long>{
	
	
	@EntityGraph(attributePaths = {"categories", "categories.category"})
	Page<Firm> findAll(Pageable pageable);
	
	@Query("""
	        SELECT DISTINCT f 
	        FROM Firm f
	        JOIN FirmYear fy ON fy.firm = f
	        JOIN fy.yearRange yr
	        WHERE :year BETWEEN yr.startYear AND yr.endYear
	    """)
	    List<Firm> findAllByYear(int year);
	
	Page<Firm> findByFirmContainingIgnoreCase(String search, Pageable pageable);
	
	@Query("""
		    SELECT new sad.storereg.dto.master.FirmCheckDTO(
		        f.id,
		        f.firm,
		        CASE WHEN fy.id IS NOT NULL THEN 1 ELSE 0 END
		    )
		    FROM Firm f
		    LEFT JOIN FirmYear fy
		        ON fy.firm.id = f.id
		       AND fy.category.code = :categoryCode
		       AND fy.yearRange.id = :yearRangeId
		    WHERE (:search IS NULL OR LOWER(f.firm) LIKE LOWER(CONCAT('%', :search, '%')))
		""")
		Page<FirmCheckDTO> findFirmsWithCheckedFlag(
		        @Param("search") String search,
		        @Param("categoryCode") String categoryCode,
		        @Param("yearRangeId") Integer yearRangeId,
		        Pageable pageable
		);

	
}
