package sad.storereg.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;

import java.util.Properties;

@Configuration
public class KaptchaConfig {

	@Bean
    public DefaultKaptcha captchaProducer() {
        DefaultKaptcha defaultKaptcha = new DefaultKaptcha();
        Properties properties = new Properties();
        
        properties.setProperty("kaptcha.border", "no");
        properties.setProperty("kaptcha.textproducer.font.color", "black");
        properties.setProperty("kaptcha.image.width", "200");
        properties.setProperty("kaptcha.image.height", "50");
        properties.setProperty("kaptcha.textproducer.font.size", "40");
        properties.setProperty("kaptcha.textproducer.char.string", "0123456789");  // Numeric characters only
        properties.setProperty("kaptcha.session.key", "captchaCode");
        properties.setProperty("kaptcha.textproducer.char.length", "5"); // Length of CAPTCHA text
        properties.setProperty("kaptcha.textproducer.font.names", "Arial");
        properties.setProperty("kaptcha.noise.color", "black");
        properties.setProperty("kaptcha.obscurificator.impl", "com.google.code.kaptcha.impl.WaterRipple");
        //properties.setProperty("kaptcha.background.clear.from", "217,229,243");
        properties.setProperty("kaptcha.background.clear.from", "231,240,230");
        properties.setProperty("kaptcha.background.clear.to", "255,255,255");


        Config config = new Config(properties);
        defaultKaptcha.setConfig(config);
        return defaultKaptcha;
    }
}
