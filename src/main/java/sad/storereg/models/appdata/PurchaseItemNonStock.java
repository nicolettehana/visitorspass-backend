package sad.storereg.models.appdata;

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
@Table(name = "purchase_items_non_stock", schema = "appdata")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseItemNonStock {
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "purchase_items_non_stock_seq")
    @SequenceGenerator(
            name = "purchase_items_non_stock_seq",
            sequenceName = "appdata.purchase_items_cash_memo_id_seq",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id", nullable = false)
    private PurchaseNonStock purchase;

    @Column(nullable = false)
    private String item;

    @Column(nullable = false)
    private String unit;

    @Column(nullable = false)
    private Double rate;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "gstPercentage")
    private Double gstPercentage;

    private Double cgst;
    
    private Double sgst;
    
    private Double amount;

    private String category;

}
