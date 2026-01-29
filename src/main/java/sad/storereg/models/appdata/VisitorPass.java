package sad.storereg.models.appdata;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "visitor_pass", schema = "appdata")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitorPass {
	
	@Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "visitor_pass_id_seq"
    )
    @SequenceGenerator(
        name = "visitor_pass_id_seq",
        sequenceName = "appdata.visitor_pass_id_seq",
        allocationSize = 1
    )
    private Long id;

    private String path;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visitor_id", nullable = false)
    private Visitor visitor;

}
