package sad.storereg.services.auth;

import java.io.IOException;
import java.util.Date;

import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Service;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sad.storereg.annotations.Auditable;
import sad.storereg.config.JwtService;
import sad.storereg.exception.ObjectNotFoundException;
import sad.storereg.logs.AuditService;
import sad.storereg.logs.Login;
import sad.storereg.logs.LoginRepository;
import sad.storereg.repo.auth.UserRepository;
import sad.storereg.services.appdata.CoreServices;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutSuccessHandler{
	
	private final JwtService jwtService;
	private final LoginRepository loginRepo;
	private final CoreServices coreServices;
	private final CookieService cookieService;
	private final AuditService auditService;
	
	//@Auditable
	@Override
	public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) 
			throws IOException, ServletException{
		//final String accessToken = extractTokenFromCookie(request, "access_token");
		//final String refreshToken = extractTokenFromCookie(request, "refresh_token");
		final String authHeader = request.getHeader("Authorization");

		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			
			String username = jwtService.extractUsername(authHeader.substring(7));
			jwtService.invalidateToken(username, coreServices.getClientIp(request));
			SecurityContextHolder.clearContext();
			
			String clientIp = coreServices.getClientIp(request);
			
			auditService.saveAuditTrail(
	                "logout",
	                username,
	                request.getRequestURI(),
	                "User logged out",
	                "Success",
	                clientIp,
	                200
	            );
	
			Login login = Login.builder().username(username).uri(request.getRequestURI()).httpMethod(request.getMethod())
					.ts(new Date()).httpStatus(response.getStatus()).build();
			loginRepo.save(login);
			MDC.put("username", username);
			log.info("Logout");
			MDC.remove(username);
		}
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().flush();
		
		
		
		
		//if (accessToken != null) {
		//	String username = jwtService.extractUsername(accessToken);
		//    jwtService.invalidateToken(username, coreServices.getClientIp(request));
		//    jwtService.invalidateRefreshToken(refreshToken);
		//    SecurityContextHolder.clearContext();
		    
		  //  Login login = Login.builder().username(username).uri(request.getRequestURI()).httpMethod(request.getMethod())
		//			.ts(new Date()).httpStatus(response.getStatus()).build();
		//	loginRepo.save(login);
		//	MDC.put("username", username);
		//	log.info("Logout");
		//	MDC.remove(username);
		}
		
//		ResponseCookie clearAccessToken = ResponseCookie.from("access_token", "")
//	            .httpOnly(true)
//	            .secure(true)
//	            .path("/")
//	            .sameSite("None")
//	            .maxAge(0)
//	            .build();
//		ResponseCookie clearAccessToken = cookieService.getCookie("access_token","",0);

//	    ResponseCookie clearRefreshToken = ResponseCookie.from("refresh_token", "")
//	            .httpOnly(true)
//	            .secure(true)
//	            .path("/")
//	            .sameSite("None")
//	            .maxAge(0)
//	            .build();
//		ResponseCookie clearRefreshToken = cookieService.getCookie("refresh_token","",0);

//	    response.addHeader(HttpHeaders.SET_COOKIE, clearAccessToken.toString());
//	    response.addHeader(HttpHeaders.SET_COOKIE, clearRefreshToken.toString());
//		response.setStatus(HttpServletResponse.SC_OK);
		//response.getWriter().flush();
//	}
	
	private String extractTokenFromCookie(HttpServletRequest request, String cookieName) {
	    if (request.getCookies() != null) {
	        for (Cookie cookie : request.getCookies()) {
	            if (cookieName.equals(cookie.getName())) {
	                return cookie.getValue();
	            }
	        }
	    }
	    return null;
	}
}
