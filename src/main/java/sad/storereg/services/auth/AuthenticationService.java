package sad.storereg.services.auth;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.Map;

import javax.crypto.Cipher;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sad.storereg.annotations.Auditable;
import sad.storereg.config.JwtService;
import sad.storereg.dto.auth.AuthenticationRequest;
import sad.storereg.dto.auth.AuthenticationResponse;
import sad.storereg.dto.auth.RegisterRequest;
import sad.storereg.exception.InternalServerError;
import sad.storereg.exception.ObjectNotFoundException;
import sad.storereg.exception.UnauthorizedException;
import sad.storereg.logs.Login;
import sad.storereg.logs.LoginRepository;
import sad.storereg.models.auth.CurrentUsers;
import sad.storereg.models.auth.PasswordResetToken;
import sad.storereg.models.auth.Role;
import sad.storereg.models.auth.User;
import sad.storereg.repo.auth.CurrentUsersRepository;
import sad.storereg.repo.auth.PasswordTokenRepository;
import sad.storereg.repo.auth.UserRepository;
import sad.storereg.services.appdata.CoreServices;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {
	
	private final UserRepository userRepo;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;
	private final CoreServices coreServices;
	private final PasswordTokenRepository passwordTokenRepo;
	private final LoginRepository loginRepo;
	private final CookieService cookieService;
	private final CurrentUsersRepository currentUsersRepo;
	
	@Value("${keys.dir}")
	private String keysDir;
	@Value("${application.security.jwt.expiration}")
	private long jwtExpiration;
	@Value("${application.security.jwt.refresh-token.expiration}")
	private long refreshExpiration;
	
	@Auditable
	@Transactional
	public User register(RegisterRequest request) {
		
		//Optional<User> userr = userRepo.findByUsername(request.getUsername());
		String mobileNo= (decryptPassword(request.getMobileNo()));
		Optional<User> userr = userRepo.findByUsername(mobileNo);

		if (userr.isPresent())
			throw new InternalServerError("Username already registered");
		
		// Check if mobile number already exists
	    Optional<User> existingUserByMobileNo = userRepo.findByMobileNo(mobileNo);
	    if (existingUserByMobileNo.isPresent()) {
	        throw new InternalServerError("User with this mobile number is already registered.");
	    }
//	    Optional<User> existingUserByEmailNo = userRepo.findByEmail(request.get);
//	    if (existingUserByMobileNo.isPresent()) {
//	        throw new InternalServerError("User with this mobile number is already registered.");
//	    }
		
		Role role = Role.valueOf(request.getRole().toString().toUpperCase());

		String pw = request.getPassword() == null ? "password" : decryptPassword(request.getPassword());			

		var user = User.builder().name(request.getName()).designation(request.getDesignation())
				.department(request.getDepartment())
				.username(mobileNo).email(request.getUsername()).password(passwordEncoder.encode(pw))
				.role(role).isEnabled(true).
				entryDate(Timestamp.from(Instant.now())).mobileNo(mobileNo).build();
		
		userRepo.save(user);
		
		return user;
	}
	
	@Auditable
	public ResponseEntity<?> authStep1(AuthenticationRequest request, HttpServletRequest httpRequest) {
	    String username = decryptPassword(request.getUsername());
	    String password = decryptPassword(request.getPassword());

	    try {
	        authenticationManager.authenticate(
	            new UsernamePasswordAuthenticationToken(username, password)
	        );
	    } catch (BadCredentialsException | UsernameNotFoundException ex) {
	        throw new UnauthorizedException("Incorrect username or password");
	    } catch (DisabledException ex) {
	        throw new UnauthorizedException("Account is disabled");
	    }

	    // ✅ Generate OTP using existing service
	    //GetOtpResponseDTO otpResponse = otpService.sendOtpSignUp(httpRequest, username, "login");

	    return ResponseEntity.ok(Map.of(
	        "message", "OTP sent successfully"
	        //"otpToken", otpResponse.getOtpToken()
	    ));
	}

	@Auditable
	public AuthenticationResponse authenticate2(AuthenticationRequest request, HttpServletRequest httpRequest, HttpServletResponse response)
			throws BadCredentialsException, UsernameNotFoundException, IOException {
		try {
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(decryptPassword(request.getUsername()),
					decryptPassword(request.getPassword())));
		} catch (BadCredentialsException|UsernameNotFoundException exception) {
			throw new UnauthorizedException("Incorrect username or password");
		} catch (DisabledException exception) {
			throw new UnauthorizedException("Account is disabled");
		}
		jwtService.invalidateUserWithDiffIP(decryptPassword(request.getUsername()), coreServices.getClientIp(httpRequest));
		var user = userRepo.findByUsername(decryptPassword(request.getUsername())).orElseThrow(()->new ObjectNotFoundException("User not found"));
		var jwtToken = jwtService.generateToken(user);
		var refreshToken = jwtService.generateRefreshToken(user);
	    
		Login login = Login.builder().username(user.getUsername()).uri(httpRequest.getRequestURI()).httpMethod(httpRequest.getMethod())
				.ts(new Date()).httpStatus(response.getStatus()).build();
		loginRepo.save(login);
		
		MDC.put("username", user.getUsername());
		log.info("Login");
		MDC.remove("username");
		
		return AuthenticationResponse.builder().accessToken(jwtToken).refreshToken(refreshToken)
				.role(user.getRole().toString()).build();
	}
	
	
	
	@Auditable
	public AuthenticationResponse authenticate3(AuthenticationRequest request, HttpServletRequest httpRequest, HttpServletResponse response)
			throws BadCredentialsException, UsernameNotFoundException, IOException {
		try {
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(decryptPassword(request.getUsername()),
					decryptPassword(request.getPassword())));
		} catch (BadCredentialsException|UsernameNotFoundException exception) {
			throw new UnauthorizedException("Incorrect username or password");
		} catch (DisabledException exception) {
			throw new UnauthorizedException("Account is disabled");
		}
		jwtService.invalidateUserWithDiffIP(decryptPassword(request.getUsername()), coreServices.getClientIp(httpRequest));
		var user = userRepo.findByUsername(decryptPassword(request.getUsername())).orElseThrow(()->new ObjectNotFoundException("User not found"));
		var jwtToken = jwtService.generateToken(user);
		var refreshToken = jwtService.generateRefreshToken(user);
		// ✅ Create secure, HTTP-only cookies
//	    ResponseCookie accessCookie = ResponseCookie.from("access_token", jwtToken)
//	        .httpOnly(true)
//	        .secure(false) // Set to false if testing locally over HTTP
//	        .path("/")
//	        .sameSite("Strict")
//	        .maxAge(Duration.ofMillis(jwtExpiration).getSeconds())
//	        //.maxAge(Duration.ofDays(1))
//	        .build();
	    //ResponseCookie accessCookie = cookieService.getCookie("access_token", jwtToken, jwtExpiration);

//	    ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
//	        .httpOnly(true)
//	        .secure(false)
//	        .path("/")
//	        .sameSite("Strict")
//	        .maxAge(Duration.ofMillis(refreshExpiration).getSeconds())
//	        //.maxAge(Duration.ofDays(1))
//	        .build();
	    //ResponseCookie refreshCookie = cookieService.getCookie("refresh_token", refreshToken, refreshExpiration);
	    
	    // ✅ Set cookies in response header
	    //response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
	    //response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
		
		Login login = Login.builder().username(user.getUsername()).uri(httpRequest.getRequestURI()).httpMethod(httpRequest.getMethod())
				.ts(new Date()).httpStatus(response.getStatus()).build();
		loginRepo.save(login);
		
		MDC.put("username", user.getUsername());
		log.info("Login");
		MDC.remove("username");
		
//		CurrentUsers currentUser = CurrentUsers.builder().username(user.getUsername()).token(jwtToken).entrydate(new Date()).build();
//		CurrentUsers currentUser2 = CurrentUsers.builder().username(user.getUsername()).token(refreshToken).entrydate(new Date()).build();
//		currentUsersRepo.save(currentUser);
//		currentUsersRepo.save(currentUser2);
		
		//return AuthenticationResponse.builder().role(user.getRole().toString()).build();
		return AuthenticationResponse.builder().accessToken(jwtToken).refreshToken(refreshToken)
				.role(user.getRole().toString()).build();
	}
	
	public String decryptPassword(String encryptedPassword) {

		try {
			
			Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWITHSHA-512ANDMGF1PADDING");
			File privateKeyFile = new File(keysDir + "//private.key");
			byte[] privateKeyBytes = Files.readAllBytes(privateKeyFile.toPath());
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
			PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
			
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			byte[] decrypt = cipher.doFinal(Base64.getDecoder().decode(encryptedPassword));

			return new String(decrypt);
		} catch (Exception e) {
			throw new InternalServerError("Failed to decrypt password", e);
		}
	}
	
	@Transactional
	public void refreshToken2(HttpServletRequest request, HttpServletResponse response) throws IOException {
		final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		final String refreshToken;
		final String userEmail;
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			return;
		}
		refreshToken = authHeader.substring(7);
		userEmail = jwtService.extractUsername(refreshToken);
		if (userEmail != null) {
			var user = this.userRepo.findByUsername(userEmail).orElseThrow(()-> new ObjectNotFoundException("User not found"));
			if (jwtService.isTokenValid(refreshToken, user, coreServices.getClientIp(request))) {
				var accessToken = jwtService.generateToken(user);
				var authResponse = AuthenticationResponse.builder().accessToken(accessToken).role(user.getRole().toString())
						.refreshToken(refreshToken).build();
				new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
			} else 
				throw new UnauthorizedException("Invalidated Token");
		}
	}
	
	@Transactional
	public void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
	    // 1. Extract refresh token from cookie
	    String refreshToken = null;
	    if (request.getCookies() != null) {
	        for (Cookie cookie : request.getCookies()) {
	            if ("refresh_token".equals(cookie.getName())) {
	                refreshToken = cookie.getValue();
	                break;
	            }
	        }
	    }

	    if (refreshToken == null) {
	        throw new UnauthorizedException("Refresh token missing");
	    }

	    // 2. Validate refresh token
	    String userEmail = jwtService.extractUsername(refreshToken);
	    if (userEmail != null) {
	        var user = userRepo.findByUsername(userEmail)
	            .orElseThrow(() -> new ObjectNotFoundException("User not found"));

	        if (jwtService.isTokenValid(refreshToken, user, coreServices.getClientIp(request))) {
	            // 3. Invalidate old refresh token
	            jwtService.invalidateRefreshToken(refreshToken);  // If you're using DB to track tokens

	            // 4. Generate new tokens
	            String newAccessToken = jwtService.generateToken(user);
	            String newRefreshToken = jwtService.generateRefreshToken(user);

	            // 5. Set new tokens in cookies
//	            ResponseCookie accessCookie = ResponseCookie.from("access_token", newAccessToken)
//	                    .httpOnly(false)
//	                    .secure(true)
//	                    .path("/")
//	                    .maxAge(Duration.ofMillis(jwtExpiration).getSeconds())
//	                    .sameSite("None")
//	                    .build();
	            ResponseCookie accessCookie = cookieService.getCookie("access_token", newAccessToken, jwtExpiration);

//	            ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", newRefreshToken)
//	                    .httpOnly(true)
//	                    .secure(true)
//	                    .path("/")
//	                    .maxAge(Duration.ofMillis(refreshExpiration).getSeconds())
//	                    .sameSite("None")
//	                    .build();

	            ResponseCookie refreshCookie = cookieService.getCookie("refresh_token", newRefreshToken, refreshExpiration);
	            
	            response.setHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
	            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

	            // 6. Optionally return user info (without tokens)
	            AuthenticationResponse authResponse = AuthenticationResponse.builder()
	                    .role(user.getRole().toString())
	                    .build();

	            new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
	        } else {
	            throw new UnauthorizedException("Invalidated or expired refresh token");
	        }
	    }
	}


	private void createKeys() {
		try {
			Path keyDirectory = Paths.get(keysDir).toAbsolutePath().normalize();
			Files.createDirectories(keyDirectory);

			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize(2048);
			KeyPair keyPair = keyPairGenerator.generateKeyPair();
			PublicKey publicKey = keyPair.getPublic();
			PrivateKey privateKey = keyPair.getPrivate();

			Path publicKeyPath = keyDirectory.resolve("public.key").normalize();
			try (FileOutputStream fos = new FileOutputStream(publicKeyPath.toFile())) {
				fos.write(publicKey.getEncoded());
			}

			Path privateKeyPath = keyDirectory.resolve("private.key").normalize();
			try (FileOutputStream fos = new FileOutputStream(privateKeyPath.toFile())) {
				fos.write(privateKey.getEncoded());
			}

		} catch (Exception e) {
			throw new InternalServerError("Failed to generate Key Pair for encryption", e);
		}
	}

	public String getPublicKey() throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
		// Here check the file if it has the keys or not. If not then generate new
		File publicKeyFile = new File(keysDir + "//public.key");
		byte[] publicKeyBytes;
		if (publicKeyFile.exists() && !publicKeyFile.isDirectory()) {
			publicKeyBytes = Files.readAllBytes(publicKeyFile.toPath());
		} else {
			createKeys();
			publicKeyBytes = Files.readAllBytes(publicKeyFile.toPath());
		}
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
		PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
		return Base64.getEncoder().encodeToString(publicKey.getEncoded());
	}
	
	public String validatePasswordResetToken(String token) {
	    Optional<PasswordResetToken> passToken = passwordTokenRepo.findByToken(token);

	    if(passToken.isEmpty())
	    	return "invalidToken";
	    else if(isTokenExpired(passToken.get()))
	    	return "expired";
	    else {
	    	passwordTokenRepo.deleteById(passToken.get().getId());
	    	return null;
	    }
	}

	private boolean isTokenExpired(PasswordResetToken passToken) {
	    final Calendar cal = Calendar.getInstance();
	    return passToken.expiry.before(cal.getTime());
	}

}
