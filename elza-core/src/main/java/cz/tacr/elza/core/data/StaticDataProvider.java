package cz.tacr.elza.core.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import cz.tacr.elza.domain.*;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.repository.ComplementTypeRepository;
import cz.tacr.elza.repository.PackageRepository;
import cz.tacr.elza.repository.PartyNameFormTypeRepository;
import cz.tacr.elza.repository.PartyTypeComplementTypeRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.RelationTypeRepository;
import cz.tacr.elza.repository.RelationTypeRoleTypeRepository;

public class StaticDataProvider {

    private final RuleSystemProvider ruleSystemProvider = new RuleSystemProvider();

    private List<RulPackage> packages;

    private List<ParPartyNameFormType> partyNameFormTypes;

    private List<ParComplementType> complementTypes;

    private List<ApType> apTypes;

    private List<RelationType> relationTypes;

    private Map<Integer, RulPackage> packageIdMap;

    private Map<Integer, ParPartyNameFormType> partyNameFormTypeIdMap;

    private Map<String, ParPartyNameFormType> partyNameFormTypeCodeMap;

    private Map<String, ParComplementType> complementTypeCodeMap;

    private Map<String, PartyTypeComplementTypes> partyTypeComplementTypesCodeMap;

    private Map<Integer, ApType> apTypeIdMap;

    private Map<String, ApType> apTypeCodeMap;

    private Map<Integer, RelationTypeImpl> relationTypeIdMap;

    private Map<String, RelationTypeImpl> relationTypeCodeMap;

    StaticDataProvider() {
    }

    public RuleSystemProvider getRuleSystems() {
        return ruleSystemProvider;
    }

    public List<RulPackage> getPackages() {
        return packages;
    }

    public List<ParPartyNameFormType> getPartyNameFormTypes() {
        return partyNameFormTypes;
    }

    public List<ParComplementType> getComplementTypes() {
        return complementTypes;
    }

    public List<ApType> getApTypes() {
        return apTypes;
    }

    public List<RelationType> getRelationTypes() {
        return relationTypes;
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

    public ParComplementType getComplementTypeByCode(String code) {
        Validate.notEmpty(code);
        return complementTypeCodeMap.get(code);
    }

    public PartyTypeComplementTypes getComplementTypesByPartyTypeCode(String code) {
        Validate.notEmpty(code);
        return partyTypeComplementTypesCodeMap.get(code);
    }

    /**
     * Returns fully initialized register type by id.
     * This object and all his referenced entities are detached.
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

    /**
     * Init all values. Method must be called inside transaction and synchronized.
     */
    void init(StaticDataService service) {
        ruleSystemProvider.init(service.ruleSetRepository, service.itemTypeRepository,
                service.itemSpecRepository, service.structuredTypeRepository);
        initPackages(service.packageRepository);
        initPartyNameFormTypes(service.partyNameFormTypeRepository);
        initComplementTypes(service.complementTypeRepository, service.partyTypeComplementTypeRepository);
        initApTypes(service.apTypeRepository);
        initRelationTypes(service.relationTypeRepository, service.relationTypeRoleTypeRepository);
    }

    private void initPackages(PackageRepository packageRepository) {
        List<RulPackage> packages = packageRepository.findAll();

        // update fields
        this.packages = Collections.unmodifiableList(packages);
        this.packageIdMap = createLookup(packages, RulPackage::getPackageId);
    }

    private void checkPackageReference(RulPackage rulPackage) {
        Validate.isTrue(rulPackage == packageIdMap.get(rulPackage.getPackageId()));
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

        Map<String, ParComplementType> codeMap = new HashMap<>(cmplTypes.size());
        for (ParComplementType cmplType : cmplTypes) {
            checkPackageReference(cmplType.getRulPackage());
            codeMap.put(cmplType.getCode(), cmplType);
        }

        // create initialized complement type groups
        Map<String, PartyTypeComplementTypes> partyTypeComplementTypesCodeMap = new HashMap<>();
        for (PartyType pt : PartyType.values()) {
            PartyTypeComplementTypes group = new PartyTypeComplementTypes(pt);
            group.init(codeMap, partyTypeComplementTypeRepository);
            partyTypeComplementTypesCodeMap.put(pt.getCode(), group);
        }

        // update fields
        this.complementTypes = Collections.unmodifiableList(cmplTypes);
        this.complementTypeCodeMap = codeMap;
        this.partyTypeComplementTypesCodeMap = partyTypeComplementTypesCodeMap;
    }

    private void initApTypes(ApTypeRepository apTypeRepository) {
        List<ApType> apTypes = apTypeRepository.findAll();

        Map<Integer, ApType> idMap = createLookup(apTypes, ApType::getApTypeId);

        for (ApType rt : apTypes) {
            // ensure reference equality (single transaction)
            if (rt.getParentApType() != null) {
                checkPackageReference(rt.getRulPackage());
                Validate.isTrue(rt.getParentApType() == idMap.get(rt.getParentApType().getApTypeId()));
            }
            // update reference to party type
            if (rt.getPartyType() != null) {
                PartyType partyType = PartyType.fromId(rt.getPartyType().getPartyTypeId());
                rt.setPartyType(partyType.getEntity());
            }
        }
        // update fields
        this.apTypes = Collections.unmodifiableList(apTypes);
        this.apTypeIdMap = idMap;
        this.apTypeCodeMap = createLookup(apTypes, ApType::getCode);
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

    public static <K, V> Map<K, V> createLookup(Collection<V> values, Function<V, K> keyMapping) {
        Map<K, V> lookup = new HashMap<>(values.size());
        values.forEach(is -> lookup.put(keyMapping.apply(is), is));
        return lookup;
    }
}
