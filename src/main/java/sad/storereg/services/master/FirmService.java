package sad.storereg.services.master;

import java.util.List;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.io.ByteArrayOutputStream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import sad.storereg.dto.master.CreateFirmDTO;
import sad.storereg.dto.master.FirmApproveDTO;
import sad.storereg.dto.master.FirmCheckDTO;
import sad.storereg.dto.master.FirmYearDTO;
import sad.storereg.dto.master.FirmsDTO;
import sad.storereg.exception.ObjectNotFoundException;
import sad.storereg.exception.UnauthorizedException;
import sad.storereg.models.master.Category;
import sad.storereg.models.master.Firm;
import sad.storereg.models.master.FirmCategory;
import sad.storereg.models.master.FirmYear;
import sad.storereg.models.master.YearRange;
import sad.storereg.repo.master.CategoryRepository;
import sad.storereg.repo.master.FirmCategoryRepository;
import sad.storereg.repo.master.FirmYearRepository;
import sad.storereg.repo.master.FirmsRepository;
import sad.storereg.repo.master.YearRangeRepository;
import sad.storereg.services.appdata.ExcelServices;
import sad.storereg.services.appdata.PurchaseService;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.usermodel.BorderStyle;


@Service
@RequiredArgsConstructor
public class FirmService {
	
	private final FirmsRepository firmRepository;
	private final FirmCategoryRepository firmCategoryRepository;
	private final CategoryRepository categoryRepository;
	private final FirmYearRepository firmYearRepository;
	private final YearRangeRepository yearRangeRepository;
	private final PurchaseService purchaseService;
	private final ExcelServices excelService;

	public Page<FirmsDTO> getFirms(Pageable pageable, String search, String category) {
	    Page<Firm> page;

	    if (category == null || category.isEmpty() || category.equals("All")) {
	        page = firmRepository.findAll(pageable);
	    } else {
	        // Query firms by category through FirmCategory
	        Page<FirmCategory> fcPage = firmCategoryRepository.findByCategory_Code(category, pageable);

	        // Extract unique firms
	        List<Firm> firms = fcPage.stream()
	                .map(FirmCategory::getFirm)
	                .distinct()
	                .toList();

	        page = new PageImpl<>(firms, pageable, fcPage.getTotalElements());
	    }
	    
	    return page.map(this::convertToDto2);
	    
	}
	
	private FirmsDTO convertToDto2(Firm firm) {

	    List<FirmYear> firmYears =
	            firmYearRepository.findByFirm_Id(firm.getId());

	    // Group by YearRange ID (NOT entity instance)
	    Map<Integer, List<FirmYear>> byYearRange =
	            firmYears.stream()
	                .collect(Collectors.groupingBy(
	                    fy -> fy.getYearRange().getId()
	                ));

	    List<YearRange> yearRanges =
	            byYearRange.values().stream()
	                .map(fyList -> {

	                    YearRange source = fyList.get(0).getYearRange();

	                    // CREATE A NEW INSTANCE (important)
	                    YearRange yr = new YearRange();
	                    yr.setId(source.getId());
	                    yr.setStartYear(source.getStartYear());
	                    yr.setEndYear(source.getEndYear());

	                    List<String> categoryCodes =
	                            fyList.stream()
	                                .map(fy -> fy.getCategory().getCode())
	                                .distinct()
	                                .sorted()  
	                                .toList();

	                    yr.setCategoryCodes(categoryCodes);

	                    return yr;
	                })
	                .toList();

	    return FirmsDTO.builder()
	            .id(firm.getId())
	            .firm(firm.getFirm())
	            .categories(
	                firm.getCategories().stream()
	                    .map(FirmCategory::getCategory)
	                    .toList()
	            )
	            .yearRanges(yearRanges)
	            .build();
	}
	
	public Page<FirmsDTO> searchFirms(Pageable pageable, String search, Integer yearRangeId) {
	    
	    
	    if(yearRangeId==null) {
	    	Page<Firm> page = firmRepository.findByFirmContainingIgnoreCase(search, pageable);
	    	return page.map(this::convertToDto2);
	    }
	    else {
	    	Page<FirmYear> firmYears =
	    	        firmYearRepository.findByYearRangeIdAndFirmNameLike(yearRangeId, search, pageable);
	    	// --- GROUP RESULT BY FIRM ---
	        Map<Long, FirmsDTO> dtoMap = new LinkedHashMap<>();

	        for (FirmYear fy : firmYears.getContent()) {

	            Long firmId = fy.getFirm().getId();

	            // Create DTO only once per firm
	            dtoMap.computeIfAbsent(firmId, id -> {
	                FirmsDTO dto = new FirmsDTO();
	                dto.setId(id);
	                dto.setFirm(fy.getFirm().getFirm());
	                dto.setCategories(new ArrayList<>());
	                dto.setYearRanges(new ArrayList<>());
	                return dto;
	            });

	            FirmsDTO dto = dtoMap.get(firmId);

	            // Add category if not already present
	            if (!dto.getCategories().contains(fy.getCategory())) {
	                dto.getCategories().add(fy.getCategory());
	            }

	            // Add yearRange if not already present
	            if (!dto.getYearRanges().contains(fy.getYearRange())) {
	                dto.getYearRanges().add(fy.getYearRange());
	            }
	        }

	        // Convert map to list
	        List<FirmsDTO> mergedList = new ArrayList<>(dtoMap.values());

	        // Pagination metadata stays from FirmYear page
	        return new PageImpl<>(mergedList, pageable, firmYears.getTotalElements());
	    }
	}
	
	private FirmsDTO convertToDto(Firm firm) {

	    // Fetch FirmYear rows for this firm
	    List<FirmYear> firmYears = firmYearRepository.findByFirm_Id(firm.getId());

	    return FirmsDTO.builder()
	            .id(firm.getId())
	            .firm(firm.getFirm())

	            // All categories belonging to this Firm
	            .categories(
	                firm.getCategories().stream()
	                    .map(FirmCategory::getCategory)
	                    .toList()
	            )

	            // YearRanges from FirmYear table
	            .yearRanges(
	                firmYears.stream()
	                        .map(FirmYear::getYearRange)
	                        .distinct()
	                        .toList()
	            )

	            .build();
	}
	
    @Transactional
    public String createFirm(CreateFirmDTO request) {
    	
    	Firm firm = Firm.builder()
    			.firm(request.getFirmName()).build();
    	firmRepository.save(firm);
    	
    	for (String code : request.getCategories()) {
    		// Get category entity by code
            Category category = categoryRepository.findById(code)
                    .orElseThrow(() -> new RuntimeException("Category code not found: " + code));
    	    FirmCategory firmCategory = FirmCategory.builder().firm(firm).category(category).build();
    	    firmCategoryRepository.save(firmCategory);
    	}
        return "Firm added";
    }
    
    public Page<FirmsDTO> getFirms_old(Integer yearRangeId, String categoryCode, Pageable pageable) {

        Page<FirmYear> firmYears;

        if (categoryCode != null && !categoryCode.isEmpty()) {
            firmYears = firmYearRepository.findByYearRange_IdAndCategory_Code(
                    yearRangeId, categoryCode, pageable
            );
        } else {
            firmYears = firmYearRepository.findByYearRange_Id(
                    yearRangeId, pageable
            );
        }

        // Build DTO list using ONLY category & yearRange from FirmYear
        List<FirmsDTO> dtos = firmYears.getContent()
                .stream()
                .map(fy -> {
                    FirmsDTO dto = new FirmsDTO();
                    dto.setId(fy.getFirm().getId());
                    dto.setFirm(fy.getFirm().getFirm());

                    // Only the category from this firm-year record
                    dto.setCategories(List.of(fy.getCategory()));

                    // Only the year range from this firm-year record
                    dto.setYearRanges(List.of(fy.getYearRange()));

                    return dto;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, firmYears.getTotalElements());
    }
    
    public Page<FirmsDTO> getFirms(Integer yearRangeId, String categoryCode, Pageable pageable) {

        Page<FirmYear> firmYears;

        if (categoryCode != null && !categoryCode.isEmpty()) {
            firmYears = firmYearRepository.findByYearRange_IdAndCategory_Code(
                    yearRangeId, categoryCode, pageable
            );
        } else {
            firmYears = firmYearRepository.findByYearRange_Id(
                    yearRangeId, pageable
            );
        }

        // --- GROUP RESULT BY FIRM ---
        Map<Long, FirmsDTO> dtoMap = new LinkedHashMap<>();

        for (FirmYear fy : firmYears.getContent()) {

            Long firmId = fy.getFirm().getId();

            // Create DTO only once per firm
            dtoMap.computeIfAbsent(firmId, id -> {
                FirmsDTO dto = new FirmsDTO();
                dto.setId(id);
                dto.setFirm(fy.getFirm().getFirm());
                dto.setCategories(new ArrayList<>());
                dto.setYearRanges(new ArrayList<>());
                return dto;
            });

            FirmsDTO dto = dtoMap.get(firmId);

            // Add category if not already present
            if (!dto.getCategories().contains(fy.getCategory())) {
                dto.getCategories().add(fy.getCategory());
            }

            // Add yearRange if not already present
            if (!dto.getYearRanges().contains(fy.getYearRange())) {
                dto.getYearRanges().add(fy.getYearRange());
            }
        }

        // Convert map to list
        List<FirmsDTO> mergedList = new ArrayList<>(dtoMap.values());

        // Pagination metadata stays from FirmYear page
        return new PageImpl<>(mergedList, pageable, firmYears.getTotalElements());
    }

    public List<FirmsDTO> getFirmsList() {
        // Fetch all firms
        List<Firm> firms = firmRepository.findAll();

        // Convert to DTO
        return firms.stream()
                .map(this::convertToDto)
                .toList();
    }
    
    @Transactional
    public String createFirmYear(FirmYearDTO request) {

        // --- Validate Firm ---
        Firm firm = firmRepository.findById(request.getFirmId())
                .orElseThrow(() -> new RuntimeException("Firm not found with id: " + request.getFirmId()));

        // --- Validate YearRange ---
        YearRange yearRange = yearRangeRepository.findById(request.getYearRangeId())
                .orElseThrow(() -> new RuntimeException("YearRange not found with id: " + request.getYearRangeId()));

        List<FirmYear> created = new ArrayList<>();

        // --- Create one FirmYear per category ---
        for (String categoryCode : request.getCategories()) {

            Category category = categoryRepository.findById(categoryCode)
                    .orElseThrow(() -> new RuntimeException("Category not found: " + categoryCode));

            if(firmYearRepository.findByYearRange_IdAndCategory_CodeAndFirm_Id(yearRange.getId(), categoryCode, firm.getId()).isEmpty()){
            
	            FirmYear fy = new FirmYear();
	            fy.setFirm(firm);
	            fy.setYearRange(yearRange);
	            fy.setCategory(category);
	
	            created.add(firmYearRepository.save(fy));
            }
        }

        return "Firm approved";
    }
    
    public List<FirmsDTO> getFirmsListByDate(LocalDate date) {
    	
    	int year = date.getYear();
    	List<Firm> firms = new ArrayList<>();
//		Optional<YearRange> yearRange = yearRangeRepository.findByStartYearLessThanEqualAndEndYearGreaterThanEqual(year, year);
//
//		List<Firm> firms = new ArrayList<>();
//		if(yearRange.isEmpty()) {
//			firms = null;
//			
//		}
//		else
			firms = firmRepository.findAllByYear(year);

			if (firms == null || firms.isEmpty()) {
		        return List.of(); // return empty list instead of null
		    }

        // Convert to DTO
        return firms.stream()
                .map(this::convertToDto)
                .toList();
    }
    
    public Page<FirmCheckDTO> getAllFirms(
            Integer yearRangeId,
            String categoryCode,
            String search,
            Pageable pageable) {

    	return firmRepository.findFirmsWithCheckedFlag(
                search,
                categoryCode,
                yearRangeId,
                pageable
        );
    }
    
    @Transactional
    public String updateFirmYear(FirmApproveDTO request) {

        // --- Validate Firm ---
        Firm firm = firmRepository.findById(request.getFirmId())
                .orElseThrow(() -> new UnauthorizedException("Firm not found with id: " + request.getFirmId()));

        // --- Validate YearRange ---
        YearRange yearRange = yearRangeRepository.findById(request.getYearRangeId())
                .orElseThrow(() -> new UnauthorizedException("YearRange not found with id: " + request.getYearRangeId()));
        
        Category category = categoryRepository.findByCode(request.getCategoryCode()).orElseThrow(() -> new UnauthorizedException("Catgeory not found with code: " + request.getCategoryCode()));

        Optional<FirmYear> fy= firmYearRepository.findByYearRange_IdAndCategory_CodeAndFirm_Id(yearRange.getId(), category.getCode(), firm.getId());
        if(fy.isPresent())
        {
        	if(purchaseService.purchaseExist(firm, yearRange, category))
        		throw new UnauthorizedException("Purchase/Purchases already made for this Firm, Year and category");
        	else {
        		firmYearRepository.deleteById(fy.get().getId());
        		return "Firm un-approved";
        	}
        }
        else {
	        FirmYear firmYear = new FirmYear();
	        firmYear.setFirm(firm);
	        firmYear.setCategory(category);
	        firmYear.setYearRange(yearRange);
	        firmYearRepository.save(firmYear);
	        return "Firm approved";
        }
    }
    
    @Transactional
    public String updateFirm(FirmsDTO request) {
    	
    	if(request.getId()==null || request.getFirm()==null || request.getFirm().length()==0) {
    		throw new UnauthorizedException("Firm ID and Firm are required");
    	}
    
    	Firm firm = firmRepository.findById(request.getId()).orElseThrow(()-> new ObjectNotFoundException("Invalid Firm ID"));
    	
    	firm.setFirm(request.getFirm());
    	firmRepository.save(firm);
        return "Firm updated";
    }
    
    public List<FirmsDTO> getFirmsForExport(Integer yearRangeId, String categoryCode) {

        Page<FirmYear> firmYears;

        if (categoryCode != null && !categoryCode.isEmpty()) {
            firmYears = firmYearRepository.findByYearRange_IdAndCategory_Code(
                    yearRangeId, categoryCode, Pageable.unpaged()
            );
        } else {
            firmYears = firmYearRepository.findByYearRange_Id(
                    yearRangeId, Pageable.unpaged()
            );
        }

        Map<Long, FirmsDTO> dtoMap = new LinkedHashMap<>();

        for (FirmYear fy : firmYears.getContent()) {

            Long firmId = fy.getFirm().getId();

            dtoMap.computeIfAbsent(firmId, id -> {
                FirmsDTO dto = new FirmsDTO();
                dto.setId(id);
                dto.setFirm(fy.getFirm().getFirm());
                dto.setCategories(new ArrayList<>());
                dto.setYearRanges(new ArrayList<>());
                return dto;
            });

            FirmsDTO dto = dtoMap.get(firmId);

            if (!dto.getCategories().contains(fy.getCategory())) {
                dto.getCategories().add(fy.getCategory());
            }

            if (!dto.getYearRanges().contains(fy.getYearRange())) {
                dto.getYearRanges().add(fy.getYearRange());
            }
        }

        return new ArrayList<>(dtoMap.values());
    }

    
    public byte[] exportApprovedFirms(Integer yearRangeId, String categoryCode) throws IOException {

        List<FirmsDTO> firms = getFirmsForExport(yearRangeId, categoryCode);
        

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        	
        	Sheet sheet = workbook.createSheet("Approved Firms");
        	Map<String, CellStyle> styles = excelService.createStyles(workbook);

        	int rowIdx = 0;

        	rowIdx = excelService.createTitleRow(
	                workbook,
	                sheet,
	                rowIdx,
	                "Approved Firms",
	                0,
	                2
	        );
        	rowIdx++; // blank row after title

        	rowIdx = excelService.createLabelValueRow(
	                sheet,
	                rowIdx,
	                "Year:",
	                getYearText(firms),
	                styles.get("bold")
	        );
        	// ===== YEAR =====

        	// ===== CATEGORY =====
        	String cat = categoryCode;
        	if(categoryCode!=null && categoryCode!="") {
        		Optional<Category> category = categoryRepository.findByCode(categoryCode);
        		if(category.isPresent())
        			cat=category.get().getName();
        	}
        	rowIdx = excelService.createLabelValueRow(
	                sheet,
	                rowIdx,
	                "Category:",
	                (categoryCode==null || categoryCode.length()==0) ? "All" : cat,
	                styles.get("bold")
	        );
        	
        	rowIdx++; // blank row after Category

        	Row genRow = sheet.createRow(rowIdx++);
        	Cell genLabel = genRow.createCell(0);
        	genLabel.setCellValue("Generated Date:");
        	genLabel.setCellStyle(styles.get("bold"));

        	genRow.createCell(1)
        	      .setCellValue(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        	rowIdx++; // blank row

        	// ===== TABLE HEADER =====
        	String[] headers = {"Sl No.", "Firm", "Category"};

            rowIdx = excelService.createTableHeaderRow(
                    sheet,
                    rowIdx,
                    headers,
                    styles.get("headerBorder")
            );

        	// ===== TABLE DATA WITH MERGED CELLS =====
        	int slNo = 1;

        	for (FirmsDTO dto : firms) {

        	    int startRow = rowIdx;
        	    int categoryCount = dto.getCategories().size();

        	    for (Category category : dto.getCategories()) {
        	        Row row = sheet.createRow(rowIdx++);
        	        //row.createCell(2).setCellValue(category.getName());
        	        Cell catCell = row.createCell(2);
        	        catCell.setCellValue(category.getName());

        	        catCell.setCellStyle(styles.get("border"));

        	    }

        	    int endRow = rowIdx - 1;

        	 // Sl No cell (only once)
        	 Row firstRow = sheet.getRow(startRow);

        	 Cell slCell = firstRow.createCell(0);
        	 slCell.setCellValue(slNo++);
        	 slCell.setCellStyle(styles.get("border"));

        	 Cell firmCell = firstRow.createCell(1);
        	 firmCell.setCellValue(dto.getFirm());
        	 firmCell.setCellStyle(styles.get("border"));

        	 // Merge Sl No and Firm cells if multiple categories
        	 if (categoryCount > 1) {
        		 CellRangeAddress slMerge =
     	    	        new CellRangeAddress(startRow, endRow, 0, 0);
     	    	CellRangeAddress firmMerge =
     	    	        new CellRangeAddress(startRow, endRow, 1, 1);
        	     sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, 0, 0));
        	     sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, 1, 1));
        	     excelService.applyBorder(slMerge, sheet);
        	     excelService.applyBorder(firmMerge, sheet);
        	    	
        	 }

        	}

        	// Auto-size
        	for (int i = 0; i < 3; i++) {
        	    sheet.autoSizeColumn(i);
        	}

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // ===== HELPERS =====
    private String getYearText(List<FirmsDTO> firms) {
        if (firms.isEmpty() || firms.get(0).getYearRanges().isEmpty()) {
            return "";
        }

        YearRange yr = firms.get(0).getYearRanges().get(0);
        return yr.getStartYear() + " - " + yr.getEndYear();
    }
    
    public byte[] exportAllFirms() throws IOException {

    	List<Firm> f = firmRepository.findAll();
    	List<FirmsDTO> firms = f.stream()
    	        .map(this::convertToDto2)
    	        .toList();


        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("All Firms");
            Map<String, CellStyle> styles = excelService.createStyles(workbook);

            int rowIdx = 0;
            rowIdx = excelService.createTitleRow(
	                workbook,
	                sheet,
	                rowIdx,
	                "All Firms",
	                0,
	                2
	        );

            rowIdx++;

            // ===== GENERATED DATE =====
            
            rowIdx = excelService.createLabelValueRow(
	                sheet,
	                rowIdx,
	                "Generated on:",
	                LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
	                styles.get("bold")
	        );

            rowIdx++;

            // ===== TABLE HEADER =====

            String[] headers = {"Sl. No.", "Firm", "Year Approved"};

            rowIdx = excelService.createTableHeaderRow(
                    sheet,
                    rowIdx,
                    headers,
                    styles.get("headerBorder")
            );


            // ===== TABLE DATA =====
            int slNo = 1;

            for (FirmsDTO dto : firms) {

                int startRow = rowIdx;
                List<YearRange> yearRanges = dto.getYearRanges();

                if (yearRanges == null || yearRanges.isEmpty()) {
                    Row row = sheet.createRow(rowIdx++);
                    excelService.createCell(row, 0, slNo++, styles);
                    excelService.createCell(row, 1, dto.getFirm(), styles);
                    excelService.createCell(row, 2, "", styles);
                    continue;
                }

                // Create one row per year range
                for (YearRange yr : yearRanges) {
                    Row row = sheet.createRow(rowIdx++);
                    excelService.createCell(row, 2, formatYearRange(yr), styles);
                }

                int endRow = rowIdx - 1;

                // Write Sl No & Firm only once
                Row firstRow = sheet.getRow(startRow);

                excelService.createCell(firstRow, 0, slNo++, styles);
                excelService.createCell(firstRow, 1, dto.getFirm(), styles);

                // Merge if more than one year range
                if (yearRanges.size() > 1) {
                	CellRangeAddress slMerge =
         	    	        new CellRangeAddress(startRow, endRow, 0, 0);
         	    	CellRangeAddress firmMerge =
         	    	        new CellRangeAddress(startRow, endRow, 1, 1);
                    sheet.addMergedRegion(
                        new CellRangeAddress(startRow, endRow, 0, 0)
                    );
                    sheet.addMergedRegion(
                        new CellRangeAddress(startRow, endRow, 1, 1)
                    );
            	     excelService.applyBorder(slMerge, sheet);
            	     excelService.applyBorder(firmMerge, sheet);
                }
            }


            // ===== AUTO SIZE =====
            for (int i = 0; i < 3; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private String formatYearRange(YearRange yr) {

        String yearPart = yr.getStartYear() + "-" + yr.getEndYear();

        if (yr.getCategoryCodes() == null || yr.getCategoryCodes().isEmpty()) {
            return yearPart;
        }

        String categories =
                String.join(", ", yr.getCategoryCodes());

        return yearPart + " (" + categories + ")";
    }

}
