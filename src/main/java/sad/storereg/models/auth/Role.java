package sad.storereg.models.auth;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Role {
	
	USER(Collections.emptySet()),
	ADMIN(Collections.emptySet()),
	SAD(Collections.emptySet()),
	PUR(Collections.emptySet())	,
	ISS(Collections.emptySet())	
	;

	  @Getter
	  private final Set<Permission> permissions;

	  public List<SimpleGrantedAuthority> getAuthorities() {
	    var authorities = getPermissions()
	            .stream()
	            .map(permission -> new SimpleGrantedAuthority(permission.getPermission()))
	            .collect(Collectors.toList());
	    authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));
	    return authorities;
	  }

}
