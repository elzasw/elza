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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;

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
import cz.tacr.elza.domain.RulItemTypeDeclaration;
import cz.tacr.elza.domain.RulItemSpecAssignDeclaration;
import cz.tacr.elza.domain.RulItemSpecDeclaration;
import cz.tacr.elza.domain.SysLanguage;
import cz.tacr.elza.core.data.PackageTexts;
import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.PackageCode;
import cz.tacr.elza.domain.RulItemTypeSpecAssign;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPackageDependency;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.domain.table.ElzaColumn;
import cz.tacr.elza.domain.viewDefinition.StringViewDefinition;
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
import cz.tacr.elza.repository.ApRevItemRepository;
import cz.tacr.elza.repository.ItemTypeDeclarationRepository;
import cz.tacr.elza.repository.ItemSpecAssignDeclarationRepository;
import cz.tacr.elza.repository.ItemSpecDeclarationRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.CachedNodeRepository;
import cz.tacr.elza.repository.DataDateRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DataStringRepository;
import cz.tacr.elza.repository.DataStringRepository.OnlyValues;
import cz.tacr.elza.repository.DataTextRepository;
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
    private DataTextRepository dataTextRepository;

    @Autowired
    private CachedNodeRepository cachedNodeRepository;

    @Autowired
    private ApTypeRepository apTypeRepository;

    @Autowired
    private PackageRepository packageRepository;

    @Autowired
    private PackageDependencyRepository packageDependencyRepository;

    @Autowired
    private ItemTypeDeclarationRepository declarationRepository;

    @Autowired
    private ApRevItemRepository apRevItemRepository;

    @Autowired
    private ItemSpecDeclarationRepository specDeclarationRepository;

    @Autowired
    private ItemSpecAssignDeclarationRepository assignDeclarationRepository;

    @Autowired
    private PackageTexts packageTexts;

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
     * Item types whose declarations changed; summarized at the end of the update
     */
    private Map<Integer, RulItemType> declaredItemTypes = new LinkedHashMap<>();

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
                Objects.requireNonNull(lastPackageViewOrder);
                if (lastPackageViewOrder > lastUsedOrderPos) {
                    lastUsedOrderPos = lastPackageViewOrder;
                }
            }
            
            if(es.getKey().equals(rulPackage.getPackageId())) {
                packageFound = true;
                // prepare number of received types
                // declarations of item types of other packages take no position
                int numReceivedTypes = ownXmlTypes(xmlItemTypes, rulPackage).size();
                
                // check if some shifts are needed
                if(numReceivedTypes>es.getValue().size()) {
                    shiftBy = numReceivedTypes - es.getValue().size();
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
            lastUsedOrderPos += shiftBy;
        }
        return result;
    }

    /**
     * Item types of the XML the package owns or creates; the others are declarations of item types
     * owned by other packages.
     */
    private List<ItemType> ownXmlTypes(ItemTypes xmlItemTypes, RulPackage rulPackage) {
        if (xmlItemTypes == null || xmlItemTypes.getItemTypes() == null) {
            return Collections.emptyList();
        }
        return xmlItemTypes.getItemTypes().stream()
                .filter(t -> !isForeign(t.getCode(), rulPackage))
                .toList();
    }

    /**
     * The item type exists and is owned by another package.
     */
    private boolean isForeign(String code, RulPackage rulPackage) {
        RulItemType itemType = allItemTypesByCode.get(code);
        return itemType != null && !itemType.getRulPackage().getPackageId().equals(rulPackage.getPackageId());
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
    /**
     * Declarations of specifications by the package: the first package creates a specification, further
     * packages add declarations (texts, category, assignments to item types). The assignments of a
     * specification are the union of the assignments of its declarations. A specification no longer
     * declared by any package is removed, not while items use it.
     */
    private void processItemSpecs(ItemSpecs xmlItemSpecs,
                                  @Nonnull RulPackage rulPackage) {
        Map<String, RulItemSpec> allSpecsByCode = itemSpecRepository.findAll().stream()
                .collect(toMap(RulItemSpec::getCode, Function.identity()));
        Map<String, RulItemSpecDeclaration> oldDeclarations = new HashMap<>();
        specDeclarationRepository.findByRulPackage(rulPackage)
                .forEach(d -> oldDeclarations.put(d.getItemSpec().getCode(), d));
        List<RulItemSpecAssignDeclaration> oldAssigns = new ArrayList<>(
                assignDeclarationRepository.findByDeclarations(oldDeclarations.values()));

        Map<String, RulItemSpecDeclaration> declarations = new LinkedHashMap<>();
        Map<Integer, RulItemSpec> affected = new LinkedHashMap<>();
        // assignments of the package per item type code, in the order of the file
        Map<String, List<XmlSpecAssignment>> xmlSpecAssigmentsByType = new LinkedHashMap<>();

        if (xmlItemSpecs != null && CollectionUtils.isNotEmpty(xmlItemSpecs.getItemSpecs())) {
            List<ItemSpec> ownXmlSpecs = new ArrayList<>();
            Map<String, RulItemSpec> ownSpecsByCode = new HashMap<>();

            for (ItemSpec xmlItemSpec : xmlItemSpecs.getItemSpecs()) {
                String itemSpecCode = xmlItemSpec.getCode();
                if (declarations.containsKey(itemSpecCode)) {
                    throw new SystemException("Duplicitní kód specifikace: " + itemSpecCode, BaseCode.DB_INTEGRITY_PROBLEM)
                            .set("code", itemSpecCode);
                }
                checkSpecLength(xmlItemSpec);

                RulItemSpec rulItemSpec = allSpecsByCode.get(itemSpecCode);
                boolean foreign = rulItemSpec != null
                        && !rulItemSpec.getPackage().getPackageId().equals(rulPackage.getPackageId());
                if (foreign) {
                    // RECORD_REF classes of a specification stay with its owner; a declaration may repeat them
                    if (!sameApTypes(xmlItemSpec.getItemAptypes(), itemAptypeRepository.findByItemSpec(rulItemSpec))) {
                        throw new BusinessException("Specification " + itemSpecCode
                                + " is owned by another package, its classes cannot be declared",
                                PackageCode.ITEM_SPEC_CONFLICT)
                                .set("code", itemSpecCode)
                                .set("otherPackageCode", rulItemSpec.getPackage().getCode());
                    }
                } else {
                    if (rulItemSpec == null) {
                        rulItemSpec = new RulItemSpec();
                        logger.debug("Creating new specification: {}", itemSpecCode);
                    }
                    convertRulItemSpec(rulPackage, xmlItemSpec, rulItemSpec);
                    rulItemSpec = itemSpecRepository.save(rulItemSpec);
                    allSpecsByCode.put(itemSpecCode, rulItemSpec);
                    ownXmlSpecs.add(xmlItemSpec);
                    ownSpecsByCode.put(itemSpecCode, rulItemSpec);
                }

                RulItemSpecDeclaration declaration = oldDeclarations.remove(itemSpecCode);
                if (declaration == null) {
                    declaration = new RulItemSpecDeclaration();
                }
                declaration.setItemSpec(rulItemSpec);
                declaration.setRulPackage(rulPackage);
                declaration.setName(xmlItemSpec.getName());
                declaration.setShortcut(xmlItemSpec.getShortcut());
                declaration.setDescription(xmlItemSpec.getDescription());
                declaration.setCategory(categoryOf(xmlItemSpec));
                declaration = specDeclarationRepository.save(declaration);
                declarations.put(itemSpecCode, declaration);
                affected.put(rulItemSpec.getItemSpecId(), rulItemSpec);

                if (xmlItemSpec.getItemTypeAssigns() != null) {
                    for (ItemTypeAssign xmlSpecAssignment : xmlItemSpec.getItemTypeAssigns()) {
                        xmlSpecAssigmentsByType.computeIfAbsent(xmlSpecAssignment.getCode(), c -> new ArrayList<>())
                                .add(new XmlSpecAssignment(itemSpecCode,
                                        StringUtils.trimToNull(xmlSpecAssignment.getViewAfter())));
                    }
                }
            }

            processItemAptypesByItemSpecs(ownXmlSpecs, ownSpecsByCode);
        }

        declareAssignments(xmlSpecAssigmentsByType, declarations, oldAssigns, affected);

        // declarations the package no longer has (entities, not a bulk query: instances loaded earlier in
        // the transaction would otherwise stay in the session and refer to removed declarations)
        oldDeclarations.values().forEach(d -> affected.put(d.getItemSpecId(), d.getItemSpec()));
        assignDeclarationRepository.deleteAll(assignDeclarationRepository.findByDeclarations(oldDeclarations.values()));
        specDeclarationRepository.deleteAll(oldDeclarations.values());
        specDeclarationRepository.flush();

        resolveItemSpecs(affected.values(), rulPackage);
    }

    /**
     * Assignment declarations of the package, rebuilt from the file; the position is the order of the
     * assignment among the package's assignments of the item type.
     */
    private void declareAssignments(final Map<String, List<XmlSpecAssignment>> xmlSpecAssigmentsByType,
                                    final Map<String, RulItemSpecDeclaration> declarations,
                                    final List<RulItemSpecAssignDeclaration> oldAssigns,
                                    final Map<Integer, RulItemSpec> affected) {
        Map<String, RulItemSpecAssignDeclaration> oldByKey = new HashMap<>();
        for (RulItemSpecAssignDeclaration assign : oldAssigns) {
            oldByKey.put(assign.getItemSpecDeclarationId() + "/" + assign.getItemTypeId(), assign);
        }
        for (Entry<String, List<XmlSpecAssignment>> entry : xmlSpecAssigmentsByType.entrySet()) {
            RulItemType itemType = allItemTypesByCode.get(entry.getKey());
            Validate.notNull(itemType, "Item type not found %s", entry.getKey());
            List<XmlSpecAssignment> required = entry.getValue();
            for (int pos = 0; pos < required.size(); pos++) {
                RulItemSpecDeclaration declaration = declarations.get(required.get(pos).specCode());
                RulItemSpecAssignDeclaration assign = oldByKey
                        .remove(declaration.getItemSpecDeclarationId() + "/" + itemType.getItemTypeId());
                if (assign == null) {
                    assign = new RulItemSpecAssignDeclaration();
                    assign.setSpecDeclaration(declaration);
                    assign.setItemType(itemType);
                }
                assign.setPosition(pos + 1);
                assign.setViewAfterSpecCode(required.get(pos).viewAfter());
                assignDeclarationRepository.save(assign);
            }
        }
        for (RulItemSpecAssignDeclaration removed : oldByKey.values()) {
            affected.put(removed.getSpecDeclaration().getItemSpecId(), removed.getSpecDeclaration().getItemSpec());
            assignDeclarationRepository.delete(removed);
        }
        assignDeclarationRepository.flush();
    }

    /**
     * Specifications whose declarations changed: a specification still declared takes owner, texts and
     * the union of the assignments from its declarations; one without declarations is removed.
     */
    private void resolveItemSpecs(final Collection<RulItemSpec> itemSpecs, final RulPackage rulPackage) {
        if (itemSpecs.isEmpty()) {
            return;
        }
        PackageDeclarations summary = packageDeclarations();
        // loaded at once: a package import touches all its specifications
        Map<Integer, List<RulItemSpecDeclaration>> declarationsBySpec = specDeclarationRepository
                .findByItemSpecs(itemSpecs).stream()
                .collect(Collectors.groupingBy(RulItemSpecDeclaration::getItemSpecId));
        Map<Integer, List<RulItemSpecAssignDeclaration>> assignsByDeclaration = assignDeclarationRepository
                .findByItemSpecs(itemSpecs).stream()
                .collect(Collectors.groupingBy(RulItemSpecAssignDeclaration::getItemSpecDeclarationId));
        Map<Integer, List<RulItemTypeSpecAssign>> assignmentsBySpec = itemTypeSpecAssignRepository
                .findByItemSpecIn(itemSpecs).stream()
                .collect(Collectors.groupingBy(a -> a.getItemSpec().getItemSpecId()));
        List<RulItemSpec> removed = new ArrayList<>();
        for (RulItemSpec itemSpec : itemSpecs) {
            List<RulItemSpecDeclaration> remaining = declarationsBySpec.getOrDefault(itemSpec.getItemSpecId(),
                                                                                     List.of());
            if (remaining.isEmpty()) {
                removed.add(itemSpec);
                continue;
            }
            Integer previousOwnerId = itemSpec.getPackage().getPackageId();
            summary.summarize(itemSpec, remaining);
            if (!previousOwnerId.equals(itemSpec.getPackage().getPackageId())) {
                // RECORD_REF classes belong to the declaration of the owner
                deleteItemApTypes.addAll(itemAptypeRepository.findByItemSpec(itemSpec));
            }
            RulItemSpec saved = itemSpecRepository.save(itemSpec);
            List<RulItemSpecAssignDeclaration> assigns = remaining.stream()
                    .flatMap(d -> assignsByDeclaration.getOrDefault(d.getItemSpecDeclarationId(), List.of()).stream())
                    .toList();
            syncAssignments(saved, remaining, assigns,
                            assignmentsBySpec.getOrDefault(itemSpec.getItemSpecId(), List.of()), summary);
        }
        checkSpecsRemovable(removed);
        deleteItemSpecs.addAll(removed);
        itemTypeSpecAssignRepository.flush();
    }

    /**
     * The assignments of the specification: one per item type assigned by any declaration; the position
     * and {@code view-after} from the owner's declaration of the pair, else from the winning one.
     */
    private void syncAssignments(final RulItemSpec itemSpec, final List<RulItemSpecDeclaration> declarations,
                                 final List<RulItemSpecAssignDeclaration> assigns,
                                 final List<RulItemTypeSpecAssign> assignments,
                                 final PackageDeclarations summary) {
        Map<Integer, Integer> rank = new HashMap<>();
        List<RulItemSpecDeclaration> ordered = summary.ordered(declarations, RulItemSpecDeclaration::getRulPackage);
        for (int i = 0; i < ordered.size(); i++) {
            boolean owner = ordered.get(i).getPackageId().equals(itemSpec.getPackage().getPackageId());
            rank.put(ordered.get(i).getItemSpecDeclarationId(), owner ? -1 : i);
        }
        Map<Integer, RulItemSpecAssignDeclaration> byItemType = new HashMap<>();
        for (RulItemSpecAssignDeclaration assign : assigns) {
            byItemType.merge(assign.getItemTypeId(), assign,
                             (a, b) -> rank.get(a.getItemSpecDeclarationId()) <= rank.get(b.getItemSpecDeclarationId())
                                     ? a : b);
        }
        Map<Integer, RulItemTypeSpecAssign> current = new HashMap<>();
        assignments.forEach(a -> current.put(a.getItemType().getItemTypeId(), a));
        for (RulItemSpecAssignDeclaration assign : byItemType.values()) {
            RulItemTypeSpecAssign assignment = current.remove(assign.getItemTypeId());
            // the owner's assignments keep their order; others follow them (postSpecsOrder renumbers)
            boolean ownerAssigns = rank.get(assign.getItemSpecDeclarationId()) < 0;
            int viewOrder = ownerAssigns ? assign.getPosition() : 1000 + assign.getPosition();
            if (assignment == null) {
                assignment = new RulItemTypeSpecAssign(assign.getItemType(), itemSpec, viewOrder);
                logger.debug("Specification '{}' assigned to item type '{}'", itemSpec.getCode(),
                             assign.getItemType().getCode());
            } else if (ownerAssigns) {
                assignment.setViewOrder(viewOrder);
            }
            assignment.setViewAfterSpecCode(assign.getViewAfterSpecCode());
            itemTypeSpecAssignRepository.save(assignment);
        }
        // assignments no package declares any more
        for (RulItemTypeSpecAssign removed : current.values()) {
            long used = itemRepository.countByTypeAndSpec(removed.getItemType(), itemSpec)
                    + apItemRepository.countByTypeAndSpec(removed.getItemType(), itemSpec)
                    + apRevItemRepository.countByTypeAndSpec(removed.getItemType(), itemSpec);
            if (used > 0) {
                throw specInUse(itemSpec.getCode() + " / " + removed.getItemType().getCode(), used);
            }
            deleteSpecToTypeAssignments.add(removed);
        }
    }

    /**
     * Specifications to be removed: refused while items use them.
     */
    private void checkSpecsRemovable(final List<RulItemSpec> itemSpecs) {
        for (RulItemSpec itemSpec : itemSpecs) {
            long used = itemRepository.countBySpec(itemSpec) + apItemRepository.countBySpec(itemSpec)
                    + apRevItemRepository.countBySpec(itemSpec);
            if (used > 0) {
                throw specInUse(itemSpec.getCode(), used);
            }
        }
    }

    private static AbstractException specInUse(final String codes, final long count) {
        return new BusinessException("Specification is used by descriptions or entities: " + codes,
                PackageCode.ITEM_SPEC_IN_USE)
                .set("codes", codes)
                .set("count", count);
    }

    private PackageDeclarations packageDeclarations() {
        SysLanguage defaultLanguage = packageTexts.defaultLanguage();
        return new PackageDeclarations(packageDependencyRepository.findAll(),
                defaultLanguage != null ? defaultLanguage.getLanguageId() : null);
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
        summarizeDeclaredItemTypes();

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
            assignDeclarationRepository.deleteAll(assignDeclarationRepository.findByItemTypes(deleteItemTypes));

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
        RulPackage rulPackage = puc.getPackage();
        Map<String, RulItemTypeDeclaration> oldDeclarations = new HashMap<>();
        declarationRepository.findByRulPackage(rulPackage)
                .forEach(d -> oldDeclarations.put(d.getItemType().getCode(), d));

        // prepare list of updated/new items
        if (itemTypes != null && CollectionUtils.isNotEmpty(itemTypes.getItemTypes())) {

            Set<String> rulItemTypeCodes = new HashSet<>();

            for (ItemType itemType : itemTypes.getItemTypes()) {
                // check code duplicity
                String itemTypeCode = itemType.getCode();
                if (!rulItemTypeCodes.add(itemTypeCode)) {
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

                // an item type of another package: a declaration, which takes no position
                if (isForeign(itemTypeCode, rulPackage)) {
                    RulItemType foreignType = allItemTypesByCode.get(itemTypeCode);
                    checkAgreement(itemType, newDataType, foreignType);
                    // RECORD_REF classes stay with the owner; a declaration may repeat them
                    if (!sameApTypes(itemType.getItemAptypes(), itemAptypeRepository.findByItemType(foreignType))) {
                        throw conflict(itemTypeCode, "item-aptypes", foreignType);
                    }
                    declare(foreignType, rulPackage, oldDeclarations.remove(itemTypeCode), itemType.getName(),
                            itemType.getShortcut(), descriptionOf(itemType), canBeOrderedOf(itemType),
                            itemType.getStringLengthLimit(), viewDefinitionJson(itemType));
                    continue;
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
                    // declared by other packages too: the stored data must stay as they declare them
                    if (hasOtherDeclarations(rulItemType, rulPackage)) {
                        checkAgreement(itemType, newDataType, rulItemType);
                    }
                    modified = updateDBItemType(rulItemType, itemType, newDataType);
                } else {
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
                declare(rulItemType, rulPackage, oldDeclarations.remove(itemTypeCode), rulItemType.getName(),
                        rulItemType.getShortcut(), rulItemType.getDescription(), rulItemType.getCanBeOrdered(),
                        rulItemType.getStringLengthLimit(), rulItemType.getViewDefinitionJson());
                itemTypesAfterUpdate.add(rulItemType);
            }

            processItemAptypesByItemTypes(itemTypesAfterUpdate, ownXmlTypes(itemTypes, rulPackage));
        }

        // declarations the package no longer has
        declarationRepository.deleteAll(oldDeclarations.values());
        declarationRepository.flush();
        oldDeclarations.values().forEach(d -> declaredItemTypes.put(d.getItemTypeId(), d.getItemType()));

        // own item types no longer declared: another package takes over, or they are removed
        List<RulItemType> removed = new ArrayList<>();
        for (RulItemType itemType : origDBItemsByCode.values()) {
            if (declarationRepository.findByItemTypes(List.of(itemType)).isEmpty()) {
                declaredItemTypes.remove(itemType.getItemTypeId());
                removed.add(itemType);
            } else {
                // RECORD_REF classes belong to the declaration of the owner
                deleteItemApTypes.addAll(itemAptypeRepository.findByItemType(itemType));
            }
        }
        checkRemovable(removed, rulPackage);
        deleteItemTypes.addAll(removed);
    }

    /**
     * Creates or updates the declaration of the item type by the package.
     */
    private void declare(RulItemType itemType, RulPackage rulPackage, RulItemTypeDeclaration declaration,
                         String name, String shortcut, String description, Boolean canBeOrdered,
                         Integer stringLengthLimit, String viewDefinition) {
        if (declaration == null) {
            declaration = new RulItemTypeDeclaration();
        }
        declaration.setItemType(itemType);
        declaration.setRulPackage(rulPackage);
        declaration.setName(name);
        declaration.setShortcut(shortcut);
        declaration.setDescription(description);
        declaration.setCanBeOrdered(canBeOrdered);
        declaration.setStringLengthLimit(stringLengthLimit);
        declaration.setViewDefinition(viewDefinition);
        declarationRepository.save(declaration);
        declaredItemTypes.put(itemType.getItemTypeId(), itemType);
    }

    private boolean hasOtherDeclarations(RulItemType itemType, RulPackage rulPackage) {
        return declarationRepository.findByItemTypes(List.of(itemType)).stream()
                .anyMatch(d -> !d.getPackageId().equals(rulPackage.getPackageId()));
    }

    /**
     * Values deciding how data of the item type are stored must agree with the existing item type.
     */
    private void checkAgreement(ItemType xmlItemType, DataType dataType, RulItemType itemType) {
        String code = xmlItemType.getCode();
        if (DataType.fromId(itemType.getDataTypeId()) != dataType) {
            throw conflict(code, "data-type", itemType);
        }
        if (!Objects.equals(itemType.getUseSpecification(), xmlItemType.getUseSpecification())) {
            throw conflict(code, "use-specification", itemType);
        }
        if (dataType == DataType.STRUCTURED) {
            String structuredType = itemType.getStructuredType() != null ? itemType.getStructuredType().getCode() : null;
            if (!Objects.equals(structuredType, xmlItemType.getStructureType())) {
                throw conflict(code, "structure-type", itemType);
            }
        }
        if (dataType == DataType.JSON_TABLE) {
            @SuppressWarnings("unchecked")
            List<ElzaColumn> columns = (List<ElzaColumn>) itemType.getViewDefinition();
            List<Column> xmlColumns = xmlItemType.getColumnsDefinition();
            int size = columns != null ? columns.size() : 0;
            if (size != (xmlColumns != null ? xmlColumns.size() : 0)
                    || (size > 0 && !canUpdateColumns(columns, xmlColumns))) {
                throw conflict(code, "columns-definitions", itemType);
            }
        }
    }

    private static AbstractException conflict(String code, String attribute, RulItemType itemType) {
        return new BusinessException("Item type " + code + " is declared by another package with another "
                + attribute, PackageCode.ITEM_TYPE_CONFLICT)
                .set("code", code)
                .set("attribute", attribute)
                .set("otherPackageCode", itemType.getRulPackage().getCode());
    }

    /**
     * Item types to be removed: refused while descriptions or entities use them, or specifications of
     * other packages are assigned to them.
     */
    private void checkRemovable(List<RulItemType> itemTypes, RulPackage rulPackage) {
        if (itemTypes.isEmpty()) {
            return;
        }
        List<String> used = itemTypes.stream().filter(t -> countUsage(t) + apRevItemRepository.countByType(t) > 0)
                .map(RulItemType::getCode).toList();
        if (!used.isEmpty()) {
            throw new BusinessException("Item types are used by descriptions or entities: " + used,
                    PackageCode.ITEM_TYPE_IN_USE)
                    .set("codes", String.join(", ", used));
        }
        String foreign = assignDeclarationRepository.findByItemTypes(itemTypes).stream()
                .map(a -> a.getSpecDeclaration().getRulPackage())
                .filter(p -> !p.getPackageId().equals(rulPackage.getPackageId()))
                .map(RulPackage::getCode)
                .distinct()
                .collect(Collectors.joining(", "));
        if (!foreign.isEmpty()) {
            throw new BusinessException("Specifications of other packages are assigned to a removed item type",
                    PackageCode.FOREIGN_DEPENDENCY)
                    .set("foreignPackageCodes", foreign);
        }
    }

    /**
     * Owner, texts and values of item types whose declarations changed.
     */
    private void summarizeDeclaredItemTypes() {
        if (declaredItemTypes.isEmpty()) {
            return;
        }
        PackageDeclarations summary = packageDeclarations();
        for (RulItemType itemType : declaredItemTypes.values()) {
            summary.summarize(itemType, declarationRepository.findByItemTypes(List.of(itemType)));
            RulItemType saved = itemTypeRepository.save(itemType);
            allItemTypesByCode.put(saved.getCode(), saved);
        }
        itemTypeRepository.flush();
    }

    /**
     * Removes the declarations of a deleted package: a specification or item type still declared by
     * another package stays (owned by the winning declaration when the package owned it), the others are
     * removed.
     */
    public void deletePackageDeclarations(RulPackage rulPackage) {
        List<RulItemSpecDeclaration> specDeclarations = specDeclarationRepository.findByRulPackage(rulPackage);
        Map<Integer, RulItemSpec> affectedSpecs = new LinkedHashMap<>();
        specDeclarations.forEach(d -> affectedSpecs.put(d.getItemSpecId(), d.getItemSpec()));
        assignDeclarationRepository.deleteAll(assignDeclarationRepository.findByDeclarations(specDeclarations));
        specDeclarationRepository.deleteAll(specDeclarations);
        specDeclarationRepository.flush();
        resolveItemSpecs(affectedSpecs.values(), rulPackage);

        List<RulItemTypeDeclaration> declarations = declarationRepository.findByRulPackage(rulPackage);
        declarationRepository.deleteAll(declarations);
        declarationRepository.flush();
        List<RulItemType> removed = new ArrayList<>();
        for (RulItemTypeDeclaration declaration : declarations) {
            RulItemType itemType = declaration.getItemType();
            boolean owned = itemType.getRulPackage().getPackageId().equals(rulPackage.getPackageId());
            if (declarationRepository.findByItemTypes(List.of(itemType)).isEmpty()) {
                removed.add(itemType);
            } else {
                if (owned) {
                    deleteItemApTypes.addAll(itemAptypeRepository.findByItemType(itemType));
                }
                declaredItemTypes.put(itemType.getItemTypeId(), itemType);
            }
        }
        checkRemovable(removed, rulPackage);
        deleteItemTypes.addAll(removed);
        summarizeDeclaredItemTypes();
        cleanUp();
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

            // specifications with view-after are moved right behind their anchor
            ritsaList = AnchoredOrder.apply(ritsaList,
                    a -> a.getItemSpec().getCode(),
                    RulItemTypeSpecAssign::getViewAfterSpecCode,
                    a -> logger.warn("Specification '{}' of item type '{}' should follow '{}', which is not a specification of this type; placed last",
                                     a.getItemSpec().getCode(), rulItemType.getCode(), a.getViewAfterSpecCode()));

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
                    case STRING:
                    	changeDataType2String(currDataType, dbItemType);
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
    
    /**
     * Change current data type to string
     */
    private void changeDataType2String(DataType currDataType, RulItemType dbItemType) {
        if (currDataType.equals(DataType.TEXT)) {
            // Do the conversion
            changeText2String(dbItemType);
            return;
        }

        throw new SystemException("Unsupported conversion from type to STRING", BaseCode.DB_INTEGRITY_PROBLEM)
                .set("currDataType", currDataType)
                .set("itemTypeId", dbItemType.getItemTypeId())
                .set("itemTypeCode", dbItemType.getCode());    	
    }
    
    private void changeText2String(RulItemType dbItemType) {

        // Invalidate node cache by item type
        numDroppedCachedNode += cachedNodeRepository.deleteByItemType(dbItemType);

        // Iterate ArrData from arr_item
        List<Integer> dataIds = dataRepository.findIdsByItemTypeFromArrItem(dbItemType);
        ObjectListIterator<Integer> oli = new ObjectListIterator<>(dataIds);
        while (oli.hasNext()) {
            List<Integer> ids = oli.next();
            changeText2String(ids);
        }

        // Iterate ArrData from ap_item
        dataIds = dataRepository.findIdsByItemTypeFromApItem(dbItemType);
        oli = new ObjectListIterator<>(dataIds);
        while (oli.hasNext()) {
            List<Integer> ids = oli.next();
            changeText2String(ids);
        }
    }
    
    private void changeText2String(List<Integer> ids) {
    	logger.info("Converting from text to string, dataIds: {}", ids);
        // request all current arr_data_string
        Collection<DataTextRepository.OnlyValues> srcValues = dataTextRepository.findValuesByDataIdIn(ids);
        // drop all old strings
        dataTextRepository.deleteMasterOnly(ids);
        // update data type
        dataRepository.updateDataType(ids, DataType.STRING.getId());
        // insert new arr_data_date
        for (DataTextRepository.OnlyValues srcValue : srcValues) {
        	// convert values
        	String srcTextValue = srcValue.getTextValue();
        	String trgStringValue = srcTextValue.replaceAll("\\r?\\n", "; ")
        			.replaceAll("\\s{2,}", " ");
        	if(!srcTextValue.equals(trgStringValue)) {
        		logger.info("Converting from text to string, dataId: {}, targetValue: {}, srcValue: {}", 
        				srcValue.getDataId(), trgStringValue, srcTextValue);
        	}
        	
            dataStringRepository.insertMasterOnly(srcValue.getDataId(), trgStringValue);
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
        long result = itemRepository.countByType(dbItemType);
        result += apItemRepository.countByType(dbItemType);
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

        Objects.requireNonNull(dbItemType.getDataTypeId());
        Objects.requireNonNull(dbItemType.getRulPackage());

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

        String description = descriptionOf(itemType);
        if (!Objects.equals(dbItemType.getDescription(), description)) {
            dbItemType.setDescription(description);
            modified = true;
        }

        Boolean canBeOrdered = canBeOrderedOf(itemType);
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

        Object viewDefinition = viewDefinitionOf(itemType);
        // compare previous and new view definition
        if (!Objects.equals(viewDefinition, dbItemType.getViewDefinition())) {
            dbItemType.setViewDefinition(viewDefinition);
            modified = true;
        }

        return modified;
    }

    /**
     * The RECORD_REF classes of a declaration equal those stored for the owner (both may be empty).
     */
    private static boolean sameApTypes(@Nullable List<ItemAptype> declared, List<RulItemAptype> stored) {
        Set<String> declaredCodes = declared == null ? Set.of()
                : declared.stream().map(ItemAptype::getRegisterType).collect(Collectors.toSet());
        Set<String> storedCodes = stored.stream().map(a -> a.getApType().getCode()).collect(Collectors.toSet());
        return declaredCodes.equals(storedCodes);
    }

    private static String descriptionOf(ItemType itemType) {
        return itemType.getDescription() == null ? itemType.getName() : itemType.getDescription();
    }

    private static Boolean canBeOrderedOf(ItemType itemType) {
        return itemType.getCanBeOrdered() == null ? false : itemType.getCanBeOrdered();
    }

    /**
     * View definition of the XML item type: columns of a table, display type or mask.
     */
    private static Object viewDefinitionOf(ItemType itemType) {
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
        	// TODO:: consider moving to package domain.viewDefinition
            viewDefinition = cz.tacr.elza.domain.integer.DisplayType.valueOf(displayType.name());
        } else 
        if(itemType.getMask() != null) {
        	StringViewDefinition stringViewDefinition = new StringViewDefinition();
        	stringViewDefinition.setMask(itemType.getMask());
        	viewDefinition = stringViewDefinition;
		}
        return viewDefinition;
    }

    /**
     * View definition of the XML item type as stored (JSON).
     */
    private static String viewDefinitionJson(ItemType itemType) {
        RulItemType stored = new RulItemType();
        stored.setViewDefinition(viewDefinitionOf(itemType));
        return stored.getViewDefinitionJson();
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
        rulItemSpec.setName(itemSpec.getName());
        rulItemSpec.setCode(itemSpec.getCode());
        rulItemSpec.setDescription(itemSpec.getDescription());
        rulItemSpec.setShortcut(itemSpec.getShortcut());
        rulItemSpec.setPackage(rulPackage);
        rulItemSpec.setCategory(categoryOf(itemSpec));
    }

    private static void checkSpecLength(final ItemSpec itemSpec) {
        if ((itemSpec.getCode() != null && itemSpec.getCode().length() > 50)
            || (itemSpec.getShortcut() != null && itemSpec.getShortcut().length() > 50)) {
            throw new SystemException("Item spec code or shortcut is too long", BaseCode.INVALID_LENGTH)
                .set("iteSpec.code", itemSpec.getCode())
                .set("iteSpec.shortcut", itemSpec.getShortcut());
        }
    }

    private static String categoryOf(final ItemSpec itemSpec) {
        if (CollectionUtils.isEmpty(itemSpec.getCategories())) {
            return null;
        }
        List<String> categories = itemSpec.getCategories().stream().map(Category::getValue).collect(Collectors.toList());
        return StringUtils.join(categories, CATEGORY_SEPARATOR);
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
            logger.info("Added table rul_item_aptype by itemSpecs, size=" + itemAptypesNew.size());
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
            logger.info("Added table rul_item_aptype by itemTypes, size=" + itemAptypesNew.size());
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

    /**
     * Specification assigned to an item type in the imported XML, with its optional anchor.
     */
    private record XmlSpecAssignment(String specCode, String viewAfter) {
    }
}
