package sad.storereg.services.master;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sad.storereg.exception.UnauthorizedException;
import sad.storereg.models.master.YearRange;
import sad.storereg.repo.master.YearRangeRepository;

@RequiredArgsConstructor
@Service
public class YearRangeService {

	private final YearRangeRepository yearRangeRepository;

    public List<YearRange> getAllYearRanges() {
    	try {
    		return yearRangeRepository.findAllByOrderByStartYearDesc();
    	}catch(Exception ex) {
    		throw ex;
    	}
    }
    
    public String createYearRange(YearRange request) {
    	Integer newStart = request.getStartYear();
        Integer newEnd = request.getEndYear();

        if (newStart > newEnd) {
            throw new IllegalArgumentException("Start year cannot be greater than end year");
        }

        boolean overlapExists =
                yearRangeRepository.existsByStartYearLessThanEqualAndEndYearGreaterThanEqual(
                        newEnd,
                        newStart
                );

        if (overlapExists) {
            throw new UnauthorizedException("Year range overlaps with an existing range");
        }
        
    	YearRange yearRange  = new YearRange();
    	yearRange.setStartYear(request.getStartYear());
    	yearRange.setEndYear(request.getEndYear());
    	yearRange.setEntryDate(LocalDateTime.now());
    	yearRangeRepository.save(yearRange);
    	return "Added successfully";
    }
}
