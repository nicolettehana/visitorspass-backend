package sad.storereg.logs;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import sad.storereg.dto.auth.AuthenticationRequest;
import sad.storereg.dto.auth.ChangePasswordRequest;
import sad.storereg.dto.auth.RegisterRequest;
import sad.storereg.dto.auth.ResetPasswordRequest;
import sad.storereg.services.appdata.CoreServices;
import sad.storereg.services.auth.AuthenticationService;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {
	
	private final AuditService auditService;
	private final CoreServices coreServices;
	private final AuthenticationService authService;
	
	@Around("@annotation(sad.storereg.annotations.Auditable)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable{
        // Capture details of the HTTP request
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        HttpServletResponse response = attributes != null ? attributes.getResponse() : null;
        
        String action = joinPoint.getSignature().getName();
        String username = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "Anonymous";
        //Long userCode = request.getUserPrincipal() != null ? request.getUserPrincipal().get : "Anonymous";
        String uri = (request != null) ? request.getRequestURI() : "Unknown URI";
        String clientIp = (request != null) ? coreServices.getClientIp(request) : "Unknown IP";

        Object[] args = joinPoint.getArgs();
        String description = extractDetails(args);
        if(uri.equals("/quarters/auth/authenticate")) {
        	username = authService.decryptPassword(description);
        	description = "Authenticate";
        }
        if(uri.equals("/quarters/auth/logout")) {
        	description = "User logged out ";
        }
        
        Object result;
        int status=-1;
        
        try {
            // Proceed with the original method call
            result = joinPoint.proceed();
            if (response != null) {
                status = response.getStatus();  // Capture response status if available
            }
            
        } catch (Exception ex) {
            // If an exception occurs, assume status 500
            status = 500;
            auditService.saveAuditTrail(action, username, uri, description, "Failed", clientIp, status);
            throw ex; // Rethrow the exception so that it's not swallowed
        }
        
        auditService.saveAuditTrail(action, username, uri, description,"Success",clientIp, status);
        return result;
    }

    private String extractDetails(Object[] args) {
        // This is where you can parse the method arguments to log specific details
    	if (args != null && args.length > 0 && args[0] instanceof AuthenticationRequest) {
            AuthenticationRequest authRequest = (AuthenticationRequest) args[0];
            return authRequest.getUsername(); // Extract only the username
        }
    	if (args != null && args.length > 0 && args[0] instanceof RegisterRequest) {
            RegisterRequest regRequest = (RegisterRequest) args[0];
            return regRequest.getUsername(); // Extract only the username
        }
    	if (args != null && args.length > 0 && args[0] instanceof ChangePasswordRequest) {
            ChangePasswordRequest changeRequest = (ChangePasswordRequest) args[0];
            return changeRequest.getUsername(); // Extract only the username
        }
    	if (args != null && args.length > 0 && args[0] instanceof ResetPasswordRequest) {
            ResetPasswordRequest request = (ResetPasswordRequest) args[0];
            return request.getUsername(); // Extract only the username
        }
        return args != null && args.length > 0 ? args[0].toString() : "No details available";
    }

}
