package cz.tacr.elza.core.data;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import cz.tacr.elza.domain.*;
import cz.tacr.elza.repository.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.common.db.HibernateUtils;

public class StaticDataProvider {

    private List<RulPackage> packages;

    private List<ApType> apTypes;

    private List<StructType> structuredTypes;

    private List<ItemType> itemTypes;

    private List<RulRuleSet> ruleSets;

    private List<RulItemSpec> itemSpecs;

    private List<ApExternalIdType> apEidTypes;

    private List<RulPartType> partTypes;

    private List<SysLanguage> sysLanguages;

    private List<ApExternalSystem> apExternalSystems;

    private Map<Integer, RulPackage> packageIdMap;

    private Map<Integer, ApType> apTypeIdMap;

    private Map<String, ApType> apTypeCodeMap;

    private Map<Integer, StructType> structuredTypeIdMap = new HashMap<>();

    private Map<String, StructType> structuredTypeCodeMap = new HashMap<>();

    private Map<Integer, RulPartType> partTypeIdMap = new HashMap<>();

    private Map<String, RulPartType> partTypeCodeMap = new HashMap<>();

    private Map<Integer, ItemType> itemTypeIdMap;

    private Map<String, ItemType> itemTypeCodeMap;

    private Map<Integer, RulRuleSet> ruleSetIdMap;

    private Map<String, RulRuleSet> ruleSetCodeMap;

    private Map<Integer, RulItemSpec> itemSpecIdMap;

    private Map<String, RulItemSpec> itemSpecCodeMap;

    private Map<Integer, ApExternalIdType> apEidTypeIdMap;

    private Map<String, ApExternalIdType> apEidTypeCodeMap;

    private Map<Integer, SysLanguage> sysLanguageIdMap;

    private Map<String, SysLanguage> sysLanguageCodeMap;

    private Map<Integer, ApTypeRoles> apTypeRolesIdMap;

    private Map<String, ApExternalSystem> apExternalSystemCodeMap = new HashMap<>();

    private static StaticDataProvider self;

    StaticDataProvider() {
    }

    public List<RulPackage> getPackages() {
        return packages;
    }

    public List<ApType> getApTypes() {
        return apTypes;
    }

    public List<ApExternalIdType> getApEidTypes() {
        return apEidTypes;
    }

    public List<SysLanguage> getSysLanguages() {
        return sysLanguages;
    }

    public List<RulRuleSet> getRuleSets() {
        return ruleSets;
    }

    public RulPackage getPackageById(Integer id) {
        Validate.notNull(id);
        return packageIdMap.get(id);
    }

    /**
     * Returns fully initialized register type by id. This object and all his
     * referenced entities are detached.
     */
    public ApType getApTypeById(Integer id) {
        Validate.notNull(id);
        return apTypeIdMap.get(id);
    }

    public ApType getApTypeByCode(String code) {
        Validate.notEmpty(code);
        return apTypeCodeMap.get(code);
    }

    public ApExternalIdType getApEidTypeById(Integer id) {
        Validate.notNull(id);
        return apEidTypeIdMap.get(id);
    }

    public ApExternalIdType getApEidTypeByCode(String code) {
        Validate.notEmpty(code);
        return apEidTypeCodeMap.get(code);
    }

    public SysLanguage getSysLanguageById(Integer id) {
        Validate.notNull(id);
        return sysLanguageIdMap.get(id);
    }

    public SysLanguage getSysLanguageByCode(String code) {
        Validate.notEmpty(code);
        return sysLanguageCodeMap.get(code);
    }

    public List<StructType> getStructuredTypes() {
        return structuredTypes;
    }

    public List<RulPartType> getPartTypes() {
        return partTypes;
    }

    public StructType getStructuredTypeById(Integer id) {
        Validate.notNull(id);
        return structuredTypeIdMap.get(id);
    }

    public StructType getStructuredTypeByCode(String code) {
        Validate.notEmpty(code);
        return structuredTypeCodeMap.get(code);
    }

    public RulPartType getPartTypeById(Integer id) {
        Validate.notNull(id);
        return partTypeIdMap.get(id);
    }

    public RulPartType getPartTypeByCode(String code) {
        Validate.notEmpty(code);
        return partTypeCodeMap.get(code);
    }

    public List<ItemType> getItemTypes() {
        return itemTypes;
    }

    public ItemType getItemTypeById(Integer id) {
        Validate.notNull(id);
        return itemTypeIdMap.get(id);
    }

    /**
     *
     * @param code
     * @return Return null if item type does not exists
     */
    public ItemType getItemTypeByCode(String code) {
        Validate.notEmpty(code);
        return itemTypeCodeMap.get(code);
    }

    public RulRuleSet getRuleSetById(Integer id) {
        Validate.notNull(id);
        return ruleSetIdMap.get(id);
    }

    public RulRuleSet getRuleSetByCode(String code) {
        Validate.notEmpty(code);
        return ruleSetCodeMap.get(code);
    }

    public RulItemSpec getItemSpecById(Integer id) {
        Validate.notNull(id);
        return itemSpecIdMap.get(id);
    }

    public RulItemSpec getItemSpecByCode(String code) {
        Validate.notEmpty(code);
        return itemSpecCodeMap.get(code);
    }

    public ApTypeRoles getApTypeRolesById(Integer id) {
        Validate.notNull(id);
        return apTypeRolesIdMap.get(id);
    }

    public List<ApExternalSystem> getApExternalSystems() {
        return apExternalSystems;
    }

    public ApExternalSystem getApExternalSystemByCode(String code) {
        Validate.notNull(code);
        return apExternalSystemCodeMap.get(code);
    }


    /* initialization methods */

    /**
     * Initialize all static values. Caller must must ensure synchronized. Method
     * needs to be called in active transaction.
     */
    void init(StaticDataService service) {
        initRuleSets(service.ruleSetRepository);
        initStructuredTypes(service.structuredTypeRepository, service.structureDefinitionRepository);
        initItemTypes( service.itemTypeRepository, service.itemSpecRepository, service.itemTypeSpecAssignRepository);
        initPackages(service.packageRepository);
        initApTypes(service.apTypeRepository);
        initApEidTypes(service.apEidTypeRepository);
        initSysLanguages(service.sysLanguageRepository);
        initPartTypes(service.partTypeRepository);
        initApExternalSystems(service.apExternalSystemRepository);
        self = this;
    }

    private void initRuleSets(RuleSetRepository ruleSetRepository) {
        List<RulRuleSet> ruleSets = ruleSetRepository.findAll();

        this.ruleSets = Collections.unmodifiableList(ruleSets);
        this.ruleSetIdMap = createLookup(ruleSets, RulRuleSet::getRuleSetId);
        this.ruleSetCodeMap = createLookup(ruleSets, RulRuleSet::getCode);
    }

    private void initItemTypes(ItemTypeRepository itemTypeRepository, ItemSpecRepository itemSpecRepository, ItemTypeSpecAssignRepository itemTypeSpecAssignRepository) {
        List<RulItemType> itemTypes = itemTypeRepository.findAll();

        this.itemTypes = new ArrayList<>();
        this.itemSpecs = new ArrayList<>();

        itemTypeIdMap = new HashMap<>(itemTypes.size());
        itemSpecIdMap = new HashMap<>();

        for (RulItemType it : itemTypes) {
            it = HibernateUtils.unproxy(it);

            // update data type reference from cache
            DataType dataType = DataType.fromId(it.getDataTypeId());
            it.setDataType(dataType.getEntity());

            // create initialized rule system item type
            ItemType rsit = new ItemType(it, dataType);

            // add to lookups
            this.itemTypes.add(rsit);
            itemTypeIdMap.put(it.getItemTypeId(), rsit);
        }

        // get all specifications
        List<RulItemSpec> itemSpecs = itemSpecRepository.findAll();
        List<RulItemTypeSpecAssign> itemTypeSpecAssigns = itemTypeSpecAssignRepository.findAll();

        for(RulItemTypeSpecAssign itsa : itemTypeSpecAssigns) {
            ItemType rsit = itemTypeIdMap.get(itsa.getItemType().getItemTypeId());
            itsa.getItemSpec().setViewOrder(itsa.getViewOrder());
            addItemSpec(itsa.getItemSpec(), rsit);
        }

        // seal up item types
        this.itemTypes.forEach(ItemType::sealUp);

        this.itemTypeCodeMap = StaticDataProvider.createLookup(itemTypeIdMap.values(), ItemType::getCode);
        this.itemSpecCodeMap = StaticDataProvider.createLookup(itemSpecIdMap.values(), RulItemSpec::getCode);
    }

    /**
     * Add specification to lookup collections
     * @param is
     * @param rsit
     */
    private void addItemSpec(RulItemSpec is, ItemType rsit) {
        Validate.isTrue(rsit.hasSpecifications(), "Type does not allow specifications, typeCode: %s", rsit.getCode());

        // prepare real object
        is = HibernateUtils.unproxy(is);

        rsit.addItemSpec(is);
        // add to the lookups
        this.itemSpecs.add(is);
        itemSpecIdMap.put(is.getItemSpecId(), is);
    }

    private void initStructuredTypes(StructuredTypeRepository structuredTypeRepository,
                                     StructureDefinitionRepository structureDefinitionRepository) {
        List<RulStructuredType> structuredTypes = structuredTypeRepository.findAll();

        ArrayList<StructType> structTypes = new ArrayList<>(structuredTypes.size());

        // Prepare structured types
        structuredTypes.forEach(st -> {
            // read attrs definition
            List<RulStructureDefinition> attrDefs = structureDefinitionRepository
                    .findByStructTypeAndDefTypeOrderByPriority(st, RulStructureDefinition.DefType.ATTRIBUTE_TYPES);

            StructType structType = new StructType(st, attrDefs);
            structTypes.add(structType);

            structuredTypeIdMap.put(st.getStructuredTypeId(), structType);
            structuredTypeCodeMap.put(st.getCode(), structType);
        });

        this.structuredTypes = Collections.unmodifiableList(structTypes);
    }

    private void initPartTypes(PartTypeRepository partTypeRepository) {
        List<RulPartType> partTypes = partTypeRepository.findAll();

        if (CollectionUtils.isNotEmpty(partTypes)) {
            for (RulPartType partType : partTypes) {
                partTypeIdMap.put(partType.getPartTypeId(), partType);
                partTypeCodeMap.put(partType.getCode(), partType);
            }
        }

        this.partTypes = Collections.unmodifiableList(partTypes);
    }

    private void initApExternalSystems(ApExternalSystemRepository apExternalSystemRepository) {
        List<ApExternalSystem> apExternalSystems = apExternalSystemRepository.findAll();

        if (CollectionUtils.isNotEmpty(apExternalSystems)) {
            for (ApExternalSystem apExternalSystem : apExternalSystems) {
                apExternalSystemCodeMap.put(apExternalSystem.getCode(), apExternalSystem);
            }
        }

        this.apExternalSystems = Collections.unmodifiableList(apExternalSystems);
    }

    private void initPackages(PackageRepository packageRepository) {
        List<RulPackage> packages = packageRepository.findAll();

        // update fields
        this.packages = Collections.unmodifiableList(packages);
        this.packageIdMap = createLookup(packages, RulPackage::getPackageId);
    }

    private void checkPackageReference(RulPackage rulPackage) {
        if (rulPackage == null) {
            Validate.notNull(rulPackage);
        }
        RulPackage currPackage = packageIdMap.get(rulPackage.getPackageId());
        if (rulPackage != currPackage) {
            Validate.isTrue(rulPackage == currPackage);
        }
    }

    private void initApTypes(ApTypeRepository apTypeRepository) {
        List<ApType> apTypes = apTypeRepository.findAll();

        // We have to create lookup of copied objects
        Map<Integer, ApType> idMap = apTypes.stream().collect(Collectors.toMap(ApType::getApTypeId,
                                                                               ApType::makeCopy));

        // TODO: should be collection ordered?

        List<ApType> result = new ArrayList<>(apTypes.size());
        for (ApType rt : idMap.values()) {
            checkPackageReference(rt.getRulPackage());

            // switch parent reference
            Integer parentId = rt.getParentApTypeId();
            if (parentId != null) {
                ApType parent = idMap.get(parentId);
                Validate.notNull(parent);
                rt.setParentApType(parent);
            }

            result.add(rt);
        }
        // update fields
        this.apTypes = Collections.unmodifiableList(result);
        this.apTypeIdMap = idMap;
        this.apTypeCodeMap = createLookup(result, ApType::getCode);
    }



    private void initApEidTypes(ApExternalIdTypeRepository apEidTypeRepository) {
        List<ApExternalIdType> eidTypes = apEidTypeRepository.findAll();

        // update fields
        this.apEidTypes = Collections.unmodifiableList(eidTypes);
        this.apEidTypeIdMap = createLookup(eidTypes, ApExternalIdType::getExternalIdTypeId);
        this.apEidTypeCodeMap = createLookup(eidTypes, ApExternalIdType::getCode);
    }

    private void initSysLanguages(SysLanguageRepository sysLanguageRepository) {
        List<SysLanguage> languages = sysLanguageRepository.findAll();

        // update fields
        this.sysLanguages = Collections.unmodifiableList(languages);
        this.sysLanguageIdMap = createLookup(languages, SysLanguage::getLanguageId);
        this.sysLanguageCodeMap = createLookup(languages, SysLanguage::getCode);
    }

    public static <K, V> Map<K, V> createLookup(Collection<V> values, Function<V, K> keyMapping) {
        Map<K, V> lookup = new HashMap<>(values.size());
        values.forEach(is -> lookup.put(keyMapping.apply(is), is));
        return lookup;
    }

    public static StaticDataProvider getInstance() {
        if (self == null) {
            throw new IllegalStateException("Provider nebyl ještě inicializován");
        }
        return self;
    }
}
