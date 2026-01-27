package sad.storereg.repo.appdata;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import sad.storereg.models.appdata.Purchase;

public interface PurchaseRepository extends JpaRepository<Purchase, Long>{
	
	@Query("""
	        SELECT DISTINCT p 
	        FROM Purchase p
	        JOIN FETCH p.firm f
	        JOIN FETCH p.items pi
	        JOIN FETCH pi.item i
	        JOIN FETCH i.category c
	        LEFT JOIN FETCH pi.subItem si
	        WHERE p.date BETWEEN :startDate AND :endDate
	        AND (:category IS NULL OR c.code = :category)
	        AND (
	            :searchValue IS NULL 
	            OR LOWER(f.firm) LIKE LOWER(CONCAT('%', :searchValue, '%'))
	            OR LOWER(i.name) LIKE LOWER(CONCAT('%', :searchValue, '%'))
	            OR LOWER(si.name) LIKE LOWER(CONCAT('%', :searchValue, '%'))
	        )
	    """)
	    List<Purchase> searchPurchasesList(LocalDate startDate,
	                                   LocalDate endDate,
	                                   String category,
	                                   String searchValue);
	
	@EntityGraph(attributePaths = {
            "firm",
            "items",
            "items.item",
            "items.item.category",
            "items.subItem"
    })
	
	@Query("""
		    SELECT p FROM Purchase p
		    JOIN p.firm f
		    JOIN p.items pi
		    JOIN pi.item i
		    WHERE p.date BETWEEN :startDate AND :endDate
		      AND (:category IS NULL OR i.category.code = :category)
		      AND (
		            :searchValue IS NULL
		            OR LOWER(f.firm) LIKE LOWER(CONCAT('%', :searchValue, '%'))
		            OR LOWER(i.name) LIKE LOWER(CONCAT('%', :searchValue, '%'))
		          )
		      AND (
		            :status = 'A'
		            OR (:status = 'P' AND p.billNo IS NULL)
		            OR (:status = 'R' AND p.billNo IS NOT NULL)
		          )
		""")
		Page<Purchase> searchPurchases(
		        @Param("startDate") LocalDate startDate,
		        @Param("endDate") LocalDate endDate,
		        @Param("category") String category,
		        @Param("searchValue") String searchValue,
		        @Param("status") String status,
		        Pageable pageable
		);
	
	@Query("""
		    SELECT p FROM Purchase p
		    JOIN p.firm f
		    JOIN p.items pi
		    JOIN pi.item i
		    WHERE p.billDate BETWEEN :startDate AND :endDate
		      AND (:category IS NULL OR i.category.code = :category)
		      AND (
		            :searchValue IS NULL
		            OR LOWER(f.firm) LIKE LOWER(CONCAT('%', :searchValue, '%'))
		            OR LOWER(i.name) LIKE LOWER(CONCAT('%', :searchValue, '%'))
		          )
		      AND (
		            :status = 'A'
		            OR (:status = 'P' AND p.billNo IS NULL)
		            OR (:status = 'R' AND p.billNo IS NOT NULL)
		          )
		""")
		Page<Purchase> searchPurchaseReceipts(
		        @Param("startDate") LocalDate startDate,
		        @Param("endDate") LocalDate endDate,
		        @Param("category") String category,
		        @Param("searchValue") String searchValue,
		        @Param("status") String status,
		        Pageable pageable
		);

//    @Query("""
//        SELECT p FROM Purchase p
//        JOIN p.firm f
//        JOIN p.items pi
//        JOIN pi.item i
//        WHERE p.date BETWEEN :startDate AND :endDate
//        AND (:category IS NULL OR i.category.code = :category)
//        AND (
//            :searchValue IS NULL
//            OR LOWER(f.firm) LIKE LOWER(CONCAT('%', :searchValue, '%'))
//            OR LOWER(i.name) LIKE LOWER(CONCAT('%', :searchValue, '%'))
//        )
//    """)
//    Page<Purchase> searchPurchases(
//            LocalDate startDate,
//            LocalDate endDate,
//            String category,
//            String searchValue,
//            Pageable pageable
//    );
	
	@Query(
	        value = """
	        SELECT 
	            COALESCE((
	                SELECT SUM(pi.quantity)
	                FROM appdata.purchase_items pi
	                JOIN appdata.purchase p ON p.id = pi.purchase_id
	                WHERE pi.item_id = :itemId
	                  AND COALESCE(pi.sub_item_id, -1) = COALESCE(:subItemId, -1)
	                  AND pi.unit_id = :unitId
	                  AND p.date <= :issueDate
	            ), 0)
	            -
	            COALESCE((
	                SELECT SUM(ii.quantity)
	                FROM appdata.issue_items ii
	                JOIN appdata.issue i ON i.id = ii.issue_id
	                WHERE ii.item_id = :itemId
	                  AND COALESCE(ii.sub_item_id, -1) = COALESCE(:subItemId, -1)
	                  AND ii.unit_id = :unitId
	                  AND i.date <= :issueDate
	            ), 0)
	        AS availableStock
	        """,
	        nativeQuery = true
	    )
	    Integer getAvailableStock(
	        @Param("itemId") Long itemId,
	        @Param("subItemId") Long subItemId,
	        @Param("unitId") Integer unitId,
	        @Param("issueDate") LocalDate issueDate
	    );
	
	@Query(
		    value = """
		        SELECT 
		            u.id AS unitId,
		            u.name AS unitName,
		            COALESCE((
		                SELECT SUM(pi.quantity)
		                FROM appdata.purchase_items pi
		                JOIN appdata.purchase p ON p.id = pi.purchase_id
		                WHERE pi.item_id = :itemId
		                  AND COALESCE(pi.sub_item_id, -1) = COALESCE(:subItemId, -1)
		                  AND pi.unit_id = u.id
		                  AND p.date <= :issueDate
		            ), 0)
		            -
		            COALESCE((
		                SELECT SUM(ii.quantity)
		                FROM appdata.issue_items ii
		                JOIN appdata.issue i ON i.id = ii.issue_id
		                WHERE ii.item_id = :itemId
		                  AND COALESCE(ii.sub_item_id, -1) = COALESCE(:subItemId, -1)
		                  AND ii.unit_id = u.id
		                  AND i.date <= :issueDate
		            ), 0)
		        AS availableStock
		        FROM master.units u
		        WHERE u.id IN (
		            SELECT DISTINCT unit_id 
		            FROM appdata.purchase_items 
		            WHERE item_id = :itemId AND COALESCE(sub_item_id, -1) = COALESCE(:subItemId, -1)
		            UNION
		            SELECT DISTINCT unit_id
		            FROM appdata.issue_items 
		            WHERE item_id = :itemId AND COALESCE(sub_item_id, -1) = COALESCE(:subItemId, -1)
		        )
		        ORDER BY u.id
		        """,
		    nativeQuery = true
		)
		List<Object[]> getAvailableStockForAllUnits(
		    @Param("itemId") Long itemId,
		    @Param("subItemId") Long subItemId,
		    @Param("issueDate") LocalDate issueDate
		);
		
		@Query("""
			    SELECT pi.unit.id, SUM(pi.quantity)
			    FROM PurchaseItems pi
			    JOIN pi.purchase p
			    WHERE pi.item.id = :itemId
			      AND (:subItemId IS NULL OR pi.subItem.id = :subItemId)
			      AND p.date > :fromDate AND p.date <= :toDate
			    GROUP BY pi.unit.id
			""")
			List<Object[]> getPurchasedAfter(
			        @Param("itemId") Long itemId,
			        @Param("subItemId") Long subItemId,
			        @Param("fromDate") LocalDate fromDate,
			        @Param("toDate") LocalDate toDate
			);

			
			@Query("""
				    SELECT COALESCE(SUM(pi.quantity), 0)
				    FROM PurchaseItems pi
				    JOIN pi.purchase p
				    WHERE pi.item.id = :itemId
				      AND (:subItemId IS NULL OR pi.subItem.id = :subItemId)
				      AND p.date > :fromDate
				      AND p.date <= :toDate
				""")
				int sumPurchasedAfter(Long itemId, Long subItemId, LocalDate fromDate, LocalDate toDate);
			
			@Query("""
				    SELECT pi.item.category.name,
				           pi.item.category.code,
				           SUM(pi.amount)
				    FROM PurchaseItems pi
				    WHERE pi.purchase.date BETWEEN :fromDate AND :toDate
				    GROUP BY pi.item.category.name, pi.item.category.code
				""")
				List<Object[]> getCategoryTotals(
				        @Param("fromDate") LocalDate fromDate,
				        @Param("toDate") LocalDate toDate);


			@Query("""
			        SELECT COALESCE(SUM(pi.quantity),0)
			        FROM PurchaseItems pi
			        WHERE pi.item.id = :itemId
			          AND ((:subItemId IS NULL AND pi.subItem IS NULL) OR pi.subItem.id = :subItemId)
			          AND pi.unit.id = :unitId
			          AND pi.purchase.date BETWEEN :startDate AND :endDate
			    """)
			    Integer sumQtyByItemSubItemUnitBetween(@Param("itemId") Long itemId,
			                                           @Param("subItemId") Long subItemId,
			                                           @Param("unitId") Integer unitId,
			                                           @Param("startDate") LocalDate startDate,
			                                           @Param("endDate") LocalDate endDate);

			    @Query("""
			        SELECT COALESCE(SUM(pi.quantity),0)
			        FROM PurchaseItems pi
			        WHERE pi.item.id = :itemId
			          AND ((:subItemId IS NULL AND pi.subItem IS NULL) OR pi.subItem.id = :subItemId)
			          AND pi.unit.id = :unitId
			          AND pi.purchase.date <= :endDate
			    """)
			    Integer sumQtyByItemSubItemUnitUntil(@Param("itemId") Long itemId,
			                                         @Param("subItemId") Long subItemId,
			                                         @Param("unitId") Integer unitId,
			                                         @Param("endDate") LocalDate endDate);
			    
			    @Query("""
			            SELECT DISTINCT p
			            FROM Purchase p
			            JOIN p.items pi
			            JOIN pi.item i
			            WHERE p.firm.id = :firmId
			              AND p.date BETWEEN :startDate AND :endDate
			              AND i.category.code = :categoryCode
			        """)
			        List<Purchase> findPurchasesByFirmDateRangeAndCategory(
			                @Param("firmId") Long firmId,
			                @Param("startDate") LocalDate startDate,
			                @Param("endDate") LocalDate endDate,
			                @Param("categoryCode") String categoryCode
			        );
			    
			    @Query("""
			    	    SELECT DISTINCT p
			    	    FROM Purchase p
			    	    JOIN FETCH p.items pi
			    	    JOIN FETCH pi.item i
			    	    WHERE p.date BETWEEN :startDate AND :endDate
			    	      AND (:category IS NULL OR i.category.code = :category)
			    	      AND (
            :status = 'A'
            OR (:status = 'P' AND p.billNo IS NULL)
            OR (:status = 'R' AND p.billNo IS NOT NULL)
          )
			    	""")
			    	List<Purchase> getPurchases(
			    	        @Param("startDate") LocalDate startDate,
			    	        @Param("endDate") LocalDate endDate,
			    	        @Param("category") String category,
			    	        @Param("status") String status
			    	);
			    
			    @Query("""
			    	    SELECT DISTINCT p
			    	    FROM Purchase p
			    	    JOIN FETCH p.items pi
			    	    JOIN FETCH pi.item i
			    	    WHERE p.billDate BETWEEN :startDate AND :endDate
			    	      AND (:category IS NULL OR i.category.code = :category)
			    	      AND (
            :status = 'A'
            OR (:status = 'P' AND p.billNo IS NULL)
            OR (:status = 'R' AND p.billNo IS NOT NULL)
          )
			    	""")
			    	List<Purchase> getPurchaseReceipts(
			    	        @Param("startDate") LocalDate startDate,
			    	        @Param("endDate") LocalDate endDate,
			    	        @Param("category") String category,
			    	        @Param("status") String status
			    	);

			    Optional<Purchase> findById(Long purchaseId);

}
