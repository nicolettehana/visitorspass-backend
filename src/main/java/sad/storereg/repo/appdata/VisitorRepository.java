package sad.storereg.repo.appdata;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import sad.storereg.models.appdata.Visitor;

public interface VisitorRepository extends JpaRepository<Visitor, Long>{
	
	Page<Visitor> findByVisitDateTimeBetween(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );
	
	@Query("""
	        SELECT v
	        FROM Visitor v
	        WHERE v.visitDateTime BETWEEN :startDateTime AND :endDateTime
	          AND (
	                :search IS NULL
	                OR LOWER(v.name) LIKE LOWER(CONCAT('%', :search, '%'))
	                OR LOWER(v.mobileNo) LIKE LOWER(CONCAT('%', :search, '%'))
	                OR LOWER(v.address) LIKE LOWER(CONCAT('%', :search, '%'))
	          )
	    """)
	    Page<Visitor> searchVisitorsBetweenDates(
	            @Param("startDateTime") LocalDateTime startDateTime,
	            @Param("endDateTime") LocalDateTime endDateTime,
	            @Param("search") String search,
	            Pageable pageable
	    );
	
	List<Visitor> findByVisitDateTimeBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);

}
