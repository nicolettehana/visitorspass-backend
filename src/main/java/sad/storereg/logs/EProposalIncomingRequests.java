package sad.storereg.logs;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "e_proposal_incoming_requests", schema = "logs")
public class EProposalIncomingRequests {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "status")
    private String status;

    @Column(name = "user_code")
    private Long userCode;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "letter_no")
    private String letterNo;

    @Column(name = "memo_no")
    private String memoNo;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "user_department")
    private String userDepartment;

    @Column(name = "user_designation")
    private String userDesignation;
    
    @Column(name="user_office")
    private String userOffice;
    
    @Column(name = "entrydate")
    private LocalDateTime entrydate;
    
    @Column(name="type")
    private String type;
}
