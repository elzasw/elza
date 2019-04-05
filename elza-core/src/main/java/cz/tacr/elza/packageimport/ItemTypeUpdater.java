package cz.tacr.elza.packageimport;

import static cz.tacr.elza.packageimport.PackageService.ITEM_TYPE_XML;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.common.datetime.MultiFormatParser;
import cz.tacr.elza.core.ElzaLocale;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemSpecRegister;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPackageDependency;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.domain.table.ElzaColumn;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.PackageCode;
import cz.tacr.elza.packageimport.xml.Category;
import cz.tacr.elza.packageimport.xml.Column;
import cz.tacr.elza.packageimport.xml.DisplayType;
import cz.tacr.elza.packageimport.xml.ItemSpec;
import cz.tacr.elza.packageimport.xml.ItemSpecRegister;
import cz.tacr.elza.packageimport.xml.ItemSpecs;
import cz.tacr.elza.packageimport.xml.ItemType;
import cz.tacr.elza.packageimport.xml.ItemTypes;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.CachedNodeRepository;
import cz.tacr.elza.repository.DataDateRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DataStringRepository;
import cz.tacr.elza.repository.DataStringRepository.OnlyValues;
import cz.tacr.elza.repository.ItemRepository;
import cz.tacr.elza.repository.ItemSpecRegisterRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.PackageDependencyRepository;
import cz.tacr.elza.repository.PackageRepository;

/**
 * Class to update item types in DB
 *
 * Class will use types from XML and try to synchronize them in DB
 *
 *
 */
@Component
@Scope("prototype")
public class ItemTypeUpdater {

    private static Logger logger = LoggerFactory.getLogger(ItemTypeUpdater.class);

	/**
	 * Item types loaded from the given package
	 */
	List<RulItemType> rulItemTypesOrig;

    @Autowired
    private ElzaLocale elzaLocale;

	@Autowired
	private ItemTypeRepository itemTypeRepository;

	@Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ApItemRepository apItemRepository;

    @Autowired
    private ItemSpecRepository itemSpecRepository;

    @Autowired
    private ItemSpecRegisterRepository itemSpecRegisterRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    DataDateRepository dataDateRepository;

    @Autowired
    private DataStringRepository dataStringRepository;

    @Autowired
    CachedNodeRepository cachedNodeRepository;

    @Autowired
	private ApTypeRepository apTypeRepository;

    @Autowired
    private PackageRepository packageRepository;

    @Autowired
    private PackageDependencyRepository packageDependencyRepository;

    @Autowired
    private EntityManager em;

    public static final String CATEGORY_SEPARATOR = "|";

    /**
     * Max used view order
     */
    int maxViewOrderPos = 0;

    int lastUsedViewOrder = -1;

    /**
     * Number of nodes dropped in arr_cached_node table
     * 
     * If this number is greater then zero Node cache has to be
     * reconstructed
     */
    int numDroppedCachedNode = 0;

	public ItemTypeUpdater() {
	}


    /**
     * Porovnávání typů sloupců.
     *
     * @param elzaColumnList porovnávaný list ElzaColumn
     * @param columnList     porovnávaný list Column
     * @return jsou změněný neměnitelný položky?
     */
    private boolean equalsColumns(final List<ElzaColumn> elzaColumnList, final List<Column> columnList) {
        if (elzaColumnList.size() != columnList.size()) {
            return false;
        }

        for (int i = 0; i < elzaColumnList.size(); i++) {
            ElzaColumn elzaColumn = elzaColumnList.get(i);
            Column column = columnList.get(i);
            if (!elzaColumn.getCode().equals(column.getCode())
                    || !elzaColumn.getDataType().toString().equals(column.getDataType())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Zpracování specifikací atributů.
     * @param itemSpecs        seznam importovaných specifikací
     * @param rulDescItemTypes  seznam typů atributů
     * @param rulPackage
     */
    private void processDescItemSpecs(
                                      @Nullable final ItemSpecs itemSpecs,
            final List<RulItemType> rulDescItemTypes, RulPackage rulPackage)
    {

        List<RulItemSpec> rulDescItemSpecs = itemSpecRepository.findByRulPackage(rulPackage);
        List<RulItemSpec> rulDescItemSpecsNew = new ArrayList<>();

        // item type code -> local view order
        Map<String, Integer> viewOrderMap = new HashMap<>();

        if (itemSpecs != null && !CollectionUtils.isEmpty(itemSpecs.getItemSpecs())) {
            for (ItemSpec itemSpec : itemSpecs.getItemSpecs()) {
                List<RulItemSpec> findItems = rulDescItemSpecs.stream()
                        .filter((r) -> r.getCode().equals(itemSpec.getCode())).collect(
                                Collectors.toList());
                RulItemSpec item;
                if (findItems.size() > 0) {
                    item = findItems.get(0);
                } else {
                    item = new RulItemSpec();
                }

                convertRulDescItemSpec(rulPackage, itemSpec, item, rulDescItemTypes);

                Integer nextViewOrder = viewOrderMap.computeIfAbsent(item.getItemType().getCode(), next -> 1);
                item.setViewOrder(nextViewOrder);
                viewOrderMap.put(item.getItemType().getCode(), ++nextViewOrder);

                rulDescItemSpecsNew.add(item);
            }
        }

        rulDescItemSpecsNew = itemSpecRepository.save(rulDescItemSpecsNew);

        processDescItemSpecsRegister((itemSpecs != null) ? itemSpecs.getItemSpecs() : Collections.emptyList(),
                                     rulDescItemSpecsNew);

        List<RulItemSpec> rulDescItemSpecsDelete = new ArrayList<>(rulDescItemSpecs);
        rulDescItemSpecsDelete.removeAll(rulDescItemSpecsNew);
        for (RulItemSpec descItemSpec : rulDescItemSpecsDelete) {
            itemSpecRegisterRepository.deleteByItemSpec(descItemSpec);
        }
        itemSpecRepository.delete(rulDescItemSpecsDelete);
    }

	/**
	 * Do the update
	 * @param itemTypes
	 * @param itemSpecs
     * @param puc
     * @return return list of updated types
	 */
    public List<RulItemType> update(final ItemTypes itemTypes,
                                    final ItemSpecs itemSpecs,
                                    final PackageContext puc) {
		prepareForUpdate(puc);

        List<RulItemType> rulItemTypesUpdated = new ArrayList<>();
        if (itemTypes != null) {
            // prepare list of updated/new items
            List<ItemType> itemTypesList = itemTypes.getItemTypes();
            if (!CollectionUtils.isEmpty(itemTypesList)) {
                rulItemTypesUpdated = updateItemTypes(itemTypesList, puc);
                // try to save updated items
                rulItemTypesUpdated = itemTypeRepository.save(rulItemTypesUpdated);
            }

        }
        List<RulItemType> rulItemTypesAllByRules = new ArrayList<>(rulItemTypesUpdated);
        rulItemTypesAllByRules.addAll(itemTypeRepository.findByRulPackage(puc.getPackage()));

        // update specifications
        processDescItemSpecs(itemSpecs, rulItemTypesAllByRules, puc.getPackage());
        postSpecsOrder();

		// delete unused item types
		List<RulItemType> rulDescItemTypesDelete = new ArrayList<>(rulItemTypesOrig);
		rulDescItemTypesDelete.removeAll(rulItemTypesUpdated);
		itemTypeRepository.delete(rulDescItemTypesDelete);

		return rulItemTypesUpdated;
	}

    /**
     * Seřazení specifikací podle balíčků.
     */
    private void postSpecsOrder() {

        // potřeba seřadit podle typu, balíčku a "lokálnímu" ražení
        Sort sort = new Sort(
                new Sort.Order(Sort.Direction.ASC, "itemType"),
                new Sort.Order(Sort.Direction.ASC, "rulPackage"),
                new Sort.Order(Sort.Direction.ASC, "viewOrder")
        );
        List<RulItemSpec> itemSpecList = itemSpecRepository.findAll(sort);

        final List<RulPackage> sortedPackages = getSortedPackages();

        // item type id -> list spec
        Map<Integer, List<RulItemSpec>> itemSpecMap = itemSpecList.stream()
                .collect(Collectors.groupingBy(RulItemSpec::getItemTypeId));

        for (List<RulItemSpec> rulItemSpecs : itemSpecMap.values()) {

            // seřazení podle priority balíčků (lokální seřazení se změní na globální)
            rulItemSpecs.sort((o1, o2) -> {
                int i1 = sortedPackages.indexOf(o1.getPackage());
                int i2 = sortedPackages.indexOf(o2.getPackage());
                return Integer.compare(i1, i2);
            });

            // provede přečíslování
            for (int i = 0; i < rulItemSpecs.size(); i++) {
                RulItemSpec rulItemSpec = rulItemSpecs.get(i);
                rulItemSpec.setViewOrder(i + 1);
            }
        }

        itemSpecRepository.save(itemSpecList);
    }

    /**
     * Vrací všechny balíčky serazené podle topologického řazení - podle závislostí mezi sebou.
     *
     * @return seznam balíčků
     */
    private List<RulPackage> getSortedPackages() {
        List<RulPackage> packages = packageRepository.findAll();
        PackageUtils.Graph<RulPackage> g = new PackageUtils.Graph<>(packages.size());
        List<RulPackageDependency> dependencies = packageDependencyRepository.findAll();
        dependencies.forEach(d -> g.addEdge(d.getRulPackage(), d.getDependsOnPackage()));
        return g.topologicalSort();
    }

    /**
	 * Update items types
	 * @param itemTypes
     * @param puc
     * @return Return new list of active item types
	 */
    private List<RulItemType> updateItemTypes(List<ItemType> itemTypes,
                                              PackageContext puc) {
        List<RulItemType> rulItemTypesUpdated = new ArrayList<>();
		for (ItemType itemType : itemTypes) {
            RulItemType dbItemType = updateItemType(itemType, puc);
            rulItemTypesUpdated.add(dbItemType);
        }
        return rulItemTypesUpdated;
    }

    /**
     * Update single item type
     * 
     * @param itemType
     * @param puc
     * @return
     */
    private RulItemType updateItemType(ItemType itemType, PackageContext puc) {
        // type already exists
        DataType newDataType = DataType.fromCode(itemType.getDataType());
        if (newDataType == null) {
            throw new SystemException("Incorrect data type: " + itemType.getDataType(), BaseCode.ID_NOT_EXIST)
                    .set("dataType", itemType.getDataType())
                    .set("code", itemType.getCode());
        }

        RulItemType dbItemType = getItemTypeByCode(rulItemTypesOrig, itemType.getCode());
        if (dbItemType != null) {
            updateDBItemType(dbItemType, itemType, newDataType);
        } else {
            dbItemType = prepareNewItemType(itemType, newDataType);
        }

        // copy values from VO
        converItemType(itemType, dbItemType, puc);

        // update view order
        dbItemType.setViewOrder(lastUsedViewOrder);

        return dbItemType;
    }


    private RulItemType prepareNewItemType(ItemType itemType, DataType newDataType) {
        RulItemType dbItemType = new RulItemType();
        dbItemType.setDataType(newDataType.getEntity());

        lastUsedViewOrder = getNextViewOrderPos();

        return dbItemType;
    }

    /**
     * Update existing item type with new values
     * 
     * @param dbItemType
     * @param itemType
     * @param newDataType
     */
    private void updateDBItemType(RulItemType dbItemType, ItemType itemType, DataType newDataType) {
        DataType currDataType = DataType.fromId(dbItemType.getDataTypeId());
        if (!currDataType.equals(newDataType)) {
            // check if such item exists
            long countDescItems = countUsage(dbItemType);
            if (countDescItems > 0) {
                switch (newDataType) {
                case DATE:
                    changeDataType2Date(currDataType, dbItemType);
                    break;
                default:
                    throw new SystemException("Unsupported data type conversion", BaseCode.DB_INTEGRITY_PROBLEM)
                            .set("currDataType", currDataType)
                            .set("newDataType", newDataType)
                            .set("itemTypeId", dbItemType.getItemTypeId())
                            .set("itemTypeCode", dbItemType.getCode());
                }
            }
            // type was updated
            dbItemType.setDataType(newDataType.getEntity());
        }
        
        // provedla se změna pro použití specifikace?
        if (!dbItemType.getUseSpecification().equals(itemType.getUseSpecification())) {

            // je nutné zkontrolovat, jestli neexistuje nějaký záznam                
            long countDescItems = countUsage(dbItemType);
            if (countDescItems > 0) {
                throw new SystemException("Nelze změnit použití specifikace u typu " + dbItemType.getCode()
                        + ", protože existují záznamy, které typ využívají");
            }
        }

        Object viewDefinition = dbItemType.getViewDefinition();
        if (viewDefinition != null) {
            switch (currDataType) {
            case JSON_TABLE: {
                if (!equalsColumns((List<ElzaColumn>) viewDefinition, itemType.getColumnsDefinition())) {
                    long countDescItems = countUsage(dbItemType);
                    if (countDescItems > 0) {
                        throw new SystemException("Nelze změnit definici sloupců (datový typ a kód) u typu "
                                + dbItemType.getCode() + ", protože existují záznamy, které typ využívají");
                    }
                }
                break;
            }
            }
        }

        // update view order
        int i = dbItemType.getViewOrder();
        if (i <= lastUsedViewOrder) {
            lastUsedViewOrder = getNextViewOrderPos();
        } else {
            lastUsedViewOrder = i;
        }
    }

    private void changeDataType2Date(DataType currDataType, RulItemType dbItemType) {
        if (currDataType.equals(DataType.STRING)) {
            // Do the conversion
            changeString2Date(dbItemType);
            return;
        }

        throw new SystemException("Unsupported conversion from type to DATE", BaseCode.DB_INTEGRITY_PROBLEM)
                    .set("currDataType", currDataType)
                    .set("itemTypeId", dbItemType.getItemTypeId())
                .set("itemTypeCode", dbItemType.getCode());
    }

    private void changeString2Date(RulItemType dbItemType) {
        // prepare date parser
        MultiFormatParser mfp = new MultiFormatParser();
        mfp.appendFormat(DateTimeFormatter.BASIC_ISO_DATE)
                .appendFormat(DateTimeFormatter.ISO_DATE);
        // Format for 13.10.2015
        DateTimeFormatter locFormatter1 = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(elzaLocale
                .getLocale());
        mfp.appendFormat(locFormatter1);
        DateTimeFormatter locFormatter2 = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(elzaLocale
                .getLocale());
        mfp.appendFormat(locFormatter2);
        DateTimeFormatter locFormatter3 = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(elzaLocale
                .getLocale());
        mfp.appendFormat(locFormatter3);
        DateTimeFormatter locFormatter4 = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(elzaLocale
                .getLocale());
        mfp.appendFormat(locFormatter4);

        // Invalidate node cache by item type
        numDroppedCachedNode += cachedNodeRepository.deleteByItemType(dbItemType);

        // Iterate ArrData from arr_item
        List<Integer> dataIds = dataRepository.findIdsByItemTypeFromArrItem(dbItemType);
        ObjectListIterator<Integer> oli = new ObjectListIterator<>(dataIds);
        while (oli.hasNext()) {
            List<Integer> ids = oli.next();
            changeString2DatePart(ids, mfp);
        }

        // Iterate ArrData from ap_item
        dataIds = dataRepository.findIdsByItemTypeFromApItem(dbItemType);
        oli = new ObjectListIterator<>(dataIds);
        while (oli.hasNext()) {
            List<Integer> ids = oli.next();
            changeString2DatePart(ids, mfp);
        }
    }

    /**
     * Process data partition
     * 
     * @param ids
     * @param mfp
     */
    private void changeString2DatePart(List<Integer> ids, MultiFormatParser mfp) {
        // request all current arr_data_string
        Collection<DataStringRepository.OnlyValues> srcValues = dataStringRepository.findValuesByDataIdIn(ids);
        // drop all old strings
        dataStringRepository.deleteMasterOnly(ids);
        // update data type
        dataRepository.updateDataType(ids, DataType.DATE.getId());
        // insert new arr_data_date
        for (OnlyValues srcValue : srcValues) {
            // parse current value
            LocalDate locDate = mfp.parseDate(srcValue.getValue(), LocalDate.now());

            dataDateRepository.insertMasterOnly(srcValue.getDataId(), locDate);
        }

    }

    /**
     * Return next view_order position
     * 
     * @return next value
     */
    private int getNextViewOrderPos() {
    	return ++this.maxViewOrderPos;
	}


	/**
     * Get item type by code
     * @param rulItemTypes list of item types
     * @param code Item type code
     * @return Return first item type with given code or null if does not exists
     */
	private RulItemType getItemTypeByCode(List<RulItemType> rulItemTypes, String code) {
		for(RulItemType itemType: rulItemTypes) {
			if(code.equals(itemType.getCode())) {
				return itemType;
			}
		}
		return null;
	}

    /**
     * Count how many times is type used
     * 
     * @param dbItemType
     * @return
     */
    long countUsage(RulItemType dbItemType) {
        // check items
        long result = itemRepository.getCountByType(dbItemType);
        result += apItemRepository.getCountByType(dbItemType);
        return result;
    }


	/**
     * Převod VO na DAO typu atributu.
     * @param itemType    VO typu
     * @param dbItemType DAO typy
     * @param puc      balíček
     */
    private void converItemType(final ItemType itemType,
                                        final RulItemType dbItemType,
                                        PackageContext puc) {

        Validate.notNull(dbItemType.getDataTypeId() != null);

        dbItemType.setCode(itemType.getCode());
        dbItemType.setName(itemType.getName());

        RulStructuredType rulStructureType = null;
        if (DataType.STRUCTURED == DataType.fromCode(itemType.getDataType())) {
            List<RulStructuredType> findStructureTypes = puc.getStructuredTypes().stream()
                    .filter((r) -> r.getCode().equals(itemType.getStructureType()))
                    .collect(Collectors.toList());
            if (findStructureTypes.size() > 0) {
                rulStructureType = findStructureTypes.get(0);
            } else {
                throw new SystemException("Kód " + itemType.getStructureType() + " neexistuje v RulStructureType", BaseCode.ID_NOT_EXIST);
            }
        }

        RulStructuredType apFragmentType = null;
        if (DataType.APFRAG_REF == DataType.fromCode(itemType.getDataType())) {
            List<RulStructuredType> findStructureTypes = puc.getStructuredTypes().stream()
                    .filter((r) -> r.getCode().equals(itemType.getFragmentType()))
                    .collect(Collectors.toList());
            if (findStructureTypes.size() > 0) {
                apFragmentType = findStructureTypes.get(0);
            } else {
                throw new SystemException("Kód " + itemType.getFragmentType() + " neexistuje v RulStructureType", BaseCode.ID_NOT_EXIST);
            }
        }

        dbItemType.setShortcut(itemType.getShortcut());
        dbItemType.setDescription(itemType.getDescription());
        dbItemType.setIsValueUnique(itemType.getIsValueUnique());
        dbItemType.setCanBeOrdered(itemType.getCanBeOrdered());
        dbItemType.setUseSpecification(itemType.getUseSpecification());
        dbItemType.setStructuredType(rulStructureType);
        dbItemType.setFragmentType(apFragmentType);

        if (itemType.getColumnsDefinition() != null) {
            List<ElzaColumn> elzaColumns = new ArrayList<>(itemType.getColumnsDefinition().size());
            for (Column column : itemType.getColumnsDefinition()) {
                ElzaColumn elzaColumn = new ElzaColumn();
                elzaColumn.setCode(column.getCode());
                elzaColumn.setName(column.getName());
                elzaColumn.setDataType(ElzaColumn.DataType.valueOf(column.getDataType()));
                elzaColumn.setWidth(column.getWidth());
                elzaColumns.add(elzaColumn);
            }

            dbItemType.setViewDefinition(elzaColumns);
        }

        DisplayType displayType = itemType.getDisplayType();
        if (displayType != null) {
            dbItemType.setViewDefinition(cz.tacr.elza.domain.integer.DisplayType.valueOf(displayType.name()));
        }

        dbItemType.setRulPackage(puc.getPackage());
    }

    /**
     * Převod VO na DAO specifikace atributu.
     *
     * @param rulPackage       balíček
     * @param itemSpec     VO specifikace
     * @param rulDescItemSpec  DAO specifikace
     * @param rulDescItemTypes seznam typů atributů
     */
    private void convertRulDescItemSpec(final RulPackage rulPackage,
                                        final ItemSpec itemSpec,
                                        final RulItemSpec rulDescItemSpec,
                                        final List<RulItemType> rulDescItemTypes) {
        rulDescItemSpec.setName(itemSpec.getName());
        rulDescItemSpec.setCode(itemSpec.getCode());
        rulDescItemSpec.setDescription(itemSpec.getDescription());
        rulDescItemSpec.setShortcut(itemSpec.getShortcut());
        rulDescItemSpec.setPackage(rulPackage);

        List<RulItemType> findItems = rulDescItemTypes.stream()
                .filter((r) -> r.getCode().equals(itemSpec.getItemType()))
                .collect(Collectors.toList());

        RulItemType item;

        if (findItems.size() > 0) {
            item = findItems.get(0);
        } else {
            throw new BusinessException("Typ s kódem " + itemSpec.getItemType() + " nenalezen",
                    PackageCode.CODE_NOT_FOUND)
                            .set("code", itemSpec.getItemType()).set("file", ITEM_TYPE_XML);
        }

        if (CollectionUtils.isNotEmpty(itemSpec.getCategories())) {
            List<String> categories = itemSpec.getCategories().stream().map(Category::getValue).collect(Collectors.toList());
            rulDescItemSpec.setCategory(StringUtils.join(categories, CATEGORY_SEPARATOR));
        } else {
            rulDescItemSpec.setCategory(null);
        }

        rulDescItemSpec.setItemType(item);
    }


    /**
     * Zpracování napojení specifikací na ap.
     *
     * @param itemSpecs
     *            seznam importovaných specifikací
     * @param rulDescItemSpecs
     *            seznam specifikací atributů (nový v DB)
     */
    private void processDescItemSpecsRegister(@Nonnull final List<ItemSpec> itemSpecs,
                                              @Nonnull final List<RulItemSpec> rulDescItemSpecs) {

        List<ApType> apTypes = apTypeRepository.findAll();

        List<RulItemSpecRegister> speAPTypeNew = new ArrayList<>();

        List<RulItemSpecRegister> rulItemSpecRegisters = new ArrayList<>();
        
        Map<String, ItemSpec> itemSpecLookup = itemSpecs.stream().collect(Collectors.toMap(ItemSpec::getCode, Function.identity()));
        Validate.isTrue(itemSpecLookup.size()==itemSpecs.size(), "List of specification contains duplicated code");

        for (RulItemSpec rulDescItemSpec : rulDescItemSpecs) {
            // Find input item spec from source
            /*List<ItemSpec> findItemsSpec = itemSpecs.stream().filter(
                    (r) -> r.getCode().equals(rulDescItemSpec.getCode())).collect(Collectors.toList());

            Validate.isTrue(findItemsSpec.size() == 1, "Cannot find code in itemSpecs, code: {}",
                            rulDescItemSpec.getCode());
            ItemSpec item = findItemsSpec.get(0);*/
            ItemSpec item = itemSpecLookup.get(rulDescItemSpec.getCode());
            Validate.notNull(item, "Cannot find code in itemSpecs, code: {}",
                    rulDescItemSpec.getCode());

            List<RulItemSpecRegister> dbSpecs = itemSpecRegisterRepository
                    .findByDescItemSpecId(rulDescItemSpec);
            rulItemSpecRegisters.addAll(dbSpecs);

            if (!CollectionUtils.isEmpty(item.getItemSpecRegisters())) {
                for (ItemSpecRegister itemSpecRegister : item.getItemSpecRegisters()) {
                    List<RulItemSpecRegister> findItems = dbSpecs.stream()
                            .filter((r) -> r.getApType().getCode().equals(
                                    itemSpecRegister.getRegisterType())).collect(Collectors.toList());
                    RulItemSpecRegister itemRegister;
                    if (findItems.size() > 0) {
                        itemRegister = findItems.get(0);
                    } else {
                        itemRegister = new RulItemSpecRegister();
                    }

                    convertRulDescItemSpecsRegister(rulDescItemSpec, itemRegister, apTypes,
                            itemSpecRegister);

                    speAPTypeNew.add(itemRegister);
                }
            }
        }

        speAPTypeNew = itemSpecRegisterRepository.save(speAPTypeNew);

        // Collection of APTypes for spec to remove
        List<RulItemSpecRegister> specAPTypeDel = new ArrayList<>(rulItemSpecRegisters);
        specAPTypeDel.removeAll(speAPTypeNew);
        if(specAPTypeDel.size()>0) {
            for (RulItemSpecRegister specAPType : specAPTypeDel) {
                logger.info("Dropping specification to accesss point, spec: {}, apType: {}", specAPType.getApType()
                        .getCode(), specAPType.getApType().getCode());
            }
            itemSpecRegisterRepository.delete(specAPTypeDel);
        }

    }


    /**
     * Převod VO na DAO napojení specifikací na ap.
     *
     * @param rulDescItemSpec         seznam specifikací
     * @param rulItemSpecRegister seznam DAO napojení
     * @param apTypes        seznam typů ap.
     * @param itemSpecRegister    seznam VO napojení
     */
    private void convertRulDescItemSpecsRegister(final RulItemSpec rulDescItemSpec,
                                                 final RulItemSpecRegister rulItemSpecRegister,
                                                 final List<ApType> apTypes,
                                                 final ItemSpecRegister itemSpecRegister) {

        rulItemSpecRegister.setItemSpec(rulDescItemSpec);

        List<ApType> findItems = apTypes.stream()
                .filter((r) -> r.getCode().equals(itemSpecRegister.getRegisterType()))
                .collect(Collectors.toList());

        ApType item;

        if (findItems.size() > 0) {
            item = findItems.get(0);
        } else {
            throw new SystemException(
                    "Kód " + itemSpecRegister.getRegisterType() + " neexistuje v ApType", BaseCode.ID_NOT_EXIST);
        }

        rulItemSpecRegister.setApType(item);

    }

    /**
     * Prepare item types to be updated
     * 
     * Read items from DB
     * 
     * @param rulPackage
     */
    private void prepareForUpdate(final PackageContext rulPackage) {

        // read current items types from DB
        rulItemTypesOrig = itemTypeRepository.findByRulPackage(rulPackage.getPackage());

        // read first free view-order id
        RulItemType itemTypeHighest = itemTypeRepository.findFirstByOrderByViewOrderDesc();
        if (itemTypeHighest != null) {
            Integer maxValue = itemTypeHighest.getViewOrder();
            maxViewOrderPos = maxValue != null ? maxValue.intValue() : 0;
        }
    }

    /**
     * Return number of dropped cache nodes
     * 
     * @return
     */
    public int getNumDroppedCachedNode() {
        return this.numDroppedCachedNode;
    }

}
