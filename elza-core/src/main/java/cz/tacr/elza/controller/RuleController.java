package cz.tacr.elza.controller;

import cz.tacr.elza.config.ConfigRules;
import cz.tacr.elza.config.rules.GroupConfiguration;
import cz.tacr.elza.config.rules.TypeInfo;
import cz.tacr.elza.config.rules.ViewConfiguration;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.GroupVO;
import cz.tacr.elza.controller.vo.PackageDependencyVO;
import cz.tacr.elza.controller.vo.PackageVO;
import cz.tacr.elza.controller.vo.RulArrangementExtensionVO;
import cz.tacr.elza.controller.vo.RulDataTypeVO;
import cz.tacr.elza.controller.vo.RulPolicyTypeVO;
import cz.tacr.elza.controller.vo.RulRuleSetVO;
import cz.tacr.elza.controller.vo.RulTemplateVO;
import cz.tacr.elza.controller.vo.TypeInfoVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.core.data.RuleSystemProvider;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulArrangementExtension;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPackageDependency;
import cz.tacr.elza.domain.RulPolicyType;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.packageimport.PackageService;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.service.PolicyService;
import cz.tacr.elza.service.RuleService;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Kontroler pro pravidla.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 7. 1. 2016
 */
@RestController
@RequestMapping("/api/rule")
public class RuleController {

    @Autowired
    private ClientFactoryVO factoryVo;

    @Autowired
    private DataTypeRepository dataTypeRepository;

    @Autowired
    private RuleSetRepository ruleSetRepository;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private PackageService packageService;

    @Autowired
    private PolicyService policyService;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private ConfigRules elzaRules;

    @Autowired
    private StaticDataService staticDataService;

    @RequestMapping(value = "/getRuleSets", method = RequestMethod.GET)
    public List<RulRuleSetVO> getRuleSets() {
        return factoryVo.createRuleSetList(ruleSetRepository.findAll());
    }
    @RequestMapping(value = "/dataTypes", method = RequestMethod.GET)
    public List<RulDataTypeVO> getDataTypes() {
        List<RulDataType> dataTypes = dataTypeRepository.findAll();
        return factoryVo.createDataTypeList(dataTypes);
    }

    @RequestMapping(value = "/descItemTypes", method = RequestMethod.GET)
    public List<RulDescItemTypeExtVO> getDescItemTypes() {
        List<RulItemTypeExt> descItemTypes = ruleService.getAllDescriptionItemTypes();
        return factoryVo.createDescItemTypeExtList(descItemTypes);
    }

    @RequestMapping(value = "/templates", method = RequestMethod.GET)
    public List<RulTemplateVO> getTemplates(@RequestParam(value = "code", required = false) final String outputTypeCode) {
        List<RulTemplate> templates = ruleService.getTemplates(outputTypeCode);
        return factoryVo.createTemplates(templates);
    }

    // zatím totožná s getDescItemTypes(), časem se možná změní
    @RequestMapping(value = "/outputItemTypes", method = RequestMethod.GET)
    public List<RulDescItemTypeExtVO> getOutputItemTypes() {
        List<RulItemTypeExt> descItemTypes = ruleService.getAllDescriptionItemTypes();
        return factoryVo.createDescItemTypeExtList(descItemTypes);
    }

    @RequestMapping(value = "/getPackages", method = RequestMethod.GET)
    public List<PackageVO> getPackages() {
        List<RulPackage> packages = packageService.getPackages();
        List<PackageVO> packageVO = factoryVo.createSimpleEntity(packages, PackageVO.class);
        Map<Integer, PackageVO> packageVOMap = packageVO.stream().collect(Collectors.toMap(PackageVO::getPackageId, Function.identity()));
        List<RulPackageDependency> packagesDependencies = packageService.getPackagesDependencies();
        for (RulPackageDependency dependency : packagesDependencies) {
            PackageVO pSource = packageVOMap.get(dependency.getPackageId());
            PackageVO pTarget = packageVOMap.get(dependency.getDependsOnPackageId());
            List<PackageDependencyVO> dependencies = pSource.getDependencies();
            if (dependencies == null) {
                dependencies = new ArrayList<>();
                pSource.setDependencies(dependencies);
            }
            dependencies.add(new PackageDependencyVO(pTarget.getCode(), dependency.getMinVersion()));

            List<PackageDependencyVO> dependenciesBy = pTarget.getDependenciesBy();
            if (dependenciesBy == null) {
                dependenciesBy = new ArrayList<>();
                pTarget.setDependenciesBy(dependenciesBy);
            }
            dependenciesBy.add(new PackageDependencyVO(pSource.getCode(), pSource.getVersion()));
        }
        return packageVO;
    }

    @RequestMapping(value = "/deletePackage/{code}", method = RequestMethod.GET)
    @Transactional
    public void deletePackage(@PathVariable(value = "code") final String code) {
        Assert.notNull(code, "Kód musí být vyplněn");
        packageService.deletePackage(code);
    }

    @RequestMapping(value = "/exportPackage/{code}", method = RequestMethod.GET)
    public void exportPackageRest(@PathVariable(value = "code") final String code,
                                  HttpServletResponse response) {
        Assert.notNull(code, "Kód musí být vyplněn");
        try {
            File file = packageService.exportPackage(code);
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "inline; filename=" + code + "-package.zip");
            response.setContentLength((int) file.length());
            InputStream is = new FileInputStream(file);
            IOUtils.copy(is, response.getOutputStream());
            response.flushBuffer();
        } catch (IOException ex) {
            throw new SystemException("Problem pri zapisu souboru", ex);
        }
    }

    /**
     * Vrací typy oprávnění podle verze fondu.
     *
     * @param fundVersionId identifikátor verze AS
     * @return seznam typů oprávnění
     */
    @RequestMapping(value = "/policy/types/{fundVersionId}", method = RequestMethod.GET)
    public List<RulPolicyTypeVO> getPolicyTypes(@PathVariable(value = "fundVersionId") final Integer fundVersionId) {
        Assert.notNull(fundVersionId, "Verze fondu musí být vyplněna");

        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
        Assert.notNull(fundVersion, "Nebyla nalezena verze fondu s id " + fundVersionId);

        List<RulPolicyType> policyTypes = policyService.getPolicyTypes(fundVersion);
        return factoryVo.createPolicyTypes(policyTypes);
    }

    /**
     * Získání skupin pro AS.
     *
     * @param fundVersionId identifikátor verze AS
     * @return seznam skupin
     */
    @RequestMapping(value = "/groups/{fundVersionId}", method = RequestMethod.GET)
    public List<GroupVO> getGroups(@PathVariable(value = "fundVersionId") final Integer fundVersionId) {
        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
        Integer fundId = fundVersion.getFundId();

        String ruleCode = fundVersion.getRuleSet().getCode();
        ViewConfiguration viewConfig = elzaRules.getViewConfiguration(ruleCode, fundId);

        List<GroupVO> result = new ArrayList<>();
        StaticDataProvider sdp = staticDataService.getData();
        RuleSystemProvider rsp = sdp.getRuleSystems();
        RuleSystem rs = rsp.getByRuleSetCode(ruleCode);

        List<RuleSystemItemType> ruleSystemItemTypes = new ArrayList<>(rs.getItemTypes());
        for (GroupConfiguration configuration : viewConfig.getGroups()) {
            GroupVO group = new GroupVO(configuration.getCode(), configuration.getName());
            List<TypeInfoVO> typeInfos = new ArrayList<>(configuration.getTypes().size());
            for (TypeInfo typeInfo : configuration.getTypes()) {
                String code = typeInfo.getCode();
                RuleSystemItemType itemType = rs.getItemTypeByCode(code);
                ruleSystemItemTypes.remove(itemType);
                typeInfos.add(new TypeInfoVO(itemType.getItemTypeId(), typeInfo.getWidth()));
            }
            group.setItemTypes(typeInfos);
            result.add(group);
        }

        GroupVO defaultGroup = new GroupVO("DEFAULT", "Bez skupiny");
        List<TypeInfoVO> typeInfos = new ArrayList<>();
        for (RuleSystemItemType ruleSystemItemType : ruleSystemItemTypes) {
            String code = ruleSystemItemType.getCode();
            RuleSystemItemType itemType = rs.getItemTypeByCode(code);
            typeInfos.add(new TypeInfoVO(itemType.getItemTypeId(), 1));
        }
        defaultGroup.setItemTypes(typeInfos);
        result.add(defaultGroup);

        return result;
    }

    /**
     * Vrací typy oprávnění.
     *
     * @return seznam typů oprávnění
     */
    @RequestMapping(value = "/policy/types", method = RequestMethod.GET)
    public List<RulPolicyTypeVO> getAllPolicyTypes() {
        List<RulPolicyType> policyTypes = policyService.getPolicyTypes();
        return factoryVo.createPolicyTypes(policyTypes);
    }

    /**
     * Nastaví/smazaní viditelnost typu oprávnění.
     *
     * @param nodeId              identifikátor node ke kterému se hodnota vztahuje.
     * @param fundVersionId       identifikátor verze AS
     * @param visiblePolicyParams parametry nastavení
     */
    @Transactional
    @RequestMapping(value = "/policy/{nodeId}/{fundVersionId}", method = RequestMethod.PUT)
    public void setVisiblePolicy(@PathVariable(value = "nodeId") final Integer nodeId,
                                 @PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                 @RequestBody final VisiblePolicyParams visiblePolicyParams) {
        Assert.notNull(nodeId, "Identifikátor JP musí být vyplněn");
        Assert.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");
        Assert.notNull(visiblePolicyParams, "Parametry musí být vyplněny");



        ArrNode node = nodeRepository.findOne(nodeId);
        Assert.notNull(node, "Uzel s id=" + nodeId + " neexistuje");

        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
        Assert.notNull(fundVersion, "Verze fondu s id=" + fundVersionId + " neexistuje");

        ruleService.syncNodeExtensions(fundVersion.getFundVersionId(), nodeId, visiblePolicyParams.getNodeExtensions());
        policyService.setVisiblePolicy(node, fundVersion, visiblePolicyParams.getPolicyTypeIdsMap(),
                visiblePolicyParams.getIncludeSubtree());
    }

    /**
     * Získání nastavení pravidel zobrazení pro uzly.
     *
     * @param nodeId
     *            identifikátor node ke kterému hledám oprávnění
     * @param fundVersionId
     *            identifikátor verze AS
     * @return mapa uzlů map typů a jejich zobrazení
     */
    @RequestMapping(value = "/policy/{nodeId}/{fundVersionId}", method = RequestMethod.GET)
    @Transactional
    public VisiblePolicyTypes getVisiblePolicy(@PathVariable(value = "nodeId") final Integer nodeId,
                                               @PathVariable(value = "fundVersionId") final Integer fundVersionId) {
        Assert.notNull(nodeId, "Identifikátor JP musí být vyplněn");
        Assert.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");

        ArrNode node = nodeRepository.findOne(nodeId);
        Assert.notNull(node, "Uzel s id=" + nodeId + " neexistuje");

        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
        Assert.notNull(fundVersion, "Verze fondu s id=" + fundVersionId + " neexistuje");

        // TODO: Develop new request in policy service to prepare list of active
        //       policy types. Recursive query could be used

        VisiblePolicyTypes result = new VisiblePolicyTypes();

        Map<Integer, Boolean> visibleTypeIdsPolicy = policyService.getVisiblePolicyIds(node.getNodeId(), fundVersion, true);
        result.setPolicyTypeIdsMap(visibleTypeIdsPolicy);

        Map<Integer, Boolean> nodeVisibleTypeIdsPolicy = policyService.getVisiblePolicyIds(node.getNodeId(), fundVersion, false);
        result.setNodePolicyTypeIdsMap(nodeVisibleTypeIdsPolicy);

        final List<RulArrangementExtension> availableExtensions = ruleService.findArrangementExtensionsByFundVersionId(fundVersion.getFundVersionId());
        result.setAvailableExtensions(factoryVo.createSimpleEntity(availableExtensions, RulArrangementExtensionVO.class));

        final List<RulArrangementExtension> nodeExtensions = ruleService.findArrangementExtensionsByNodeId(fundVersion.getFundVersionId(), node.getNodeId());
        result.setNodeExtensions(factoryVo.createSimpleEntity(nodeExtensions, RulArrangementExtensionVO.class));

        final List<RulArrangementExtension> parentExtensions = ruleService.findAllArrangementExtensionsByNodeId(node.getNodeId());
        result.setParentExtensions(factoryVo.createSimpleEntity(parentExtensions, RulArrangementExtensionVO.class));

        return result;
    }

    @RequestMapping(value="/importPackage", method=RequestMethod.POST)
    public void importPackageRest(@RequestParam("file") final MultipartFile file){
        Assert.notNull(file, "Soubor musí být vyplněn");
        File temp = null;
        try {
            temp = File.createTempFile("importPackage", ".zip");
            try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(temp, false))) {
                IOUtils.copy(file.getInputStream(), outputStream);
            }
            packageService.importPackage(temp);
        } catch (IOException e) {
            throw new SystemException("Nepodařilo se vytvořit dočasný soubor pro import", e);
        } finally {
            if (temp != null) {
                temp.deleteOnExit();
            }
        }
    }

    /**
     * Vstupní parametry pro nastavení oprávnění.
     */
    public static class VisiblePolicyParams {
        /**
         * Zohlednit i podstrom
         */
        private Boolean includeSubtree;

        /**
         * Mapa typů oprávnění a jejich zobrazení
         */
        private Map<Integer, Boolean> policyTypeIdsMap;
        private Set<Integer> nodeExtensions;

        public Boolean getIncludeSubtree() {
            return includeSubtree;
        }

        public void setIncludeSubtree(final Boolean includeSubtree) {
            this.includeSubtree = includeSubtree;
        }

        public Map<Integer, Boolean> getPolicyTypeIdsMap() {
            return policyTypeIdsMap;
        }

        public void setPolicyTypeIdsMap(final Map<Integer, Boolean> policyTypeIdsMap) {
            this.policyTypeIdsMap = policyTypeIdsMap;
        }

        public Set<Integer> getNodeExtensions() {
            return nodeExtensions;
        }

        public void setNodeExtensions(Set<Integer> nodeExtensions) {
            this.nodeExtensions = nodeExtensions;
        }
    }

    /**
     * Výstupní data pro zjištění oprávnění.
     */
    public static class VisiblePolicyTypes {

        private List<RulArrangementExtensionVO> parentExtensions;

        private List<RulArrangementExtensionVO> nodeExtensions;

        private List<RulArrangementExtensionVO> availableExtensions;

        /**
         * Mapa typů oprávnění a jejich zobrazení z rodiče
         */
        private Map<Integer, Boolean> policyTypeIdsMap;

        /**
         * Node specific settings
         */
        private Map<Integer, Boolean> nodePolicyTypeIdsMap;

        public List<RulArrangementExtensionVO> getParentExtensions() {
            return parentExtensions;
        }

        public void setParentExtensions(List<RulArrangementExtensionVO> parentExtensions) {
            this.parentExtensions = parentExtensions;
        }

        public List<RulArrangementExtensionVO> getNodeExtensions() {
            return nodeExtensions;
        }

        public void setNodeExtensions(List<RulArrangementExtensionVO> nodeExtensions) {
            this.nodeExtensions = nodeExtensions;
        }

        public List<RulArrangementExtensionVO> getAvailableExtensions() {
            return availableExtensions;
        }

        public void setAvailableExtensions(List<RulArrangementExtensionVO> availableExtensions) {
            this.availableExtensions = availableExtensions;
        }

        public Map<Integer, Boolean> getPolicyTypeIdsMap() {
            return policyTypeIdsMap;
        }

        public void setPolicyTypeIdsMap(final Map<Integer, Boolean> policyTypeIdsMap) {
            this.policyTypeIdsMap = policyTypeIdsMap;
        }

        public void setNodePolicyTypeIdsMap(Map<Integer,Boolean> nodePolicyTypeIdsMap) {
            this.nodePolicyTypeIdsMap = nodePolicyTypeIdsMap;
        }

        public Map<Integer, Boolean> getNodePolicyTypeIdsMap() {
            return nodePolicyTypeIdsMap;
        }
    }

}
