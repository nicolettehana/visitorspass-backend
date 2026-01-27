package sad.storereg.services.auth;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class CookieService {

	public ResponseCookie getCookie(String cookieName, String cookie, long maxAge) {
		return ResponseCookie.from(cookieName, cookie)
                .httpOnly(false)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofMillis(maxAge).getSeconds())
                .sameSite("None")
                .build();
	}
}
