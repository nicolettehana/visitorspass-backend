package sad.storereg.models.master;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

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
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "items", schema = "master")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"subItems"})
public class Item {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @JsonIgnore
    @Column(name = "entrydate")
    private LocalDateTime entryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "category",          // column in items table
            referencedColumnName = "code", // PK of category table
            nullable = false
    )
    private Category category;
    
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<SubItems> subItems = new ArrayList<>();
    
    @Transient
    private String unit;
    
    @Transient
    private Double rate;
    
    @Transient
    private String balance;

}
