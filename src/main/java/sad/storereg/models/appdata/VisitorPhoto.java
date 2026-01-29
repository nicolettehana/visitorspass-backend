package sad.storereg.models.appdata;

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
@Table(name = "visitor_photo", schema = "appdata")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitorPhoto {
	
	@Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "visitor_photo_id_seq"
    )
    @SequenceGenerator(
        name = "visitor_photo_id_seq",
        sequenceName = "appdata.visitor_photo_id_seq",
        allocationSize = 1
    )
    private Long id;

    private String path;

    private String extension;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visitor_id", nullable = false)
    private Visitor visitor;

}
