package sad.storereg.repo.appdata;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import sad.storereg.models.appdata.Issue;

public interface IssueRepository extends JpaRepository<Issue, Long>{
	
	@Query("""
	        SELECT iss FROM Issue iss
	        JOIN iss.items it
	        WHERE iss.date BETWEEN :startDate AND :endDate
	        AND (:category IS NULL OR it.categoryCode = :category)
	        AND (
	            :searchValue IS NULL
	            OR LOWER(iss.issueTo) LIKE LOWER(CONCAT('%', :searchValue, '%'))
	            OR LOWER(iss.remarks) LIKE LOWER(CONCAT('%', :searchValue, '%'))
	        )
	    """)
	Page<Issue> searchIssues(
	        LocalDate startDate,
	        LocalDate endDate,
	        String category,
	        String searchValue,
	        Pageable pageable
	);
	
	@Query("""
		    SELECT ii.unit.id, SUM(ii.quantity)
		    FROM IssueItem ii
		    JOIN ii.issue i
		    WHERE ii.item.id = :itemId
		      AND (:subItemId IS NULL OR ii.subItem.id = :subItemId)
		      AND i.date > :fromDate AND i.date <= :toDate
		    GROUP BY ii.unit.id
		""")
		List<Object[]> getIssuedAfter(
		        @Param("itemId") Long itemId,
		        @Param("subItemId") Long subItemId,
		        @Param("fromDate") LocalDate fromDate,
		        @Param("toDate") LocalDate toDate
		);
		
		@Query("""
			    SELECT COALESCE(SUM(ii.quantity), 0)
			    FROM IssueItem ii
			    JOIN ii.issue i
			    WHERE ii.item.id = :itemId
			      AND (:subItemId IS NULL OR ii.subItem.id = :subItemId)
			      AND i.date > :fromDate
			      AND i.date <= :toDate
			""")
			int sumIssuedAfter(Long itemId, Long subItemId, LocalDate fromDate, LocalDate toDate);

		@Query("""
		        SELECT COALESCE(SUM(ii.quantity),0)
		        FROM IssueItem ii
		        WHERE ii.item.id = :itemId
		          AND ((:subItemId IS NULL AND ii.subItem IS NULL) OR ii.subItem.id = :subItemId)
		          AND ii.unit.id = :unitId
		          AND ii.issue.date BETWEEN :startDate AND :endDate
		    """)
		    Integer sumQtyByItemSubItemUnitBetween(@Param("itemId") Long itemId,
		                                          @Param("subItemId") Long subItemId,
		                                          @Param("unitId") Integer unitId,
		                                          @Param("startDate") LocalDate startDate,
		                                          @Param("endDate") LocalDate endDate);

		    @Query("""
		        SELECT COALESCE(SUM(ii.quantity),0)
		        FROM IssueItem ii
		        WHERE ii.item.id = :itemId
		          AND ((:subItemId IS NULL AND ii.subItem IS NULL) OR ii.subItem.id = :subItemId)
		          AND ii.unit.id = :unitId
		          AND ii.issue.date <= :endDate
		    """)
		    Integer sumQtyByItemSubItemUnitUntil(@Param("itemId") Long itemId,
		                                        @Param("subItemId") Long subItemId,
		                                        @Param("unitId") Integer unitId,
		                                        @Param("endDate") LocalDate endDate);
		    
		    @Query("""
		    	    SELECT DISTINCT iss
		    	    FROM Issue iss
		    	    JOIN FETCH iss.items it
		    	    WHERE iss.date BETWEEN :startDate AND :endDate
		    	      AND (:category IS NULL OR it.categoryCode = :category)
		    	""")
		    	List<Issue> getIssues(
		    	    @Param("startDate") LocalDate startDate,
		    	    @Param("endDate") LocalDate endDate,
		    	    @Param("category") String category
		    	);


}
