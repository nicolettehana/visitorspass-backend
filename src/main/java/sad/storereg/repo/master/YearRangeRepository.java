package sad.storereg.repo.master;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sad.storereg.models.master.YearRange;

public interface YearRangeRepository extends JpaRepository<YearRange, Integer>{
	
	Optional<YearRange> findByStartYearLessThanEqualAndEndYearGreaterThanEqual(int year1, int year2);

	List<YearRange> findAllByOrderByStartYearDesc();
	
	boolean existsByStartYearLessThanEqualAndEndYearGreaterThanEqual(
            Integer endYear,
            Integer startYear
    );

}
