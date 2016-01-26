package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.ElzaRules;
import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.api.vo.NodeTypeOperation;
import cz.tacr.elza.api.vo.RelatedNodeDirection;
import cz.tacr.elza.asynchactions.UpdateConformityInfoService;
import cz.tacr.elza.controller.factory.ExtendedObjectsFactory;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformity;
import cz.tacr.elza.domain.ArrNodeConformityError;
import cz.tacr.elza.domain.ArrNodeConformityExt;
import cz.tacr.elza.domain.ArrNodeConformityMissing;
import cz.tacr.elza.domain.ArrVersionConformity;
import cz.tacr.elza.domain.RulDescItemConstraint;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemSpecExt;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.vo.DataValidationResult;
import cz.tacr.elza.drools.RulesExecutor;
import cz.tacr.elza.exception.LockVersionChangeException;
import cz.tacr.elza.repository.DescItemConstraintRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;
import cz.tacr.elza.repository.FindingAidVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeConformityErrorRepository;
import cz.tacr.elza.repository.NodeConformityMissingRepository;
import cz.tacr.elza.repository.NodeConformityRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.VersionConformityRepository;
import cz.tacr.elza.utils.ObjectListIterator;
import cz.tacr.elza.validation.ArrDescItemsPostValidator;


/**
 * Servisní třída pro pravidla.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 13.01.2016
 */
@Service
public class RuleService {

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private UpdateConformityInfoService updateConformityInfoService;
    @Autowired
    private ArrangementService arrangementService;
    @Autowired
    private RulesExecutor rulesExecutor;
    @Autowired
    private ElzaRules elzaRules;

    @Autowired
    private VersionConformityRepository versionConformityRepository;
    @Autowired
    private LevelRepository levelRepository;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private FindingAidVersionRepository findingAidVersionRepository;
    @Autowired
    private ArrDescItemsPostValidator descItemsPostValidator;
    @Autowired
    private NodeConformityRepository nodeConformityInfoRepository;
    @Autowired
    private NodeConformityMissingRepository nodeConformityMissingRepository;
    @Autowired
    private NodeConformityErrorRepository nodeConformityErrorsRepository;
    @Autowired
    private ExtendedObjectsFactory extendedObjectsFactory;
    @Autowired
    private DescItemSpecRepository descItemSpecRepository;
    @Autowired
    private DescItemTypeRepository descItemTypeRepository;
    @Autowired
    private DescItemConstraintRepository descItemConstraintRepository;

    /**
     * Nastavení stavu u verze archivní pomůcky.
     *
     * @param state            stav
     * @param stateDescription popis stavu
     * @param version          verze ap
     */
    public void setVersionConformityInfo(final ArrVersionConformity.State state,
                                         final String stateDescription,
                                         final ArrFindingAidVersion version) {
        Assert.notNull(version);
        ArrVersionConformity conformityInfo = versionConformityRepository
                .findByVersion(version);

        if (conformityInfo == null) {
            conformityInfo = new ArrVersionConformity();
        }

        conformityInfo.setVersion(version);
        conformityInfo.setState(state);
        conformityInfo.setStateDescription(stateDescription);
        versionConformityRepository.save(conformityInfo);
    }


    /**
     * Provede validaci atributů vybraného uzlu a nastaví jejich validační hodnoty.
     *
     * @param faLevelId   id uzlu
     * @param faVersionId id verze
     * @param strategies  strategie vyhodnocovani
     * @return stav validovaného uzlu
     */
    public ArrNodeConformityExt setConformityInfo(final Integer faLevelId, final Integer faVersionId,
                                                  final Set<String> strategies) {
        Assert.notNull(faLevelId);
        Assert.notNull(faVersionId);
        Assert.notNull(strategies);

        ArrLevel level = levelRepository.findOne(faLevelId);
        Integer nodeId = level.getNode().getNodeId();

        ArrNode nodeBeforeValidation = nodeRepository.getOne(nodeId);
        Integer nodeVersionBeforeValidation = nodeBeforeValidation.getVersion();

        ArrFindingAidVersion version = findingAidVersionRepository.findOne(faVersionId);

        if (!arrangementService.validLevelInVersion(level, version)) {
            throw new IllegalArgumentException("Level s id " + faLevelId + " nespadá do verze s id " + faVersionId);
        }

        List<DataValidationResult> validationResults = descItemsPostValidator
                .postValidateNodeDescItems(level, version, strategies);
        List<DataValidationResult> scriptResults = rulesExecutor
                .executeDescItemValidationRules(level, version, strategies);
        validationResults.addAll(scriptResults);

        ArrNodeConformityExt result = updateNodeConformityInfo(level, version, validationResults);

        entityManager.detach(nodeBeforeValidation);
        ArrNode nodeAfterValidation = nodeRepository.getOne(nodeId);
        Integer nodeVersionAfterValidation = nodeAfterValidation.getVersion();

        if (!nodeVersionBeforeValidation.equals(nodeVersionAfterValidation)) {
            throw new LockVersionChangeException("Behem validace doslo ke zmene verze uzlu " + nodeId);
        }

        return result;
    }


    /**
     * Provede uložení stavu pro daný uzel podle výsledku validace.
     *
     * @param level             validaovaný uzel
     * @param version           verze, do které spadá uzel
     * @param validationResults seznam validačních chyb
     */
    private ArrNodeConformityExt updateNodeConformityInfo(final ArrLevel level,
                                                          final ArrFindingAidVersion version,
                                                          final List<DataValidationResult> validationResults) {

        ArrNodeConformity conformityInfo = nodeConformityInfoRepository
                .findByNodeAndFaVersion(level.getNode(), version);

        if (conformityInfo != null && conformityInfo.getState().equals(ArrNodeConformity.State.OK)) {
            conformityInfo.setDate(new Date());
        } else {
            if (conformityInfo != null) {
                deleteConformityInfo(Arrays.asList(conformityInfo));
            }
            conformityInfo = new ArrNodeConformity();
            conformityInfo.setNode(level.getNode());
            conformityInfo.setFaVersion(version);
            conformityInfo.setDate(new Date());
        }


        if (validationResults.isEmpty()) {
            conformityInfo.setState(ArrNodeConformity.State.OK);
            nodeConformityInfoRepository.save(conformityInfo);
        } else {
            conformityInfo.setState(ArrNodeConformity.State.ERR);
            nodeConformityInfoRepository.save(conformityInfo);

            for (DataValidationResult validationResult : validationResults) {
                switch (validationResult.getResultType()) {
                    case MISSING:
                        ArrNodeConformityMissing missing = new ArrNodeConformityMissing();
                        missing.setNodeConformity(conformityInfo);
                        missing.setDescItemType(validationResult.getType());
                        missing.setDescItemSpec(validationResult.getSpec());
                        missing.setDescription(validationResult.getMessage());
                        nodeConformityMissingRepository.save(missing);
                        break;
                    case ERROR:
                        ArrNodeConformityError error = new ArrNodeConformityError();
                        error.setNodeConformity(conformityInfo);
                        error.setDescItem(validationResult.getDescItem());
                        error.setDescription(validationResult.getMessage());
                        nodeConformityErrorsRepository.save(error);
                        break;
                }
            }

            setVersionConformityInfo(ArrVersionConformity.State.ERR,
                    "Nejméně jedna jednotka popisu se nachází v chybovém stavu", version);
        }

        return extendedObjectsFactory.createNodeConformityInfoExt(conformityInfo, true);
    }

    /**
     * Získání stavu JP pro seznam JP.
     *
     * @param nodeIds   seznam id nodů, od kterých se získat stav
     * @param version   verze archivní pomůcky
     * @return  stavy JP
     */
    public Map<Integer, ArrNodeConformityExt> getNodeConformityInfoForNodes(final Collection<Integer> nodeIds,
                                                                           final ArrFindingAidVersion version) {
        Map<Integer, ArrNodeConformityExt> result = new HashMap<>();

        if (nodeIds.size() == 0) {
            return result;
        }

        ObjectListIterator<Integer> iteratorNodeIds = new ObjectListIterator<>(nodeIds);

        while (iteratorNodeIds.hasNext()) {
            List<Integer> partNodeIds = iteratorNodeIds.next();

            List<ArrNodeConformity> conformityInfos = nodeConformityInfoRepository
                    .findByNodeIdsAndFaVersion(partNodeIds, version);

            ArrayList<Integer> conformityInfoIds = conformityInfos.stream().map(ArrNodeConformity::getNodeConformityId)
                    .collect(Collectors.toCollection(ArrayList::new));

            ObjectListIterator<Integer> iteratorConformityIds = new ObjectListIterator<>(conformityInfoIds);

            Map<Integer, List<ArrNodeConformityMissing>> missings = new HashMap<>();
            Map<Integer, List<ArrNodeConformityError>> errors = new HashMap<>();

            while (iteratorConformityIds.hasNext()) {
                List<Integer> partIds = iteratorConformityIds.next();

                List<ArrNodeConformityMissing> partMissings = nodeConformityMissingRepository
                        .findByConformityIds(partIds);

                for (ArrNodeConformityMissing partMissing : partMissings) {
                    Integer conformityId = partMissing.getNodeConformity().getNodeConformityId();
                    List<ArrNodeConformityMissing> missingList = missings.get(conformityId);

                    if (missingList == null) {
                        missingList = new ArrayList<>();
                        missings.put(conformityId, missingList);
                    }

                    missingList.add(partMissing);
                }

                List<ArrNodeConformityError> partErrors = nodeConformityErrorsRepository.findByConformityIds(partIds);
                for (ArrNodeConformityError partError : partErrors) {
                    Integer conformityId = partError.getNodeConformity().getNodeConformityId();

                    List<ArrNodeConformityError> errorList = errors.get(conformityId);

                    if (errorList == null) {
                        errorList = new ArrayList<>();
                        errors.put(conformityId, errorList);
                    }

                    errorList.add(partError);
                }
            }

            for (ArrNodeConformity conformityInfo : conformityInfos) {

                ArrNodeConformityExt conformity = new ArrNodeConformityExt();
                BeanUtils.copyProperties(conformityInfo, conformity);

                conformity.setErrorList(errors.get(conformity.getNodeConformityId()));
                conformity.setMissingList(missings.get(conformity.getNodeConformityId()));

                result.put(conformityInfo.getNode().getNodeId(), conformity);
            }

        }

        return result;
    }

    /**
     * Provede úpravů (smazání) stavů uzlů podle pravidel.
     *
     * @param faVersionId       verze nodů
     * @param nodeIds           seznam id nodů, od kterých se má prohledávat
     * @param nodeTypeOperation typ operace
     * @param createDescItems   hodnoty atributů k vytvoření
     * @param updateDescItems   hodnoty atributů k upravení
     * @param deleteDescItems   hodnoty atributů ke smazání
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
     * Provede vytvoření stavů uzlů podle pravidel u nové verze AP.
     *
     * @param findingAidVersion verze AP
     */
    public void conformityInfoAll(final ArrFindingAidVersion findingAidVersion) {

        ArrNode rootNode = findingAidVersion.getRootLevel().getNode();

        List<ArrNode> nodes = nodeRepository
                .findNodesByDirection(rootNode, findingAidVersion, RelatedNodeDirection.ALL);

        nodes.add(rootNode);

        if (!nodes.isEmpty()) {
            updateConformityInfoService.updateInfoForNodesAfterCommit(nodes, findingAidVersion);
        }
    }

    /**
     * Zjistí podle pravidel dopad na změnu stavů uzlů.
     *
     * @param faVersionId       verze nodů
     * @param nodeTypeOperation typ operace
     * @param createDescItems   hodnoty atributů k vytvoření
     * @param updateDescItems   hodnoty atributů k upravení
     * @param deleteDescItems   hodnoty atributů ke smazání
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
            List<ArrNodeConformity> deleteInfos = nodeConformityInfoRepository
                    .findByNodesAndVersion(deleteNodes, version);

            deleteConformityInfo(deleteInfos);
            setVersionConformityInfo(null, null, version);
            updateConformityInfoService.updateInfoForNodesAfterCommit(deleteNodes, version);
        }
    }

    /**
     * Smaže všechny vybrané stavy.
     *
     * @param infos stavy ke smazání
     */
    private void deleteConformityInfo(final Collection<ArrNodeConformity> infos) {

        if (CollectionUtils.isNotEmpty(infos)) {
            List<ArrNodeConformityMissing> missing = nodeConformityMissingRepository
                    .findByNodeConformityInfos(infos);
            if (CollectionUtils.isNotEmpty(missing)) {
                nodeConformityMissingRepository.delete(missing);
            }

            List<ArrNodeConformityError> errors = nodeConformityErrorsRepository.findByNodeConformityInfos(infos);
            if (CollectionUtils.isNotEmpty(errors)) {
                nodeConformityErrorsRepository.delete(errors);
            }

            nodeConformityInfoRepository.delete(infos);
        }
    }

    /**
     * Získání rozšířených typů hodnot atributů se specifikacemi.
     * Používá výchozí strategie
     *
     * @param findingAidVersionId identifikátor verze archivní pomůcky
     * @param nodeId              identifikátor uzlu
     * @return seznam typů hodnot atributů se specifikacemi
     */
    public List<RulDescItemTypeExt> getDescriptionItemTypes(final Integer findingAidVersionId,
                                                            final Integer nodeId) {
        Assert.notNull(findingAidVersionId);
        Assert.notNull(nodeId);

        Set<String> strategies = elzaRules.getStrategies(findingAidVersionId);

        ArrFindingAidVersion version = findingAidVersionRepository.findOne(findingAidVersionId);

        if (version == null) {
            throw new IllegalArgumentException("Verze archivni pomucky neexistuje");
        }

        ArrNode node = nodeRepository.findOne(nodeId);

        if (node == null) {
            throw new IllegalArgumentException("Uzel neexistuje");
        }

        return getDescriptionItemTypes(version, node, strategies);
    }

    /**
     * Získání rozšířených typů hodnot atributů se specifikacemi.
     *
     * @param version    verze archivní pomůcky
     * @param node       uzel
     * @param strategies seznam strategií
     * @return seznam typů hodnot atributů se specifikacemi
     */
    public List<RulDescItemTypeExt> getDescriptionItemTypes(final ArrFindingAidVersion version,
                                                            final ArrNode node,
                                                            final Set<String> strategies) {

        ArrLevel level = levelRepository.findNodeInRootTreeByNodeId(node, version.getRootLevel().getNode(),
                version.getLockChange());

        Assert.notNull(strategies);
        List<RulDescItemType> itemTypeList = descItemTypeRepository.findAll();

        List<RulDescItemTypeExt> rulDescItemTypeExtList = createExt(itemTypeList);

        // projde všechny typy atributů
        for (RulDescItemTypeExt rulDescItemTypeExt : rulDescItemTypeExtList) {

            rulDescItemTypeExt.setType(RulDescItemType.Type.IMPOSSIBLE);
            rulDescItemTypeExt.setRepeatable(true);

            // projde všechny podmínky typů
            for (RulDescItemConstraint rulDescItemConstraint : rulDescItemTypeExt.getRulDescItemConstraintList()) {
                if (rulDescItemConstraint.getRepeatable() != null && rulDescItemConstraint.getRepeatable()
                        .equals(false)) {
                    rulDescItemTypeExt.setRepeatable(false);
                    break;
                }
            }

            // projde všechny specifikace typů atributů
            for (RulDescItemSpecExt rulDescItemSpecExt : rulDescItemTypeExt.getRulDescItemSpecList()) {

                rulDescItemSpecExt.setType(RulDescItemSpec.Type.IMPOSSIBLE);
                rulDescItemSpecExt.setRepeatable(true);

                // projde všechny podmínky specifikací
                for (RulDescItemConstraint rulDescItemConstraint : rulDescItemSpecExt.getRulDescItemConstraintList()) {
                    if (rulDescItemConstraint.getRepeatable() != null && rulDescItemConstraint.getRepeatable()
                            .equals(false)) {
                        rulDescItemSpecExt.setRepeatable(false);
                        break;
                    }
                }
            }
        }

        return rulesExecutor.executeDescItemTypesRules(level, rulDescItemTypeExtList, version, strategies);
    }

    /**
     * Vytvoření seznamu rozšířených typů hodnot atributů se specifikacemi podle seznamu typů hodnot atributů.
     *
     * TODO: refactor
     *
     * @param itemTypeList seznam typů hodnot atributů
     * @return seznam typů hodnot atributů se specifikacemi
     */
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

}
