package sad.storereg.logs;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import sad.storereg.models.auth.User;

@RestController
@RequiredArgsConstructor
public class LogController {
	
	private final AuditTrailRepository auditTrailRepo;
	
	@GetMapping(path="/audit-trail", params = { "page", "size", "fromDate", "toDate"})
	public Iterable<AuditTrail> get(@RequestParam("fromDate") final LocalDate fromDate, @RequestParam("toDate") final LocalDate toDate ,@RequestParam("page") final int page, @RequestParam("size") final int size, HttpServletRequest request, @AuthenticationPrincipal User user) {

		PageRequest pageable = PageRequest.of(page, size, Direction.fromString("DESC"), "id");
		LocalDateTime startDateTime = fromDate.atStartOfDay();

	    LocalDateTime endDateTime = toDate.atTime(LocalTime.MAX); 
		Page<AuditTrail> pagedLogs = auditTrailRepo.findByTimestampBetween(startDateTime, endDateTime, pageable);
				//.findByEventDateBetweenAndMessageNotAndEventDateBetweenAndMessageNot(fromDate, toDate.plusDays(1), "Login", fromDate, toDate.plusDays(1), "Logout", pageable);
		return pagedLogs;
	}

}
