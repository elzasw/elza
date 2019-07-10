package cz.tacr.elza.packageimport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApRuleSystem;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.PackageCode;
import cz.tacr.elza.packageimport.xml.APTypeXml;
import cz.tacr.elza.packageimport.xml.APTypes;
import cz.tacr.elza.packageimport.xml.common.OtherCode;
import cz.tacr.elza.packageimport.xml.common.OtherCodes;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.RegistryRoleRepository;

/**
 * Update AP types
 */
public class APTypeUpdater {

    public static final String AP_TYPE_XML = "ap_type.xml";

    final private ApAccessPointRepository accessPointRepository;

    final private ApStateRepository apStateRepository;

    final private ApTypeRepository apTypeRepository;

    final private RegistryRoleRepository registryRoleRepository;

    private APTypes apXmlTypes = null;

    /**
     * Final list of apTypes
     */
    private List<ApType> apTypes = new ArrayList<>();

    private Map<String, ApType> apTypesMap = new HashMap<>();

    private final List<ParPartyType> parPartyTypes;

    private List<ApRuleSystem> apRuleSystems;

    private StaticDataProvider staticDataProvider;

    public APTypeUpdater(final ApStateRepository apStateRepository,
                         final ApTypeRepository apTypeRepository,
                         final RegistryRoleRepository registryRoleRepository,
                         final ApAccessPointRepository accessPointRepository,
                         final List<ParPartyType> parPartyTypes,
                         final List<ApRuleSystem> apRuleSystems,
                         final StaticDataProvider staticDataProvider) {
        this.apStateRepository = apStateRepository;
        this.apTypeRepository = apTypeRepository;
        this.registryRoleRepository = registryRoleRepository;
        this.accessPointRepository = accessPointRepository;
        this.parPartyTypes = parPartyTypes;
        this.apRuleSystems = apRuleSystems;
        this.staticDataProvider = staticDataProvider;
    }

    private void addApType(ApType apType) {
        apTypes.add(apType);
        apTypesMap.put(apType.getCode(), apType);
    }

    ApType getApTypeByCode(String code) {
        return apTypesMap.get(code);
    }

    /**
     * Konverze VO -> DO.
     *
     * @param rulPackage    balíček
     * @param apTypeXml     vztah typů tříd - VO
     * @param apType        vztah typů tříd - DO
     * @param parPartyTypes seznam typů osob
     */
    private void convertToApType(final RulPackage rulPackage,
                                 final APTypeXml apTypeXml,
                                 final ApType apType) {
        apType.setRulPackage(rulPackage);
        apType.setCode(apTypeXml.getCode());
        apType.setName(apTypeXml.getName());
        apType.setReadOnly(apTypeXml.isReadOnly());

        // read rules
        if (StringUtils.isNotEmpty(apTypeXml.getRuleSystem())) {
            ApRuleSystem apRuleSystem = PackageService.findEntity(apRuleSystems, apTypeXml.getRuleSystem(),
                    ApRuleSystem::getCode);
            if (apRuleSystem == null) {
                throw new ObjectNotFoundException("Cannot find rules for access point type", BaseCode.ID_NOT_EXIST)
                        .setId(apTypeXml.getRuleSystem());
            }
            apType.setRuleSystem(apRuleSystem);
        } else {
            apType.setRuleSystem(null);
        }

        // prepare parent ApType
        ApType parent = null;
        String parentCode = apTypeXml.getParentType();
        if (parentCode != null) {
            parent = this.getApTypeByCode(parentCode);
            if (parent == null) {
                // parent is not in source xml
                // try to find it in static data (from another package)
                parent = staticDataProvider.getApTypeByCode(parentCode);
                // check that not this package
                if (parent != null) {
                    String parentPkgCode = parent.getRulPackage().getCode();
                    String updatePkgCode = rulPackage.getCode();
                    if (parentPkgCode.equals(updatePkgCode)) {
                        throw new BusinessException("Parent ApType not found in new package.",
                                PackageCode.CODE_NOT_FOUND)
                                .set("code", apTypeXml.getCode())
                                .set("parentCode", parentCode)
                                .set("file", AP_TYPE_XML);
                    }
                }
            }

            if (parent == null) {
                throw new BusinessException("Parent ApType not found.",
                        PackageCode.CODE_NOT_FOUND)
                        .set("code", apTypeXml.getCode())
                        .set("parentCode", parentCode)
                        .set("file", AP_TYPE_XML);
            }
        }
        apType.setParentApType(parent);

        // set party type
        if (apTypeXml.getPartyType() != null) {
            // check party type - if exists
            ParPartyType parPartyType = PackageService.findEntity(parPartyTypes, apTypeXml.getPartyType(),
                    ParPartyType::getCode);
            if (parPartyType == null) {
                throw new BusinessException("ParPartyType s code=" + apTypeXml.getPartyType() + " nenalezen",
                        PackageCode.CODE_NOT_FOUND).set("code", apTypeXml.getPartyType()).set("file",
                        AP_TYPE_XML);
            }
            apType.setPartyType(parPartyType);
        }
    }

    /**
     * Zpracování vztahy typu třídy.
     *
     * @param registerTypes vztahy typů tříd
     * @param rulPackage    balíček
     * @param parPartyTypes seznam typů osob
     */
    private void processApTypes(
            @NotNull final RulPackage rulPackage,
            @NotNull final List<ParPartyType> parPartyTypes) {
        // TODO: nacitani AP type musi byt serazeno podle urovni (recursive query) aby mohl byt zbytek
        // (nezaktualizovane typy) odstranen hierarchicky (linked hash map uchova poradi)
        Map<String, ApType> oldTypeCodeMap = apTypeRepository.findByRulPackage(rulPackage)
                .stream().collect(Collectors.toMap(
                        ApType::getCode,
                        (input) -> {
                            return HibernateUtils.unproxy(input);
                        },
                        (v1, v2) -> {
                            throw new SystemException(
                                    "Duplicate AP code, value=" + v1.getCode(),
                                    BaseCode.DB_INTEGRITY_PROBLEM);
                        },
                        LinkedHashMap::new));

        Map<ApType, ApType> mapTypes = new HashMap<>();

        if (apXmlTypes != null && CollectionUtils.isNotEmpty(apXmlTypes.getRegisterTypes())) {
            for (APTypeXml apXmlType : apXmlTypes.getRegisterTypes()) {
                ApType type = oldTypeCodeMap.remove(apXmlType.getCode());
                if (type == null) {
                    type = new ApType();
                }
                convertToApType(rulPackage, apXmlType, type);
                addApType(type);

                // check if old types still exists
                OtherCodes otherCodes = apXmlType.getOtherCodes();
                if (otherCodes != null && otherCodes.getOtherCodes() != null) {
                    for (OtherCode otherCode : otherCodes.getOtherCodes()) {
                        // try to get from old codes
                        ApType otherType = oldTypeCodeMap.get(otherCode.getCode());
                        if (otherType != null) {
                            mapTypes.put(otherType, type);
                        }
                    }
                }
            }
        }

        // save new types
        apTypeRepository.save(this.apTypes);

        // map old types to new types
        for (Entry<ApType, ApType> mapType : mapTypes.entrySet()) {
            // todo[ap_state]
            accessPointRepository.updateApTypeByApType(mapType.getKey(), mapType.getValue());
            apStateRepository.updateApTypeByApType(mapType.getKey(), mapType.getValue());
        }

        // drop old types
        Collection<ApType> oldTypes = oldTypeCodeMap.values();
        oldTypes.forEach(registryRoleRepository::deleteByApType);

        apTypeRepository.delete(oldTypes);
    }

    public void run(PackageContext pkgCtx) {
        this.apXmlTypes = pkgCtx.convertXmlStreamToObject(APTypes.class,
                AP_TYPE_XML);

        processApTypes(pkgCtx.getPackage(), parPartyTypes);

    }

    public List<ApType> getApTypes() {
        return this.apTypes;
    }
}
