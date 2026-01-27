package sad.storereg.controller.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.web.csrf.CsrfToken;
import jakarta.servlet.http.HttpServletRequest;

@RestController
public class SecurityController {
	
	@GetMapping("/csrf-token")
    public CsrfToken csrfToken(HttpServletRequest request) {
		return (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    }

}
