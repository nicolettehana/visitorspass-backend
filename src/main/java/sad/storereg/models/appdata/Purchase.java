package sad.storereg.models.appdata;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import sad.storereg.models.master.Firm;

@Data
@Entity
@Table(name = "purchase", schema = "appdata")
public class Purchase {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "firm_id", nullable = false)
    private Firm firm;

    @Column(name = "entrydate")
    private LocalDateTime entryDate;
    
    @Column(name = "file_no")
    private String fileNo;
    
    @Column(name = "remarks")
    private String remarks;
    
    @Column(name = "total_cost")
    private Double totalCost;
    
    @Column(name = "bill_no")
    private String billNo;
    
    @Column(name = "bill_date", nullable = false)
    private LocalDate billDate;
    
    @Column(name = "gst_percentage")
    private Double gstPercentage;
    
    @Column(name = "receipt_entrydate")
    private LocalDateTime receiptEntryDate;
    
    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseItems> items = new ArrayList<>();

}
