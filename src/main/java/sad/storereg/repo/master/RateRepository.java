package sad.storereg.repo.master;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import sad.storereg.models.master.Item;
import sad.storereg.models.master.Rate;
import sad.storereg.models.master.SubItems;
import sad.storereg.models.master.YearRange;

public interface RateRepository extends JpaRepository<Rate, Long>{
	
	Page<Rate> findByYearRange_Id(Integer yearRangeId, Pageable pageable);
	Page<Rate> findByCategory_Code(String code, Pageable pageable);
	Page<Rate> findByCategory_CodeAndYearRange_Id(String code, Integer yearRangeId, Pageable pageable);
	//Optional<Rate> findByObjectTypeAndObjectIdAndYearRange_Id(String objectType, Long objectId, Integer yearRangeId);
	Optional<Rate> findByItem_IdAndSubItemIsNullAndYearRange_IdAndUnit_Id(
	        Long itemId,
	        Integer yearRangeId, Integer unitId
	);
	Optional<Rate> findByItem_IdAndSubItem_IdAndYearRange_Id(
	        Long itemId, Long subItemId,
	        Integer yearRangeId
	);
	Optional<Rate> findByItem_IdAndSubItem_IdAndYearRange_IdAndUnit_Id(
	        Long itemId, Long subItemId,
	        Integer yearRangeId, Integer unitId
	);
	
	@Query("SELECT r FROM Rate r WHERE r.item.id = :itemId " +
		       "AND ((:subItemId IS NULL AND r.subItem IS NULL) OR r.subItem.id = :subItemId) " +
		       "AND r.yearRange.id = :yearRangeId")
		List<Rate> findRatesByItemAndOptionalSubItem(
		        @Param("itemId") Long itemId,
		        @Param("subItemId") Long subItemId,
		        @Param("yearRangeId") Integer yearRangeId);

	@Query("SELECT r FROM Rate r WHERE r.item.id = :itemId " +
		       "AND ((:subItemId IS NULL AND r.subItem IS NULL) OR r.subItem.id = :subItemId) "
		       )
		List<Rate> findRatesByItemAndOptionalSubItemm(
		        @Param("itemId") Long itemId,
		        @Param("subItemId") Long subItemId
		        );

	
	List<Rate> findByYearRange_Id(Integer yearRangeId);
	
	@Query("SELECT r FROM Rate r WHERE r.item.id = :itemId " +
		       "AND ((:subItemId IS NULL AND r.subItem IS NULL) OR r.subItem.id = :subItemId) " +
		       "AND r.yearRange.id = :yearRangeId AND r.unit.id = :unitId")
		Optional<Rate> findRates(
		        @Param("itemId") Long itemId,
		        @Param("subItemId") Long subItemId,
		        @Param("yearRangeId") Integer yearRangeId,
		        @Param("unitId") Integer unitId);
	
	@Query("""
		    select distinct r.item.id
		    from Rate r
		    where (:yearRangeId is null or r.yearRange.id = :yearRangeId)
		      and (:categoryCode is null or r.category.code = :categoryCode)
		    order by r.item.id
		""")
		Page<Long> findDistinctItemIds(
		    @Param("yearRangeId") Integer yearRangeId,
		    @Param("categoryCode") String categoryCode,
		    Pageable pageable
		);
	
	List<Rate> findByYearRange_IdAndItem_IdIn(
		    Integer yearRangeId,
		    List<Long> itemIds,
		    Sort sort
		);

	List<Rate> findByItemAndYearRange(Item item, YearRange yearRange);

    List<Rate> findBySubItemAndYearRange(SubItems subItem, YearRange yearRange);
    
    List<Rate> findByItem(Item item);

    List<Rate> findBySubItem(SubItems subItem);
}
