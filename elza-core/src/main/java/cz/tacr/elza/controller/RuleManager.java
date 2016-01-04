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
import cz.tacr.elza.api.ArrNodeConformityInfoExt;
import cz.tacr.elza.api.exception.ConcurrentUpdateException;
import cz.tacr.elza.api.vo.NodeTypeOperation;
import cz.tacr.elza.api.vo.RelatedNodeDirection;
import cz.tacr.elza.asynchactions.UpdateConformityInfoService;
import cz.tacr.elza.controller.factory.ExtendedObjectsFactory;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrFindingAidVersionConformityInfo;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformityErrors;
import cz.tacr.elza.domain.ArrNodeConformityInfo;
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
import cz.tacr.elza.repository.FindingAidVersionConformityInfoRepository;
import cz.tacr.elza.repository.FindingAidVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeConformityErrorsRepository;
import cz.tacr.elza.repository.NodeConformityInfoRepository;
import cz.tacr.elza.repository.NodeConformityMissingRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.RuleSetRepository;
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
        ArrFindingAidVersionConformityInfo, RulPackage> {

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
    private NodeConformityInfoRepository nodeConformityInfoRepository;

    @Autowired
    private NodeConformityErrorsRepository nodeConformityErrorsRepository;

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
    private FindingAidVersionConformityInfoRepository findingAidVersionConformityInfoRepository;

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

    @Override
    public ArrNodeConformityInfoExt setConformityInfo(final Integer faLevelId, final Integer faVersionId,
                                                      final Set<String> strategies) {
        Assert.notNull(faLevelId);
        Assert.notNull(faVersionId);
        Assert.notNull(strategies);

        ArrLevel level = levelRepository.findOne(faLevelId);
        Integer nodeId = level.getNode().getNodeId();

        ArrNode nodeBeforeValidation = nodeRepository.getOne(nodeId);
        Integer nodeVersionBeforeValidation = nodeBeforeValidation.getVersion();

        ArrFindingAidVersion version = findingAidVersionRepository.findOne(faVersionId);

        if (!arrangementManager.validLevelInVersion(level, version)) {
            throw new IllegalArgumentException("Level s id " + faLevelId + " nespadá do verze s id " + faVersionId);
        }

        List<DataValidationResult> validationResults = descItemsPostValidator.postValidateNodeDescItems(level, version, strategies);
        List<DataValidationResult> scriptResults = rulesExecutor.executeDescItemValidationRules(level, version, strategies);
        validationResults.addAll(scriptResults);
        
        ArrNodeConformityInfoExt result = updateNodeConformityInfo(level, version, validationResults);

        entityManager.detach(nodeBeforeValidation);
        ArrNode nodeAfterValidation = nodeRepository.getOne(nodeId);
        Integer nodeVersionAfterValidation = nodeAfterValidation.getVersion();

        if (!nodeVersionBeforeValidation.equals(nodeVersionAfterValidation)) {
            throw new LockVersionChangeException("Behem validace doslo ke zmene verze uzlu " + nodeId);
        }

        return result;
    }

    @Override
    public void setVersionConformityInfo(final ArrFindingAidVersionConformityInfo.State state,
                                         final String stateDescription,
                                         final ArrFindingAidVersion version) {
        Assert.notNull(version);
        ArrFindingAidVersionConformityInfo conformityInfo = findingAidVersionConformityInfoRepository
                .findByFaVersion(version);

        if (conformityInfo == null) {
            conformityInfo = new ArrFindingAidVersionConformityInfo();
        }

        conformityInfo.setFaVersion(version);
        conformityInfo.setState(state);
        conformityInfo.setStateDescription(stateDescription);
        findingAidVersionConformityInfoRepository.save(conformityInfo);
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

    /**
     * Provede uložení stavu pro daný uzel podle výsledku validace.
     * @param level validaovaný uzel
     * @param version verze, do které spadá uzel
     * @param validationResults seznam validačních chyb
     */
    private ArrNodeConformityInfoExt updateNodeConformityInfo(final ArrLevel level,
                                          final ArrFindingAidVersion version,
                                          final List<DataValidationResult> validationResults) {

        ArrNodeConformityInfo conformityInfo = nodeConformityInfoRepository
                .findByNodeAndFaVersion(level.getNode(), version);

        if (conformityInfo != null && conformityInfo.getState().equals(ArrNodeConformityInfo.State.OK)) {
            conformityInfo.setDate(new Date());
        } else {
            if(conformityInfo != null){
                deleteConformityInfo(Arrays.asList(conformityInfo));
            }
            conformityInfo = new ArrNodeConformityInfo();
            conformityInfo.setNode(level.getNode());
            conformityInfo.setFaVersion(version);
            conformityInfo.setDate(new Date());
        }


        if (validationResults.isEmpty()) {
            conformityInfo.setState(ArrNodeConformityInfo.State.OK);
            nodeConformityInfoRepository.save(conformityInfo);
        } else {
            conformityInfo.setState(ArrNodeConformityInfo.State.ERR);
            nodeConformityInfoRepository.save(conformityInfo);

            for (DataValidationResult validationResult : validationResults) {
                switch (validationResult.getResultType()) {
                    case MISSING:
                        ArrNodeConformityMissing missing = new ArrNodeConformityMissing();
                        missing.setNodeConformityInfo(conformityInfo);
                        missing.setDescItemType(validationResult.getType());
                        missing.setDescItemSpec(validationResult.getSpec());
                        missing.setDescription(validationResult.getMessage());
                        nodeConformityMissingRepository.save(missing);
                        break;
                    case ERROR:
                        ArrNodeConformityErrors error = new ArrNodeConformityErrors();
                        error.setNodeConformityInfo(conformityInfo);
                        error.setDescItem(validationResult.getDescItem());
                        error.setDescription(validationResult.getMessage());
                        nodeConformityErrorsRepository.save(error);
                        break;
                }
            }

            setVersionConformityInfo(ArrFindingAidVersionConformityInfo.State.ERR,
                    "Nejméně jedna jednotka popisu se nachází v chybovém stavu", version);
        }

        return extendedObjectsFactory.createNodeConformityInfoExt(conformityInfo, true);
    }





    /**
     * Provede úpravů (smazání) stavů uzlů podle pravidel.
     *
     * @param faVersionId verze nodů
     * @param nodeIds seznam id nodů, od kterých se má prohledávat
     * @param nodeTypeOperation typ operace
     * @param createDescItems hodnoty atributů k vytvoření
     * @param updateDescItems hodnoty atributů k upravení
     * @param deleteDescItems hodnoty atributů ke smazání
     * @return seznam dopadů
     */
    public Set<RelatedNodeDirection> conformityInfo(final Integer faVersionId,
                                                       final Collection<Integer> nodeIds,
                                                       final NodeTypeOperation nodeTypeOperation,
                                                       final List<ArrDescItem> createDescItems,
                                                       final List<ArrDescItem> updateDescItems,
                                                       final List<ArrDescItem> deleteDescItems) {

        Set<RelatedNodeDirection> impactOnConformityInfo = getImpactOnConformityInfo(faVersionId, nodeTypeOperation,
                createDescItems, updateDescItems, deleteDescItems);

        deleteConformityInfo(faVersionId, nodeIds, impactOnConformityInfo);

        return impactOnConformityInfo;
    }

    /**
     * Pro vybrané nody s danou verzí smaže všechny stavy v daných směrech od nodů.
     *
     * @param faVersionId      verze nodů
     * @param nodeIds          seznam id nodů, od kterých se má prohledávat
     * @param deleteDirections směry prohledávání (null pokud se mají smazat stavy zadaných nodů .
     */
    private void deleteConformityInfo(final Integer faVersionId,
                                     final Collection<Integer> nodeIds,
                                     final Collection<RelatedNodeDirection> deleteDirections) {
        Assert.notNull(faVersionId);
        Assert.notEmpty(nodeIds);

        List<ArrNode> nodes = nodeRepository.findAll(nodeIds);
        ArrFindingAidVersion version = findingAidVersionRepository.findOne(faVersionId);

        Set<ArrNode> deleteNodes = new HashSet<>();

        if (CollectionUtils.isEmpty(deleteDirections)) {
            deleteNodes.addAll(nodes);
        } else {

            for (RelatedNodeDirection deleteDirection : deleteDirections) {
                for (ArrNode node : nodes) {
                    deleteNodes.addAll(nodeRepository.findNodesByDirection(node, version, deleteDirection));
                }
            }
        }


        if (!deleteNodes.isEmpty()) {
            List<ArrNodeConformityInfo> deleteInfos = nodeConformityInfoRepository
                    .findByNodesAndVersion(deleteNodes, version);

            deleteConformityInfo(deleteInfos);
            setVersionConformityInfo(null, null, version);
            updateConformityInfoService.updateInfoForNodesAfterCommit(deleteNodes, version);
        }
    }

    /**
     * Zjistí podle pravidel dopad na změnu stavů uzlů.
     *
     * @param faVersionId verze nodů
     * @param nodeTypeOperation typ operace
     * @param createDescItems hodnoty atributů k vytvoření
     * @param updateDescItems hodnoty atributů k upravení
     * @param deleteDescItems hodnoty atributů ke smazání
     * @return seznam dopadů
     */
    private Set<RelatedNodeDirection> getImpactOnConformityInfo(final Integer faVersionId,
                                                                final NodeTypeOperation nodeTypeOperation,
                                                                final List<ArrDescItem> createDescItems,
                                                                final List<ArrDescItem> updateDescItems,
                                                                final List<ArrDescItem> deleteDescItems) {

        ArrFindingAidVersion version = findingAidVersionRepository.findOne(faVersionId);

        if (version == null) {
            throw new IllegalArgumentException("Verze archivni pomucky neexistuje");
        }

        return rulesExecutor
                .executeImpactOfChangesLevelStateRules(createDescItems, updateDescItems, deleteDescItems,
                        nodeTypeOperation, version);
    }

    /**
     * Smaže všechny vybrané stavy.
     *
     * @param infos stavy ke smazání
     */
    private void deleteConformityInfo(final Collection<ArrNodeConformityInfo> infos) {

        if (CollectionUtils.isNotEmpty(infos)) {
            List<ArrNodeConformityMissing> missing = nodeConformityMissingRepository
                    .findByNodeConformityInfos(infos);
            if (CollectionUtils.isNotEmpty(missing)) {
                nodeConformityMissingRepository.delete(missing);
            }

            List<ArrNodeConformityErrors> errors = nodeConformityErrorsRepository.findByNodeConformityInfos(infos);
            if (CollectionUtils.isNotEmpty(errors)) {
                nodeConformityErrorsRepository.delete(errors);
            }

            nodeConformityInfoRepository.delete(infos);
        }
    }

}
