package sad.storereg.dto.appdata;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitorRequestDto {
	
	@NotBlank(message = "Visitor name is required")
    private String name;

    @NotNull(message = "Number of visitors is required")
    @Min(value = 1, message = "At least one visitor is required")
    private Integer noOfVisitors;

    @NotBlank(message = "State is required")
    private String state;

    private String address;

    @NotBlank(message = "Purpose is required")
    private String purpose;

    private String purposeDetails;

    @NotBlank(message = "Mobile number is required")
    @Pattern(
        regexp = "^[0-9]{10}$",
        message = "Mobile number must be 10 digits"
    )
    private String mobileNo;

    @Email(message = "Invalid email format")
    private String email;

    @NotNull(message = "Visit date/time is required")
    private LocalDateTime visitDateTime;

}
