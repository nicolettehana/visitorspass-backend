package sad.storereg.controller.auth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import sad.storereg.annotations.Auditable;
import sad.storereg.dto.auth.ChangePasswordRequest;
import sad.storereg.dto.auth.RegisterRequest;
import sad.storereg.dto.auth.UpdateMobileDTO;
import sad.storereg.exception.InternalServerError;
import sad.storereg.exception.ObjectNotFoundException;
import sad.storereg.exception.UnauthorizedException;
import sad.storereg.models.auth.User;
import sad.storereg.repo.auth.UserRepository;
import sad.storereg.services.appdata.CoreServices;
import sad.storereg.services.auth.AuthenticationService;

@RestController
@RequestMapping(path = "/users")
@RequiredArgsConstructor
public class UserController {
	
	private final CoreServices coreService;
	private final UserRepository userRepo;
	private final AuthenticationService authService;
	private final PasswordEncoder passwordEncoder;
	
	@GetMapping("/profile")
	public ResponseEntity<Map<String, Object>> getuserInfo(@AuthenticationPrincipal User user) {
		try {
			Map<String, Object> userInfo = new HashMap<>();

			String email = user.getEmail();

			if (email != null) 
				email=coreService.convertEmail(email);
			
			String mobileno = user.getMobileNo();
			if (mobileno != null)
				mobileno="******".concat(mobileno.substring(6));

			userInfo.put("username", user.getUsername());
			userInfo.put("email", coreService.convertEmail(user.getEmail()));
			userInfo.put("mobileno", mobileno);
			userInfo.put("role", user.getRole());
			userInfo.put("name", user.getName());
			userInfo.put("department", user.getDepartment());
			userInfo.put("office", coreService.getOfficeName(user.getOfficeCode()));
			userInfo.put("designation", user.getDesignation());
//			if(user.getOfficeCode()!=null)
//				userInfo.put("office", coreService.getOffice(user.getOfficeCode()).getOfficeName());

			return new ResponseEntity<>(userInfo, HttpStatus.OK);

		} catch (Exception ex) {
			throw new InternalServerError("Unable to fetch user information", ex);
		}
	}
	
	@Auditable
	@PostMapping("/change-password")
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request,
			@AuthenticationPrincipal User user, HttpServletRequest httpRequest) {
		
		Map<String, String> map = new HashMap<>();
		User u = userRepo.findByUsername(user.getUsername()).orElseThrow(()->new ObjectNotFoundException("Invalid username"));
		try {

			if (!(passwordEncoder.matches(authService.decryptPassword(request.getOldPassword()), u.getPassword())))
				throw new UnauthorizedException("Incorrect password");
			
			u.setPassword(passwordEncoder.encode(authService.decryptPassword(request.getNewPassword())));
			//u.get().setUpdatedBy(user.getUsername());
			userRepo.save(u);

			map.put("message", "Password Changed Successfully");

			return new ResponseEntity<>(map, HttpStatus.OK);

		} catch (ObjectNotFoundException | UnauthorizedException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerError("Unable to change password", ex);
		}
	}
	
	@Auditable
	@Transactional
	@PostMapping("/update")
	public ResponseEntity<Map<String, String>> update(@Valid @RequestBody RegisterRequest request, @AuthenticationPrincipal User user,
			HttpServletRequest httpRequest) {
		try {
			Map<String, String> map = new HashMap<>();
			
			User u = userRepo.findByUsername(user.getUsername()).orElseThrow(()->new ObjectNotFoundException("Invalid username"));
			
			if(userRepo.findAllByMobileNoAndUsernameNot(request.getMobileNo(), user.getUsername()).size()>0) {
				throw new UnauthorizedException("Mobile no. is already registered");
			}
			
			u.setDepartment(request.getDepartment());
			u.setDesignation(request.getDesignation());
			//u.setMobileNo(request.getMobileNo());
			//u.setUsername(request.getMobileNo());
			u.setName(request.getName());
			u.setEmail(request.getEmail());

			map.put("detail", "Profile updated.");

			return new ResponseEntity<>(map, HttpStatus.OK);

		} catch (UnauthorizedException|InternalServerError ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerError("Unable to update profile", ex);
		}
	}
	
	@GetMapping(path="/all", params = {"page", "size"})
	public Iterable<User> getusers(@RequestParam("page") final int page, @RequestParam("size") final int size, @AuthenticationPrincipal User user) {

		try {
			final String role = user.getRole().name();

			PageRequest pageable = PageRequest.of(page, size, Direction.fromString("ASC"), "id");
			Page<User> users = userRepo.findAll(pageable);
			
			List<User> updatedUsers = users.stream()
				    .peek(userItem -> userItem.setEmail(coreService.convertEmail(userItem.getEmail())))
				    .collect(Collectors.toList());

			Page<User> updatedPage = new PageImpl<>(updatedUsers, pageable, users.getTotalElements());

			return updatedPage;
		}catch(ObjectNotFoundException|UnauthorizedException ex) {throw ex;}
		catch (Exception ex) {
			throw new InternalServerError("Unable to fetch users", ex);
		}
	}
	
	@Auditable
	@PostMapping("/enable-disable")
	public ResponseEntity<Map<String,Object>> enableDisable(@RequestBody Map<String, String> payload, @AuthenticationPrincipal User user) {
		Map<String,Object> map=new HashMap<>();
		try {
			Optional<User> user2 = userRepo.findByUsername(payload.get("username"));
			if(user2.isEmpty())
				throw new ObjectNotFoundException("User not Found");

			if(user2.get().getUsername().equals(user.getUsername()))
				throw new UnauthorizedException("Cannot Enable/Disable Self");

			if(user2.get().isEnabled())
				user2.get().setEnabled(false);
			else
				user2.get().setEnabled(true);
			userRepo.save(user2.get());

			map.put("message","Successfully enabled/disabled user");
			return new ResponseEntity<>(map, HttpStatus.OK);
		}catch(ObjectNotFoundException|UnauthorizedException ex) {throw ex;}
		catch(Exception ex) {
			throw new InternalServerError("Unable to perform action", ex);
		}
	}
	
	
	
	@Auditable
	@PostMapping("/verify-otp-update-mobile")
	public ResponseEntity<Map<String, String>> verifyOTPSignUp(@Valid @RequestBody UpdateMobileDTO request,
			HttpServletRequest httpRequest, @AuthenticationPrincipal User user) {
		try {
			Map<String, String> map = new HashMap<>();
			User u = userRepo.findByUsername(user.getUsername()).orElseThrow(()->new ObjectNotFoundException("Invalid username"));
			
			if(userRepo.findByUsername(authService.decryptPassword(request.getMobileNo())).isPresent()) {
				throw new UnauthorizedException("Mobile no. is already registered");
			}
			
//			if(otpService.verifyOTP(request.getOtp(), authService.decryptPassword(request.getMobileNo()), httpRequest, 0, request.getOtpToken().toString())) {
//				u.setMobileNo(authService.decryptPassword(request.getMobileNo()));
//				u.setUsername(authService.decryptPassword(request.getMobileNo()));
//			}
			//jwtService.invalidateToken(null, null);
			map.put("detail", "Mobile no. updated");
			return ResponseEntity.ok(map);

		} catch (UnauthorizedException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerError("Unable to perform action", ex);
		}
	}	
}
