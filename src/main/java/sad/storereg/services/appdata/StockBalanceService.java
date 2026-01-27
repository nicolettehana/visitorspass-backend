package sad.storereg.services.appdata;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.time.LocalDate;
import java.util.ArrayList;


import org.apache.commons.lang3.tuple.Pair;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sad.storereg.dto.appdata.CategoryStockResponse;
import sad.storereg.dto.appdata.ItemStockDTO;
import sad.storereg.dto.appdata.SubItemStockDTO;
import sad.storereg.models.appdata.StockBalance;
import sad.storereg.models.master.Item;
import sad.storereg.models.master.SubItems;
import sad.storereg.repo.appdata.IssueRepository;
import sad.storereg.repo.appdata.PurchaseRepository;
import sad.storereg.repo.appdata.StockBalanceRepository;
import sad.storereg.repo.master.ItemRepository;

@RequiredArgsConstructor
@Service
public class StockBalanceService {
	
	private final ItemRepository itemRepository;
	private final StockBalanceRepository stockBalanceRepository;
	private final PurchaseRepository purchaseRepo;
	private final IssueRepository issueRepo;

//	public CategoryStockResponse getStockByCategory(String categoryCode) {
//
//	    List<Item> items = itemRepository.findAllByCategory_Code(categoryCode);
//	    List<ItemStockDTO> result = new ArrayList<>();
//
//	    for (Item item : items) {
//
//	        if (item.getSubItems().isEmpty()) {
//	            // item without subItems → 1 stock value
//	            Integer stock = getLatestBalance(item, null);
//
//	            result.add(new ItemStockDTO(
//	                    item.getName(),
//	                    stock,
//	                    null
//	            ));
//	        } else {
//	            // item with subItems → list of subItem + stock
//	            List<SubItemStockDTO> subItemList = new ArrayList<>();
//
//	            for (SubItems si : item.getSubItems()) {
//	                Integer stock = getLatestBalance(item, si);
//
//	                subItemList.add(new SubItemStockDTO(
//	                        si.getName(),
//	                        stock
//	                ));
//	            }
//
//	            result.add(new ItemStockDTO(
//	                    item.getName(),
//	                    null,
//	                    subItemList
//	            ));
//	        }
//	    }
//
//	    return new CategoryStockResponse(
//	            items.get(0).getCategory().getName(),
//	            result
//	    );
//	}

	private Integer getLatestBalance(Item item, SubItems subItem) {
	    Long subItemId = (subItem != null) ? subItem.getId() : null;
	    StockBalance sb = stockBalanceRepository.findLatestBalance(item.getId(), subItemId);
	    return sb != null ? sb.getBalance() : 0;
	}
	
	public Map<Long, Integer> getAvailableStockUsingBalance(
	        Long itemId, Long subItemId, LocalDate date
	) {

	    // 1. Get latest stock balance checkpoint
	    StockBalance latest = stockBalanceRepository.getLatestBalance1(itemId, subItemId, date);

	    int baseBalance = (latest != null) ? latest.getBalance() : 0;
	    LocalDate balanceDate = (latest != null) ? latest.getDate() : LocalDate.MIN;

	    // Key: unitId, Value: stock
	    Map<Long, Integer> stockMap = new HashMap<>();

	    // Set base balance (same for all units)
	    stockMap.put(0L, baseBalance);

	    // 2. Purchases after balance date
	    List<Object[]> purchased = purchaseRepo.getPurchasedAfter(
	            itemId, subItemId, balanceDate, date
	    );

	    for (Object[] row : purchased) {
	        Long unitId = (Long) row[0];
	        Integer qty = ((Number) row[1]).intValue();
	        stockMap.merge(unitId, qty, Integer::sum);
	    }

	    // 3. Issues after balance date
	    List<Object[]> issued = issueRepo.getIssuedAfter(
	            itemId, subItemId, balanceDate, date
	    );

	    for (Object[] row : issued) {
	        Long unitId = (Long) row[0];
	        Integer qty = ((Number) row[1]).intValue();
	        stockMap.merge(unitId, -qty, Integer::sum);
	    }

	    return stockMap;
	}
	
	public List<CategoryStockResponse> getStockFiltered(Integer level) {
	    // Map<CategoryCode, CategoryStockResponse>
	    Map<String, CategoryStockResponse> categoryMap = new LinkedHashMap<>();
	    LocalDate today = LocalDate.now();

	    List<Item> items = itemRepository.findAll();

	    for (Item item : items) {
	        String categoryCode = item.getCategory().getCode();
	        String categoryName = item.getCategory().getName();

	        // Initialize CategoryStockResponse in the map if not already present
	        categoryMap.computeIfAbsent(categoryCode, c -> new CategoryStockResponse(categoryName, categoryCode, new ArrayList<>()));

	        if (item.getSubItems().isEmpty()) {
	            // No sub-items → item-level stock
	            Integer stock = getFinalStock(item.getId(), null, today);
	            if (matchesFilter(stock, level)) {
	                categoryMap.get(categoryCode).getItems().add(new ItemStockDTO(item.getName(), stock, null));
	            }

	        } else {
	            // Has sub-items → calculate per sub-item
	            List<SubItemStockDTO> subList = new ArrayList<>();
	            for (SubItems si : item.getSubItems()) {
	                Integer stock = getFinalStock(item.getId(), si.getId(), today);
	                if (matchesFilter(stock, level)) {
	                    subList.add(new SubItemStockDTO(si.getName(), stock));
	                }
	            }

	            if (!subList.isEmpty()) {
	                categoryMap.get(categoryCode).getItems().add(new ItemStockDTO(item.getName(), null, subList));
	            }
	        }
	    }

	    // Convert map values to list
	    return new ArrayList<>(categoryMap.values());
	}

	// Filter logic
	private boolean matchesFilter(Integer stock, Integer level) {
	    if (level == 0) {
	        return stock == 0;
	    } else if (level <= 10) {
	        return stock >= 1 && stock <= level;  // between 1 and level
	    } else {
	        return stock >= 11 && stock <= level; // between 11 and level
	    }
	}

	// Calculate final stock for item/subitem
	private Integer getFinalStock(Long itemId, Long subItemId, LocalDate date) {
	    // 1. Latest stock balance checkpoint
	    StockBalance latest = stockBalanceRepository.getLatestBalance1(itemId, subItemId, date);

	    int baseBalance = (latest != null) ? latest.getBalance() : 0;
	    LocalDate balanceDate = (latest != null) ? latest.getDate() : LocalDate.of(2020, 1, 1);

	    int purchasedQty = purchaseRepo.sumPurchasedAfter(itemId, subItemId, balanceDate, date);
	    int issuedQty = issueRepo.sumIssuedAfter(itemId, subItemId, balanceDate, date);

	    return baseBalance + purchasedQty - issuedQty;
	}


    // FILTER LOGIC
//	private boolean matchesFilter(Integer stock, Integer level) {
//	    if (level == 0) {
//	        return stock == 0;
//	    } else if (level <= 10) {
//	        return stock >= 1 && stock <= level;  // between 1 and level
//	    } else {
//	        return stock >= 11 && stock <= level; // between 11 and level
//	    }
//	}
//
//
//    // FINAL STOCK FOR ITEM/SUBITEM USING STOCK_BALANCE
//    private Integer getFinalStock(Long itemId, Long subItemId, LocalDate date) {
//
//        // 1. Latest stock balance checkpoint
//        StockBalance latest = stockBalanceRepository.getLatestBalance1(itemId, subItemId, date);
//
//        int baseBalance = (latest != null) ? latest.getBalance() : 0;
//        LocalDate balanceDate = (latest != null) ? latest.getDate() : LocalDate.of(2020, 1, 1);
//
//        int purchasedQty = purchaseRepo.sumPurchasedAfter(itemId, subItemId, balanceDate, date);
//        int issuedQty = issueRepo.sumIssuedAfter(itemId, subItemId, balanceDate, date);
//
//        return baseBalance + purchasedQty - issuedQty;
//    }


}
