package cz.tacr.elza.packageimport;

import static java.util.stream.Collectors.toMap;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.common.datetime.MultiFormatParser;
import cz.tacr.elza.core.ElzaLocale;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.RulItemAptype;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeSpecAssign;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPackageDependency;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.domain.table.ElzaColumn;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.packageimport.xml.Category;
import cz.tacr.elza.packageimport.xml.Column;
import cz.tacr.elza.packageimport.xml.DisplayType;
import cz.tacr.elza.packageimport.xml.ItemAptype;
import cz.tacr.elza.packageimport.xml.ItemSpec;
import cz.tacr.elza.packageimport.xml.ItemSpecs;
import cz.tacr.elza.packageimport.xml.ItemType;
import cz.tacr.elza.packageimport.xml.ItemTypeAssign;
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
import cz.tacr.elza.repository.ItemTypeSpecAssignRepository;
import cz.tacr.elza.repository.PackageDependencyRepository;
import cz.tacr.elza.repository.PackageRepository;

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
    private ItemTypeSpecAssignRepository itemTypeSpecAssignRepository;

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
     * ApType by CODE
     */
    private Map<String, ApType> apTypeByCode;

    /**
     * List of item types by package
     */
    private LinkedHashMap<Integer, List<RulItemType>> typesByPackageId = new LinkedHashMap<>();

    /**
     * Next view order position for imported items
     */
    int nextViewOrderPos = 1;

    /**
     * Last used position from all items
     * 
     * It can be used for temporary shifts
     */
    int lastUsedOrderPos = 0;

    /**
     * Map of all item types
     * 
     * Map includes all previous DB item types and also new item types
     */
    private Map<String, RulItemType> allItemTypesByCode = new HashMap<>();

    private List<RulItemTypeSpecAssign> deleteSpecToTypeAssignments = new ArrayList<>();

    private List<RulItemAptype> deleteItemApTypes = new ArrayList<>();

    private List<RulItemSpec> deleteItemSpecs = new ArrayList<>();

    /**
     * Item types to be deleted at the cleanup
     */
    private List<RulItemType> deleteItemTypes = new ArrayList<>();

    /**
     * Number of nodes dropped in arr_cached_node table
     *
     * If this number is greater then zero Node cache has to be
     * reconstructed
     */
    int numDroppedCachedNode = 0;


	public ItemTypeUpdater() {
	}


    @PostConstruct
    public void postConstruct() {
        List<ApType> typeList = apTypeRepository.findAll();
        apTypeByCode = typeList.stream()
                .collect(toMap(apType -> apType.getCode(), Function.identity()));

        // split item types by packages
        List<RulItemType> itemTypes = this.itemTypeRepository.findAllOrderByViewOrderAsc();
        itemTypes.forEach(t -> {
            allItemTypesByCode.put(t.getCode(), t);

            List<RulItemType> list = this.typesByPackageId.computeIfAbsent(t.getRulPackage().getPackageId(),
                                                                           s -> new ArrayList<>());
            list.add(t);
        });
    }


    /**
     * Prepare item types to be updated
     * <p>
     * Shift items if needed and count start pos
     * 
     * @param xmlItemTypes
     * @return List of current itemTypes
     */
    private List<RulItemType> prepareForUpdate(ItemTypes xmlItemTypes, final RulPackage rulPackage) {
                
        List<RulItemType> shiftItemTypes = new ArrayList<>();

        List<RulItemType> result = Collections.emptyList();

        int shiftBy = 0;
        boolean packageFound = false;

        // maximum position for shifting current types
        for(Entry<Integer, List<RulItemType>> es: typesByPackageId.entrySet()) {
            List<RulItemType> currentPackageTypes = es.getValue();
            if (currentPackageTypes.size() > 0) {
                Integer lastPackageViewOrder = currentPackageTypes.get(currentPackageTypes.size() - 1).getViewOrder();
                Validate.notNull(lastPackageViewOrder);
                if (lastPackageViewOrder > lastUsedOrderPos) {
                    lastUsedOrderPos = lastPackageViewOrder;
                }
            }
            
            if(es.getKey().equals(rulPackage.getPackageId())) {
                packageFound = true;
                // prepare number of received types
                int numReceivedTypes;
                if(xmlItemTypes!=null&&xmlItemTypes.getItemTypes()!=null) {
                    numReceivedTypes = xmlItemTypes.getItemTypes().size();
                } else {
                    numReceivedTypes = 0;
                }
                
                // check if some shifts are needed
                if(numReceivedTypes>es.getValue().size()) {
                    shiftBy = es.getValue().size() - numReceivedTypes;
                }
                result = currentPackageTypes;
            } else
            if (!packageFound) {
                // items before current package can stay on place
                // just count next view pos
                nextViewOrderPos = lastUsedOrderPos + 1;
            } else {
                if (shiftBy > 0) {
                    // items has to be shifted
                    shiftItemTypes.addAll(es.getValue());
                }
            }
        }
        // shift items
        if(shiftItemTypes.size()>0) {
            for(int pos = shiftItemTypes.size()-1; pos>=0; pos--) {
                RulItemType itemType = shiftItemTypes.get(pos);
                itemType.setViewOrder(itemType.getViewOrder() + shiftBy);
                // flush each single shifted item - check DB constraint
                itemType = this.itemTypeRepository.saveAndFlush(itemType);
                this.allItemTypesByCode.put(itemType.getCode(), itemType);
            }
        }
        return result;
    }

    /**
     * Porovnávání typů sloupců.
     *
     * @param elzaColumnList
     *            porovnávaný list ElzaColumn (stávající v DB)
     * @param columnList
     *            porovnávaný list Column
     * @return jsou změněný neměnitelný položky?
     */
    private boolean canUpdateColumns(final List<ElzaColumn> elzaColumnList, final List<Column> columnList) {
        // kontrola, zda sloupce neubyly
        if (elzaColumnList.size() > columnList.size()) {
            return false;
        }

        // porovnání stávající definice
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
     * @param xmlItemSpecs seznam importovaných specifikací
     */
    private void processItemSpecs(ItemSpecs xmlItemSpecs,
                                  @Nonnull RulPackage rulPackage) {

        // read current specs from DB
        List<RulItemSpec> dbItemSpecs = itemSpecRepository.findByRulPackage(rulPackage);
        Map<String, RulItemSpec> dbOldSpecsByCode = dbItemSpecs.stream()
                .collect(toMap(RulItemSpec::getCode, Function.identity()));
        
        Map<String, List<String>> xmlSpecAssigmentsByType = new HashMap<>();

        // kolekce vsech zpracovanych kodu
        Map<String, RulItemSpec> dbUpdatedSpecsByCode = new LinkedHashMap<>();

        if (xmlItemSpecs != null && CollectionUtils.isNotEmpty(xmlItemSpecs.getItemSpecs())) {

            for (ItemSpec xmlItemSpec : xmlItemSpecs.getItemSpecs()) {

                String itemSpecCode = xmlItemSpec.getCode();
                if (dbUpdatedSpecsByCode.containsKey(itemSpecCode)) {
                    throw new SystemException("Duplicitní kód specifikace: " + itemSpecCode, BaseCode.DB_INTEGRITY_PROBLEM)
                            .set("code", itemSpecCode);
                }

                // pouzijeme remove() - co zbyde nechame nakonec smazat z DB
                RulItemSpec rulItemSpec = dbOldSpecsByCode.remove(itemSpecCode);
                if (rulItemSpec == null) {
                    rulItemSpec = new RulItemSpec();
                    logger.debug("Creating new specification: {}", itemSpecCode);
                }
                convertRulItemSpec(rulPackage, xmlItemSpec, rulItemSpec);

                RulItemSpec rulItemSpecSaved = itemSpecRepository.save(rulItemSpec);
                dbUpdatedSpecsByCode.put(rulItemSpec.getCode(), rulItemSpecSaved);

                // prepare list of assigned specs per type
                if (xmlItemSpec.getItemTypeAssigns() != null) {
                    for (ItemTypeAssign xmlSpecAssignment : xmlItemSpec.getItemTypeAssigns()) {
                        // get list of assigned spec to the type 
                        List<String> specs = xmlSpecAssigmentsByType.computeIfAbsent(xmlSpecAssignment.getCode(),
                                                                                     c -> new ArrayList<>());
                        // append new spec
                        specs.add(itemSpecCode);
                    }
                }
            }

            processItemAptypesByItemSpecs(xmlItemSpecs.getItemSpecs(), dbUpdatedSpecsByCode);
        }

        // delete unused item specs
        this.deleteItemSpecs.addAll(dbOldSpecsByCode.values());
        
        // assign specs to types - solve order issue
        assignItemTypesToSpec(xmlSpecAssigmentsByType,
                              dbItemSpecs,
                              dbUpdatedSpecsByCode);
        
    }

    /**
     * Assigne item types to specification
     * 
     * @param xmlSpecAssigmentsByType
     *            Specification assigment from XML.
     *            Map key is typeCode.
     * @param dbPrevItemSpecs
     *            Previouse specifications
     * @param dbUpdatedSpecsByCode
     *            Updated specifications by code
     */
    private void assignItemTypesToSpec(final Map<String, List<String>> xmlSpecAssigmentsByType,
                                       final List<RulItemSpec> dbPrevItemSpecs,
                                       final Map<String, RulItemSpec> dbUpdatedSpecsByCode) {

        // prepare list of types for modification
        // - all types assigned to new specs
        // - list of types for prev assignments
        
        // Update spec assignments
        Map<String, List<RulItemTypeSpecAssign>> specAssigmentsByType = new HashMap<>();
        if (CollectionUtils.isNotEmpty(dbPrevItemSpecs)) {
            // get current assignments
            List<RulItemTypeSpecAssign> dbAssignedSpecTypes = itemTypeSpecAssignRepository
                    .findByItemSpecIn(dbPrevItemSpecs);
            specAssigmentsByType = dbAssignedSpecTypes.stream().collect(Collectors.groupingBy(a -> a.getItemType().getCode()));            
        } else {
            specAssigmentsByType = Collections.emptyMap();
        }
        
        // iterate xml requiremens
        for (Entry<String, List<String>> itemTypeAssignedSpecs : xmlSpecAssigmentsByType.entrySet()) {
            String itemTypeCode = itemTypeAssignedSpecs.getKey();
            List<String> requieredSpecs = itemTypeAssignedSpecs.getValue();

            List<RulItemTypeSpecAssign> currDbAssignedSpecs = specAssigmentsByType.get(itemTypeCode);

            Map<String, RulItemTypeSpecAssign> currDbAssignmentsBySpecCode = currDbAssignedSpecs != null
                    ? currDbAssignedSpecs.stream()
                            .collect(Collectors.toMap(dba -> dba.getItemSpec().getCode(), Function.identity()))
                    : Collections.emptyMap();
            // get required specs            
            // 
            for (int pos = 0; pos < requieredSpecs.size(); pos++) {
                String specCode = requieredSpecs.get(pos);
                RulItemTypeSpecAssign assignment = currDbAssignmentsBySpecCode.remove(specCode);
                int nextViewOrder = pos + 1;
                if (assignment == null) {
                    RulItemType itemType = allItemTypesByCode.get(itemTypeCode);
                    Validate.notNull(itemType, "Item type not found %s", itemTypeCode);
                    RulItemSpec itemSpec = dbUpdatedSpecsByCode.get(specCode);
                    Validate.notNull(itemSpec, "Item spec not found %s", specCode);

                    assignment = new RulItemTypeSpecAssign(itemType, itemSpec, nextViewOrder);
                    assignment = itemTypeSpecAssignRepository.save(assignment);

                    logger.debug("Specification '{}' assigned to item type '{}'", specCode, itemTypeCode);

                } else {
                    if (assignment.getViewOrder() != nextViewOrder) {
                        assignment.setViewOrder(nextViewOrder);
                    }
                    assignment = itemTypeSpecAssignRepository.save(assignment);
                }
            }
            // delete remaining assignments
            deleteSpecToTypeAssignments.addAll(currDbAssignmentsBySpecCode.values());
        }
        itemTypeSpecAssignRepository.flush();
    }

    /**
     * Do the update
     *
     * @return return list of updated types
     */
    public void update(ItemTypes xmlItemTypes,
                       ItemSpecs xmlItemSpecs,
                       @Nonnull final PackageContext pkgCtx) {

        List<RulItemType> dbItemTypes = prepareForUpdate(xmlItemTypes, pkgCtx.getPackage());
        processItemTypes(dbItemTypes, xmlItemTypes, pkgCtx);

        // update specifications
        processItemSpecs(xmlItemSpecs, pkgCtx.getPackage());

        postSpecsOrder(allItemTypesByCode.values());

        cleanUp();
    }

    /**
     * Clean up after update
     * 
     * Method will delete remaining items
     */
    private void cleanUp() {
        if (CollectionUtils.isNotEmpty(deleteSpecToTypeAssignments)) {
            itemTypeSpecAssignRepository.deleteAll(deleteSpecToTypeAssignments);
            itemTypeSpecAssignRepository.flush();
        }
        if (CollectionUtils.isNotEmpty(deleteItemApTypes)) {
            itemAptypeRepository.deleteAll(deleteItemApTypes);
            itemAptypeRepository.flush();
        }

        if (CollectionUtils.isNotEmpty(deleteItemSpecs)) {
            for (RulItemSpec rulItemSpec : deleteItemSpecs) {
                itemAptypeRepository.deleteByItemSpec(rulItemSpec);
                itemAptypeRepository.flush();
            }
            itemTypeSpecAssignRepository.deleteByItemSpecIn(deleteItemSpecs);
            itemTypeSpecAssignRepository.flush();
            itemSpecRepository.deleteAll(deleteItemSpecs);
            itemSpecRepository.flush();
        }

        if (CollectionUtils.isNotEmpty(deleteItemTypes)) {
            for (RulItemType rulItemType : deleteItemTypes) {
                itemAptypeRepository.deleteByItemType(rulItemType);
                itemAptypeRepository.flush();
            }
            itemTypeSpecAssignRepository.deleteByItemTypeIn(deleteItemTypes);

            itemTypeRepository.deleteAll(deleteItemTypes);
            itemTypeRepository.flush();
        }
    }

    private void processItemTypes(List<RulItemType> dbItemTypesOrig,
                                  ItemTypes itemTypes,
                                  @Nonnull PackageContext puc) {

        Map<String, RulItemType> origDBItemsByCode = new HashMap<>();
        // prepare map by viewOrder
        Map<Integer, RulItemType> origDBItemsByPos = new HashMap<>();
        for (RulItemType dbItemTypeOrig : dbItemTypesOrig) {
            origDBItemsByCode.put(dbItemTypeOrig.getCode(), dbItemTypeOrig);
            origDBItemsByPos.put(dbItemTypeOrig.getViewOrder(), dbItemTypeOrig);
        }

        List<RulItemType> itemTypesAfterUpdate = new ArrayList<>();

        // prepare list of updated/new items
        if (itemTypes != null && CollectionUtils.isNotEmpty(itemTypes.getItemTypes())) {

            Set<String> rulItemTypeCodes = new HashSet<>();

            for (ItemType itemType : itemTypes.getItemTypes()) {
                // check code duplicity
                String itemTypeCode = itemType.getCode();
                if (rulItemTypeCodes.contains(itemTypeCode)) {
                    throw new SystemException("Duplicitní kód typu: " + itemTypeCode, BaseCode.ID_EXIST)
                            .set("code", itemTypeCode);
                }

                // get type
                DataType newDataType = DataType.fromCode(itemType.getDataType());
                if (newDataType == null) {
                    throw new SystemException("Incorrect data type: " + itemType.getDataType(), BaseCode.ID_NOT_EXIST)
                            .set("dataType", itemType.getDataType())
                            .set("code", itemTypeCode);
                }

                // pouzijeme remove() - co zbyde bude smazano z DB
                RulItemType rulItemType = origDBItemsByCode.remove(itemTypeCode);
                // prepare positions
                if (rulItemType != null) {
                    // remove original position and mark it as free
                    origDBItemsByPos.remove(rulItemType.getViewOrder());
                }
                RulItemType rulItemTypeOnSamePos = origDBItemsByPos.remove(this.nextViewOrderPos);
                if (rulItemTypeOnSamePos != null &&
                        rulItemTypeOnSamePos != rulItemType) {
                    // put colliding item as last and save
                    rulItemTypeOnSamePos.setViewOrder(++lastUsedOrderPos);
                    rulItemTypeOnSamePos = itemTypeRepository.saveAndFlush(rulItemTypeOnSamePos);
                    origDBItemsByCode.put(rulItemTypeOnSamePos.getCode(), rulItemTypeOnSamePos);
                    origDBItemsByPos.put(rulItemTypeOnSamePos.getViewOrder(), rulItemTypeOnSamePos);
                }
                // now nextViewOrderPos is free and can be used
                boolean modified;
                if (rulItemType != null) {
                    modified = updateDBItemType(rulItemType, itemType, newDataType);
                } else {
                    // check existance of same code in other package
                    rulItemType = this.allItemTypesByCode.get(itemTypeCode);
                    if (rulItemType != null) {
                        throw new SystemException("Duplicitní kód typu v jiném balíčku: " + itemTypeCode,
                                BaseCode.ID_EXIST)
                                .set("code", itemTypeCode);
                    }
                    modified = true;
                    rulItemType = prepareNewItemType(itemType, newDataType, puc);
                }

                // copy values from VO
                modified |= convertRulItemType(itemType, rulItemType, puc);

                // update view order
                if (!Objects.equals(this.nextViewOrderPos, rulItemType.getViewOrder())) {
                    rulItemType.setViewOrder(nextViewOrderPos);
                    modified = true;
                }
                nextViewOrderPos++;

                // save updated items
                if (modified) {
                    logger.info("Updating item type, code: {}", itemTypeCode);
                    rulItemType = itemTypeRepository.saveAndFlush(rulItemType);
                    allItemTypesByCode.put(rulItemType.getCode(), rulItemType);
                }
                itemTypesAfterUpdate.add(rulItemType);
            }

            processItemAptypesByItemTypes(itemTypesAfterUpdate, itemTypes.getItemTypes());
        }

        // delete unused item types
        deleteItemTypes.addAll(origDBItemsByCode.values());
    }

    /**
     * Seřazení záznamů v tabulce RulItemSpecAssing (rul_item_type_spec_assign)
     * 
     * @param rulItemTypes
     */
    private void postSpecsOrder(Collection<RulItemType> itemTypes) {

        itemTypeSpecAssignRepository.flush();

        List<RulItemTypeSpecAssign> assignmentList = itemTypeSpecAssignRepository.findByItemTypesSorted(itemTypes);
        Map<Integer, List<RulItemTypeSpecAssign>> assignmentsByType = assignmentList.stream()
                .collect(Collectors.groupingBy(a -> a.getItemType().getItemTypeId()));

        final List<RulPackage> sortedPackages = getSortedPackages();

        for (RulItemType rulItemType : itemTypes) {
            List<RulItemTypeSpecAssign> ritsaList = assignmentsByType.get(rulItemType.getItemTypeId());
            if (CollectionUtils.isEmpty(ritsaList)) {
                continue;
            }

            // seřazení podle priority balíčků
            ritsaList.sort((o1, o2) -> {
                int i1 = sortedPackages.indexOf(o1.getItemSpec().getPackage());
                int i2 = sortedPackages.indexOf(o2.getItemSpec().getPackage());
                return Integer.compare(i1, i2);
            });

            // provede přečíslování
            for (int i = 0; i < ritsaList.size(); i++) {
                RulItemTypeSpecAssign ritsa = ritsaList.get(i);
                ritsa.setViewOrder(i + 1);
            }
            itemTypeSpecAssignRepository.saveAll(ritsaList);
        }
    }

    /**
     * Vrací všechny balíčky serazené podle topologického řazení - podle závislostí
     * mezi sebou.
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

    private RulItemType prepareNewItemType(ItemType itemType, DataType newDataType, PackageContext puc) {
        RulItemType dbItemType = new RulItemType();
        dbItemType.setDataType(newDataType.getEntity());
        dbItemType.setRulPackage(puc.getPackage());
        return dbItemType;
    }

    /**
     * Update existing item type with new values
     * 
     * @return Return if itemType was modified
     */
    private boolean updateDBItemType(RulItemType dbItemType, ItemType itemType, DataType newDataType) {
        boolean modified = false;

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
            modified = true;
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

        // TODO: consider moving to other place
        Object viewDefinition = dbItemType.getViewDefinition();
        if (viewDefinition != null) {
            switch (currDataType) {
                case JSON_TABLE: {
                    if (!canUpdateColumns((List<ElzaColumn>) viewDefinition, itemType.getColumnsDefinition())) {
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
        return modified;
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

        // z důvodu kompatibility v JAVA 11+, kde je již formát českého datumu opraven (v Locale)
        DateTimeFormatter locFormatter5 = DateTimeFormatter.ofPattern("d.M.yyyy");
        mfp.appendFormat(locFormatter5);

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
            LocalDate locDate = mfp.parseDate(srcValue.getStringValue(), LocalDate.now());

            dataDateRepository.insertMasterOnly(srcValue.getDataId(), locDate);
        }

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
     * @param itemType
     *            XML definice typu
     * @param dbItemType
     *            DAO typy
     * @param puc
     *            balíček
     * @return if modified
     */
    private boolean convertRulItemType(final ItemType itemType,
                                    final RulItemType dbItemType,
                                    final PackageContext puc) {

        boolean modified = false;

        Validate.notNull(dbItemType.getDataTypeId());
        Validate.notNull(dbItemType.getRulPackage());

        if (!Objects.equals(dbItemType.getCode(), itemType.getCode())) {
            dbItemType.setCode(itemType.getCode());
            modified = true;
        }
        if (!Objects.equals(dbItemType.getName(), itemType.getName())) {
            dbItemType.setName(itemType.getName());
            modified = true;
        }

        if (!Objects.equals(dbItemType.getShortcut(), itemType.getShortcut())) {
            dbItemType.setShortcut(itemType.getShortcut());
            modified = true;
        }

        String description = itemType.getDescription() == null ? itemType.getName() : itemType.getDescription();
        if (!Objects.equals(dbItemType.getDescription(), description)) {
            dbItemType.setDescription(description);
            modified = true;
        }

        Boolean isValueUnique = itemType.getIsValueUnique() == null ? false : itemType.getIsValueUnique();
        if (!Objects.equals(dbItemType.getIsValueUnique(), isValueUnique)) {
            dbItemType.setIsValueUnique(isValueUnique);
            modified = true;
        }

        Boolean canBeOrdered = itemType.getCanBeOrdered() == null ? false : itemType.getCanBeOrdered();
        if (!Objects.equals(dbItemType.getCanBeOrdered(), canBeOrdered)) {
            dbItemType.setCanBeOrdered(canBeOrdered);
            modified = true;
        }

        if (!Objects.equals(dbItemType.getUseSpecification(), itemType.getUseSpecification())) {
            dbItemType.setUseSpecification(itemType.getUseSpecification());
            modified = true;
        }

        if (!Objects.equals(dbItemType.getStringLengthLimit(), itemType.getStringLengthLimit())) {
            dbItemType.setStringLengthLimit(itemType.getStringLengthLimit());
            modified = true;
        }

        // check and find structured type
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
        if (rulStructureType != null || dbItemType.getStructuredType() != null) {
            if (dbItemType.getStructuredType() == null ||
                    rulStructureType == null ||
                    !Objects.equals(dbItemType.getStructuredTypeId(), rulStructureType.getStructuredTypeId())) {
                dbItemType.setStructuredType(rulStructureType);
                modified = true;
            }
        }

        Object viewDefinition = null;
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
            viewDefinition = elzaColumns;
        }

        DisplayType displayType = itemType.getDisplayType();
        if (displayType != null) {
            viewDefinition = cz.tacr.elza.domain.integer.DisplayType.valueOf(displayType.name());
        }
        if (!Objects.equals(viewDefinition, dbItemType.getViewDefinition())) {
            dbItemType.setViewDefinition(viewDefinition);
            modified = true;
        }

        return modified;
    }

    /**
     * Převod VO na DAO specifikace atributu.
     *
     * @param rulPackage  balíček
     * @param itemSpec    VO specifikace
     * @param rulItemSpec DAO specifikace
     */
    private void convertRulItemSpec(final RulPackage rulPackage,
                                    final ItemSpec itemSpec,
                                    final RulItemSpec rulItemSpec) {
        // check length code and shortcut
        if ((itemSpec.getCode() != null && itemSpec.getCode().length() > 50)
            || (itemSpec.getShortcut() != null && itemSpec.getShortcut().length() > 50)) {
            throw new SystemException("Item spec code or shortcut is too long", BaseCode.INVALID_LENGTH)
                .set("iteSpec.code", itemSpec.getCode())
                .set("iteSpec.shortcut", itemSpec.getShortcut());
        }

        rulItemSpec.setName(itemSpec.getName());
        rulItemSpec.setCode(itemSpec.getCode());
        rulItemSpec.setDescription(itemSpec.getDescription());
        rulItemSpec.setShortcut(itemSpec.getShortcut());
        rulItemSpec.setPackage(rulPackage);

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
     * @param xmlItemSpecs         seznam importovaných specifikací
     * @param rulItemSpecsCache seznam specifikací atributů (nový v DB)
     */
    private void processItemAptypesByItemSpecs(List<ItemSpec> xmlItemSpecs,
                                               @Nonnull Map<String, RulItemSpec> rulItemSpecsByCode
                                               ) {

        if (CollectionUtils.isEmpty(xmlItemSpecs)) {
            return;
        }

        List<RulItemSpec> dbItemSpecs = xmlItemSpecs.stream().map(is -> rulItemSpecsByCode.get(is.getCode()))
                .collect(Collectors.toList());

        List<RulItemAptype> dbItemAptypes = itemAptypeRepository.findByItemSpecs(dbItemSpecs);
        
        // roztrideni dle typu
        Map<String, List<RulItemAptype>> dbItemsApTypesByCode = dbItemAptypes.stream()
                .collect(Collectors.groupingBy(apt -> apt.getItemSpec().getCode()));

        List<RulItemAptype> itemAptypesNew = new ArrayList<>();

        for (ItemSpec xmlItemSpec : xmlItemSpecs) {
            RulItemSpec rulItemSpec = rulItemSpecsByCode.get(xmlItemSpec.getCode());
            Validate.notNull(rulItemSpec, "Cannot find code in itemSpecs, code: {}", xmlItemSpec.getCode());

            List<RulItemAptype> dbItemApTypes = dbItemsApTypesByCode.get(xmlItemSpec.getCode());
            Map<String, RulItemAptype> typesByApType = dbItemApTypes != null ? dbItemApTypes.stream().collect(Collectors
                    .toMap(iat -> iat.getApType().getCode(), Function.identity()))
                    : Collections.emptyMap();

            if (xmlItemSpec.getItemAptypes() != null) {
                for (ItemAptype xmlApType : xmlItemSpec.getItemAptypes()) {
                    RulItemAptype rulItemAptype = typesByApType.remove(xmlApType.getRegisterType());
                    if (rulItemAptype == null) {
                        ApType apType = apTypeByCode.get(xmlApType.getRegisterType());

                        Validate.notNull(apType, "Cannot find code in ApTypes, code: {}", xmlApType.getRegisterType());

                        rulItemAptype = prepareNewRulItemAptype(apType, rulItemSpec, null);
                        itemAptypesNew.add(rulItemAptype);
                    }
                }
            }

            // Collection of APTypes to delete            
            this.deleteItemApTypes.addAll(typesByApType.values());
        }

        if (CollectionUtils.isNotEmpty(itemAptypesNew)) {
            itemAptypesNew = itemAptypeRepository.saveAll(itemAptypesNew);
        }
    }

    /**
     * Zpracování napojení typů na ap.
     * 
     * @param itemTypesAfterUpdate
     *
     * @param xmlItemTypes
     *            seznam importovaných typů
     */
    private void processItemAptypesByItemTypes(List<RulItemType> itemTypesAfterUpdate,
                                               List<ItemType> xmlItemTypes) {

        if (CollectionUtils.isEmpty(xmlItemTypes)) {
            return;
        }

        List<RulItemAptype> itemAptypesNew = new ArrayList<>();

        List<RulItemAptype> rulItemAptypeDb = itemAptypeRepository.findByItemTypes(itemTypesAfterUpdate);
        // split by item typ
        Map<String, List<RulItemAptype>> itemApTypesByCode = rulItemAptypeDb.stream()
                .collect(Collectors.groupingBy(tb -> tb.getItemType().getCode()));

        for (ItemType xmlItemType : xmlItemTypes) {

            RulItemType itemType = allItemTypesByCode.get(xmlItemType.getCode());
            Validate.notNull(itemType, "Cannot find code in itemTypes, code: {}", xmlItemType.getCode());

            Map<Integer, RulItemAptype> itemAptypeByAptypeId = new HashMap<>();
            List<RulItemAptype> itemApTypes = itemApTypesByCode.get(itemType.getCode());
            if (itemApTypes != null) {
                for (RulItemAptype rulItemAptype : itemApTypes) {
                    itemAptypeByAptypeId.put(rulItemAptype.getApType().getApTypeId(), rulItemAptype);
                }
            }
            
            // update itemAptypes
            if(xmlItemType.getItemAptypes()!=null) {
                for (ItemAptype xmlApType : xmlItemType.getItemAptypes()) {
                    ApType apType = apTypeByCode.get(xmlApType.getRegisterType());

                    Validate.notNull(apType, "Cannot find code in ApTypes, code: {}", xmlApType.getRegisterType());
                    // check if mapping exists
                    RulItemAptype itemApType = itemAptypeByAptypeId.remove(apType.getApTypeId());
                    if (itemApType == null) {
                        // mapping not exists -> create new one
                        itemApType = prepareNewRulItemAptype(apType, null, itemType);
                        itemAptypesNew.add(itemApType);
                    }
                }
            }
            
            // delete remaining itemApTypes
            this.deleteItemApTypes.addAll(itemAptypeByAptypeId.values());
        }

        if (CollectionUtils.isNotEmpty(itemAptypesNew)) {
            itemAptypesNew = itemAptypeRepository.saveAll(itemAptypesNew);
        }
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
     * Return number of dropped cache nodes
     */
    public int getNumDroppedCachedNode() {
        return this.numDroppedCachedNode;
    }

}
