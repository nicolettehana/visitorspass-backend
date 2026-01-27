package sad.storereg.models.master;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "firms", schema = "master")
@Data
@ToString(exclude = "categories")
@EqualsAndHashCode(exclude = "categories")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Firm {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String firm;

    @Column(name = "entrydate")
    private LocalDateTime entryDate;

    @OneToMany(mappedBy = "firm", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FirmCategory> categories = new ArrayList<>();
    
    @Transient
    private List<YearRange> yearRanges = new ArrayList<>();

}
