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

import cz.tacr.elza.domain.*;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.config.ConfigRules;
import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.api.vo.NodeTypeOperation;
import cz.tacr.elza.api.vo.RelatedNodeDirection;
import cz.tacr.elza.asynchactions.UpdateConformityInfoService;
import cz.tacr.elza.controller.factory.ExtendedObjectsFactory;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.vo.DataValidationResult;
import cz.tacr.elza.drools.RulesExecutor;
import cz.tacr.elza.exception.LockVersionChangeException;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeConformityErrorRepository;
import cz.tacr.elza.repository.NodeConformityMissingRepository;
import cz.tacr.elza.repository.NodeConformityRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.utils.ObjectListIterator;


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
    private ConfigRules elzaRules;

    @Autowired
    private LevelRepository levelRepository;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private FundVersionRepository fundVersionRepository;
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

    /**
     * Provede validaci atributů vybraného uzlu a nastaví jejich validační hodnoty.
     *
     * @param faLevelId   id uzlu
     * @param fundVersionId id verze
     * @return stav validovaného uzlu
     */
    public ArrNodeConformityExt setConformityInfo(final Integer faLevelId, final Integer fundVersionId) {
        Assert.notNull(faLevelId);
        Assert.notNull(fundVersionId);

        ArrLevel level = levelRepository.findOne(faLevelId);
        Integer nodeId = level.getNode().getNodeId();

        ArrNode nodeBeforeValidation = nodeRepository.getOne(nodeId);
        Integer nodeVersionBeforeValidation = nodeBeforeValidation.getVersion();

        ArrFundVersion version = fundVersionRepository.findOne(fundVersionId);

        if (!arrangementService.validLevelInVersion(level, version)) {
            throw new IllegalArgumentException("Level s id " + faLevelId + " nespadá do verze s id " + fundVersionId);
        }

        List<DataValidationResult> validationResults = rulesExecutor.executeDescItemValidationRules(level, version);
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
                                                          final ArrFundVersion version,
                                                          final List<DataValidationResult> validationResults) {

        ArrNodeConformity conformityInfo = nodeConformityInfoRepository
                .findByNodeAndFundVersion(level.getNode(), version);

        if (conformityInfo != null && conformityInfo.getState().equals(ArrNodeConformity.State.OK)) {
            conformityInfo.setDate(new Date());
        } else {
            if (conformityInfo != null) {
                deleteConformityInfo(Arrays.asList(conformityInfo));
            }
            conformityInfo = new ArrNodeConformity();
            conformityInfo.setNode(level.getNode());
            conformityInfo.setFundVersion(version);
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
                        missing.setPolicyType(validationResult.getPolicyType());
                        nodeConformityMissingRepository.save(missing);
                        break;
                    case ERROR:
                        ArrNodeConformityError error = new ArrNodeConformityError();
                        error.setNodeConformity(conformityInfo);
                        error.setDescItem(validationResult.getDescItem());
                        error.setDescription(validationResult.getMessage());
                        error.setPolicyType(validationResult.getPolicyType());
                        nodeConformityErrorsRepository.save(error);
                        break;
                }
            }

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
                                                                           final ArrFundVersion version) {
        Map<Integer, ArrNodeConformityExt> result = new HashMap<>();

        if (nodeIds.size() == 0) {
            return result;
        }

        ObjectListIterator<Integer> iteratorNodeIds = new ObjectListIterator<>(nodeIds);

        while (iteratorNodeIds.hasNext()) {
            List<Integer> partNodeIds = iteratorNodeIds.next();

            List<ArrNodeConformity> conformityInfos = nodeConformityInfoRepository
                    .findByNodeIdsAndFundVersion(partNodeIds, version);

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
     * @param fundVersionId       verze nodů
     * @param nodeIds           seznam id nodů, od kterých se má prohledávat
     * @param nodeTypeOperation typ operace
     * @param createDescItems   hodnoty atributů k vytvoření
     * @param updateDescItems   hodnoty atributů k upravení
     * @param deleteDescItems   hodnoty atributů ke smazání
     * @return seznam dopadů
     */
    public Set<RelatedNodeDirection> conformityInfo(final Integer fundVersionId,
                                                    final Collection<Integer> nodeIds,
                                                    final NodeTypeOperation nodeTypeOperation,
                                                    final List<ArrDescItem> createDescItems,
                                                    final List<ArrDescItem> updateDescItems,
                                                    final List<ArrDescItem> deleteDescItems) {

        Set<RelatedNodeDirection> impactOnConformityInfo = getImpactOnConformityInfo(fundVersionId, nodeTypeOperation,
                createDescItems, updateDescItems, deleteDescItems);

        deleteConformityInfo(fundVersionId, nodeIds, impactOnConformityInfo);

        return impactOnConformityInfo;
    }

    /**
     * Provede vytvoření stavů uzlů podle pravidel u nové verze AP.
     *
     * @param fundVersion verze AP
     */
    public void conformityInfoAll(final ArrFundVersion fundVersion) {

        ArrNode rootNode = fundVersion.getRootNode();

        List<ArrNode> nodes = nodeRepository
                .findNodesByDirection(rootNode, fundVersion, RelatedNodeDirection.ALL);

        nodes.add(rootNode);

        if (!nodes.isEmpty()) {
            updateConformityInfoService.updateInfoForNodesAfterCommit(nodes, fundVersion);
        }
    }

    /**
     * Zjistí podle pravidel dopad na změnu stavů uzlů.
     *
     * @param fundVersionId       verze nodů
     * @param nodeTypeOperation typ operace
     * @param createDescItems   hodnoty atributů k vytvoření
     * @param updateDescItems   hodnoty atributů k upravení
     * @param deleteDescItems   hodnoty atributů ke smazání
     * @return seznam dopadů
     */
    private Set<RelatedNodeDirection> getImpactOnConformityInfo(final Integer fundVersionId,
                                                                final NodeTypeOperation nodeTypeOperation,
                                                                final List<ArrDescItem> createDescItems,
                                                                final List<ArrDescItem> updateDescItems,
                                                                final List<ArrDescItem> deleteDescItems) {

        ArrFundVersion version = fundVersionRepository.findOne(fundVersionId);

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
     * @param fundVersionId      verze nodů
     * @param nodeIds          seznam id nodů, od kterých se má prohledávat
     * @param deleteDirections směry prohledávání (null pokud se mají smazat stavy zadaných nodů .
     */
    private void deleteConformityInfo(final Integer fundVersionId,
                                      final Collection<Integer> nodeIds,
                                      final Collection<RelatedNodeDirection> deleteDirections) {
        Assert.notNull(fundVersionId);
        Assert.notEmpty(nodeIds);

        List<ArrNode> nodes = nodeRepository.findAll(nodeIds);
        ArrFundVersion version = fundVersionRepository.findOne(fundVersionId);

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
                    .findByNodesAndFundVersion(deleteNodes, version);

            deleteConformityInfo(deleteInfos);
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
     * @param fundVersionId identifikátor verze archivní pomůcky
     * @param nodeId              identifikátor uzlu
     * @return seznam typů hodnot atributů se specifikacemi
     */
    public List<RulDescItemTypeExt> getDescriptionItemTypes(final Integer fundVersionId,
                                                            final Integer nodeId) {
        Assert.notNull(fundVersionId);
        Assert.notNull(nodeId);

        ArrFundVersion version = fundVersionRepository.findOne(fundVersionId);

        if (version == null) {
            throw new IllegalArgumentException("Verze archivni pomucky neexistuje");
        }

        ArrNode node = nodeRepository.findOne(nodeId);

        if (node == null) {
            throw new IllegalArgumentException("Uzel neexistuje");
        }

        return getDescriptionItemTypes(version, node);
    }

    /**
     * Získání rozšířených typů hodnot atributů se specifikacemi.
     *
     * @param version    verze archivní pomůcky
     * @param node       uzel
     * @return seznam typů hodnot atributů se specifikacemi
     */
    public List<RulDescItemTypeExt> getDescriptionItemTypes(final ArrFundVersion version,
                                                            final ArrNode node) {

        ArrLevel level = levelRepository.findNodeInRootTreeByNodeId(node, version.getRootNode(),
                version.getLockChange());

        List<RulDescItemTypeExt> rulDescItemTypeExtList = getAllDescriptionItemTypes();

        return rulesExecutor.executeDescItemTypesRules(level, rulDescItemTypeExtList, version);
    }

    /**
     * Získání všech hodnot typů atributů se specifikacemi.
     *
     * @return typy hodnot atributů
     */
    public List<RulDescItemTypeExt> getAllDescriptionItemTypes() {
        List<RulDescItemType> itemTypeList = descItemTypeRepository.findAll();

        List<RulDescItemTypeExt> rulDescItemTypeExtList = createExt(itemTypeList);

        // projde všechny typy atributů
        for (RulDescItemTypeExt rulDescItemTypeExt : rulDescItemTypeExtList) {

            rulDescItemTypeExt.setType(RulDescItemType.Type.IMPOSSIBLE);
            rulDescItemTypeExt.setRepeatable(true);

            // projde všechny specifikace typů atributů
            for (RulDescItemSpecExt rulDescItemSpecExt : rulDescItemTypeExt.getRulDescItemSpecList()) {

                rulDescItemSpecExt.setType(RulDescItemSpec.Type.IMPOSSIBLE);
                rulDescItemSpecExt.setRepeatable(true);

            }
        }
        return rulDescItemTypeExtList;
    }

    /**
     * Vytvoření seznamu rozšířených typů hodnot atributů se specifikacemi podle seznamu typů hodnot atributů.
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
                }
            }
            result.add(descItemTypeExt);
        }

        return result;
    }

}
