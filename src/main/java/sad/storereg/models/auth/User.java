package sad.storereg.models.auth;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users", schema="auth")
public class User implements UserDetails{
	  
	@Id
	@GeneratedValue(strategy= GenerationType.AUTO)
	@JsonProperty(access = Access.WRITE_ONLY)
	private Long id;
	
	private String username;
	
	@Column(name="mobile_no")
	private String mobileNo;
	
	private String email;
	
	private String name;
	
	@JsonProperty(access = Access.WRITE_ONLY)
	private String password;
	
	private String department;
	
	private String designation;
	
	@Column(name = "is_enabled")
	private boolean isEnabled;
	
	@Column(name = "entrydate")
	private Timestamp entryDate;
	
	@Enumerated(EnumType.STRING)
	private Role role;
	
	@Column(name="office_code")
	private Integer officeCode;
	  
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// TODO Auto-generated method stub
		return List.of(new SimpleGrantedAuthority(role.name()));
	}
	
	@Override
	  public String getPassword() {
	    return password;
	  }

	  @Override
	  public String getUsername() {
	    return username;
	  }

	  @Override
	  public boolean isAccountNonExpired() {
	    return true;
	  }

	  @Override
	  public boolean isAccountNonLocked() {
	    return true;
	  }

	  @Override
	  public boolean isCredentialsNonExpired() {
	    return true;
	  }

	  @Override
	  public boolean isEnabled() {
	    return this.isEnabled;
	  }

}
