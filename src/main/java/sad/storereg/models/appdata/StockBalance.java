package sad.storereg.models.appdata;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import sad.storereg.models.master.Item;
import sad.storereg.models.master.SubItems;
import jakarta.persistence.ForeignKey;


@Entity
@Table(
    name = "stock_balance",
    schema = "appdata",
    uniqueConstraints = {
        @UniqueConstraint(name = "ukey_items_date", columnNames = {"item_id", "sub_item_id", "date"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
public class StockBalance {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

	@NonNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "item_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fkey_item_id")
    )
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "sub_item_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fkey_sub_item_id")
    )
    private SubItems subItem;

    @NonNull
    @Column(nullable = false)
    private LocalDate date;

    @NonNull
    @Column(nullable = false)
    private Integer balance;

    @Column(name = "entrydate")
    private LocalDateTime entryDate = LocalDateTime.now();

}
