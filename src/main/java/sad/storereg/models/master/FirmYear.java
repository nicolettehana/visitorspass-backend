package sad.storereg.models.master;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "firm_year", schema = "master")
@RequiredArgsConstructor
@Data
public class FirmYear {

	 	@Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "firm_id", nullable = false)
	    private Firm firm;

	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "year_range_id", nullable = false)
	    private YearRange yearRange;

	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "category_code", referencedColumnName = "code", nullable = false)
	    private Category category;
}
