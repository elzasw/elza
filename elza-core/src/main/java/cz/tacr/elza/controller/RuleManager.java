package cz.tacr.elza.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.api.ArrNodeConformityExt;
import cz.tacr.elza.api.exception.ConcurrentUpdateException;
import cz.tacr.elza.api.vo.NodeTypeOperation;
import cz.tacr.elza.api.vo.RelatedNodeDirection;
import cz.tacr.elza.asynchactions.UpdateConformityInfoService;
import cz.tacr.elza.controller.factory.ExtendedObjectsFactory;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrNodeConformity;
import cz.tacr.elza.domain.ArrNodeConformityError;
import cz.tacr.elza.domain.ArrVersionConformity;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformityMissing;
import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemConstraint;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemSpecExt;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.RulFaView;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.vo.DataValidationResult;
import cz.tacr.elza.domain.vo.FaViewDescItemTypes;
import cz.tacr.elza.drools.RulesExecutor;
import cz.tacr.elza.exception.LockVersionChangeException;
import cz.tacr.elza.packageimport.PackageService;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.DescItemConstraintRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;
import cz.tacr.elza.repository.FaViewRepository;
import cz.tacr.elza.repository.VersionConformityRepository;
import cz.tacr.elza.repository.FindingAidVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeConformityErrorRepository;
import cz.tacr.elza.repository.NodeConformityRepository;
import cz.tacr.elza.repository.NodeConformityMissingRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.validation.ArrDescItemsPostValidator;


/**
 * Implementace API pro práci s pravidly.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 30. 7. 2015
 */
@RestController
@RequestMapping("/api/ruleSetManager")
public class RuleManager implements cz.tacr.elza.api.controller.RuleManager<RulDataType, RulDescItemType,
        RulDescItemSpec, RulFaView, NodeTypeOperation, RelatedNodeDirection, ArrDescItem, ArrFindingAidVersion,
        ArrVersionConformity, RulPackage> {

    private static final String VIEW_SPECIFICATION_SEPARATOR = "|";
    private static final String VIEW_SPECIFICATION_SEPARATOR_REGEX = "\\|";

    @Autowired
    private RuleSetRepository ruleSetRepository;

    @Autowired
    private DescItemTypeRepository descItemTypeRepository;

    @Autowired
    private DescItemSpecRepository descItemSpecRepository;

    @Autowired
    private DescItemConstraintRepository descItemConstraintRepository;

    @Autowired
    private ArrangementTypeRepository arrangementTypeRepository;

    @Autowired
    private FindingAidVersionRepository findingAidVersionRepository;

    @Autowired
    private FaViewRepository faViewRepository;

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private NodeConformityRepository nodeConformityInfoRepository;

    @Autowired
    private NodeConformityErrorRepository nodeConformityErrorsRepository;

    @Autowired
    private NodeConformityMissingRepository nodeConformityMissingRepository;

    @Autowired
    private RulesExecutor rulesExecutor;

    @Autowired
    private ArrDescItemsPostValidator descItemsPostValidator;

    @Autowired
    private ArrangementManager arrangementManager;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ExtendedObjectsFactory extendedObjectsFactory;

    @Autowired
    private UpdateConformityInfoService updateConformityInfoService;

    @Autowired
    private VersionConformityRepository findingAidVersionConformityInfoRepository;

    @Autowired
    private PackageService packageService;

    @Override
    @RequestMapping(value = "/getDescItemSpecById", method = RequestMethod.GET)
    public RulDescItemSpec getDescItemSpecById(@RequestParam(value = "descItemSpecId") Integer descItemSpecId) {
        Assert.notNull(descItemSpecId);

        return descItemSpecRepository.findOne(descItemSpecId);
    }

    @Override
    @RequestMapping(value = "/getRuleSets", method = RequestMethod.GET)
    public List<RulRuleSet> getRuleSets() {
        return ruleSetRepository.findAll();
    }

    @Override
    @RequestMapping(value = "/getArrangementTypes", method = RequestMethod.GET)
    public List<RulArrangementType> getArrangementTypes(Integer ruleSetId) {
        Assert.notNull(ruleSetId);

        return arrangementTypeRepository.findByRuleSetId(ruleSetId);
    }

    @Override
    @RequestMapping(value = "/getDescriptionItemTypes", method = RequestMethod.GET)
    public List<RulDescItemTypeExt> getDescriptionItemTypes(
            @RequestParam(value = "ruleSetId") Integer ruleSetId) {
        List<RulDescItemType> itemTypeList = descItemTypeRepository.findAll();
        return createExt(itemTypeList);
    }

    // TODO: smazat -> přepsáno do servisky
    @Override
    @RequestMapping(value = "/getDescriptionItemTypesForNode/{faVersionId}/{nodeId}", method = RequestMethod.POST)
    public List<RulDescItemTypeExt> getDescriptionItemTypesForNode(
            @PathVariable(value = "faVersionId") Integer faVersionId,
            @PathVariable(value = "nodeId") Integer nodeId,
            @RequestBody Set<String> strategies) {
    	
    	// Pravdepodobne vhodne zapouzdrit do jedne funkce
        ArrFindingAidVersion version = findingAidVersionRepository.findOne(faVersionId);
        if (version == null) {
            throw new IllegalArgumentException("Verze archivni pomucky neexistuje");
        }
        ArrNode node = nodeRepository.findOne(nodeId);
        ArrLevel level = levelRepository.findNodeInRootTreeByNodeId(node, version.getRootLevel().getNode(),
                version.getLockChange());
    	
    	
        Assert.notNull(strategies);
        List<RulDescItemType> itemTypeList = descItemTypeRepository.findAll();

        List<RulDescItemTypeExt> rulDescItemTypeExtList = createExt(itemTypeList);

        // projde všechny typy atributů
        for (RulDescItemTypeExt rulDescItemTypeExt : rulDescItemTypeExtList) {

            rulDescItemTypeExt.setType(RulDescItemType.Type.POSSIBLE);
            rulDescItemTypeExt.setRepeatable(true);

            // projde všechny podmínky typů
            for (RulDescItemConstraint rulDescItemConstraint : rulDescItemTypeExt.getRulDescItemConstraintList()) {
                if (rulDescItemConstraint.getRepeatable() != null && rulDescItemConstraint.getRepeatable().equals(false)) {
                    rulDescItemTypeExt.setRepeatable(false);
                    break;
                }
            }

            // projde všechny specifikace typů atributů
            for (RulDescItemSpecExt rulDescItemSpecExt : rulDescItemTypeExt.getRulDescItemSpecList()) {

                rulDescItemSpecExt.setType(RulDescItemSpec.Type.POSSIBLE);
                rulDescItemSpecExt.setRepeatable(true);

                // projde všechny podmínky specifikací
                for (RulDescItemConstraint rulDescItemConstraint : rulDescItemSpecExt.getRulDescItemConstraintList()) {
                    if (rulDescItemConstraint.getRepeatable() != null && rulDescItemConstraint.getRepeatable().equals(false)) {
                        rulDescItemSpecExt.setRepeatable(false);
                        break;
                    }
                }
            }
        }

        return rulesExecutor.executeDescItemTypesRules(level, rulDescItemTypeExtList, version, strategies);
    }

    @Override
    @RequestMapping(value = "/getDescItemSpecsFortDescItemType", method = RequestMethod.GET)
    public List<RulDescItemSpec> getDescItemSpecsFortDescItemType(
            @RequestBody() RulDescItemType rulDescItemType) {
        List<RulDescItemSpec> itemList = descItemSpecRepository.findByDescItemType(rulDescItemType);
        return itemList;
    }

    @Override
    @RequestMapping(value = "/getDataTypeForDescItemType", method = RequestMethod.GET)
    public RulDataType getDataTypeForDescItemType(
            @RequestBody() RulDescItemType rulDescItemType) {
        List<RulDataType> typeList = descItemTypeRepository.findRulDataType(rulDescItemType);
        return typeList.get(0);
    }

    // TODO: smazat -> přesunuto do servisky
    private List<RulDescItemTypeExt> createExt(final List<RulDescItemType> itemTypeList) {
        if (itemTypeList.isEmpty()) {
            return new LinkedList<>();
        }

        List<RulDescItemSpec> listDescItem = descItemSpecRepository.findByItemTypeIds(itemTypeList);
        Map<Integer, List<RulDescItemSpec>> itemSpecMap =
                ElzaTools.createGroupMap(listDescItem, p -> p.getDescItemType().getDescItemTypeId());

        List<RulDescItemConstraint> findItemConstList =
                descItemConstraintRepository.findByItemTypeIds(itemTypeList);
        Map<Integer, List<RulDescItemConstraint>> itemConstrainMap =
                ElzaTools.createGroupMap(findItemConstList, p -> p.getDescItemType().getDescItemTypeId());

        List<RulDescItemConstraint> findItemSpecConstList;
        if (listDescItem.isEmpty()) {
            findItemSpecConstList = new ArrayList<>();
        } else {
            findItemSpecConstList = descItemConstraintRepository.findByItemSpecIds(listDescItem);
        }
        Map<Integer, List<RulDescItemConstraint>> itemSpecConstrainMap =
                ElzaTools.createGroupMap(findItemSpecConstList, p -> p.getDescItemSpec().getDescItemSpecId());

        List<RulDescItemTypeExt> result = new LinkedList<>();
        for (RulDescItemType rulDescItemType : itemTypeList) {
            RulDescItemTypeExt descItemTypeExt = new RulDescItemTypeExt();
            BeanUtils.copyProperties(rulDescItemType, descItemTypeExt);
            List<RulDescItemSpec> itemSpecList =
                    itemSpecMap.get(rulDescItemType.getDescItemTypeId());
            if (itemSpecList != null) {
                for (RulDescItemSpec rulDescItemSpec : itemSpecList) {
                    RulDescItemSpecExt descItemSpecExt = new RulDescItemSpecExt();
                    BeanUtils.copyProperties(rulDescItemSpec, descItemSpecExt);
                    descItemTypeExt.getRulDescItemSpecList().add(descItemSpecExt);
                    List<RulDescItemConstraint> itemConstrainList =
                            itemSpecConstrainMap.get(rulDescItemSpec.getDescItemSpecId());
                    if (itemConstrainList != null) {
                        descItemSpecExt.getRulDescItemConstraintList().addAll(itemConstrainList);
                    }
                }
            }
            List<RulDescItemConstraint> itemConstrainList =
                    itemConstrainMap.get(rulDescItemType.getDescItemTypeId());
            if (itemConstrainList != null) {
                descItemTypeExt.getRulDescItemConstraintList().addAll(itemConstrainList);
            }
            result.add(descItemTypeExt);
        }

        return result;
    }

    @Override
    @RequestMapping(value = "/getFaViewDescItemTypes", method = RequestMethod.GET)
    public FaViewDescItemTypes getFaViewDescItemTypes(@RequestParam(value = "faVersionId") Integer faVersionId) {
        Assert.notNull(faVersionId);
        ArrFindingAidVersion version = findingAidVersionRepository.getOne(faVersionId);
        RulRuleSet ruleSet = version.getRuleSet();
        RulArrangementType arrangementType = version.getArrangementType();

        List<RulFaView> faViewList =
                faViewRepository.findByRuleSetAndArrangementType(ruleSet, arrangementType);
        if (faViewList.size() > 1) {
            throw new IllegalStateException("Bylo nalezeno více záznamů (" + faViewList.size()
                    + ") podle RuleSetId " + ruleSet.getRuleSetId() + " a ArrangementTypeId "
                    + arrangementType.getArrangementTypeId());
        } else if (faViewList.isEmpty()) {
            RulFaView faView = new RulFaView();
            faView.setRuleSet(ruleSet);
            faView.setArrangementType(arrangementType);
            faView.setViewSpecification("");

            FaViewDescItemTypes result = new FaViewDescItemTypes();
            result.setDescItemTypes(new LinkedList<>());
            result.setRulFaView(faViewRepository.save(faView));
            return result;
            /*throw new IllegalStateException(
                    "Nebyl nalezen záznam podle RuleSetId " + ruleSet.getRuleSetId()
                            + " a ArrangementTypeId " + arrangementType.getArrangementTypeId());*/
        }
        RulFaView faView = faViewList.get(0);

        String itemTypesStr = faView.getViewSpecification();
        List<Integer> resultIdList = new LinkedList<>();
        if (StringUtils.isNotBlank(itemTypesStr)) {
            String[] itemTypes = itemTypesStr.split(VIEW_SPECIFICATION_SEPARATOR_REGEX);

            for (String itemTypeIdStr : itemTypes) {
                resultIdList.add(Integer.valueOf(itemTypeIdStr));
            }
        }
        final List<RulDescItemType> resultList = descItemTypeRepository.findAll(resultIdList);
        Collections.sort(resultList, new Comparator<RulDescItemType>() {

            @Override
            public int compare(RulDescItemType r1, RulDescItemType r2) {
                Integer position1 = resultIdList.indexOf(r1.getDescItemTypeId());
                Integer position2 = resultIdList.indexOf(r2.getDescItemTypeId());
                return position1.compareTo(position2);
            }

        });

        FaViewDescItemTypes result = new FaViewDescItemTypes();
        result.setRulFaView(faView);
        result.setDescItemTypes(resultList);

        return result;
    }

    @Override
    @RequestMapping(value = "/saveFaViewDescItemTypes", method = RequestMethod.PUT)
    @Transactional
    public List<Integer> saveFaViewDescItemTypes(@RequestBody RulFaView rulFaView,
                                                 @RequestParam(value = "descItemTypeIds") Integer[] descItemTypeIds) {
        Assert.notNull(rulFaView);

        Integer faViewId = rulFaView.getFaViewId();
        if (!faViewRepository.exists(faViewId)) {
            throw new ConcurrentUpdateException("Nastavení zobrazení sloupců s identifikátorem " + faViewId + " již neexistuje.");
        }

        String itemTypesStr = null;
        for (Integer itemTypeId : descItemTypeIds) {
            if (itemTypesStr == null) {
                itemTypesStr = itemTypeId.toString();
            } else {
                itemTypesStr += VIEW_SPECIFICATION_SEPARATOR + itemTypeId.toString();
            }
        }
        rulFaView.setViewSpecification(StringUtils.defaultString(itemTypesStr));
        faViewRepository.save(rulFaView);

        return Arrays.asList(descItemTypeIds);
    }


    @RequestMapping(value = "/getPackages", method = RequestMethod.GET)
    @Override
    @Transactional
    public List<RulPackage> getPackages() {
        return packageService.getPackages();
    }

    @Override
    @Transactional
    public void importPackage(final File file) {
        Assert.notNull(file);
        packageService.importPackage(file);
    }

    @RequestMapping(value = "/deletePackage/{code}", method = RequestMethod.GET)
    @Override
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
            File file = exportPackage(code);
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

    @Override
    public File exportPackage(final String code) {
        Assert.notNull(code);
        return packageService.exportPackage(code);
    }

    @RequestMapping(value="/importPackage", method=RequestMethod.POST)
    @Transactional
    public void importPackageRest(@RequestParam("file") final MultipartFile file){
        Assert.notNull(file);
        File temp = null;
        try {
            temp = File.createTempFile("importPackage", ".zip");
            file.transferTo(temp);
            importPackage(temp);
        } catch (IOException e) {
            throw new IllegalStateException("Nepodařilo se vytvořit dočasný soubor pro import", e);
        } finally {
            if (temp != null) {
                temp.deleteOnExit();
            }
        }
    }

}
