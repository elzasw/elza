package cz.tacr.elza.packageimport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.RulApTypeDeclaration;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.PackageCode;
import cz.tacr.elza.packageimport.xml.APTypeXml;
import cz.tacr.elza.packageimport.xml.APTypes;
import cz.tacr.elza.packageimport.xml.common.OtherCode;
import cz.tacr.elza.packageimport.xml.common.OtherCodes;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.ApTypeDeclarationRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.EntityRuleRepository;
import cz.tacr.elza.repository.RuleSetApTypeRepository;

/**
 * Update AP types
 */
public class APTypeUpdater {

    public static final String AP_TYPE_XML = "ap_type.xml";

    final private ApAccessPointRepository accessPointRepository;

    final private ApStateRepository apStateRepository;

    final private ApTypeRepository apTypeRepository;

    final private EntityRuleRepository entityRuleRepository;

    final private RuleSetApTypeRepository ruleSetApTypeRepository;

    final private ApTypeDeclarationRepository declarationRepository;

    final private PackageDeclarations summary;

    private APTypes apXmlTypes = null;

    /**
     * Final list of apTypes
     */
    private List<ApType> apTypes = new ArrayList<>();

    private Map<String, ApType> apTypesMap = new HashMap<>();

    private StaticDataProvider staticDataProvider;

    public APTypeUpdater(final ApStateRepository apStateRepository,
                         final ApTypeRepository apTypeRepository,
                         final ApAccessPointRepository accessPointRepository,
                         final EntityRuleRepository entityRuleRepository,
                         final RuleSetApTypeRepository ruleSetApTypeRepository,
                         final ApTypeDeclarationRepository declarationRepository,
                         final PackageDeclarations summary,
                         final StaticDataProvider staticDataProvider) {
        this.apStateRepository = apStateRepository;
        this.apTypeRepository = apTypeRepository;
        this.entityRuleRepository = entityRuleRepository;
        this.ruleSetApTypeRepository = ruleSetApTypeRepository;
        this.declarationRepository = declarationRepository;
        this.summary = summary;
        this.accessPointRepository = accessPointRepository;
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
     * Parent of a declared class: a class of the same file, or an existing class that stays - declared
     * by another package.
     */
    private ApType resolveParent(final RulPackage rulPackage, final APTypeXml apTypeXml,
                                 final Map<String, ApType> existing) {
        String parentCode = apTypeXml.getParentType();
        if (parentCode == null) {
            return null;
        }
        ApType parent = getApTypeByCode(parentCode);
        if (parent == null) {
            parent = existing.get(parentCode);
            if (parent != null && !declaredByOther(parent, rulPackage)) {
                // the parent was declared only by this package and is no longer in the file
                parent = null;
            }
        }
        if (parent == null) {
            throw new BusinessException("Parent ApType not found.", PackageCode.CODE_NOT_FOUND)
                    .set("code", apTypeXml.getCode())
                    .set("parentCode", parentCode)
                    .set("file", AP_TYPE_XML);
        }
        return parent;
    }

    private boolean declaredByOther(final ApType apType, final RulPackage rulPackage) {
        if (apType.getApTypeId() == null) {
            return false;
        }
        return declarationRepository.findByApTypes(List.of(apType)).stream()
                .anyMatch(d -> !d.getPackageId().equals(rulPackage.getPackageId()));
    }

    /**
     * Declarations of the package: a class declared by another package too keeps its parent (a
     * different one refuses the import), a class no longer declared by any package is removed; owner,
     * name and read-only of the classes are summarized from their declarations.
     *
     * @param rulPackage    balíček
     */
    private void processApTypes(
            @NotNull final RulPackage rulPackage) {
        Map<String, ApType> existing = apTypeRepository.findAll().stream()
                .map(t -> (ApType) HibernateUtils.unproxy(t))
                .collect(Collectors.toMap(ApType::getCode, t -> t));
        // TODO: nacitani AP type musi byt serazeno podle urovni (recursive query) aby mohl byt zbytek
        // (nezaktualizovane typy) odstranen hierarchicky (linked hash map uchova poradi)
        Map<String, RulApTypeDeclaration> oldDeclarations = declarationRepository.findByRulPackage(rulPackage)
                .stream().collect(Collectors.toMap(
                        d -> d.getApType().getCode(),
                        d -> d,
                        (v1, v2) -> {
                            throw new SystemException(
                                    "Duplicate AP code, value=" + v1.getApType().getCode(),
                                    BaseCode.DB_INTEGRITY_PROBLEM);
                        },
                        LinkedHashMap::new));

        Map<ApType, ApType> mapTypes = new HashMap<>();
        Map<String, APTypeXml> xmlByCode = new LinkedHashMap<>();

        if (apXmlTypes != null && CollectionUtils.isNotEmpty(apXmlTypes.getRegisterTypes())) {
            for (APTypeXml apXmlType : apXmlTypes.getRegisterTypes()) {
                xmlByCode.put(apXmlType.getCode(), apXmlType);
                ApType parent = resolveParent(rulPackage, apXmlType, existing);
                ApType type = existing.get(apXmlType.getCode());
                if (type == null) {
                    type = new ApType();
                    type.setCode(apXmlType.getCode());
                    type.setRulPackage(rulPackage);
                    type.setName(apXmlType.getName());
                    type.setReadOnly(apXmlType.isReadOnly());
                    type.setParentApType(parent);
                } else if (declaredByOther(type, rulPackage)) {
                    String parentCode = type.getParentApType() != null ? type.getParentApType().getCode() : null;
                    if (!Objects.equals(parentCode, apXmlType.getParentType())) {
                        throw new BusinessException("Entity class declared with another parent by another package",
                                PackageCode.AP_TYPE_CONFLICT)
                                .set("code", apXmlType.getCode())
                                .set("parentCode", apXmlType.getParentType())
                                .set("otherParentCode", parentCode)
                                .set("file", AP_TYPE_XML);
                    }
                } else {
                    type.setParentApType(parent);
                }
                addApType(type);

                // check if old types still exists
                OtherCodes otherCodes = apXmlType.getOtherCodes();
                if (otherCodes != null && otherCodes.getOtherCodes() != null) {
                    for (OtherCode otherCode : otherCodes.getOtherCodes()) {
                        // try to get from old codes
                        RulApTypeDeclaration other = oldDeclarations.get(otherCode.getCode());
                        if (other != null) {
                            mapTypes.put(other.getApType(), type);
                        }
                    }
                }
            }
        }

        // save new types and the declarations of the package
        apTypeRepository.saveAll(this.apTypes);
        List<RulApTypeDeclaration> declarations = new ArrayList<>();
        for (ApType type : this.apTypes) {
            APTypeXml apXmlType = xmlByCode.get(type.getCode());
            RulApTypeDeclaration declaration = oldDeclarations.remove(type.getCode());
            if (declaration == null) {
                declaration = new RulApTypeDeclaration();
            }
            declaration.setApType(type);
            declaration.setRulPackage(rulPackage);
            declaration.setName(apXmlType.getName());
            declaration.setParentApType(type.getParentApType());
            declaration.setReadOnly(apXmlType.isReadOnly());
            declarations.add(declaration);
        }
        declarationRepository.saveAll(declarations);

        // map old types to new types
        for (Entry<ApType, ApType> mapType : mapTypes.entrySet()) {
            apStateRepository.updateApTypeByApType(mapType.getKey(), mapType.getValue());
        }

        // declarations the package no longer has; a class without declarations is removed
        Collection<RulApTypeDeclaration> removed = oldDeclarations.values();
        declarationRepository.deleteAll(removed);
        declarationRepository.flush();
        List<ApType> oldTypes = new ArrayList<>();
        List<ApType> summarize = new ArrayList<>(this.apTypes);
        for (RulApTypeDeclaration declaration : removed) {
            ApType type = declaration.getApType();
            if (declarationRepository.findByApTypes(List.of(type)).isEmpty()) {
                oldTypes.add(type);
            } else {
                summarize.add(type);
            }
        }
       // TODO: smazáno - odstranění starých typů - oldTypes.forEach(registryRoleRepository::deleteByApType);

        if (!oldTypes.isEmpty()) {
            List<RulPackage> foreign = new ArrayList<>();
            entityRuleRepository.findForeignByApTypes(oldTypes, rulPackage)
                    .forEach(r -> foreign.add(r.getRulPackage()));
            ruleSetApTypeRepository.findForeignByApTypes(oldTypes, rulPackage)
                    .forEach(m -> foreign.add(m.getRulPackage()));
            PackageService.checkNoForeignEntityRules(foreign);
        }
        apTypeRepository.deleteAll(oldTypes);

        for (ApType type : summarize) {
            summary.summarize(type, declarationRepository.findByApTypes(List.of(type)));
        }
        apTypeRepository.saveAll(summarize);
    }

    public void run(PackageContext pkgCtx) {
        this.apXmlTypes = pkgCtx.convertXmlStreamToObject(APTypes.class,
                AP_TYPE_XML);

        processApTypes(pkgCtx.getPackage());

    }

    public List<ApType> getApTypes() {
        return this.apTypes;
    }
}
