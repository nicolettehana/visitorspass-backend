package sad.storereg.services.master;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sad.storereg.models.master.Menu;
import sad.storereg.repo.master.MenuRepository;

@Service
@RequiredArgsConstructor
public class MenuService {
	
	private final MenuRepository menuRepo;

	public List<Menu> getMenusByRole(String role) {
        return menuRepo.findByRole(role);
    }
}
