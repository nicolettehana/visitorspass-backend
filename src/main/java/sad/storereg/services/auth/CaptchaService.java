package sad.storereg.services.auth;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.Base64;
import java.util.HashMap;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.google.code.kaptcha.impl.DefaultKaptcha;

@Service
public class CaptchaService {
	
	@Autowired
    private DefaultKaptcha captchaProducer;
	
	@Autowired
    private RedisTemplate<String, String> redisTemplate;

	public Map<String, Object> generateCaptcha() throws IOException {
		
		String captchaText = captchaProducer.createText();
		BufferedImage captchaImage = captchaProducer.createImage(captchaText);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(captchaImage, "png", outputStream);
        byte[] imageBytes = outputStream.toByteArray();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        UUID captchaToken = UUID.randomUUID();
        
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("captchaToken", captchaToken);
        responseBody.put("captchaImage", "data:image/png;base64," + base64Image);
        
        System.out.println("Captcha: "+responseBody);
        System.out.println("Captcha String is: "+captchaText);
        
        redisTemplate.opsForValue().set(captchaToken.toString(), captchaText, 5, TimeUnit.MINUTES);
        
        return responseBody;
	}
	
	public boolean validateCaptcha(String captchaToken, String captchaInput) {

        String storedCaptchaText = redisTemplate.opsForValue().get(captchaToken);

        if (storedCaptchaText != null && storedCaptchaText.equalsIgnoreCase(captchaInput))
            return true; 
        else
            return false;
    }
}
