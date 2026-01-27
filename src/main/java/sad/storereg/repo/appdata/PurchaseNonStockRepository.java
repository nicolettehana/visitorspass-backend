package sad.storereg.repo.appdata;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import sad.storereg.models.appdata.Purchase;
import sad.storereg.models.appdata.PurchaseNonStock;

public interface PurchaseNonStockRepository extends JpaRepository<PurchaseNonStock, Long>{

	@Query("""
		    SELECT DISTINCT p FROM PurchaseNonStock p
		    JOIN p.items pi
		    WHERE p.fileDate BETWEEN :startDate AND :endDate
		      AND (:category IS NULL OR pi.category = :category)
		      AND (
		            :searchValue IS NULL
		            OR LOWER(p.receivedFrom) LIKE LOWER(CONCAT('%', :searchValue, '%'))
		            OR LOWER(p.issueTo) LIKE LOWER(CONCAT('%', :searchValue, '%'))
		            OR LOWER(pi.item) LIKE LOWER(CONCAT('%', :searchValue, '%'))
		          )
		      AND (
		            :status = 'A'
		            OR (:status = 'P' AND p.billNo IS NULL)
		            OR (:status = 'R' AND p.billNo IS NOT NULL)
		          )
		""")
		Page<PurchaseNonStock> searchPurchaseNonStock(
		        @Param("startDate") LocalDate startDate,
		        @Param("endDate") LocalDate endDate,
		        @Param("category") String category,
		        @Param("searchValue") String searchValue,
		        @Param("status") String status,
		        Pageable pageable
		);
	
	@Query("""
		    SELECT DISTINCT p FROM PurchaseNonStock p
		    JOIN p.items pi
		    WHERE p.billDate BETWEEN :startDate AND :endDate
		      AND (:category IS NULL OR pi.category = :category)
		      AND (
		            :searchValue IS NULL
		            OR LOWER(p.receivedFrom) LIKE LOWER(CONCAT('%', :searchValue, '%'))
		            OR LOWER(p.issueTo) LIKE LOWER(CONCAT('%', :searchValue, '%'))
		            OR LOWER(pi.item) LIKE LOWER(CONCAT('%', :searchValue, '%'))
		          )
		      AND (
		            :status = 'A'
		            OR (:status = 'P' AND p.billNo IS NULL)
		            OR (:status = 'R' AND p.billNo IS NOT NULL)
		          )
		""")
		Page<PurchaseNonStock> searchPurchaseReceiptsNonStock(
		        @Param("startDate") LocalDate startDate,
		        @Param("endDate") LocalDate endDate,
		        @Param("category") String category,
		        @Param("searchValue") String searchValue,
		        @Param("status") String status,
		        Pageable pageable
		);
	
	@Query("""
		    SELECT DISTINCT p FROM PurchaseNonStock p
		    JOIN p.items pi
		    WHERE p.fileDate BETWEEN :startDate AND :endDate
		      AND (:category IS NULL OR pi.category = :category)
		      AND (
		            :status = 'A'
		            OR (:status = 'P' AND p.billNo IS NULL)
		            OR (:status = 'R' AND p.billNo IS NOT NULL)
		          )
		""")
    	List<PurchaseNonStock> getPurchasesNonStock(
    	        @Param("startDate") LocalDate startDate,
    	        @Param("endDate") LocalDate endDate,
    	        @Param("category") String category,
    	        @Param("status") String status
    	);
	
	@Query("""
		    SELECT DISTINCT p FROM PurchaseNonStock p
		    JOIN p.items pi
		    WHERE p.billDate BETWEEN :startDate AND :endDate
		      AND (:category IS NULL OR pi.category = :category)
		      AND (
		            :status = 'A'
		            OR (:status = 'P' AND p.billNo IS NULL)
		            OR (:status = 'R' AND p.billNo IS NOT NULL)
		          )
		""")
    	List<PurchaseNonStock> getPurchaseReceiptsNonStock(
    	        @Param("startDate") LocalDate startDate,
    	        @Param("endDate") LocalDate endDate,
    	        @Param("category") String category,
    	        @Param("status") String status
    	);

}
