package sad.storereg.services.master;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sad.storereg.dto.appdata.CategoryCountDTO;
import sad.storereg.dto.master.ItemDTO;
import sad.storereg.models.master.Category;
import sad.storereg.models.master.Item;
import sad.storereg.models.master.SubItems;
import sad.storereg.repo.master.CategoryRepository;
import sad.storereg.repo.master.ItemRepository;
import sad.storereg.services.appdata.ExcelServices;
import sad.storereg.services.appdata.PurchaseService;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
@RequiredArgsConstructor
public class ItemService {

	private final ItemRepository itemRepository;
	private final CategoryRepository categoryRepository;
	private final PurchaseService purchaseService;
	private final ExcelServices excelService;

    public Page<Item> getItems(Pageable pageable, String search, String category) {
    	Page<Item> page;
    	if(search!=null && search.length()>0) {
    		page = itemRepository.searchByItemOrSubItemName(search, pageable);
    	}
    	else if(category==null || category.equals("") || category.equals("All"))
    		page = itemRepository.findAll(pageable);
    	else
    		page = itemRepository.findAllByCategory_Code(category, pageable);
    	
    	//page.forEach(item -> item.setBalance(purchaseService.getAvailableBalance(item.getId(),item.getSubItems()==null?null:item.getSubItems().getId(), rate.getUnit().getId(), issueDate==null?LocalDate.now():null)));

    	page.forEach(item -> {
    		if (item.getSubItems() != null) {
                item.getSubItems().forEach(subItem -> {
                    subItem.setBalance(purchaseService.getAvailableBalanceAllUnits(item.getId(),item.getSubItems().size()==0?null:subItem.getId(),LocalDate.now())); 
                });
            }
            if(item.getSubItems().size()==0) {
            	item.setBalance(purchaseService.getAvailableBalanceAllUnits(item.getId(),null,LocalDate.now()));
            };
        });
    	
        return page;
    }
    
    public List<Item> getItemsList(String search, String category) {
    	if(category==null || category.equals("") || category.equals("All"))
    		return itemRepository.findAll();
    	else return itemRepository.findAllByCategory_Code(category);
    }

    public String createItem(ItemDTO request) {

        Item item = new Item();
        item.setName(request.getItemName());
        Category category = categoryRepository.findById(request.getCategory())
                .orElseThrow(() -> new RuntimeException("Category code not found: " + request.getCategory()));
        item.setCategory(category);

     // Handle sub-items only if required
        if (Boolean.TRUE.equals(request.getHasSubItems()) &&
            request.getSubItems() != null && !request.getSubItems().isEmpty()) {

            List<SubItems> subItemList = request.getSubItems().stream()
                .map(name -> {
                    SubItems s = new SubItems();
                    s.setName(name);
                    s.setItem(item); // link back
                    return s;
                })
                .collect(Collectors.toList());

            item.setSubItems(subItemList);

        } else {
            item.setSubItems(null);
        }

        itemRepository.save(item);
        
        return("Item added");
    }
    
    public List<CategoryCountDTO> getCategoryCounts() {
        return itemRepository.getCategoryCounts();
    }
    
    public Long getTotalItems() {
    	return itemRepository.getAbsoluteTotal();
    }
    
    public byte[] getItems(String category) throws IOException {

        List<Item> items = getItemsList(null,category);

     // Calculate balances
        items.forEach(item -> {
            if (item.getSubItems() != null && !item.getSubItems().isEmpty()) {
                item.getSubItems().forEach(subItem -> {
                    subItem.setBalance(
                        purchaseService.getAvailableBalanceAllUnits(
                            item.getId(), subItem.getId(), LocalDate.now()));
                });
            } else {
                item.setBalance(
                    purchaseService.getAvailableBalanceAllUnits(
                        item.getId(), null, LocalDate.now()));
            }
        });

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Items");
            Map<String, CellStyle> styles = excelService.createStyles(workbook);

            String categoryName = (category!=null && category.length()>0)?categoryRepository.findByCode(category).get().getName():"All";
            excelService.createExcelContentItems(sheet, items, category, categoryName, styles, workbook);

            workbook.write(out);
            return out.toByteArray();
        }
    	
    }
}
