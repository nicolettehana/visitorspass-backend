package sad.storereg.config;

import java.time.LocalDate;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "file")
public class FileStorageProperties {
	
	private String uploadDir;

	public String getUploadDir(String appNo) {
		LocalDate date = LocalDate.now();
		return uploadDir + "/" + appNo + "/" + date.getYear() + "/" + date.getMonth();
	}

	public void setUploadDir(String uploadDir) {
		this.uploadDir = uploadDir;
	}

}
