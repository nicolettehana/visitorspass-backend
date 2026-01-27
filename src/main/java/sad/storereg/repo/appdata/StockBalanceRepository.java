package sad.storereg.repo.appdata;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import sad.storereg.models.appdata.StockBalance;

public interface StockBalanceRepository extends JpaRepository<StockBalance, Long>{
	
	@Query("""
	        SELECT sb
	        FROM StockBalance sb
	        WHERE sb.item.id = :itemId
	        AND (:subItemId IS NULL OR sb.subItem.id = :subItemId)
	        ORDER BY sb.date DESC
	        LIMIT 1
	    """)
	    StockBalance findLatestBalance(Long itemId, Long subItemId);

	@Query("""
		    SELECT sb FROM StockBalance sb
		    WHERE sb.item.id = :itemId
		      AND (:subItemId IS NULL OR sb.subItem.id = :subItemId)
		      AND sb.date <= :date
		    ORDER BY sb.date DESC
		    LIMIT 1
		""")
		StockBalance getLatestBalance1(Long itemId, Long subItemId, LocalDate date);

}
