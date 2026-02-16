package sad.storereg.services.master;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sad.storereg.dto.master.OfficeRequest;
import sad.storereg.exception.UnauthorizedException;
import sad.storereg.models.master.Office;
import sad.storereg.repo.master.OfficeRepository;

@Service
@RequiredArgsConstructor
public class OfficesService {
	
	private final OfficeRepository officeRepository;
	
	public List<Office> getOffices() {
        return officeRepository.findAll();    
    }
	
	public String createOffice(OfficeRequest request) {
		try {
			if(request.getOfficeCode()==null) {
				if(officeRepository.findByOfficeName(request.getOfficeName()).isPresent())
					throw new UnauthorizedException("Office exists");
		    	Office office  = new Office();
		    	office.setOfficeName(request.getOfficeName());
		    	officeRepository.save(office);
			}
			else {
				Optional<Office> office = officeRepository.findByOfficeCode(request.getOfficeCode());
				if(office.isEmpty())
					throw new UnauthorizedException("Office code invalid");
				Optional<Office> office2 = officeRepository.findByOfficeName(request.getOfficeName());
				if(office2.isPresent() && office2.get().getOfficeCode()!=request.getOfficeCode())
					throw new UnauthorizedException("Duplicate Office Name");
				office.get().setOfficeName(request.getOfficeName());
				officeRepository.save(office.get());
			}
	    	return "Added successfully";
		}catch(Exception ex) {
			throw ex;
		}
    }

}
