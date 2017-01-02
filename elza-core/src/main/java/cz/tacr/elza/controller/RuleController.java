package cz.tacr.elza.controller;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.RulDataTypeVO;
import cz.tacr.elza.controller.vo.RulPolicyTypeVO;
import cz.tacr.elza.controller.vo.RulRuleSetVO;
import cz.tacr.elza.controller.vo.RulTemplateVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPolicyType;
import cz.tacr.elza.domain.RulTemplate;
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
import java.util.List;
import java.util.Map;


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
    public List<RulPackage> getPackages() {
        return packageService.getPackages();
    }

    @RequestMapping(value = "/deletePackage/{code}", method = RequestMethod.GET)
    @Transactional
    public void deletePackage(@PathVariable(value = "code") final String code) {
        Assert.notNull(code);
        packageService.deletePackage(code);
    }

    @RequestMapping(value = "/exportPackage/{code}", method = RequestMethod.GET)
    public void exportPackageRest(@PathVariable(value = "code") final String code,
                                  HttpServletResponse response) {
        Assert.notNull(code);
        try {
            File file = packageService.exportPackage(code);
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "inline; filename=" + code + "-package.zip");
            response.setContentLength((int) file.length());
            InputStream is = new FileInputStream(file);
            IOUtils.copy(is, response.getOutputStream());
            response.flushBuffer();
        } catch (IOException ex) {
            throw new RuntimeException("Problem pri zapisu souboru", ex);
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
        Assert.notNull(nodeId);
        Assert.notNull(fundVersionId);
        Assert.notNull(visiblePolicyParams);

        ArrNode node = nodeRepository.findOne(nodeId);
        Assert.notNull(node, "Uzel s id=" + nodeId + " neexistuje");

        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
        Assert.notNull(fundVersion, "Verze fondu s id=" + fundVersionId + " neexistuje");

        policyService.setVisiblePolicy(node, fundVersion, visiblePolicyParams.getPolicyTypeIdsMap(),
                visiblePolicyParams.getIncludeSubtree());
    }

    /**
     * Získání nastavení oprávnění pro uzly.
     *
     * @param nodeId         identifikátor node ke kterému hledám oprávnění
     * @param fundVersionId  identifikátor verze AS
     * @param includeParents zohlednit zděděné oprávnění od rodičů?
     * @return mapa uzlů map typů a jejich zobrazení
     */
    @RequestMapping(value = "/policy/{nodeId}/{fundVersionId}/{includeParents}", method = RequestMethod.GET)
    public VisiblePolicyTypes getVisiblePolicy(@PathVariable(value = "nodeId") final Integer nodeId,
                                 @PathVariable(value = "fundVersionId") final Integer fundVersionId,
                                 @PathVariable(value = "includeParents") final Boolean includeParents) {
        Assert.notNull(nodeId);
        Assert.notNull(fundVersionId);
        Assert.notNull(includeParents);

        ArrNode node = nodeRepository.findOne(nodeId);
        Assert.notNull(node, "Uzel s id=" + nodeId + " neexistuje");

        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
        Assert.notNull(fundVersion, "Verze fondu s id=" + fundVersionId + " neexistuje");

        VisiblePolicyTypes result = new VisiblePolicyTypes();
        Map<Integer, Boolean> visibleTypeIdsPolicy = policyService.getVisiblePolicyIds(node.getNodeId(), fundVersion, includeParents);
        result.setPolicyTypeIdsMap(visibleTypeIdsPolicy);
        return result;
    }

    @Transactional
    @RequestMapping(value="/importPackage", method=RequestMethod.POST)
    public void importPackageRest(@RequestParam("file") final MultipartFile file){
        Assert.notNull(file);
        File temp = null;
        try {
            temp = File.createTempFile("importPackage", ".zip");
            try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(temp, false))) {
                IOUtils.copy(file.getInputStream(), outputStream);
            }
            packageService.importPackage(temp);
        } catch (IOException e) {
            throw new IllegalStateException("Nepodařilo se vytvořit dočasný soubor pro import", e);
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
    }

    /**
     * Výstupní data pro zjištění oprávnění.
     */
    public static class VisiblePolicyTypes {

        /**
         * Mapa typů oprávnění a jejich zobrazení
         */
        private Map<Integer, Boolean> policyTypeIdsMap;

        public Map<Integer, Boolean> getPolicyTypeIdsMap() {
            return policyTypeIdsMap;
        }

        public void setPolicyTypeIdsMap(final Map<Integer, Boolean> policyTypeIdsMap) {
            this.policyTypeIdsMap = policyTypeIdsMap;
        }
    }

}
