package sad.storereg.models.appdata;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "visitors", schema = "appdata")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Visitor {
	
	@Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "visitors_id_seq"
    )
    @SequenceGenerator(
        name = "visitors_id_seq",
        sequenceName = "appdata.visitors_id_seq",
        allocationSize = 1
    )
    private Long id;
	
	@Column(name = "v_pass_no")
    private String vPassNo;

    private String name;

    @Column(name = "no_of_visitors")
    private Integer noOfVisitors;

    private String state;

    private String address;

    private String purpose;

    @Column(name = "purpose_details")
    private String purposeDetails;

    @Column(name = "mobile_no")
    private String mobileNo;

    private String email;

    @Column(name = "visit_date_time")
    private LocalDateTime visitDateTime;

    private LocalDateTime entrydate;


}
