package sad.storereg.models.master;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import sad.storereg.models.auth.Roles;

@Entity
@Table(name = "menu", schema = "master")
@Data
public class Menu {
	
	@Id
	@Column(name = "menu_code", unique = true)
	private Integer menuCode;
	
	private String label;
	
	private String url;
	
	@Column(name = "sort_order")
	private Integer sortOrder;
	
	@Column(name = "parent_code")
	private Integer parentCode;

	private String icon;
	
	@ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "menu_roles",
        schema = "master",
        joinColumns = @JoinColumn(name = "menu_code", referencedColumnName = "menu_code"), 
        inverseJoinColumns = @JoinColumn(name = "role", referencedColumnName = "role")
    )
	@ToString.Exclude  
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private Set<Roles> roles;

}
