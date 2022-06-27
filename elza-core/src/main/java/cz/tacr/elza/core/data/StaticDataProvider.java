package cz.tacr.elza.core.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.domain.ApExternalIdType;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.RulArrangementExtension;
import cz.tacr.elza.domain.RulArrangementRule;
import cz.tacr.elza.domain.RulComponent;
import cz.tacr.elza.domain.RulExtensionRule;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeSpecAssign;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.RulStructureDefinition;
import cz.tacr.elza.domain.RulStructureExtensionDefinition;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.domain.RulStructuredTypeExtension;
import cz.tacr.elza.domain.SysLanguage;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApExternalIdTypeRepository;
import cz.tacr.elza.repository.ApExternalSystemRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.ArrangementExtensionRepository;
import cz.tacr.elza.repository.ArrangementRuleRepository;
import cz.tacr.elza.repository.ComponentRepository;
import cz.tacr.elza.repository.ExtensionRuleRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.ItemTypeSpecAssignRepository;
import cz.tacr.elza.repository.PackageRepository;
import cz.tacr.elza.repository.PartTypeRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.StructureDefinitionRepository;
import cz.tacr.elza.repository.StructureExtensionDefinitionRepository;
import cz.tacr.elza.repository.StructuredTypeExtensionRepository;
import cz.tacr.elza.repository.StructuredTypeRepository;
import cz.tacr.elza.repository.SysLanguageRepository;

public class StaticDataProvider {

    /**
     * Výchozí kód typu pártu.
     */
    public static final String DEFAULT_PART_TYPE = "PT_NAME";

    /**
     * Výchozí kód pro část typu tělo.
     */
    public static final String DEFAULT_BODY_PART_TYPE = "PT_BODY";

    private List<RulPackage> packages;

    private List<ApType> apTypes;

    private List<StructType> structuredTypes;

    private List<ItemType> itemTypes;

    private List<RuleSet> ruleSets;

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

    private Map<Integer, RuleSet> ruleSetIdMap;

    private Map<String, RuleSet> ruleSetCodeMap;

    private Map<Integer, RulItemSpec> itemSpecIdMap;

    private Map<String, RulItemSpec> itemSpecCodeMap;

    private Map<Integer, ApExternalIdType> apEidTypeIdMap;

    private Map<String, ApExternalIdType> apEidTypeCodeMap;

    private Map<Integer, SysLanguage> sysLanguageIdMap;

    private Map<String, SysLanguage> sysLanguageCodeMap;

    private Map<Integer, ApTypeRoles> apTypeRolesIdMap;

    private Map<String, ApExternalSystem> apExternalSystemCodeMap = new HashMap<>();

    private Map<Integer, ApExternalSystem> apExternalSystemIdMap = new HashMap<>();

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

    public List<RuleSet> getRuleSets() {
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
        Validate.notNull(id, "Parameter is null");
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

    public RulPartType getDefaultPartType() {
        RulPartType partType = partTypeCodeMap.get(DEFAULT_PART_TYPE);
        Validate.notNull(partType);
        return partType;
    }

    public RulPartType getDefaultBodyPartType() {
        RulPartType partType = partTypeCodeMap.get(DEFAULT_BODY_PART_TYPE);
        Validate.notNull(partType);
        return partType;
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

    public RuleSet getRuleSetById(Integer id) {
        Validate.notNull(id);
        return ruleSetIdMap.get(id);
    }

    public RuleSet getRuleSetByCode(String code) {
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

    public ApExternalSystem getApExternalSystemById(Integer id) {
        Validate.notNull(id);
        return apExternalSystemIdMap.get(id);
    }

    public RulItemType getItemType(String code) {
        ItemType type = getItemTypeByCode(code);
        if (type == null) {
            throw new ObjectNotFoundException("Nebyl dohledán typ atributu podle kódu: " + code, BaseCode.ID_NOT_EXIST)
                    .setId(code);
        }
        return type.getEntity();
    }

    public RulItemType getItemType(Integer id) {
        ItemType type = getItemTypeById(id);
        if (type == null) {
            throw new ObjectNotFoundException("Nebyl dohledán typ atributu podle id: " + id, BaseCode.ID_NOT_EXIST)
                    .setId(id);
        }
        return type.getEntity();
    }

    public RulItemSpec getItemSpec(String code) {
        RulItemSpec spec = getItemSpecByCode(code);
        if (spec == null) {
            throw new ObjectNotFoundException("Nebyla dohledána specifikace atributu podle kódu: " + code, BaseCode.ID_NOT_EXIST)
                    .setId(code);
        }
        return spec;
    }

    public RulItemSpec getItemSpec(Integer id) {
        RulItemSpec spec = getItemSpecById(id);
        if (spec == null) {
            throw new ObjectNotFoundException("Nebyla dohledána specifikace atributu podle id: " + id, BaseCode.ID_NOT_EXIST)
                    .setId(id);
        }
        return spec;
    }

    /* initialization methods */

    /**
     * Initialize all static values. Caller must must ensure synchronized. Method
     * needs to be called in active transaction.
     */
    void init(StaticDataService service) {
        initRuleSets(service.ruleSetRepository,
                     service.arrangementRuleRepository,
                     service.ruleSetExtRepository,
                     service.extensionRuleRepository,
                     service.componentRepository);
        initStructuredTypes(service.structuredTypeRepository,
                            service.structureDefinitionRepository,
                            service.structuredTypeExtensionRepository,
                            service.structureExtensionDefinitionRepository);
        initItemTypes( service.itemTypeRepository, service.itemSpecRepository, service.itemTypeSpecAssignRepository);
        initPackages(service.packageRepository);
        initApTypes(service.apTypeRepository);
        initApEidTypes(service.apEidTypeRepository);
        initSysLanguages(service.sysLanguageRepository);
        initPartTypes(service.partTypeRepository);
        initApExternalSystems(service.apExternalSystemRepository);
        self = this;
    }

    private void initRuleSets(RuleSetRepository ruleSetRepository,
                              ArrangementRuleRepository arrangementRuleRepository,
                              ArrangementExtensionRepository extRepository,
                              ExtensionRuleRepository extensionRuleRepository,
                              ComponentRepository componentRepository) {
        // find all components 
        //  - this allows to initialize all rules using components
        List<RulComponent> components = componentRepository.findAll();

        List<RulRuleSet> dbRuleSets = ruleSetRepository.findAll();
        // read extensions from db
        List<RulArrangementExtension> dbRuleSetExts = extRepository.findAll();
        Map<Integer, List<RulArrangementExtension>> ruleSetExtsById = dbRuleSetExts
                .stream().collect(Collectors.groupingBy(RulArrangementExtension::getRuleSetId));

        List<RulExtensionRule> dbExtRules = extensionRuleRepository.findAllFetchOrderByPriority();
        Map<Integer, List<RulExtensionRule>> extRulesByExtId = dbExtRules.stream()
                .collect(Collectors.groupingBy(RulExtensionRule::getArrangementExtensionId));

        List<RulArrangementRule> dbRules = arrangementRuleRepository.findAll();
        Map<Integer, List<RulArrangementRule>> dbRulesByRulesetId = dbRules.stream()
                .collect(Collectors.groupingBy(RulArrangementRule::getRuleSetId));

        List<RuleSet> ruleSets = dbRuleSets.stream()
                .map(rs -> {
                    List<RulArrangementRule> dbRulesPerSet = dbRulesByRulesetId.getOrDefault(rs.getRuleSetId(),
                                                                                             Collections.emptyList());
                    List<RulArrangementExtension> exts = ruleSetExtsById.getOrDefault(rs.getRuleSetId(), Collections
                            .emptyList());
                    return new RuleSet(rs, dbRulesPerSet, exts, extRulesByExtId, dbExtRules);
                })
                .collect(Collectors.toList());
        this.ruleSets = Collections.unmodifiableList(ruleSets);
        this.ruleSetIdMap = createLookup(ruleSets, RuleSet::getRuleSetId);
        this.ruleSetCodeMap = createLookup(ruleSets, RuleSet::getCode);
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
                                     StructureDefinitionRepository structureDefinitionRepository,
                                     StructuredTypeExtensionRepository structuredTypeExtensionRepository,
                                     StructureExtensionDefinitionRepository structureExtensionDefinitionRepository) {
        List<RulStructuredType> structuredTypes = structuredTypeRepository.findAll();

        ArrayList<StructType> structTypes = new ArrayList<>(structuredTypes.size());

        // Prepare structured types
        structuredTypes.forEach(st -> {
            // read attrs definition
            List<RulStructureDefinition> defs = structureDefinitionRepository.findByStructTypeOrderByPriority(st);
            List<RulStructuredTypeExtension> exts = structuredTypeExtensionRepository.findByStructureType(st);
            List<RulStructureExtensionDefinition> extDefs = structureExtensionDefinitionRepository
                    .findByStructureTypeAndDefTypeOrderByPriority(st);

            StructType structType = new StructType(st, defs, exts, extDefs);
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
                apExternalSystemIdMap.put(apExternalSystem.getExternalSystemId(), apExternalSystem);
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
            // note: We have to use getParentApType instead of getParentApTypeId.
            //       parent ID might be null if apType is saved in this transaction
            ApType origParent = rt.getParentApType();
            if (origParent != null) {
                ApType parent = idMap.get(origParent.getApTypeId());
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
