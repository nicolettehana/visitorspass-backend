package sad.storereg.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import sad.storereg.services.appdata.CoreServices;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	
	@Autowired
	private final JwtService jwtService;
	
	@Autowired CoreServices coreServices;
	private final UserDetailsService userDetailsService;
	
	@Value("${config.quarters}")
	private String configQuarters;
	
	private final Map<String, Bucket> userBuckets = new ConcurrentHashMap<>();
	
	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
			throws ServletException, IOException {
		if (request.getMethod().equals("OPTIONS")) {
			response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		}
		try {
			
			if (request.getServletPath().contains("/auth")) {
				String apiKeyHeader = request.getHeader("API-Key");
				
				String apiKey = loadConfig();
				if (apiKeyHeader == null || !apiKeyHeader.equals(apiKey)) {
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API Key");
					return;
				}
				filterChain.doFilter(request, response);
				return;
			}
//			if (request.getServletPath().contains("/api")) {
//				String apiKeyHeader = request.getHeader("Access-Key");
//				
//				String apiKey = loadConfig2();
//				if (apiKeyHeader == null || !apiKeyHeader.equals(apiKey)) {
//					response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API Key");
//					return;
//				}
//				filterChain.doFilter(request, response);
//				return;
//			}
			
			final String authHeader = request.getHeader("Authorization");
			final String jwt;
			final String username; // Extract from the JWT Token

			if (authHeader == null || !authHeader.startsWith("Bearer ")) {
				filterChain.doFilter(request, response);
				return;
			}
			jwt = authHeader.substring(7);
			
			username = jwtService.extractUsername(jwt);

			if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

				UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

				if (jwtService.isTokenValid(jwt, userDetails, coreServices.getClientIp(request))) {
					UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
							null, userDetails.getAuthorities());
					authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authToken);
				}
				
				// --- RATE LIMITING HERE ---
                Bucket bucket = userBuckets.computeIfAbsent(username, this::newBucket);

                if (!bucket.tryConsume(1)) {
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.getWriter().write("Rate limit exceeded. Try again later.");
                    return;
                }
			}

			filterChain.doFilter(request, response);
			
		} catch (ExpiredJwtException exception) {
			response.resetBuffer();
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
			HashMap<String, String> mydata = new HashMap<String, String>();
			mydata.put("message", "JWT token has expired");
			response.getOutputStream().print(new ObjectMapper().writeValueAsString(mydata));
			response.flushBuffer();
		}
	}
	
	// --- Bucket factory method ---
    private Bucket newBucket(String username) {
        // Example: default 60 requests per minute per user
        Refill refill = Refill.intervally(60, Duration.ofMinutes(1));
        Bandwidth limit = Bandwidth.classic(60, refill);
        return Bucket.builder().addLimit(limit).build();
    }
	
	private String loadConfig() {
		try {
			Path path = Paths.get(configQuarters);
			Properties properties = new Properties();
			properties.load(Files.newBufferedReader(path));

			return properties.getProperty("API_KEY");

		} catch (IOException e) {
			e.printStackTrace(); // Handle the exception appropriately
		}
		return null;
	}
	
	private String loadConfig2() {
		try {
			Path path = Paths.get(configQuarters);
			Properties properties = new Properties();
			properties.load(Files.newBufferedReader(path));

			return properties.getProperty("Access_KEY");

		} catch (IOException e) {
			e.printStackTrace(); // Handle the exception appropriately
		}
		return null;
	}
}
