package cz.tacr.elza.core.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.domain.ApExternalIdType;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ParComplementType;
import cz.tacr.elza.domain.ParPartyNameFormType;
import cz.tacr.elza.domain.ParRegistryRole;
import cz.tacr.elza.domain.ParRelationType;
import cz.tacr.elza.domain.ParRelationTypeRoleType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.domain.SysLanguage;
import cz.tacr.elza.repository.ApExternalIdTypeRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.ComplementTypeRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.PackageRepository;
import cz.tacr.elza.repository.PartyNameFormTypeRepository;
import cz.tacr.elza.repository.PartyTypeComplementTypeRepository;
import cz.tacr.elza.repository.RegistryRoleRepository;
import cz.tacr.elza.repository.RelationTypeRepository;
import cz.tacr.elza.repository.RelationTypeRoleTypeRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.StructuredTypeRepository;
import cz.tacr.elza.repository.SysLanguageRepository;

public class StaticDataProvider {

    private List<RulPackage> packages;

    private List<ParPartyNameFormType> partyNameFormTypes;

    private List<ParComplementType> cmplTypes;

    private List<ApType> apTypes;

    private List<RelationType> relationTypes;

    private List<RulStructuredType> structuredTypes;

    private List<ItemType> itemTypes;
    
    private List<RulRuleSet> ruleSets;

    private List<RulItemSpec> itemSpecs;

    private List<ApExternalIdType> apEidTypes;

    private List<SysLanguage> sysLanguages;

    private Map<Integer, RulPackage> packageIdMap;

    private Map<Integer, ParPartyNameFormType> partyNameFormTypeIdMap;

    private Map<String, ParPartyNameFormType> partyNameFormTypeCodeMap;

    private Map<Integer, ParComplementType> cmplTypeIdMap;

    private Map<String, ParComplementType> cmplTypeCodeMap;

    private Map<String, PartyTypeCmplTypes> partyTypeCmplTypesCodeMap;

    private Map<Integer, ApType> apTypeIdMap;

    private Map<String, ApType> apTypeCodeMap;

    private Map<Integer, RelationTypeImpl> relationTypeIdMap;

    private Map<String, RelationTypeImpl> relationTypeCodeMap;

    private Map<Integer, RulStructuredType> structuredTypeIdMap;

    private Map<String, RulStructuredType> structuredTypeCodeMap;

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

    StaticDataProvider() {
    }

    public List<RulPackage> getPackages() {
        return packages;
    }

    public List<ParPartyNameFormType> getPartyNameFormTypes() {
        return partyNameFormTypes;
    }

    public List<ParComplementType> getCmplTypes() {
        return cmplTypes;
    }

    public List<ApType> getApTypes() {
        return apTypes;
    }

    public List<RelationType> getRelationTypes() {
        return relationTypes;
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

    public ParPartyNameFormType getPartyNameFormTypeById(Integer id) {
        Validate.notNull(id);
        return partyNameFormTypeIdMap.get(id);
    }

    public ParPartyNameFormType getPartyNameFormTypeByCode(String code) {
        Validate.notEmpty(code);
        return partyNameFormTypeCodeMap.get(code);
    }

    public ParComplementType getCmplTypeById(Integer id) {
        Validate.notNull(id);
        return cmplTypeIdMap.get(id);
    }

    public ParComplementType getCmplTypeByCode(String code) {
        Validate.notEmpty(code);
        return cmplTypeCodeMap.get(code);
    }

    public PartyTypeCmplTypes getCmplTypesByPartyTypeCode(String code) {
        Validate.notEmpty(code);
        return partyTypeCmplTypesCodeMap.get(code);
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

    public RelationType getRelationTypeById(Integer id) {
        Validate.notNull(id);
        return relationTypeIdMap.get(id);
    }

    public RelationType getRelationTypeByCode(String code) {
        Validate.notEmpty(code);
        return relationTypeCodeMap.get(code);
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
    
    public List<RulStructuredType> getStructuredTypes() {
        return structuredTypes;
    }

    public RulStructuredType getStructuredTypeById(Integer id) {
        Validate.notNull(id);
        return structuredTypeIdMap.get(id);
    }

    public RulStructuredType getStructuredTypeByCode(String code) {
        Validate.notEmpty(code);
        return structuredTypeCodeMap.get(code);
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
    
    /* initialization methods */

    /**
     * Initialize all static values. Caller must must ensure synchronized. Method
     * needs to be called in active transaction.
     */
    void init(StaticDataService service) {
        initRuleSets(service.ruleSetRepository);
        initStructuredTypes(service.structuredTypeRepository);
        initItemTypes( service.itemTypeRepository, service.itemSpecRepository);
        initPackages(service.packageRepository);
        initPartyNameFormTypes(service.partyNameFormTypeRepository);
        initComplementTypes(service.complementTypeRepository, service.partyTypeComplementTypeRepository);
        initApTypes(service.apTypeRepository);
        initRelationTypes(service.relationTypeRepository, service.relationTypeRoleTypeRepository);
        initApEidTypes(service.apEidTypeRepository);
        initSysLanguages(service.sysLanguageRepository);
        initApTypeRoles(service.registryRoleRepository);
    }

    private void initRuleSets(RuleSetRepository ruleSetRepository) {
        List<RulRuleSet> ruleSets = ruleSetRepository.findAll();

        this.ruleSets = Collections.unmodifiableList(ruleSets);
        this.ruleSetIdMap = createLookup(ruleSets, RulRuleSet::getRuleSetId);
        this.ruleSetCodeMap = createLookup(ruleSets, RulRuleSet::getCode);
    }

    private void initItemTypes(ItemTypeRepository itemTypeRepository, ItemSpecRepository itemSpecRepository) {
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

            // prepare item spec
            initItemSpecs(rsit, itemSpecRepository);

            rsit.sealUp();
            this.itemTypes.add(rsit);

            itemTypeIdMap.put(it.getItemTypeId(), rsit);
        }
        this.itemTypeCodeMap = StaticDataProvider.createLookup(itemTypeIdMap.values(), ItemType::getCode);
        this.itemSpecCodeMap = StaticDataProvider.createLookup(itemSpecIdMap.values(), RulItemSpec::getCode);
    }

    private void initItemSpecs(ItemType rsit, ItemSpecRepository itemSpecRepository) {
        if (!rsit.hasSpecifications()) {
            return;
        }

        RulItemType itemType = rsit.getEntity();
        List<RulItemSpec> itemSpecs = itemSpecRepository.findByItemType(itemType);
        for (RulItemSpec is : itemSpecs) {
            // check if initialized in same transaction
            RulItemType itemTypeFromSpec = HibernateUtils.unproxy(is.getItemType());
            if (itemType != itemTypeFromSpec) {
                Validate.isTrue(false, "Inconsistency between itemType ({}) and itemType from specification ({})",
                                itemType, itemTypeFromSpec);
            }

            rsit.addItemSpec(is);
            this.itemSpecs.add(is);
            itemSpecIdMap.put(is.getItemSpecId(), is);
        }
    }

    private void initStructuredTypes(StructuredTypeRepository structuredTypeRepository) {
        List<RulStructuredType> structuredTypes = structuredTypeRepository.findAll();

        this.structuredTypes = Collections.unmodifiableList(structuredTypes);
        this.structuredTypeIdMap = createLookup(structuredTypes, RulStructuredType::getStructuredTypeId);
        this.structuredTypeCodeMap = createLookup(structuredTypes, RulStructuredType::getCode);
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

    private void initPartyNameFormTypes(PartyNameFormTypeRepository partyNameFormTypeRepository) {
        List<ParPartyNameFormType> formTypes = partyNameFormTypeRepository.findAll();

        // update fields
        this.partyNameFormTypes = Collections.unmodifiableList(formTypes);
        this.partyNameFormTypeIdMap = createLookup(formTypes, ParPartyNameFormType::getNameFormTypeId);
        this.partyNameFormTypeCodeMap = createLookup(formTypes, ParPartyNameFormType::getCode);
    }

    private void initComplementTypes(ComplementTypeRepository complementTypeRepository,
                                     PartyTypeComplementTypeRepository partyTypeComplementTypeRepository) {
        List<ParComplementType> cmplTypes = complementTypeRepository.findAll();

        Map<Integer, ParComplementType> idMap = new HashMap<>(cmplTypes.size());
        Map<String, ParComplementType> codeMap = new HashMap<>(cmplTypes.size());
        for (ParComplementType cmplType : cmplTypes) {
            checkPackageReference(cmplType.getRulPackage());
            idMap.put(cmplType.getComplementTypeId(), cmplType);
            codeMap.put(cmplType.getCode(), cmplType);
        }

        // create initialized complement type groups
        Map<String, PartyTypeCmplTypes> partyTypeComplementTypesCodeMap = new HashMap<>();
        for (PartyType pt : PartyType.values()) {
            PartyTypeCmplTypes group = new PartyTypeCmplTypes(pt);
            group.init(codeMap, partyTypeComplementTypeRepository);
            partyTypeComplementTypesCodeMap.put(pt.getCode(), group);
        }

        // update fields
        this.cmplTypes = Collections.unmodifiableList(cmplTypes);
        this.cmplTypeIdMap = idMap;
        this.cmplTypeCodeMap = codeMap;
        this.partyTypeCmplTypesCodeMap = partyTypeComplementTypesCodeMap;
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

            // update reference to party type
            if (rt.getPartyType() != null) {
                PartyType partyType = PartyType.fromId(rt.getPartyType().getPartyTypeId());
                rt.setPartyType(partyType.getEntity());
            }
        }
        // update fields
        this.apTypes = Collections.unmodifiableList(result);
        this.apTypeIdMap = idMap;
        this.apTypeCodeMap = createLookup(result, ApType::getCode);
    }

    private void initRelationTypes(RelationTypeRepository relationTypeRepository,
                                   RelationTypeRoleTypeRepository relationTypeRoleTypeRepository) {
        List<ParRelationType> parTypes = relationTypeRepository.findAllFetchClassType();

        // init all relation types and prepare id map
        Map<Integer, RelationTypeImpl> idMap = new HashMap<>(parTypes.size());
        List<RelationTypeImpl> types = new ArrayList<>(parTypes.size());
        for (ParRelationType parType : parTypes) {
            checkPackageReference(parType.getRulPackage());
            RelationTypeImpl type = new RelationTypeImpl(parType);
            idMap.put(type.getId(), type);
            types.add(type);
        }

        List<ParRelationTypeRoleType> parTypeRoleTypes = relationTypeRoleTypeRepository.findAllFetchRoleType();

        for (ParRelationTypeRoleType parTypeRoleType : parTypeRoleTypes) {
            checkPackageReference(parTypeRoleType.getRulPackage());
            RelationTypeImpl type = idMap.get(parTypeRoleType.getRelationTypeId());
            type.addRoleType(parTypeRoleType.getRoleType());
        }

        types.forEach(t -> t.sealUp());

        // update fields
        this.relationTypes = Collections.unmodifiableList(types);
        this.relationTypeIdMap = idMap;
        this.relationTypeCodeMap = createLookup(types, RelationType::getCode);
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

    private void initApTypeRoles(RegistryRoleRepository registryRoleRepository) {
        List<ParRegistryRole> roles = registryRoleRepository.findAll();
        
        this.apTypeRolesIdMap = new HashMap<>();
        
        for (ParRegistryRole role : roles) {
            ApType type = role.getApType();
            ApTypeRoles typeRoles = apTypeRolesIdMap.get(type.getApTypeId());
            if (typeRoles == null) {    
                typeRoles = new ApTypeRoles(type);
                apTypeRolesIdMap.put(type.getApTypeId(), typeRoles);
            }
            typeRoles.addRole(role);
        }
        
        apTypeRolesIdMap.values().forEach(ApTypeRoles::sealUp);
    }

    public static <K, V> Map<K, V> createLookup(Collection<V> values, Function<V, K> keyMapping) {
        Map<K, V> lookup = new HashMap<>(values.size());
        values.forEach(is -> lookup.put(keyMapping.apply(is), is));
        return lookup;
    }
}
