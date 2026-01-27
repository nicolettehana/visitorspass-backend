package sad.storereg.models.appdata;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
@Table(name = "purchase_non_stock", schema = "appdata")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseNonStock {

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "purchase_non_stock_seq")
    @SequenceGenerator(
            name = "purchase_non_stock_seq",
            sequenceName = "appdata.purchase_cash_memo_id_seq",
            allocationSize = 1
    )
    private Long id;

    @Column(name = "file_no", nullable = false)
    private String fileNo;

    @Column(name = "file_date", nullable = false)
    private LocalDate fileDate;

    @Column(name = "received_from", nullable = false)
    private String receivedFrom;

    @Column(name = "issue_to", nullable = false)
    private String issueTo;
    
    private String remarks;

    private LocalDateTime entrydate;

    @Column(name = "bill_no")
    private String billNo;

    @Column(name = "bill_date")
    private LocalDate billDate;

    private Double total;

    @Column(name = "receipt_date")
    private LocalDate receiptDate;
    
    @OneToMany(
            mappedBy = "purchase",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<PurchaseItemNonStock> items;
}
