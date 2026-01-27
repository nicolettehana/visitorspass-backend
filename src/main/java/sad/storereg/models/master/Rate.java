package sad.storereg.models.master;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
import lombok.ToString;

@Data
@Entity
@Table(name = "rates", schema = "master")
@RequiredArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@ToString(exclude = {"subItem"})
public class Rate {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;


	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "year_range_id", nullable = false)
	private YearRange yearRange;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "item_id")
	private Item item;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_item_id")
	private SubItems subItem;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "unit", nullable = false)
	private Unit unit;


	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category", referencedColumnName = "code", nullable = false)
	private Category category;


	@Column(nullable = false)
	private Double rate;

	@JsonIgnore
	@Column(name = "entrydate")
	private LocalDateTime entryDate;

}
