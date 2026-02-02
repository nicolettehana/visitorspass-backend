package sad.storereg.models.master;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "offices", schema = "master")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Office {
	
	@Id
    @Column(name = "office_code", nullable = false)
    private Integer officeCode;

    @Column(name = "office_name", nullable = false, length = 255)
    private String officeName;

    @Column(name = "department_code", nullable = false)
    private Integer departmentCode;

}
