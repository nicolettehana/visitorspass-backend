package sad.storereg.models.auth;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import sad.storereg.models.master.Menu;

@Entity
@Table(name = "roles", schema = "auth")
@Data
public class Roles {
	
	@Id
	@GeneratedValue(strategy= GenerationType.AUTO)
	private Integer id;
	
	@Column(name = "role", unique = true)
	private String role;
	
	private String description;
	
	@ManyToMany(mappedBy = "roles")
	@ToString.Exclude   // Prevent circular reference
    @EqualsAndHashCode.Exclude  // Exclude from equals/hashCode to avoid issues
    @JsonBackReference
    private Set<Menu> menus;

}
