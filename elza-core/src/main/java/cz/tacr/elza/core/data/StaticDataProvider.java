package cz.tacr.elza.core.data;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ParComplementType;
import cz.tacr.elza.domain.ParPartyNameFormType;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.repository.ComplementTypeRepository;
import cz.tacr.elza.repository.PackageRepository;
import cz.tacr.elza.repository.PartyNameFormTypeRepository;
import cz.tacr.elza.repository.PartyTypeComplementTypeRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;

public class StaticDataProvider {

    private final RuleSystemProvider ruleSystemProvider = new RuleSystemProvider();

    private List<RulPackage> packages;

    private List<ParPartyNameFormType> partyNameFormTypes;

    private List<ParComplementType> complementTypes;

    private List<RegRegisterType> registerTypes;

    private Map<Integer, RulPackage> packageIdMap;

    private Map<String, ParPartyNameFormType> partyNameFormTypeCodeMap;

    private Map<String, ParComplementType> complementTypeCodeMap;

    private Map<String, PartyTypeComplementTypes> partyTypeComplementTypesCodeMap;

    private Map<Integer, RegRegisterType> registerTypeIdMap;

    private Map<String, RegRegisterType> registerTypeCodeMap;

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

    public List<RegRegisterType> getRegisterTypes() {
        return registerTypes;
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
    public RegRegisterType getRegisterTypeById(Integer id) {
        Validate.notNull(id);
        return registerTypeIdMap.get(id);
    }

    public RegRegisterType getRegisterTypeByCode(String code) {
        Validate.notEmpty(code);
        return registerTypeCodeMap.get(code);
    }

    /**
     * Init all values. Method must be called inside transaction and synchronized.
     */
    void init(StaticDataService service) {
        ruleSystemProvider.init(service.ruleSetRepository, service.packetTypeRepository, service.itemTypeRepository,
                service.itemSpecRepository);
        initPackages(service.packageRepository);
        initPartyNameFormTypes(service.partyNameFormTypeRepository);
        initComplementTypes(service.complementTypeRepository, service.partyTypeComplementTypeRepository);
        initRegisterTypes(service.registerTypeRepository);
    }

    private void initPackages(PackageRepository packageRepository) {
        List<RulPackage> packages = packageRepository.findAll();

        // update fields
        this.packages = Collections.unmodifiableList(packages);
        this.packageIdMap = createLookup(packages, RulPackage::getPackageId);
    }

    private void initPartyNameFormTypes(PartyNameFormTypeRepository partyNameFormTypeRepository) {
        List<ParPartyNameFormType> formTypes = partyNameFormTypeRepository.findAll();

        // ensure reference equality
        for (ParPartyNameFormType ft : formTypes) {
            Validate.isTrue(ft.getRulPackage() == packageIdMap.get(ft.getRulPackage().getPackageId()));
        }
        // update fields
        this.partyNameFormTypes = Collections.unmodifiableList(formTypes);
        this.partyNameFormTypeCodeMap = createLookup(formTypes, ParPartyNameFormType::getCode);
    }

    private void initComplementTypes(ComplementTypeRepository complementTypeRepository,
                                     PartyTypeComplementTypeRepository partyTypeComplementTypeRepository) {
        List<ParComplementType> cmplTypes = complementTypeRepository.findAll();

        Map<String, ParComplementType> codeMap = createLookup(cmplTypes, ParComplementType::getCode);

        // create initialized complement type groups
        Map<String, PartyTypeComplementTypes> partyTypeComplementTypesCodeMap = new HashMap<>();
        for (PartyType pt : PartyType.values()) {
            PartyTypeComplementTypes group = new PartyTypeComplementTypes(pt);
            group.init(codeMap, partyTypeComplementTypeRepository);
            partyTypeComplementTypesCodeMap.put(pt.getCode(), group);
        }
        // ensure reference equality
        for (ParComplementType ct : cmplTypes) {
            Validate.isTrue(ct.getRulPackage() == packageIdMap.get(ct.getRulPackage().getPackageId()));
        }
        // update fields
        this.complementTypes = Collections.unmodifiableList(cmplTypes);
        this.complementTypeCodeMap = codeMap;
        this.partyTypeComplementTypesCodeMap = partyTypeComplementTypesCodeMap;
    }

    private void initRegisterTypes(RegisterTypeRepository registerTypeRepository) {
        List<RegRegisterType> regTypes = registerTypeRepository.findAll();

        Map<Integer, RegRegisterType> idMap = createLookup(regTypes, RegRegisterType::getRegisterTypeId);

        for (RegRegisterType rt : regTypes) {
            // ensure reference equality (same init transaction)
            Validate.isTrue(rt.getRulPackage() == packageIdMap.get(rt.getRulPackage().getPackageId()));
            if (rt.getParentRegisterType() != null) {
                Validate.isTrue(rt.getParentRegisterType() == idMap.get(rt.getParentRegisterType().getRegisterTypeId()));
            }
            // update reference to party type
            if (rt.getPartyType() != null) {
                PartyType partyType = PartyType.fromId(rt.getPartyType().getPartyTypeId());
                rt.setPartyType(partyType.getEntity());
            }
        }
        // update fields
        this.registerTypes = Collections.unmodifiableList(regTypes);
        this.registerTypeIdMap = idMap;
        this.registerTypeCodeMap = createLookup(regTypes, RegRegisterType::getCode);
    }

    public static <K, V> Map<K, V> createLookup(Collection<V> values, Function<V, K> keyMapping) {
        Map<K, V> lookup = new HashMap<>(values.size());
        values.forEach(is -> lookup.put(keyMapping.apply(is), is));
        return lookup;
    }
}
