package sad.storereg.repo.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import sad.storereg.models.auth.Otp;

public interface OtpRepository extends JpaRepository<Otp, Long>{
	
	Optional<Otp> findByUsername(String username);
	
	Optional<Otp> findByUsernameAndForgotPasswordEqualsAndOtpTokenEquals(String username, int forgotPassword, String otToken);
	
	Optional<Otp> findByUsernameAndForgotPasswordEquals(String username, int forgotPassword);
	
	Optional<Otp> findByUsernameAndForgotPasswordIsNull(String username);
	
	Optional<Otp> findByUsernameAndIsSignUpEqualsAndOtpTokenEquals(String username, int isSignUp, String otpToken);
	
	Optional<Otp> findByUsernameAndIsSignUpEquals(String username, int isSignUp);

}
