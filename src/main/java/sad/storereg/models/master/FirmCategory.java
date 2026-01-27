package sad.storereg.models.master;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "firm_category", schema = "master")
@Data
@ToString(exclude = "firm")
@EqualsAndHashCode(exclude = "categories")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FirmCategory {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "firm", nullable = false)
    private Firm firm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "category",
            referencedColumnName = "code",
            nullable = false
    )
    private Category category;

}
