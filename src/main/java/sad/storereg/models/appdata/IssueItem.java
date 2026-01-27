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
import lombok.RequiredArgsConstructor;
import sad.storereg.models.master.Item;
import sad.storereg.models.master.SubItems;
import sad.storereg.models.master.Unit;

@Data
@RequiredArgsConstructor
@Entity
@Table(name = "issue_items", schema = "appdata")
public class IssueItem {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //@Column(name = "issue_id", nullable = false)
    //private Long issueId;

    @Column(name = "category_code", nullable = false)
    private String categoryCode;

    // Item
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    // Sub Item (nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_item_id")
    private SubItems subItem;

    @Column(nullable = false)
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;
}
