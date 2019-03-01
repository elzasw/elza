package cz.tacr.elza.packageimport;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
import cz.tacr.elza.domain.RulItemAptype;
import cz.tacr.elza.domain.RulItemSpec;
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
import cz.tacr.elza.packageimport.xml.ItemAptype;
import cz.tacr.elza.packageimport.xml.ItemSpec;
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
import cz.tacr.elza.repository.ItemAptypeRepository;
import cz.tacr.elza.repository.ItemRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.PackageDependencyRepository;
import cz.tacr.elza.repository.PackageRepository;

import static cz.tacr.elza.packageimport.PackageService.ITEM_TYPE_XML;
import static java.util.stream.Collectors.toMap;

/**
 * Class to update item types in DB
 *
 * Class will use types from XML and try to synchronize them in DB
 */
@Component
@Scope("prototype")
public class ItemTypeUpdater {

    private static Logger logger = LoggerFactory.getLogger(ItemTypeUpdater.class);

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
    private ItemAptypeRepository itemAptypeRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    DataDateRepository dataDateRepository;

    @Autowired
    private DataStringRepository dataStringRepository;

    @Autowired
    private CachedNodeRepository cachedNodeRepository;

    @Autowired
    private ApTypeRepository apTypeRepository;

    @Autowired
    private PackageRepository packageRepository;

    @Autowired
    private PackageDependencyRepository packageDependencyRepository;

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
     *
     * @param itemSpecs seznam importovaných specifikací
     */
    private void processItemSpecs(ItemSpecs itemSpecs,
                                  @Nonnull Map<String, RulItemType> rulItemTypesCache,
                                  @Nonnull Map<String, ApType> apTypeCache,
                                  @Nonnull RulPackage rulPackage) {

        Map<String, RulItemSpec> rulItemSpecOrig = itemSpecRepository.findByRulPackage(rulPackage).stream()
                .collect(toMap(rulItemSpec -> rulItemSpec.getCode(), rulItemSpec -> rulItemSpec));

        Map<String, RulItemSpec> rulItemSpecNew = new LinkedHashMap<>();

        // item type code -> local view order
        Map<String, Integer> viewOrderMap = new HashMap<>();

        if (itemSpecs != null && CollectionUtils.isNotEmpty(itemSpecs.getItemSpecs())) {

            Set<String> rulItemSpecCodes = new HashSet<>();
            List<RulItemSpec> rulItemSpecsSave = new ArrayList<>();

            for (ItemSpec itemSpec : itemSpecs.getItemSpecs()) {

                String itemSpecCode = itemSpec.getCode();
                if (rulItemSpecCodes.contains(itemSpecCode)) {
                    throw new SystemException("Duplicitní kód specifikace: " + itemSpecCode, BaseCode.DB_INTEGRITY_PROBLEM)
                            .set("code", itemSpecCode);
                }

                // pouzijeme remove() - co zbyde nechame nakonec smazat z DB !!!
                RulItemSpec rulItemSpec = rulItemSpecOrig.remove(itemSpecCode);

                if (rulItemSpec == null) {
                    rulItemSpec = new RulItemSpec();
                }

                convertRulItemSpec(rulPackage, itemSpec, rulItemSpec, rulItemTypesCache);

                Integer nextViewOrder = viewOrderMap.computeIfAbsent(rulItemSpec.getItemType().getCode(), next -> 1);
                rulItemSpec.setViewOrder(nextViewOrder);
                viewOrderMap.put(rulItemSpec.getItemType().getCode(), ++nextViewOrder);

                rulItemSpecCodes.add(itemSpecCode);
                rulItemSpecsSave.add(rulItemSpec);
            }

            // try to save updated items
            for (RulItemSpec rulItemSpec : itemSpecRepository.save(rulItemSpecsSave)) {
                rulItemSpecNew.put(rulItemSpec.getCode(), rulItemSpec);
            }

            processItemAptypesByItemSpecs(itemSpecs.getItemSpecs(), rulItemSpecNew, apTypeCache);
        }

        // delete unused item specs
        if (!rulItemSpecOrig.isEmpty()) {
            for (RulItemSpec rulItemSpec : rulItemSpecOrig.values()) {
                itemAptypeRepository.deleteByItemSpec(rulItemSpec);
            }
            itemSpecRepository.delete(rulItemSpecOrig.values());
        }
    }

    /**
     * Do the update
     *
     * @return return list of updated types
     */
    public void update(ItemTypes itemTypes,
                       ItemSpecs itemSpecs,
                       @Nonnull final PackageContext puc) {

        prepareForUpdate();

        Map<String, ApType> apTypeCache = apTypeRepository.findAll().stream()
                .collect(toMap(apType -> apType.getCode(), apType -> apType));

        processItemTypes(itemTypes, itemSpecs, puc, apTypeCache);
    }

    private void processItemTypes(ItemTypes itemTypes, ItemSpecs itemSpecs, @Nonnull PackageContext puc, @Nonnull Map<String, ApType> apTypeCache) {

        Map<String, RulItemType> rulItemTypesOrig = itemTypeRepository.findByRulPackage(puc.getPackage()).stream()
                .collect(toMap(rulItemType -> rulItemType.getCode(), rulItemType -> rulItemType));

        Map<String, RulItemType> rulItemTypeNew = new LinkedHashMap<>();

        // prepare list of updated/new items
        if (itemTypes != null && CollectionUtils.isNotEmpty(itemTypes.getItemTypes())) {

            Set<String> rulItemTypeCodes = new HashSet<>();
            List<RulItemType> rulItemTypesSave = new ArrayList<>();

            for (ItemType itemType : itemTypes.getItemTypes()) {

                String itemTypeCode = itemType.getCode();
                if (rulItemTypeCodes.contains(itemTypeCode)) {
                    throw new SystemException("Duplicitní kód typu: " + itemTypeCode, BaseCode.ID_EXIST)
                            .set("code", itemTypeCode);
                }

                // type already exists
                DataType newDataType = DataType.fromCode(itemType.getDataType());
                if (newDataType == null) {
                    throw new SystemException("Incorrect data type: " + itemType.getDataType(), BaseCode.ID_NOT_EXIST)
                            .set("dataType", itemType.getDataType())
                            .set("code", itemTypeCode);
                }

                // pouzijeme remove() - co zbyde nechame nakonec smazat z DB !!!
                RulItemType rulItemType = rulItemTypesOrig.remove(itemTypeCode);
                if (rulItemType != null) {
                    updateDBItemType(rulItemType, itemType, newDataType);
                } else {
                    rulItemType = prepareNewItemType(itemType, newDataType);
                }

                // copy values from VO
                convertRulItemType(itemType, rulItemType, puc);

                // update view order
                rulItemType.setViewOrder(lastUsedViewOrder);

                rulItemTypeCodes.add(itemTypeCode);
                rulItemTypesSave.add(rulItemType);
            }

            // try to save updated items
            for (RulItemType rulItemType : itemTypeRepository.save(rulItemTypesSave)) {
                rulItemTypeNew.put(rulItemType.getCode(), rulItemType);
            }

            processItemAptypesByItemTypes(itemTypes.getItemTypes(), rulItemTypeNew, apTypeCache);
        }

        // update specifications
        processItemSpecs(itemSpecs, rulItemTypeNew, apTypeCache, puc.getPackage());
        postSpecsOrder();

        // delete unused item types
        if (!rulItemTypesOrig.isEmpty()) {
            for (RulItemType rulItemType : rulItemTypesOrig.values()) {
                itemAptypeRepository.deleteByItemType(rulItemType);
            }
            itemTypeRepository.delete(rulItemTypesOrig.values());
        }
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

    private RulItemType prepareNewItemType(ItemType itemType, DataType newDataType) {
        RulItemType dbItemType = new RulItemType();
        dbItemType.setDataType(newDataType.getEntity());

        lastUsedViewOrder = getNextViewOrderPos();

        return dbItemType;
    }

    /**
     * Update existing item type with new values
     */
    private void updateDBItemType(RulItemType dbItemType, ItemType itemType, DataType newDataType) {
        DataType currDataType = DataType.fromId(dbItemType.getDataTypeId());
        if (!currDataType.equals(newDataType)) {
            // check if such item exists
            long countDescItems = countUsage(dbItemType);
            if (countDescItems > 0L) {
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
            if (countDescItems > 0L) {
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
                    if (countDescItems > 0L) {
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
        Collection<DataStringRepository.OnlyValues> srcValues = dataStringRepository.findValuesByDataId(ids);
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
     *
     * @param itemType VO typu
     * @param dbItemType DAO typy
     * @param puc balíček
     */
    private void convertRulItemType(final ItemType itemType,
                                    final RulItemType dbItemType,
                                    final PackageContext puc) {

        Validate.notNull(dbItemType.getDataTypeId());

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
     * @param rulPackage balíček
     * @param itemSpec VO specifikace
     * @param rulItemSpec DAO specifikace
     * @param rulItemTypes seznam typů atributů
     */
    private void convertRulItemSpec(final RulPackage rulPackage,
                                    final ItemSpec itemSpec,
                                    final RulItemSpec rulItemSpec,
                                    final Map<String, RulItemType> rulItemTypesMap) {

        rulItemSpec.setName(itemSpec.getName());
        rulItemSpec.setCode(itemSpec.getCode());
        rulItemSpec.setDescription(itemSpec.getDescription());
        rulItemSpec.setShortcut(itemSpec.getShortcut());
        rulItemSpec.setPackage(rulPackage);

        RulItemType rulItemType = rulItemTypesMap.get(itemSpec.getItemType());

        if (rulItemType == null) {
            throw new BusinessException("Typ s kódem " + itemSpec.getItemType() + " nenalezen",
                    PackageCode.CODE_NOT_FOUND)
                    .set("code", itemSpec.getItemType()).set("file", ITEM_TYPE_XML);
        }

        rulItemSpec.setItemType(rulItemType);

        if (CollectionUtils.isNotEmpty(itemSpec.getCategories())) {
            List<String> categories = itemSpec.getCategories().stream().map(Category::getValue).collect(Collectors.toList());
            rulItemSpec.setCategory(StringUtils.join(categories, CATEGORY_SEPARATOR));
        } else {
            rulItemSpec.setCategory(null);
        }
    }

    /**
     * Zpracování napojení specifikací na ap.
     *
     * @param itemSpecs seznam importovaných specifikací
     * @param rulItemSpecs seznam specifikací atributů (nový v DB)
     */
    private void processItemAptypesByItemSpecs(List<ItemSpec> itemSpecs,
                                               @Nonnull Map<String, RulItemSpec> rulItemSpecsCache,
                                               @Nonnull Map<String, ApType> apTypeCache) {

        if (CollectionUtils.isEmpty(itemSpecs)) {
            return;
        }

        Map<Integer, RulItemAptype> rulItemAptypesOrig = new HashMap<>();

        for (ItemSpec itemSpec : itemSpecs) {

            RulItemSpec rulItemSpec = rulItemSpecsCache.get(itemSpec.getCode());
            Validate.notNull(rulItemSpec, "Cannot find code in itemSpecs, code: {}", itemSpec.getCode());

            List<RulItemAptype> rulItemAptypeDb = itemAptypeRepository.findByItemSpec(rulItemSpec);

            for (RulItemAptype rulItemAptype : rulItemAptypeDb) {
                rulItemAptypesOrig.put(rulItemAptype.getItemAptypeId(), rulItemAptype);
            }

            List<RulItemAptype> rulItemAptypesNew = updateItemAptypes(itemSpec.getItemAptypes(), rulItemAptypeDb, rulItemSpec, null, apTypeCache);

            // Collection of APTypes to delete
            for (RulItemAptype rulItemAptype : itemAptypeRepository.save(rulItemAptypesNew)) {
                rulItemAptypesOrig.remove(rulItemAptype.getItemAptypeId());
            }
        }

        if (!rulItemAptypesOrig.isEmpty()) {
            if (logger.isInfoEnabled()) {
                for (RulItemAptype rulItemAptype : rulItemAptypesOrig.values()) {
                    logger.info("Dropping specification to accesss point, spec: {}, apType: {}",
                            rulItemAptype.getItemSpec().getCode(), rulItemAptype.getApType().getCode());
                }
            }
            itemAptypeRepository.delete(rulItemAptypesOrig.values());
        }
    }

    /**
     * Zpracování napojení typů na ap.
     *
     * @param itemTypes seznam importovaných typů
     * @param rulItemTypes seznam typů atributů (nový v DB)
     */
    private void processItemAptypesByItemTypes(List<ItemType> itemTypes,
                                               @Nonnull Map<String, RulItemType> rulItemTypesCache,
                                               @Nonnull Map<String, ApType> apTypeCache) {

        if (CollectionUtils.isEmpty(itemTypes)) {
            return;
        }

        Map<Integer, RulItemAptype> rulItemAptypesOrig = new HashMap<>();

        for (ItemType itemType : itemTypes) {

            RulItemType rulItemType = rulItemTypesCache.get(itemType.getCode());
            Validate.notNull(rulItemType, "Cannot find code in itemTypes, code: {}", itemType.getCode());

            List<RulItemAptype> rulItemAptypeDb = itemAptypeRepository.findByItemType(rulItemType);

            for (RulItemAptype rulItemAptype : rulItemAptypeDb) {
                rulItemAptypesOrig.put(rulItemAptype.getItemAptypeId(), rulItemAptype);
            }

            List<RulItemAptype> rulItemAptypesNew = updateItemAptypes(itemType.getItemAptypes(), rulItemAptypeDb, null, rulItemType, apTypeCache);

            // Collection of APTypes to delete
            for (RulItemAptype rulItemAptype : itemAptypeRepository.save(rulItemAptypesNew)) {
                rulItemAptypesOrig.remove(rulItemAptype.getItemAptypeId());
            }
        }

        if (!rulItemAptypesOrig.isEmpty()) {
            if (logger.isInfoEnabled()) {
                for (RulItemAptype rulItemAptype : rulItemAptypesOrig.values()) {
                    logger.info("Dropping type to accesss point, type: {}, apType: {}",
                            rulItemAptype.getItemType().getCode(), rulItemAptype.getApType().getCode());
                }
            }
            itemAptypeRepository.delete(rulItemAptypesOrig.values());
        }
    }

    private List<RulItemAptype> updateItemAptypes(List<ItemAptype> itemAptypes, List<RulItemAptype> rulItemAptypeDb, RulItemSpec rulItemSpec, RulItemType rulItemType, @Nonnull Map<String, ApType> apTypeCache) {

        List<RulItemAptype> rulItemAptypes = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(itemAptypes)) {

            for (ItemAptype itemAptype : itemAptypes) {

                RulItemAptype rulItemAptype = getRulItemAptype(rulItemAptypeDb, itemAptype.getRegisterType());

                if (rulItemAptype == null) {

                    ApType apType = getApType(apTypeCache, itemAptype.getRegisterType());

                    rulItemAptype = prepareNewRulItemAptype(apType, rulItemSpec, rulItemType);
                }

                rulItemAptypes.add(rulItemAptype);
            }
        }

        return rulItemAptypes;
    }

    @Nullable
    private RulItemAptype getRulItemAptype(@Nonnull List<RulItemAptype> rulItemAptypesDb, @Nonnull String apTypeCode) {
        List<RulItemAptype> findItems = rulItemAptypesDb.stream()
                .filter(rulItemAptype -> rulItemAptype.getApType().getCode().equals(apTypeCode))
                .collect(Collectors.toList());
        return findItems.size() > 0 ? findItems.get(0) : null;
    }

    private ApType getApType(@Nonnull Map<String, ApType> apTypeCache, String apTypeCode) {
        ApType apType = apTypeCache.get(apTypeCode);
        if (apType == null) {
            throw new SystemException("Kód " + apTypeCode + " neexistuje v ApType", BaseCode.ID_NOT_EXIST).set("apTypeCode", apTypeCode);
        }
        return apType;
    }

    private RulItemAptype prepareNewRulItemAptype(@Nonnull ApType apType, RulItemSpec rulItemSpec, RulItemType rulItemType) {
        Validate.notNull(apType, "ApType is null");
        Validate.isTrue((rulItemSpec != null ? 1 : 0) + (rulItemType != null ? 1 : 0) == 1, "Exactly one of RulItemSpec and RulItemType must be set");
        // Převod VO na DAO napojení specifikací a typů na ap.
        RulItemAptype rulItemAptype = new RulItemAptype();
        rulItemAptype.setApType(apType);
        rulItemAptype.setItemSpec(rulItemSpec);
        rulItemAptype.setItemType(rulItemType);
        return rulItemAptype;
    }

    /**
     * Prepare item types to be updated
     *
     * Read items from DB
     */
    private void prepareForUpdate() {

        // read first free view-order id
        RulItemType itemTypeHighest = itemTypeRepository.findFirstByOrderByViewOrderDesc();
        if (itemTypeHighest != null) {
            Integer maxValue = itemTypeHighest.getViewOrder();
            maxViewOrderPos = maxValue != null ? maxValue.intValue() : 0;
        }
    }

    /**
     * Return number of dropped cache nodes
     */
    public int getNumDroppedCachedNode() {
        return this.numDroppedCachedNode;
    }

}
