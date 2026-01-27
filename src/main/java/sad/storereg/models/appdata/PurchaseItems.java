package sad.storereg.models.appdata;

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
import sad.storereg.models.master.Item;
import sad.storereg.models.master.SubItems;
import sad.storereg.models.master.Unit;

@Data
@Entity
@Table(name = "purchase_items", schema = "appdata")
public class PurchaseItems {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Purchase
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    // Item
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    // Sub Item (nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_item_id")
    private SubItems subItem;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "rate", nullable = false)
    private Double rate;

    @Column(name = "amount", nullable = false)
    private Double amount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;
    
    @Column(name = "gst_percentage")
    private Double gstPercentage;
    
    @Column(name = "cgst")
    private Double cgst;
    
    @Column(name = "sgst")
    private Double sgst;

}
