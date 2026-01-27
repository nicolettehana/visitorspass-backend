package sad.storereg.models.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Permission {
	
	ADMIN_READ("admin:read"),
    ADMIN_UPDATE("admin:update"),
    ADMIN_CREATE("admin:create"),
    ADMIN_DELETE("admin:delete"),
    GAD_READ("gad:read"),
    GAD_UPDATE("gad:update"),
    GAD_CREATE("gad:create"),
    GAD_DELETE("gad:delete"),
    USER_READ("user:read"),
    USER_UPDATE("user:update"),
    USER_CREATE("user:create"),
    USER_DELETE("user:delete"),
    CH_READ("chm:read"),
    CH_UPDATE("chm:update"),
    CH_CREATE("chm:create"),
    CH_DELETE("chm:delete"),
    CS_READ("chm:read"),
    CS_UPDATE("chm:update"),
    CS_CREATE("chm:create"),
    CS_DELETE("chm:delete"),
    EST_READ("chm:read"),
    EST_UPDATE("chm:update"),
    EST_CREATE("chm:create"),
    EST_DELETE("chm:delete"),
    ;
	
	@Getter
    private final String permission;

}
