package sad.storereg.config;

import java.security.Key;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import sad.storereg.logs.AuditService;
import sad.storereg.logs.AuditTrail;
import sad.storereg.models.auth.Invalidate;
import sad.storereg.repo.auth.InvalidateRepository;

@RequiredArgsConstructor
@Service
public class JwtService {
	
	// Can Keep this in application properties file
	// private static final String SECRET_KEY =
	// "3777217A25432A46294A404E635266556A586E3272357538782F413F4428472B";
	@Value("${application.security.jwt.secret-key}")
	private String secretKey;
	@Value("${application.security.jwt.expiration}")
	private long jwtExpiration;
	@Value("${application.security.jwt.refresh-token.expiration}")
	private long refreshExpiration;
	private final InvalidateRepository invalidateRepo;
	private final AuditService auditService;

	//public JwtService(InvalidateRepository invalidateRepository) {
	//	this.invalidateRepo = invalidateRepository;
	//}
	
	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}
	
	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {

		final Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);

	}
	
	// Function to help us generate a token from userDetails only
	public String generateToken(UserDetails userDetails) {
		return generateToken(new HashMap<>(), userDetails);
	}

	// Function to help us generate a token from extraClaims and userDetails
	public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
		return buildToken(extraClaims, userDetails, jwtExpiration);
	}
	
	public String generateRefreshToken(UserDetails userDetails) {
		return buildToken(new HashMap<>(), userDetails, refreshExpiration);
	}
	
	private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
		return Jwts.builder().setClaims(extraClaims).setSubject(userDetails.getUsername())
				.setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(new Date(System.currentTimeMillis() + expiration))
				.signWith(getSignInKey(), SignatureAlgorithm.HS256).setHeaderParam("typ", "JWT").compact();
	}

	// Function to check if token is valid or not
	public boolean isTokenValid(String token, UserDetails userDetails, String ipAddress) {
		// Check if token belongs to userDetails
		final String username = extractUsername(token);

		Optional<Invalidate> invalidation = invalidateRepo.findByUsernameAndIpAddressEquals(username, ipAddress);
		boolean isValid;
		if (invalidation.isEmpty())
			isValid = true;
		else {
			isValid = invalidation.get().getInvalidatedAt().before(extractIat(token));
		}
		return (username.equals(userDetails.getUsername()) && !isTokenExpired(token) && isValid
				&& isAlgorithmValid(token));
	}
	
	private boolean isAlgorithmValid(String token) {
		// Parse the JWT string
		// Key signingKey = Keys.hmacShaKeyFor(secretKey.getBytes());
		SecretKey secret = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));

		Jws<Claims> jws = Jwts.parserBuilder().setSigningKey(secret).build().parseClaimsJws(token);

		String algo = jws.getHeader().getAlgorithm();
		if (algo.equals("HS256"))
			return true;
		else if ("none".equals(algo)) {
			return false;
			//throw new UnauthorizedException("Invalid algorithm: " + algo);
		} else {
			return false;
			//throw new UnauthorizedException("Invalid algorithm: " + algo);
		}
	}

	private boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date()); // Before today
	}

	private Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	private Date extractIat(String token) {
		return extractClaim(token, Claims::getIssuedAt);
	}

	Claims extractAllClaims(String token) {

		return Jwts.parserBuilder().setSigningKey(getSignInKey()) // Size of key depends on security level of your
																	// application (min 256) and level of trust you have
																	// in the sign in party
				.build().parseClaimsJws(token).getBody();
	}

	private Key getSignInKey() {
		byte[] keyByte = Decoders.BASE64.decode(secretKey);
		return Keys.hmacShaKeyFor(keyByte);
	}
	
	public void invalidateUserWithDiffIP(String username, String IP) {
		Optional<AuditTrail> data = auditService.getUserNotIP(username, IP);
		if(data.isPresent()) {
			Optional<Invalidate> invalidUser = invalidateRepo.findByUsernameAndIpAddressEquals(username, data.get().getIpAddress());
			if (invalidUser.isPresent()) {
				invalidUser.get().setInvalidatedAt(new Timestamp(System.currentTimeMillis()));
				invalidateRepo.save(invalidUser.get());
			}
			else {
				Invalidate invalidation = new Invalidate();
				invalidation.setUsername(username);
				invalidation.setInvalidatedAt(new Timestamp(System.currentTimeMillis()));
				invalidation.setIpAddress(data.get().getIpAddress());
				invalidateRepo.save(invalidation);
			}
		}		
	}

	public void invalidateToken(String username, String ipAddress) {
		Optional<Invalidate> invalidUser = invalidateRepo.findByUsernameAndIpAddressEquals(username, ipAddress);
		if (invalidUser.isPresent()) {
			invalidUser.get().setInvalidatedAt(new Timestamp(System.currentTimeMillis()));
			invalidateRepo.save(invalidUser.get());
		} else {
			Invalidate invalidation = new Invalidate();
			invalidation.setUsername(username);
			invalidation.setInvalidatedAt(new Timestamp(System.currentTimeMillis()));
			invalidation.setIpAddress(ipAddress);
			invalidateRepo.save(invalidation);
		}
	}
	
	public void invalidateRefreshToken(String token) {
        Key key = getSignInKey();
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        // Modify the expiration date to yesterday
        Date yesterday = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        claims.setExpiration(yesterday);

        // Re-encode the token with the new expiration date
        String invalidatedToken = Jwts.builder()
                .setClaims(claims)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
