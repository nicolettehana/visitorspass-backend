package sad.storereg.services.master;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sad.storereg.dto.master.ItemDTO;
import sad.storereg.dto.master.ItemRateCreateDTO;
import sad.storereg.dto.master.ItemRateDTO;
import sad.storereg.dto.master.SubItemRateDTO;
import sad.storereg.dto.master.UnitRateDTO;
import sad.storereg.exception.ObjectNotFoundException;
import sad.storereg.models.master.Rate;
import sad.storereg.models.master.Category;
import sad.storereg.models.master.Item;
import sad.storereg.models.master.SubItems;
import sad.storereg.models.master.Unit;
import sad.storereg.models.master.YearRange;
import sad.storereg.repo.master.CategoryRepository;
import sad.storereg.repo.master.ItemRepository;
import sad.storereg.repo.master.RateRepository;
import sad.storereg.repo.master.UnitRepository;
import sad.storereg.repo.master.YearRangeRepository;
import sad.storereg.services.appdata.ExcelServices;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@RequiredArgsConstructor
@Service
public class RateService {

	private final RateRepository rateRepository;
    private final ItemRepository itemRepository;
    private final YearRangeRepository yearRangeRepository;
    private final UnitRepository unitRepository;
    private final CategoryRepository categoryRepository;
    private final ExcelServices excelService;
    
    public Page<ItemRateDTO> getRates(
            String categoryCode,
            Integer yearRangeId,
            String search,
            Pageable pageable
    ) {

        YearRange yearRange = null;
        if (yearRangeId != null) {
            yearRange = yearRangeRepository.findById(yearRangeId)
                    .orElseThrow(() -> new RuntimeException("YearRange not found"));
        }
        
        

        Page<Item> itemPage = (search!=null && search.length()>0)? itemRepository.searchByItemOrSubItemName(search, pageable):
        	((categoryCode != null && !categoryCode.isBlank())
                ? itemRepository.findAllByCategory_Code(categoryCode, pageable)
                : itemRepository.findAll(pageable));

        YearRange finalYearRange = yearRange;

        return itemPage.map(item -> mapItem(item, finalYearRange));
    }
    
    private ItemRateDTO mapItem(Item item, YearRange yearRange) {

        ItemRateDTO dto = new ItemRateDTO();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setCategory(item.getCategory().getName());
        dto.setCategoryCode(item.getCategory().getCode());

        if (yearRange != null) {
            dto.setStartYear(yearRange.getStartYear());
            dto.setEndYear(yearRange.getEndYear());
        }

        // Item WITHOUT sub-items → unit-wise rates
        if (item.getSubItems() == null || item.getSubItems().isEmpty()) {

            if (yearRange != null) {
                List<Rate> rates = rateRepository
                        .findByItemAndYearRange(item, yearRange);

                dto.setRates(mapUnitRates(rates));
            }
            return dto;
        }

        // Item WITH sub-items
        List<SubItemRateDTO> subItemDTOs = item.getSubItems()
                .stream()
                .map(subItem -> mapSubItem(subItem, yearRange))
                .toList();

        dto.setSubItems(subItemDTOs);
        return dto;
    }

    private SubItemRateDTO mapSubItem(SubItems subItem, YearRange yearRange) {

        SubItemRateDTO dto = new SubItemRateDTO();
        dto.setId(subItem.getId());
        dto.setName(subItem.getName());

        if (yearRange != null) {
            List<Rate> rates = rateRepository
                    .findBySubItemAndYearRange(subItem, yearRange);

            dto.setRates(mapUnitRates(rates));
        }

        return dto;
    }

    private List<UnitRateDTO> mapUnitRates(List<Rate> rates) {

        return rates.stream()
                .map(rate -> new UnitRateDTO(
                        rate.getUnit().getId(),
                        rate.getUnit().getUnit(),
                        rate.getUnit().getName(),
                        rate.getRate()
                ))
                .toList();
    }
    
    public Page<ItemRateDTO> getRates_old(String category, Integer yearRangeId, Pageable pageable) {

        Page<Rate> ratePage;
        System.out.println("yearRangeId: "+yearRangeId+" category: "+category);
//        Page<Long> itemPage =
//    		    rateRepository.findDistinctItemIds(
//    		        yearRangeId, category,
//    		        pageable
//    		    );
        Page<Long> itemPage =
    		    itemRepository.findDistinctItemIds(
    		         category,
    		        pageable
    		    );
        int totalElements = itemPage.getContent().size();
        System.out.println("Distinct: "+itemPage.getContent()+" size : "+itemPage.getContent().size());
        List<Rate> rates =
    		    rateRepository.findByYearRange_IdAndItem_IdIn(
    		        yearRangeId,
    		        itemPage.getContent(),
    		        Sort.by("item.id").ascending()
    		    );
    		ratePage = new PageImpl<>(
    			    rates,
    			    itemPage.getPageable(),      
    			    totalElements
    			);

 
        Map<Long, ItemRateDTO> grouped = new LinkedHashMap<>();
        Map<Long, Map<Long, SubItemRateDTO>> itemSubItemRateMap = new HashMap<>();


        // --- FIRST PASS: process all rates ---
        for (Rate rate : ratePage.getContent()) {

            // --- ITEM RATE (subItem == null) ---
            if (rate.getSubItem() == null) {

                Item item = rate.getItem();

                grouped.putIfAbsent(
                    item.getId(),
                    new ItemRateDTO(
                        item.getId(),
                        item.getName(),
                        item.getCategory().getName(),
                        item.getCategory().getCode(),
                        rate.getYearRange().getStartYear(),
                        rate.getYearRange().getEndYear(),
                        
                        null, null,
                        new ArrayList<>(),
                        new ArrayList<>()
                    )
                );

                grouped.get(item.getId()).setUnit(rate.getUnit().getUnit());
                grouped.get(item.getId()).setRate(rate.getRate());
             // ✅ Add unit-wise rate instead of setting unit/rate directly Hazel
//                grouped.get(item.getId()).getRates().add(
//                    new UnitRateDTO(
//                        rate.getUnit().getId(),
//                        rate.getUnit().getName(),
//                        rate.getRate(),
//                        item.getId(),
//                        null,                      // subItemId
//                        rate.getUnit().getUnit(),
//                        rate.getBalance()
//                    )
//                );
            }


            // --- SUB-ITEM RATE ---
            else {

                SubItems sub = rate.getSubItem();
                Item item = sub.getItem();

                grouped.putIfAbsent(
                    item.getId(),
                    new ItemRateDTO(
                        item.getId(),
                        item.getName(),
                        item.getCategory().getName(),
                        item.getCategory().getCode(),
                        rate.getYearRange().getStartYear(),
                        rate.getYearRange().getEndYear(),
                        null, null,
                        new ArrayList<>(),
                        new ArrayList<>() //Hazel
                    )
                );

                itemSubItemRateMap.putIfAbsent(item.getId(), new HashMap<>());

                itemSubItemRateMap.get(item.getId()).put(
                    sub.getId(),
                    new SubItemRateDTO(
                        sub.getId(),
                        sub.getName(),
                        rate.getUnit().getUnit(),
                        rate.getRate(), new ArrayList<>() //Hazel
                    )
                );

                // If sub-item rates exist, item-level rate does not apply
                grouped.get(item.getId()).setUnit(null);
                grouped.get(item.getId()).setRate(null);
            }
        }


        // --- SECOND PASS: add missing subItems ---
        for (ItemRateDTO dto : grouped.values()) {

            // find a rate that belongs to this item
            Rate exampleRate = ratePage.getContent().stream()
                .filter(r -> r.getItem() != null && r.getItem().getId().equals(dto.getId()))
                .findFirst()
                .orElse(null);

            if (exampleRate == null) continue;

            Item item = exampleRate.getItem();

            for (SubItems sub : item.getSubItems()) {

                Map<Long, SubItemRateDTO> existingSubMap =
                        itemSubItemRateMap.getOrDefault(dto.getId(), new HashMap<>());

                if (!existingSubMap.containsKey(sub.getId())) {

                    // No rate defined for this subItem
                    dto.getSubItems().add(
                        new SubItemRateDTO(
                            sub.getId(),
                            sub.getName(),
                            null,
                            null, new ArrayList<>()  //Hazel
                        )
                    );
                } else {

                    dto.getSubItems().add(existingSubMap.get(sub.getId()));
                }
            }
        }


        List<ItemRateDTO> dtoList = new ArrayList<>(grouped.values());
        return new PageImpl<>(dtoList, pageable, ratePage.getTotalElements());
    }
    
    public Page<ItemRateDTO> getRatess(String category, Integer yearRangeId, Pageable pageable) {

        Page<Rate> ratePage;
        System.out.println("yearRangeId: "+yearRangeId+" category: "+category);
//        Page<Long> itemPage =
//    		    rateRepository.findDistinctItemIds(
//    		        yearRangeId, category,
//    		        pageable
//    		    );
        Page<Long> itemPage =
    		    itemRepository.findDistinctItemIds(
    		         category,
    		        pageable
    		    );
        int totalElements = itemPage.getContent().size();
        System.out.println("Distinct: "+itemPage.getContent()+" size : "+itemPage.getContent().size());
        List<Rate> rates =
    		    rateRepository.findByYearRange_IdAndItem_IdIn(
    		        yearRangeId,
    		        itemPage.getContent(),
    		        Sort.by("item.id").ascending()
    		    );
    		ratePage = new PageImpl<>(
    			    rates,
    			    itemPage.getPageable(),      
    			    totalElements
    			);

        // ---- FILTER PRIORITY ----
//        if (category != null && yearRangeId != null) {
//            ratePage = rateRepository.findByCategory_CodeAndYearRange_Id(category, yearRangeId, pageable);
//        } 
//        else if (category != null) {
//            ratePage = rateRepository.findByCategory_Code(category, pageable);
//        } 
//        else if (yearRangeId != null) {
//        	
//
//        		List<Rate> rates =
//        		    rateRepository.findByYearRange_IdAndItem_IdIn(
//        		        yearRangeId,
//        		        itemPage.getContent(),
//        		        Sort.by("item.id").ascending()
//        		    );
//        		ratePage = new PageImpl<>(
//        			    rates,
//        			    itemPage.getPageable(),      
//        			    itemPage.getTotalElements()  
//        			);
//            //ratePage = rateRepository.findByYearRange_Id(yearRangeId, pageable);
//        } 
//        else {
//        	 ratePage = rateRepository.findAll(pageable);
//        	// System.out.println("All rates: "+ratePage.getContent());
//        }

        Map<Long, ItemRateDTO> grouped = new LinkedHashMap<>();
        Map<Long, Map<Long, SubItemRateDTO>> itemSubItemRateMap = new HashMap<>();


        // --- FIRST PASS: process all rates ---
        for (Rate rate : ratePage.getContent()) {

            // --- ITEM RATE (subItem == null) ---
            if (rate.getSubItem() == null) {

                Item item = rate.getItem();

                grouped.putIfAbsent(
                    item.getId(),
                    new ItemRateDTO(
                        item.getId(),
                        item.getName(),
                        item.getCategory().getName(),
                        item.getCategory().getCode(),
                        rate.getYearRange().getStartYear(),
                        rate.getYearRange().getEndYear(),
                        
                        null, null,
                        new ArrayList<>(),new ArrayList<>() //Hazel
                    )
                );

                grouped.get(item.getId()).setUnit(rate.getUnit().getUnit());
                grouped.get(item.getId()).setRate(rate.getRate());
            }


            // --- SUB-ITEM RATE ---
            else {

                SubItems sub = rate.getSubItem();
                Item item = sub.getItem();

                grouped.putIfAbsent(
                    item.getId(),
                    new ItemRateDTO(
                        item.getId(),
                        item.getName(),
                        item.getCategory().getName(),
                        item.getCategory().getCode(),
                        rate.getYearRange().getStartYear(),
                        rate.getYearRange().getEndYear(),
                        null, null,
                        new ArrayList<>(), new ArrayList<>() //Hazel
                    )
                );

                itemSubItemRateMap.putIfAbsent(item.getId(), new HashMap<>());

                itemSubItemRateMap.get(item.getId()).put(
                    sub.getId(),
                    new SubItemRateDTO(
                        sub.getId(),
                        sub.getName(),
                        rate.getUnit().getUnit(),
                        rate.getRate(), new ArrayList<>() //Hazel
                    )
                );

                // If sub-item rates exist, item-level rate does not apply
                grouped.get(item.getId()).setUnit(null);
                grouped.get(item.getId()).setRate(null);
            }
        }


        // --- SECOND PASS: add missing subItems ---
        for (ItemRateDTO dto : grouped.values()) {

            // find a rate that belongs to this item
            Rate exampleRate = ratePage.getContent().stream()
                .filter(r -> r.getItem() != null && r.getItem().getId().equals(dto.getId()))
                .findFirst()
                .orElse(null);

            if (exampleRate == null) continue;

            Item item = exampleRate.getItem();

            for (SubItems sub : item.getSubItems()) {

                Map<Long, SubItemRateDTO> existingSubMap =
                        itemSubItemRateMap.getOrDefault(dto.getId(), new HashMap<>());

                if (!existingSubMap.containsKey(sub.getId())) {

                    // No rate defined for this subItem
                    dto.getSubItems().add(
                        new SubItemRateDTO(
                            sub.getId(),
                            sub.getName(),
                            null,
                            null, new ArrayList<>() //Hazel
                        )
                    );
                } else {

                    dto.getSubItems().add(existingSubMap.get(sub.getId()));
                }
            }
        }


        List<ItemRateDTO> dtoList = new ArrayList<>(grouped.values());
        return new PageImpl<>(dtoList, pageable, ratePage.getTotalElements());
    }


//	public Page<ItemRateDTO> getRates2(String category, Integer yearRangeId, Pageable pageable){
//		
//		Page<Rate> ratePage;
//
//        // ---- FILTER PRIORITY ----
//        if (category != null && yearRangeId != null) {
//            ratePage = rateRepository.findByCategory_CodeAndYearRange_Id(category, yearRangeId, pageable);
//        } 
//        else if (category != null) {
//            ratePage = rateRepository.findByCategory_Code(category, pageable);
//        } 
//        else if (yearRangeId != null) {
//            ratePage = rateRepository.findByYearRange_Id(yearRangeId, pageable);
//        } 
//        else {
//            ratePage = rateRepository.findAll(pageable);
//        }
//        System.out.println("Rate: "+ratePage.getContent());
//        Map<Long, ItemRateDTO> grouped = new LinkedHashMap<>();
//        Map<Long, Map<Long, SubItemRateDTO>> itemSubItemRateMap = new HashMap<>();
//
//        // --- FIRST PASS: process all rates ---
//        for (Rate rate : ratePage.getContent()) {
//
//            if (rate.getObjectType().equals("item")) {
//
//                Item item = rate.getItem();
//
//                grouped.putIfAbsent(
//                    item.getId(),
//                    new ItemRateDTO(
//                        item.getId(),
//                        item.getName(),
//                        item.getCategory().getName(),
//                        rate.getYearRange().getStartYear(),
//                        rate.getYearRange().getEndYear(),
//                        null, null,
//                        new ArrayList<>()
//                    )
//                );
//
//                grouped.get(item.getId()).setUnit(rate.getUnit().getUnit());
//                grouped.get(item.getId()).setRate(rate.getRate());
//            }
//
//            else if (rate.getObjectType().equals("subItem")) {
//
//                SubItems sub = rate.getSubItem();
//                Item item = sub.getItem();
//
//                grouped.putIfAbsent(
//                    item.getId(),
//                    new ItemRateDTO(
//                        item.getId(),
//                        item.getName(),
//                        item.getCategory().getName(),
//                        rate.getYearRange().getStartYear(),
//                        rate.getYearRange().getEndYear(),
//                        null, null,
//                        new ArrayList<>()
//                    )
//                );
//
//                itemSubItemRateMap.putIfAbsent(item.getId(), new HashMap<>());
//
//                // store the rate for this subItem
//                itemSubItemRateMap.get(item.getId()).put(
//                    sub.getId(),
//                    new SubItemRateDTO(
//                        sub.getId(),
//                        sub.getName(),
//                        rate.getUnit().getUnit(),
//                        rate.getRate()
//                    )
//                );
//
//                grouped.get(item.getId()).setUnit(null);
//                grouped.get(item.getId()).setRate(null);
//            }
//        }
//
//        // --- SECOND PASS: add missing subItems (unit = null, rate = null) ---
//        for (ItemRateDTO dto : grouped.values()) {
//
//            // find the original item
//            Rate exampleRate = ratePage.getContent().stream()
//                .filter(r -> r.getItem() != null && r.getItem().getId().equals(dto.getId()))
//                .findFirst()
//                .orElse(null);
//
//            if (exampleRate == null) continue;
//
//            Item item = exampleRate.getItem();
//
//            // iterate item.subItems()
//            for (SubItems sub : item.getSubItems()) {
//
//                Map<Long, SubItemRateDTO> existing = itemSubItemRateMap.getOrDefault(dto.getId(), new HashMap<>());
//
//                if (!existing.containsKey(sub.getId())) {
//                    // subItem has NO rate → add null unit & rate
//                    dto.getSubItems().add(
//                        new SubItemRateDTO(
//                            sub.getId(),
//                            sub.getName(),
//                            null,
//                            null
//                        )
//                    );
//                } else {
//                    // subItem has a rate → add it
//                    dto.getSubItems().add(existing.get(sub.getId()));
//                }
//            }
//        }
//
//        List<ItemRateDTO> dtoList = new ArrayList<>(grouped.values());
//        return new PageImpl<>(dtoList, pageable, ratePage.getTotalElements());
//    }
//	
	public Page<Rate> getRates1(String category, Integer yearRangeId, Pageable pageable){
		
		if (category != null && yearRangeId != null) {
	        return rateRepository.findByCategory_CodeAndYearRange_Id(category, yearRangeId, pageable);
	    }

	    if (category != null) {
	        return rateRepository.findByCategory_Code(category, pageable);
	    }

	    if (yearRangeId != null) {
	        return rateRepository.findByYearRange_Id(yearRangeId, pageable);
	    }

	    return rateRepository.findAll(pageable);
	}
	
	public String createRate(ItemRateCreateDTO request) {
		
		YearRange yearRange = yearRangeRepository.findById(request.getYearRangeId())
                .orElseThrow(() -> new RuntimeException("YearRange not found"));

		SubItems subItemm=null;
		Item item = itemRepository.findById(request.getItemId())
				.orElseThrow(() -> new RuntimeException("Item not found"));
		if(request.getSubItemId()!=null) {
			boolean exists=false;
			
			for(SubItems subItem:item.getSubItems()) {
				if(subItem.getId()==request.getSubItemId()) {
					subItemm = subItem;
					exists=true;
				}
			}
			if(!exists)
				throw new RuntimeException("Sub-item not found");
		}		
        
        Category category = categoryRepository.findById(request.getCategoryCode())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        Unit unit = unitRepository.findById(request.getUnitId())
        		.orElseThrow(()-> new RuntimeException("Unit not found"));
        if(request.getSubItemId()==null) {
        	if(rateRepository.findByItem_IdAndSubItemIsNullAndYearRange_IdAndUnit_Id(request.getItemId(), request.getYearRangeId(),
        			unit.getId()).isPresent())
        		throw new RuntimeException("Rate already present");
        }
        else {
        	if(rateRepository.findByItem_IdAndSubItem_IdAndYearRange_IdAndUnit_Id(request.getItemId(), request.getSubItemId(), request.getYearRangeId(), request.getUnitId()).isPresent())
				throw new RuntimeException("Rate already present");
        }
        
        		//.findByObjectTypeAndObjectIdAndYearRange_Id(request.getSubItemId()==null?"item":"subItem", 
        		//request.getSubItemId()==null?item.getId():request.getSubItemId(), request.getYearRangeId()).isPresent())
        	
        Rate itemRate = new Rate();

        //itemRate.setObjectType(request.getSubItemId()==null?"item":"subItem");
        //itemRate.setObjectId(request.getSubItemId()==null?item.getId():request.getSubItemId());
        //if(request.getSubItemId()==null)
        itemRate.setItem(item);
        if(request.getSubItemId()!=null)
        	itemRate.setSubItem(subItemm);
        itemRate.setYearRange(yearRange);
        itemRate.setUnit(unit);
        itemRate.setCategory(category);
        itemRate.setRate(request.getRate());
        itemRate.setEntryDate(LocalDateTime.now());

        rateRepository.save(itemRate);
		return("Rate added");
	}
	
	public Double getRate(Integer unitId, Long itemId, Long subItemId, Integer yearRangeId) {
		
		Rate rate = rateRepository.findRates(itemId, subItemId, yearRangeId, unitId).orElseThrow(() -> new ObjectNotFoundException("Rate not available"));
		return rate.getRate();
	}
	
	public String addRate(ItemRateCreateDTO request) {
		
		System.out.println("Input: "+request);
		
		YearRange yearRange = yearRangeRepository.findById(request.getYearRangeId())
                .orElseThrow(() -> new RuntimeException("YearRange not found"));

		SubItems subItemm=null;
		Item item = itemRepository.findById(request.getItemId())
				.orElseThrow(() -> new RuntimeException("Item not found"));
		if(request.getSubItemId()!=null) {
			boolean exists=false;
			System.out.println("Sub-item ID: "+request.getSubItemId());
			for(SubItems subItem:item.getSubItems()) {
				
				System.out.println("subItem ID: "+subItem.getId()+" request ID: "+request.getSubItemId());
				System.out.println("First type: "+item.getName()+" sub-item "+subItem);
				//if(subItem.getId()==request.getSubItemId()) {
				if (subItem.getId().equals(request.getSubItemId())) {
					System.out.println("True "+subItem.getId());
					subItemm = subItem;
					exists=true;
				}
			}
			if(!exists)
				throw new RuntimeException("Sub-item not found");
		}		
        
        Unit unit = unitRepository.findById(request.getUnitId())
        		.orElseThrow(()-> new RuntimeException("Unit not found"));
        
        if(request.getSubItemId()==null) {
        	if(rateRepository.findByItem_IdAndSubItemIsNullAndYearRange_IdAndUnit_Id(request.getItemId(), request.getYearRangeId(),
        			unit.getId()).isPresent())
        		throw new RuntimeException("Rate already present");
        }
        else {
        	if(rateRepository.findByItem_IdAndSubItem_IdAndYearRange_IdAndUnit_Id(request.getItemId(), request.getSubItemId(), request.getYearRangeId(), request.getUnitId()).isPresent())
				throw new RuntimeException("Rate already present");
        }
        
        		//.findByObjectTypeAndObjectIdAndYearRange_Id(request.getSubItemId()==null?"item":"subItem", 
        		//request.getSubItemId()==null?item.getId():request.getSubItemId(), request.getYearRangeId()).isPresent())
        	
        Rate itemRate = new Rate();

        //itemRate.setObjectType(request.getSubItemId()==null?"item":"subItem");
        //itemRate.setObjectId(request.getSubItemId()==null?item.getId():request.getSubItemId());
        //if(request.getSubItemId()==null)
        itemRate.setItem(item);
        if(request.getSubItemId()!=null)
        	itemRate.setSubItem(subItemm);
        itemRate.setYearRange(yearRange);
        itemRate.setUnit(unit);
        itemRate.setCategory(item.getCategory());
        itemRate.setRate(request.getRate());
        itemRate.setEntryDate(LocalDateTime.now());

        rateRepository.save(itemRate);
		return("Rate added");
	}
	
	public byte[] exportRates(String categoryCode, Integer yearRangeId) {

	    YearRange yearRange = null;
	    if (yearRangeId != null) {
	        yearRange = yearRangeRepository.findById(yearRangeId)
	                .orElseThrow(() -> new RuntimeException("YearRange not found"));
	    }

	    List<Item> items = (categoryCode != null && !categoryCode.isBlank())
	            ? itemRepository.findAllByCategory_Code(categoryCode)
	            : itemRepository.findAll();

	    try (Workbook workbook = new XSSFWorkbook();
	         ByteArrayOutputStream out = new ByteArrayOutputStream()) {

	        Sheet sheet = workbook.createSheet("Rates");
	        Map<String, CellStyle> styles = excelService.createStyles(workbook);


	        int rowIdx = 0;
	        
	        rowIdx = excelService.createTitleRow(
	                workbook,
	                sheet,
	                rowIdx,
	                "Rates",
	                0,
	                5
	        );

	        // =====================
	        // METADATA
	        // =====================
	        rowIdx++;
	        
	        rowIdx = excelService.createLabelValueRow(
	                sheet,
	                rowIdx,
	                "Year:",
	                yearRange != null
	                        ? yearRange.getStartYear() + " - " + yearRange.getEndYear()
	                        : "All",
	                styles.get("bold")
	        );

	        String categoryName = (categoryCode!=null && categoryCode.length()>0)? categoryRepository.findByCode(categoryCode).get().getName():"All";
	        rowIdx = excelService.createLabelValueRow(
	                sheet,
	                rowIdx,
	                "Category:",
	                categoryName,
	                styles.get("bold")
	        );

	        rowIdx++;

	        // =====================
	        // TABLE HEADER
	        // =====================

	        String[] headers = { "Sl No.", "Item","", "Category", "Unit", "Rate (₹)" };

	        rowIdx = excelService.createTableHeaderRow(
	                sheet,
	                rowIdx,
	                headers,
	                styles.get("headerBorder")
	        );
	        int headerRowIndex = rowIdx - 1;

	     // merge columns 2 and 3
	     sheet.addMergedRegion(new CellRangeAddress(headerRowIndex, headerRowIndex, 1, 2));

	     // ensure border style applies to merged cells
	     Row headerRow1 = sheet.getRow(headerRowIndex);
	     for (int col = 1; col <= 2; col++) {
	         headerRow1.getCell(col).setCellStyle(styles.get("headerBorder"));
	     }

	        // =====================
	        // TABLE DATA
	        // =====================
	     int slNo = 1;

	     for (Item item : items) {

	         // =====================
	         // ITEM WITHOUT SUB-ITEMS
	         // =====================
	         if (item.getSubItems() == null || item.getSubItems().isEmpty()) {

	             List<Rate> rates = yearRange != null
	                     ? rateRepository.findByItemAndYearRange(item, yearRange)
	                     : rateRepository.findByItem(item);

	             for (Rate rate : rates) {
	                 int currentRow = rowIdx;

	                 Row row = sheet.createRow(rowIdx++);
	                 row.createCell(0).setCellValue(slNo++);
	                 row.createCell(1).setCellValue(item.getName());
	                 row.createCell(2); // empty cell required for merge
	                 row.createCell(3).setCellValue(item.getCategory().getName());
	                 row.createCell(4).setCellValue(rate.getUnit().getName());
	                 row.createCell(5).setCellValue(rate.getRate());
	                 for (int col = 0; col <= 5; col++) {
	                	    Cell cell = row.getCell(col);
	                	    if (cell == null) {
	                	        cell = row.createCell(col);
	                	    }

	                	    cell.setCellStyle(
	                	        (col == 1 || col == 2)
	                	            ? styles.get("wrapBorder")
	                	            : styles.get("border")
	                	    );
	                	}

	                 // merge cell 1 & 2 horizontally
	                 CellRangeAddress region1 =
	                	        new CellRangeAddress(currentRow, currentRow, 1, 2);

	                	sheet.addMergedRegion(region1);
	                	excelService.applyBorder(region1, sheet);
	             }

	             continue;
	         }

	         // =====================
	         // ITEM WITH SUB-ITEMS
	         // =====================
	         int itemStartRow = rowIdx;
	         int slNoForItem = slNo++;

	         for (SubItems subItem : item.getSubItems()) {

	             List<Rate> rates = yearRange != null
	                     ? rateRepository.findBySubItemAndYearRange(subItem, yearRange)
	                     : rateRepository.findBySubItem(subItem);

	             for (Rate rate : rates) {
	                 Row row = sheet.createRow(rowIdx++);

	                 // Sl. No. & Item Name will be merged later
	                 row.createCell(0).setCellValue(slNoForItem);
	                 row.createCell(1).setCellValue(item.getName());
	                 row.createCell(2).setCellValue(subItem.getName());
	                 row.createCell(3).setCellValue(item.getCategory().getName()); // ✅ FIX
	                 row.createCell(4).setCellValue(rate.getUnit().getName());
	                 row.createCell(5).setCellValue(rate.getRate());
	                 for (int col = 0; col <= 5; col++) {
	                	    Cell cell = row.getCell(col);
	                	    if (cell == null) {
	                	        cell = row.createCell(col);
	                	    }
	                	 // wrap text only for item & sub-item columns
	                	    if (col == 1 || col == 2) {
	                	        cell.setCellStyle(styles.get("wrapBorder"));
	                	    } else {
	                	        cell.setCellStyle(styles.get("border"));
	                	    }
	                	}

	             }
	         }

	         int itemEndRow = rowIdx - 1;

	         // vertical merge Sl. No.
	         excelService.mergeVertically(sheet, itemStartRow, itemEndRow, 0);

	         // vertical merge Item Name
	         excelService.mergeVertically(sheet, itemStartRow, itemEndRow, 1);
	         excelService.mergeVertically(sheet, itemStartRow, itemEndRow, 3);
	     }


	        // =====================
	        // AUTO SIZE
	        // =====================
	        for (int i = 0; i < headers.length; i++) {
	            sheet.autoSizeColumn(i);
	        }

	        workbook.write(out);
	        return out.toByteArray();

	    } catch (IOException e) {
	        throw new RuntimeException("Failed to export Excel", e);
	    }
	}

}
