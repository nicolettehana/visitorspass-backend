package sad.storereg.models.auth;

import java.util.UUID;

import lombok.Data;

@Data
public class CaptchaSettings {
	
	private String captcha;

	private String hiddenCaptcha;

	private String realCaptcha;

	private UUID uuid;

}
