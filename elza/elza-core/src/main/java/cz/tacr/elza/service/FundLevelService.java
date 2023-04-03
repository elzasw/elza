package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.vo.NodeTypeOperation;
import cz.tacr.elza.domain.vo.RelatedNodeDirection;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.drools.DirectionLevel;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.repository.DaoRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.DaoSyncService.DaoDesctItemProvider;
import cz.tacr.elza.service.ItemService.FundContext;
import cz.tacr.elza.service.arrangement.DesctItemProvider;
import cz.tacr.elza.service.arrangement.MultipleItemChangeContext;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventDeleteNode;
import cz.tacr.elza.service.eventnotification.events.EventType;


/**
 * Servisní třída pro přesuny uzlů ve stromu.
 *
 * @since 18.01.2016
 */
@Service
public class FundLevelService {

    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private LevelRepository levelRepository;
    
    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private ArrangementCacheService arrangementCacheService;

    @Autowired
    private ArrangementInternalService arrangementInternalService;

    @Autowired
    private RuleService ruleService;
    
    @Autowired
    DaoRepository daoRepository;

    @Autowired
    DaoService daoService;

    @Autowired
    private DescItemRepository descItemRepository;
    
    @Autowired
    private IEventNotificationService eventNotificationService;
    
    @Autowired
    private DescriptionItemService descriptionItemService;
    
    @Autowired
    private NodeCacheService nodeCacheService;

    @Autowired
    private StaticDataService staticDataService;

    /**
     * Přesunutí uzlů před jiný.
     *
     * @param version             verze
     * @param staticNode          statický uzel (za, před, pod který přesouváme)
     * @param staticParentNode    rodič statického uzlu
     * @param transportNodes      seznam uzlů, které přesouváme
     * @param transportParentNode rodič přesouvaných uzlů
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void moveLevelsBefore(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion version,
                                 final ArrNode staticNode,
                                 final ArrNode staticParentNode,
                                 final Collection<ArrNode> transportNodes,
                                 final ArrNode transportParentNode) {

        Assert.notEmpty(transportNodes, "Musí být vyplněn alespoň jedna JP");

        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.MOVE_LEVEL, staticNode);
        arrangementService.setPrimaryNode(change, staticNode);

        ArrLevel staticLevelParent = arrangementService.lockNode(staticParentNode, version, change);
        ArrLevel transportLevelParent = transportParentNode.equals(staticParentNode)
                                        ? staticLevelParent: arrangementService.lockNode(transportParentNode, version, change);
        ArrLevel staticLevel = levelRepository.findByNode(staticNode, version.getLockChange());
        Assert.notNull(staticLevel, "Referenční level musí být vyplněn");


        List<ArrLevel> transportLevels = new ArrayList<>(transportNodes.size());
        for (ArrNode transportNode : transportNodes) {
            ArrLevel transportLevel = transportNode.equals(staticParentNode)
                                      ? staticLevelParent : arrangementService.lockNode(transportNode, version, change);

            if (transportLevel.equals(staticLevel)) {
                throw new SystemException("Nelze vložit záznam na stejné místo ve stromu");
            }

            transportLevels.add(transportLevel);
        }

        // vkládaný nesmí být rodičem uzlu za který ho vkládám
        checkCycle(transportLevels.get(0), staticLevel);

        moveLevelBefore(version, staticLevel, transportLevels, transportLevelParent, change);
    }

    /**
     * Provede přesunutí uzlů. Všechny nutné uzly musejí být již uzamčeny, zde neprobíhá kontrola zámků, pouze přesuny.
     *  @param version              verze stromu
     * @param staticLevel          statický uzel (za, před, pod který přesouváme)
     * @param transportLevels      seznam uzlů, které přesouváme
     * @param transportLevelParent rodič přesouvaných uzlů
     * @param change
     */
    private void moveLevelBefore(final ArrFundVersion version,
                                 final ArrLevel staticLevel,
                                 final List<ArrLevel> transportLevels,
                                 final ArrLevel transportLevelParent, final ArrChange change) {
        Integer versionId = version.getFundVersionId();
        arrangementService.isValidAndOpenVersion(version);

        Set<Integer> transportNodeIds = new HashSet<>();
        transportLevels.forEach((t) -> transportNodeIds.add(t.getNode().getNodeId()));

        //zbydou pouze ty, které jsou pod přesouvanými
        List<ArrLevel> nodesToShiftUp = nodesToShift(transportLevels.get(0));
        nodesToShiftUp.removeAll(transportLevels);


        List<ArrLevel> nodesToShiftDown = nodesToShift(staticLevel);
        nodesToShiftDown.add(0, staticLevel);
        nodesToShiftDown.removeAll(transportLevels);

        List<ArrLevel> updatedLevels = new ArrayList<>();

        int targetPosition = staticLevel.getPosition();
        if (transportLevelParent.getNode().equals(staticLevel.getNodeParent())) {
            ruleService.conformityInfo(versionId, transportNodeIds, NodeTypeOperation.DISCONNECT_NODE_LOCAL,
                    null, null, null);

            if (transportLevels.get(0).getPosition() > staticLevel.getPosition()) {
                updatedLevels.addAll(placeLevels(transportLevels, staticLevel.getNodeParent(),
                                                 change, targetPosition));
                targetPosition += transportLevels.size();
                updatedLevels.addAll(placeLevels(nodesToShiftDown, staticLevel.getNodeParent(),
                                                 change, targetPosition));
            } else {
                //posun up
                updatedLevels.addAll(shiftNodesWithCollection(nodesToShiftUp, transportLevels,
                                                              transportLevels.get(0).getPosition(),
                                                              staticLevel.getNodeParent(), change, staticLevel, null));
            }
        } else {
            ruleService.conformityInfo(versionId, transportNodeIds, NodeTypeOperation.DISCONNECT_NODE,
                    null, null, null);

            updatedLevels.addAll(shiftNodes(nodesToShiftUp, change, transportLevels.get(0).getPosition()));
            updatedLevels.addAll(placeLevels(transportLevels, staticLevel.getNodeParent(),
                                             change, targetPosition));
            targetPosition += transportLevels.size();
            updatedLevels.addAll(shiftNodes(nodesToShiftDown, change, targetPosition));
        }

        updatedLevels = levelRepository.saveAll(updatedLevels);
        levelRepository.flush();

        if (transportLevelParent.getNode().equals(staticLevel.getNodeParent())) {
            ruleService.conformityInfo(versionId, transportNodeIds, NodeTypeOperation.CONNECT_NODE_LOCAL,
                    null, null, null);
        } else {
            ruleService.conformityInfo(versionId, transportNodeIds, NodeTypeOperation.CONNECT_NODE,
                    null, null, null);
        }

        entityManager.flush(); //aktualizace verzí v nodech

        eventNotificationService.publishEvent(
                EventFactory.createMoveEvent(EventType.MOVE_LEVEL_BEFORE, staticLevel, transportLevels, version));


    }

    public ArrLevel findLevelByNode(final ArrNode staticNode) {
        return levelRepository.findByNode(staticNode, null);
    }

    /**
     * Přesunutí uzlů za jiný.
     *
     * @param version             verze
     * @param staticNode          statický uzel (za, před, pod který přesouváme)
     * @param staticParentNode    rodič statického uzlu
     * @param transportNodes      seznam uzlů, které přesouváme
     * @param transportParentNode rodič přesouvaných uzlů
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void moveLevelsAfter(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion version,
                                final ArrNode staticNode,
                                final ArrNode staticParentNode,
                                final List<ArrNode> transportNodes,
                                final ArrNode transportParentNode) {
        Assert.notEmpty(transportNodes, "Musí být vyplněn alespoň jedna JP");

        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.MOVE_LEVEL, staticNode);

        ArrLevel staticLevelParent = arrangementService.lockNode(staticParentNode, version, change);
        ArrLevel transportLevelParent = transportParentNode.equals(staticParentNode)
                                        ? staticLevelParent : arrangementService.lockNode(transportParentNode, version, change);
        ArrLevel staticLevel = levelRepository.findByNode(staticNode, version.getLockChange());
        Assert.notNull(staticLevel, "Referenční level musí být vyplněn");


        List<ArrLevel> transportLevels = new ArrayList<>(transportNodes.size());
        for (ArrNode transportNode : transportNodes) {
            ArrLevel transportLevel = transportNode.equals(staticParentNode)
                                      ? staticLevelParent : arrangementService.lockNode(transportNode, version, change);

            if (transportLevel.equals(staticLevel)) {
                throw new SystemException("Nelze vložit záznam na stejné místo ve stromu");
            }

            transportLevels.add(transportLevel);
        }

        // vkládaný nesmí být rodičem uzlu za který ho vkládám
        checkCycle(transportLevels.get(0), staticLevel);

        moveLevelAfter(version, staticLevel, transportLevels, transportLevelParent, change);
    }


    /**
     * Provede přesunutí uzlů. Všechny nutné uzly musejí být již uzamčeny, zde neprobíhá kontrola zámků, pouze přesuny.
     *  @param version              verze stromu
     * @param staticLevel          statický uzel (za, před, pod který přesouváme)
     * @param transportLevels      seznam uzlů, které přesouváme
     * @param transportLevelParent rodič přesouvaných uzlů
     * @param change
     */
    private void moveLevelAfter(final ArrFundVersion version,
                                final ArrLevel staticLevel,
                                final List<ArrLevel> transportLevels,
                                final ArrLevel transportLevelParent, final ArrChange change) {

        Integer versionId = version.getFundVersionId();
        arrangementService.isValidAndOpenVersion(version);

        Set<Integer> transportNodeIds = new HashSet<>();
        transportLevels.forEach((t) -> transportNodeIds.add(t.getNode().getNodeId()));

        //zbydou pouze ty, které jsou pod přesouvanými
        List<ArrLevel> nodesToShiftUp = nodesToShift(transportLevels.get(0));
        nodesToShiftUp.removeAll(transportLevels);

        List<ArrLevel> nodesToShiftDown = nodesToShift(staticLevel);
        nodesToShiftDown.removeAll(transportLevels);

        List<ArrLevel> updatedLevels = new ArrayList<>();

        int targetPositon = staticLevel.getPosition() + 1;
        if (transportLevelParent.getNode().equals(staticLevel.getNodeParent())) {
            ruleService.conformityInfo(versionId, transportNodeIds, NodeTypeOperation.DISCONNECT_NODE_LOCAL,
                    null, null, null);

            if (transportLevels.get(0).getPosition() > staticLevel.getPosition()) {
                updatedLevels.addAll(placeLevels(transportLevels, staticLevel.getNodeParent(), 
                                                 change, targetPositon));
                targetPositon += transportLevels.size();
                updatedLevels.addAll(placeLevels(nodesToShiftDown, staticLevel.getNodeParent(),
                                                 change, targetPositon));
            } else {
                //posun up
                updatedLevels.addAll(shiftNodesWithCollection(nodesToShiftUp, transportLevels, transportLevels.get(0)
                        .getPosition(),
                                                              staticLevel.getNodeParent(), change, null, staticLevel));
            }
        } else {
            ruleService.conformityInfo(versionId, transportNodeIds, NodeTypeOperation.DISCONNECT_NODE,
                    null, null, null);

            updatedLevels.addAll(shiftNodes(nodesToShiftUp, change, transportLevels.get(0).getPosition()));
            updatedLevels.addAll(placeLevels(transportLevels, staticLevel.getNodeParent(),
                                             change, targetPositon));
            targetPositon += transportLevels.size();
            updatedLevels.addAll(shiftNodes(nodesToShiftDown, change, targetPositon));
        }

        updatedLevels = levelRepository.saveAll(updatedLevels);
        levelRepository.flush();

        if (transportLevelParent.getNode().equals(staticLevel.getNodeParent())) {
            ruleService.conformityInfo(versionId, transportNodeIds, NodeTypeOperation.CONNECT_NODE_LOCAL,
                    null, null, null);
        } else {
            ruleService.conformityInfo(versionId, transportNodeIds, NodeTypeOperation.CONNECT_NODE,
                    null, null, null);
        }

        entityManager.flush(); //aktualizace verzí v nodech
        eventNotificationService.publishEvent(
                EventFactory.createMoveEvent(EventType.MOVE_LEVEL_AFTER, staticLevel, transportLevels, version));
    }


    /**
     * Přesunutí uzlů pod jiný.
     *
     * @param version             verze
     * @param staticNode          statický uzel (za, před, pod který přesouváme)
     * @param transportNodes      seznam uzlů, které přesouváme
     * @param transportParentNode rodič přesouvaných uzlů
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void moveLevelsUnder(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion version,
                                final ArrNode staticNode,
                                final Collection<ArrNode> transportNodes,
                                final ArrNode transportParentNode) {

        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.MOVE_LEVEL, staticNode);

        ArrLevel staticLevel = arrangementService.lockNode(staticNode, version, change);
        if (!staticNode.getNodeId().equals(transportParentNode.getNodeId())) {
            arrangementService.lockNode(transportParentNode, version, change);
        }
        List<ArrLevel> transportLevels = new ArrayList<>(transportNodes.size());

        for (ArrNode transportNode : transportNodes) {
            if (transportNode.equals(staticNode)) {
                throw new SystemException("Nelze vložit záznam na stejné místo ve stromu");
            }

            ArrLevel transportLevel = arrangementService.lockNode(transportNode, version, change);
            transportLevels.add(transportLevel);

        }

        // vkládaný nesmí být rodičem uzlu za který ho vkládám
        checkCycle(transportLevels.get(0), staticLevel);

        moveLevelUnder(version, staticLevel, transportLevels, change);
    }


    /**
     * Provede přesunutí uzlů. Všechny nutné uzly musejí být již uzamčeny, zde neprobíhá kontrola zámků, pouze přesuny.
     *  @param version         verze stromu
     * @param staticLevel     statický uzel (za, před, pod který přesouváme)
     * @param transportLevels seznam uzlů, které přesouváme
     * @param change
     */
    private void moveLevelUnder(final ArrFundVersion version,
                                final ArrLevel staticLevel,
                                final List<ArrLevel> transportLevels, final ArrChange change) {

        Integer versionId = version.getFundVersionId();
        arrangementService.isValidAndOpenVersion(version);

        List<Integer> transportNodeIds = transportLevels.stream()
                .map(l -> l.getNodeId()).collect(Collectors.toList());

        ruleService.conformityInfo(versionId, transportNodeIds, NodeTypeOperation.DISCONNECT_NODE,
                null, null, null);

        List<ArrLevel> updatedLevels = new ArrayList<>();


        //zbydou pouze ty, které jsou pod přesouvanými
        List<ArrLevel> nodesToShiftUp = nodesToShift(transportLevels.get(0));
        nodesToShiftUp.removeAll(transportLevels);

        updatedLevels.addAll(shiftNodes(nodesToShiftUp, change, transportLevels.get(0).getPosition()));


        Integer maxPosition = levelRepository.findMaxPositionUnderParent(staticLevel.getNode());
        if (maxPosition == null) {
            maxPosition = 0;
        }

        updatedLevels.addAll(placeLevels(transportLevels, staticLevel.getNode(), change, maxPosition + 1));


        updatedLevels = levelRepository.saveAll(updatedLevels);
        levelRepository.flush();

        ruleService.conformityInfo(versionId, transportNodeIds, NodeTypeOperation.CONNECT_NODE, null, null, null);


        entityManager.flush(); //aktualizace verzí v nodech
        eventNotificationService.publishEvent(
                EventFactory.createMoveEvent(EventType.MOVE_LEVEL_UNDER, staticLevel, transportLevels, version));
    }

    /**
     * Kaskádově smaže všechny levely od počátečního
     * 
     * @param fundVersion
     *
     * @param baselevel
     *            počáteční level
     * @param deleteChange
     *            záznam o provedených změnách
     * @param allDeletedLevels
     *            list všech levelů, které se budou mazat
     * @param deleteLevelsWithAttachedDao
     *            povolit nebo zakázat mazání úrovně s objektem dao
     * @return List of modified levels
     */
    private List<ArrLevel> deleteLevelCascade(final ArrFundVersion fundVersion,
                                       final ArrLevel baselevel, final ArrChange deleteChange,
                                       final List<ArrLevel> allDeletedLevels,
                                       final boolean deleteLevelsWithAttachedDao) {
        List<ArrLevel> deletedLevels = new ArrayList<>();
        
        List<ArrLevel> childLevels = levelRepository
                .findByParentNodeAndDeleteChangeIsNullOrderByPositionAsc(baselevel.getNode());
        for (ArrLevel childLevel : childLevels) {
            deletedLevels.addAll(deleteLevelCascade(fundVersion, childLevel, deleteChange, allDeletedLevels, deleteLevelsWithAttachedDao));
        }

        ArrNode node = baselevel.getNode();
        // check if node is part of fund
        Validate.isTrue(node.getFundId().equals(fundVersion.getFundId()), "Node is not part of same fund");

        // check if connected Dao(type=Level) exists
        if (!deleteLevelsWithAttachedDao && daoRepository.existsDaoByNodeAndDaoTypeIsLevel(node.getNodeId())) {
            throw new SystemException("Uzel " + node.getNodeId() + " má připojený objekt dao typu LEVEL")
                    .set("nodeId", node.getNodeId());
        }

        for (ArrDescItem descItem : descItemRepository.findByNodeAndDeleteChangeIsNull(baselevel.getNode())) {
            descItem.setDeleteChange(deleteChange);
            descItemRepository.save(descItem);
        }

        daoService.deleteDaoLinkByNode(fundVersion, deleteChange, node);

        // vyhledani node, ktere odkazuji na mazany
        List<ArrDescItem> arrDescItemList = descItemRepository.findByUriDataNode(node);

        arrDescItemList = arrDescItemList.stream().map(i -> {
            entityManager.detach(i);
            return (ArrDescItem) HibernateUtils.unproxy(i);
        }).collect(Collectors.toList());

        for (ArrDescItem arrDescItem : arrDescItemList) {
            //pokud se item bude mazat, není potřeba u něj předělávat UriRef
            if (!allDeletedLevels.contains(levelRepository.findByNodeIdAndDeleteChangeIsNull(arrDescItem
                    .getNodeId()))) {
                ArrDataUriRef arrDataUriRef = new ArrDataUriRef((ArrDataUriRef) arrDescItem.getData());
                arrDataUriRef.setDataId(null);
                arrDataUriRef.setArrNode(null);
                arrDataUriRef.setDeletingProcess(true);
                arrDescItem.setData(arrDataUriRef);
                descriptionItemService.updateDescriptionItem(arrDescItem, fundVersion, deleteChange);
            }
        }

        // mark as deleted
        node.setLastUpdate(deleteChange.getChangeDate().toLocalDateTime());
        nodeRepository.save(node);

        baselevel.setDeleteChange(deleteChange);
        
        deletedLevels.add(baselevel);
        return deletedLevels;
    }

    /**
     * Provede smazání levelu.
     *
     * @param version          verze stromu
     * @param deleteNode       node ke smazání
     * @param deleteNodeParent rodič nodu ke smazání
     * @param deleteLevelsWithAttachedDao povolit nebo zakázat mazání úrovně s objektem dao
     */
    // Dává smysl, aby deleteNodeParent byl null?
    // Pravděpodobně by vždy měl být non-null - nemůžeme takto mazat kořen
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrLevel deleteLevel(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion version,
                                final ArrNode deleteNode,
                                final ArrNode deleteNodeParent,
                                final boolean deleteLevelsWithAttachedDao) {
        Assert.notNull(version, "Verze AS musí být vyplněna");
        Assert.notNull(deleteNode, "Mazané JP musí být vyplněna");

        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.DELETE_LEVEL, deleteNode);

        ArrLevel deleteLevel = arrangementService.lockNode(deleteNode, version, change);
        if (deleteNodeParent != null) {
            arrangementService.lockNode(deleteNodeParent, version, change);

            if (!Objects.equals(deleteLevel.getNodeIdParent(), deleteNodeParent.getNodeId())) {
                throw new SystemException(
                        "Uzel " + deleteNode.getNodeId() + " nemá rodiče s id " + deleteNodeParent.getNodeId());
            }
        }

        ruleService.conformityInfo(version.getFundVersionId(), Arrays.asList(deleteNode.getNodeId()),
                                   NodeTypeOperation.DELETE_NODE, null, null, null);

        List<ArrLevel> updatedLevels = new ArrayList<>();
        List<ArrLevel> shiftnodes = nodesToShift(deleteLevel);
        // Prepare list of sublevels
        List<ArrLevel> allSubLevels = levelRepository.findLevelsByDirection(deleteLevel, version,
                                                                            RelatedNodeDirection.DESCENDANTS);
        updatedLevels.addAll(deleteLevelCascade(version, deleteLevel, change,
                                                allSubLevels, deleteLevelsWithAttachedDao));
        updatedLevels.addAll(shiftNodes(shiftnodes, change, deleteLevel.getPosition()));

        levelRepository.saveAll(updatedLevels);
        levelRepository.flush();

        // drop nodeInfo for deleted levels
        Set<Integer> nodeIdsToRevalidate = new HashSet<>();
        nodeIdsToRevalidate.add(deleteNode.getNodeId());
        allSubLevels.forEach(l -> nodeIdsToRevalidate.add(l.getNodeId()));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                ruleService.revalidateNodes(version.getFundVersionId(), nodeIdsToRevalidate, null, null);
            }
        });

        eventNotificationService.publishEvent(new EventDeleteNode(EventType.DELETE_LEVEL,
                version.getFundVersionId(),
                deleteNode.getNodeId(),
                (deleteNodeParent != null) ? deleteNodeParent.getNodeId() : null));

        // return final level state
        return levelRepository.getOne(deleteLevel.getLevelId());
    }

    /**
     * Provede posunutí pozice uzlů. Až narazí na uzel, přes/za který mají být vloženy přesouvané uzly, přesune je a
     * pokračuje s dalšími.
     *
     * @param shiftNodes         seznam uzlů, kterým musí být změněna pozice (neobsahuje přesouvané uzly)
     * @param transferCollection seznam přesouvaných uzlů
     * @param firstPosition      pozice prvního posouvaného uzlu
     * @param parentNode         nadřazený uzel přesouvaných uzlů (pod kterým jsou přesouvány)
     * @param change             změna uzamčení uzlu
     * @param beforeLevel        pokud přesouváme před uzel, je nastaven uzel, před který chceme přesunout
     * @param afterLevel         pokud přesouváme za uzel, je nastaven uzel, za který chceme přesunout
     */
    private List<ArrLevel> shiftNodesWithCollection(final List<ArrLevel> shiftNodes,
                                         final List<ArrLevel> transferCollection,
                                         final int firstPosition,
                                         final ArrNode parentNode,
                                         final ArrChange change,
                                         @Nullable final ArrLevel beforeLevel,
                                         @Nullable final ArrLevel afterLevel) {
        Assert.isTrue((beforeLevel == null && afterLevel != null) || (beforeLevel != null && afterLevel == null), "Musí být platné");

        List<ArrLevel> updatedLevels = new ArrayList<>();

        boolean needInsert = true;
        int position = firstPosition;
        for (ArrLevel shiftNode : shiftNodes) {
            if (needInsert && beforeLevel != null && beforeLevel.equals(shiftNode)) {
                needInsert = false;
                updatedLevels.addAll(placeLevels(transferCollection, parentNode, change, position));
                position += transferCollection.size();
            }

            ArrLevel newNode = createNewLevelVersion(shiftNode, change);
            newNode.setPosition(position++);
            updatedLevels.add(newNode);


            if (needInsert && afterLevel != null && afterLevel.equals(shiftNode)) {
                needInsert = false;
                updatedLevels.addAll(placeLevels(transferCollection, parentNode, change, position));
                position += transferCollection.size();
            }
        }

        if (needInsert) {
            updatedLevels.addAll(placeLevels(transferCollection, parentNode, change, position));
        }
        return updatedLevels;
    }

    /**
     * Vloží do rodiče seznam uzlů na danou pozici
     *
     * @param transportLevels seznam přesouvaných uzlů
     * @param parentNode      nadřazený uzel
     * @param change          změna uzamčení
     * @param firstPosition   pozice prvního posouvaného uzlu
     */
    private List<ArrLevel> placeLevels(final List<ArrLevel> transportLevels, final ArrNode parentNode,
                           final ArrChange change, final int firstPosition) {
        List<ArrLevel> ret = new ArrayList<>(transportLevels.size());

        int position = firstPosition;

        for (ArrLevel transportLevel : transportLevels) {
            ArrLevel newLevel = createNewLevelVersion(transportLevel, change);
            newLevel.setNodeParent(parentNode);
            newLevel.setPosition(position);
            ret.add(newLevel);
            position++;
        }

        return ret;
    }

    @Transactional(value = TxType.MANDATORY)
    @AuthMethod(permission = { UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR,
            UsrPermission.Permission.FUND_ARR_NODE })
    public ArrLevel addLevelUnder(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                  @AuthParam(type = AuthParam.Type.NODE) final ArrNode staticNodeParent,
                                  @Nullable final String scenarionName,
                                  @Nullable final DesctItemProvider desctItemProvider,
                                  @Nullable final String uuid,
                                  @Nullable ArrChange change) {
        Validate.notNull(staticNodeParent, "Rodič JP musí být vyplněn");

        if (change == null) {
            change = arrangementInternalService.createChange(ArrChange.Type.ADD_LEVEL, staticNodeParent);
        }
        final ArrLevel baseLevel = arrangementService.lockNode(staticNodeParent, fundVersion, change);
        Validate.notNull(baseLevel, "Referenční level musí být vyplněn");

        List<ArrLevel> levels = addLevelUnder(fundVersion, baseLevel, 1,
                                              uuid != null ? Collections.singletonList(uuid) : null,
                                              change);
        Validate.notEmpty(levels, "Level musí být vyplněn");

        ArrLevel newLevel = levels.get(0);

        ScenarioOfNewLevel scenario;
        if (StringUtils.isNotBlank(scenarionName)) {
            scenario = descriptionItemService
                    .getDescriptionItamsOfScenario(scenarionName, baseLevel,
                                                   AddLevelDirection.CHILD.getDirectionLevel(), fundVersion);
        } else {
            scenario = null;
        }

        createItemsForNewLevel(fundVersion, baseLevel, newLevel, change, scenario,
                               null, desctItemProvider);

        // send notification about new level
        eventNotificationService.publishEvent(EventFactory.createAddNodeEvent(EventType.ADD_LEVEL_UNDER, fundVersion,
                                                                              baseLevel, newLevel));

        return newLevel;
    }


    /**
     * Vloží nový uzel do stromu. Podle směru zjistí pozici, posune případné
     * sourozence a vloží uzel.
     *
     * Metoda pošle notifikaci o přidání uzlu.
     * 
     * @param version
     *            verze stromu
     * @param staticNode
     *            Statický uzel (za/před/pod který přidáváme)
     * @param staticNodeParent
     *            Rodič statického uzlu (za/před/pod který přidáváme)
     * @param direction
     *            směr přidávání
     * @param scenarionName
     *            Název scénáře, ze kterého se mají převzít výchozí hodnoty
     *            atributů.
     * @param descItemCopyTypes
     *            id typů atributu, které budou zkopírovány z uzlu přímo nadřazeným
     *            nad přidaným uzlem (jeho mladší sourozenec).
     * @param count
     *            počet přidaných úrovní (pokud je null, přidáme jeden)
     * @param uuids
     *            seznam UUID pro nové uzly, může být null
     */
    @Transactional(value = TxType.MANDATORY)
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR, UsrPermission.Permission.FUND_ARR_NODE})
    public List<ArrLevel> addNewLevel(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                      final ArrNode baseNode,
                                @AuthParam(type = AuthParam.Type.NODE) final ArrNode staticNodeParent,
                                final AddLevelDirection direction,
                                @Nullable final String scenarionName,
                                final Set<RulItemType> descItemCopyTypes,
                                @Nullable final DesctItemProvider desctItemProvider,
                                @Nullable final Integer countNewLevel,
                                @Nullable final List<String> uuids) {

        Validate.notNull(baseNode, "Refereční JP musí být vyplněna");
        Validate.notNull(staticNodeParent, "Rodič JP musí být vyplněn");

        ArrNode parentNode;
        if (direction == AddLevelDirection.CHILD) {
            parentNode = baseNode;
        } else {
            parentNode = staticNodeParent;
        }

        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.ADD_LEVEL, parentNode);

        final ArrLevel parentLevel = arrangementService.lockNode(parentNode, fundVersion, change);
        Validate.notNull(parentLevel, "Referenční level musí být vyplněn");

        int count = countNewLevel == null? 1 : countNewLevel;

        StaticDataProvider sdp = this.staticDataService.getData();
        FundContext fundContext = FundContext.newInstance(fundVersion, arrangementService, sdp);

        ArrLevel baseLevel;
        List<ArrLevel> levels = new ArrayList<>(count);
        switch (direction){
            case CHILD:
                levels = addLevelUnder(fundVersion, parentLevel, count, uuids, change);
                baseLevel = parentLevel;
                break;
            case BEFORE:
            case AFTER:
                levels = addLevelBeforeAfter(fundVersion, baseNode, parentLevel, direction, count, change);
                baseLevel = levelRepository.findByNode(baseNode, fundVersion.getLockChange());
                Validate.notNull(baseLevel, "Úroveň pro refereční JP musí existovat");
                break;
            default:
                throw new IllegalStateException("Neznámý typ směru přidání uzlu " + direction.name());
        }

        Validate.isTrue(levels.size() == count, "Level musí být vyplněn");

        // prepare source items to copy
        List<ArrDescItem> copyDescItems = null;
        if (CollectionUtils.isNotEmpty(descItemCopyTypes)) {
            ArrLevel firstLevel = levels.get(0);
            ArrLevel olderSibling = levelRepository.findOlderSibling(firstLevel, fundVersion.getLockChange());
            if (olderSibling != null) {
                copyDescItems = descItemRepository
                        .findOpenByNodeAndTypes(olderSibling.getNode(), descItemCopyTypes);
            }
        }

        // prepare scenario
        ScenarioOfNewLevel scenario;
        if (StringUtils.isNotBlank(scenarionName)) {
            scenario = descriptionItemService
                    .getDescriptionItamsOfScenario(scenarionName, baseLevel,
                                                   direction.getDirectionLevel(), fundVersion);
        } else {
            scenario = null;
        }

        MultipleItemChangeContext changeContext = descriptionItemService.createChangeContext(fundVersion.getFundVersionId());
        List<Integer> nodeIds = new ArrayList<>(levels.size());
        for (ArrLevel newLevel : levels) {
            
            createItemsForNewLevel(fundContext, changeContext, change, newLevel, scenario, copyDescItems,
                                   desctItemProvider);

            nodeIds.add(newLevel.getNodeId());

        }
        changeContext.flush();

        ruleService.conformityInfo(fundVersion.getFundVersionId(),
                                   nodeIds,
                                   NodeTypeOperation.CREATE_NODE, null, null, null);

        entityManager.flush(); //aktualizace verzí v nodech

        ArrLevel nextBaseLevel = baseLevel;
        for (ArrLevel newLevel : levels) {
            eventNotificationService.publishEvent(EventFactory.createAddNodeEvent(direction.getEventType(), fundVersion,
                                                                                  nextBaseLevel, newLevel));
            // při přidání AFTER by se měla změnit aktuální ArrLevel na předchozí
            if (direction == AddLevelDirection.AFTER) {
                nextBaseLevel = newLevel;
            }
        }

        return levels;
    }

    /**
     * Create items for new level
     * 
     * @param fundVersion
     * @param direction
     * @param baseLevel
     * @param newLevel
     * @param change
     * @param scenario
     *            might be null
     * @param copyDescItems
     *            might be null
     * @param desctItemProvider
     *            might be null
     */
    private void createItemsForNewLevel(ArrFundVersion fundVersion,
                                        ArrLevel baseLevel,
                                        ArrLevel newLevel,
                                        ArrChange change,
                                        @Nullable ScenarioOfNewLevel scenario,
                                        @Nullable List<ArrDescItem> copyDescItems,
                                        DesctItemProvider desctItemProvider) {
        StaticDataProvider sdp = this.staticDataService.getData();
        FundContext fundContext = FundContext.newInstance(fundVersion, arrangementService, sdp);

        MultipleItemChangeContext changeContext = descriptionItemService.createChangeContext(fundVersion.getFundVersionId());

        createItemsForNewLevel(fundContext, changeContext, change, newLevel, scenario, copyDescItems,
                               desctItemProvider);

        changeContext.flush();

        ruleService.conformityInfo(fundVersion.getFundVersionId(),
                                   Arrays.asList(newLevel.getNode().getNodeId()),
                                   NodeTypeOperation.CREATE_NODE, null, null, null);

        entityManager.flush(); //aktualizace verzí v nodech
    }

    private List<ArrDescItem> createItemsForNewLevel(FundContext fundContext,
                                        MultipleItemChangeContext changeContext, ArrChange change, ArrLevel newLevel,
                                        ScenarioOfNewLevel scenario, List<ArrDescItem> copyDescItems,
                                        DesctItemProvider desctItemProvider) {
        List<ArrDescItem> items = new ArrayList<>();

        if (scenario != null) {
            // Set of items type to copy from other descItems
            Set<Integer> copyItemTypeIds;
            if (CollectionUtils.isNotEmpty(copyDescItems)) {
                copyItemTypeIds = copyDescItems.stream().map(ArrDescItem::getItemTypeId).collect(Collectors.toSet());
            } else {
                copyItemTypeIds = Collections.emptySet();
            }

            for (ArrDescItem srcDescItem : scenario.getDescItems()) {
                //pokud se má typ kopírovat z předchozího uzlu, nebudeme ho vkládat ze scénáře
                if (copyItemTypeIds.contains(srcDescItem.getItemType().getItemTypeId())) {
                    continue;
                }

                // Duplicate descItem
                ArrDescItem descItem = new ArrDescItem(srcDescItem);
                descItem.setNode(newLevel.getNode());
                descItem.setCreateChange(change);
                descItem.setDeleteChange(null);
                descItem.setDescItemObjectId(arrangementService.getNextDescItemObjectId());

                ArrDescItem descItemCreated = descriptionItemService
                        .createDescriptionItemWithData(descItem, change,
                                                       fundContext, changeContext, items);
                arrangementCacheService.createDescItem(descItemCreated, changeContext);
                items.add(descItemCreated);
            }
        }
        
        if (CollectionUtils.isNotEmpty(copyDescItems)) {
            List<ArrDescItem> copiedItems = descriptionItemService
                    .copyDescItemWithDataToNode(newLevel.getNode(),
                                                copyDescItems, change,
                                                fundContext.getFundVersion(),
                                                changeContext);
            items.addAll(copiedItems);
        }
        if (desctItemProvider != null) {
            desctItemProvider.provide(newLevel, change, fundContext.getFundVersion(), changeContext);
        }

        return items;
    }

    /**
     * Vloží nový uzel před nebo za statický uzel.
     *
     * @param version
     *            verze stormu
     * @param staticNode
     *            statický uzel (před/za který přidáváme)
     * @param staticNodeParent
     *            rodič statického uzlu
     * @param direction
     *            směr přidání uzlu
     * @param count
     *            počet přidaných úrovní
     * @return přidaný uzel
     */
    private List<ArrLevel> addLevelBeforeAfter(final ArrFundVersion version,
                                               final ArrNode staticNode,
                                               final ArrLevel staticLevelParent,
                                               final AddLevelDirection direction,
                                               int count,
                                               final ArrChange change) {
        Validate.notNull(version, "Verze AS musí být vyplněna");
        Validate.notNull(staticNode, "Refereční JP musí být vyplněna");
        Validate.notNull(staticLevelParent, "Rodič JP musí být vyplněn");
        Validate.isTrue(count > 0, "Počet uzlů musí být větší než 0", count);

        arrangementService.isValidAndOpenVersion(version);

        final ArrLevel staticLevel = levelRepository.findByNode(staticNode, version.getLockChange());

        int newLevelPosition = direction.equals(AddLevelDirection.AFTER) ? staticLevel.getPosition() + 1
                                                                         : staticLevel.getPosition();
        List<ArrLevel> nodesToShift = nodesToShift(staticLevel);
        if (direction.equals(AddLevelDirection.BEFORE)) {
            nodesToShift.add(0, staticLevel);
        }


        levelRepository.saveAll(shiftNodes(nodesToShift, change, newLevelPosition + count));
        levelRepository.flush();

        // create nodes
        List<ArrNode> nodes = createNodes(version.getFund(), change, count, null);

        List<ArrLevel> levels = createLevels(change, staticLevelParent.getNode(), newLevelPosition, nodes);
        return levels;
    }

    private List<ArrLevel> createLevels(ArrChange change,
                                        ArrNode parentNode,
                                        int firstLevelPosition,
                                        List<ArrNode> createdNodes) {
        int count = createdNodes.size();
        // create levels
        List<ArrLevel> levels = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ArrNode node = createdNodes.get(i);
            ArrLevel level = createLevelObject(change, parentNode,
                                               firstLevelPosition + i, node);
            levels.add(level);
        }
        levels = levelRepository.saveAll(levels);
        levelRepository.flush();
        return levels;
    }

    public ArrLevel createLevelObject(final ArrChange createChange, final ArrNode parentNode,
                                      final int position, final ArrNode node) {
        Validate.notNull(createChange, "Change nesmí být prázdná");
        Validate.notNull(node, "Change nesmí být prázdná");

        ArrLevel level = new ArrLevel();
        level.setPosition(position);
        level.setCreateChange(createChange);
        level.setNodeParent(parentNode);
        level.setNode(node);

        return level;
    }

	public ArrLevel createLevel(final ArrChange createChange, final ArrNode parentNode, 
			final int position, final ArrNode node) {

        ArrLevel level = createLevelObject(createChange, parentNode,
                                           position, node);
		return levelRepository.saveAndFlush(level);
	}

	public ArrLevel createLevel(final ArrChange createChange, final ArrNode parentNode, final int position,
			final String uuid, final ArrFund fund) {
        Validate.notNull(createChange, "Change nesmí být prázdná");
		
		ArrNode node = arrangementService.createNode(fund, uuid, createChange);
		return createLevel(createChange, parentNode, position, node);
	}

	public ArrLevel createLevelSimple(final ArrChange createChange, final ArrNode parentNode, final int position,
			final String uuid, final ArrFund fund) {
        Validate.notNull(createChange, "Change nesmí být prázdná");

		ArrNode node = arrangementService.createNodeSimple(fund, uuid, createChange);
		return createLevel(createChange, parentNode, position, node);
	}

    /**
     * Vloží nový uzel pod statický uzel na poslední pozici.
     *
     * @param version    verze stromu
     * @param staticNode statický uzel (pod který přidáváme)
     * @param count      počet přidaných úrovní
     * @return přidaný uzel
     */
    public List<ArrLevel> addLevelUnder(final ArrFundVersion version,
                                        final ArrLevel parentLevel,
                                        int count,
                                        List<String> uuids,
                                        final ArrChange change) {
        Validate.notNull(version, "Verze AS musí být vyplněna");
        Validate.notNull(parentLevel, "Refereční JP musí být vyplněna");
        Validate.isTrue(count>0, "Level count has to be greater then zero, %d", count);

        arrangementService.isValidAndOpenVersion(version);

        Integer maxPosition = levelRepository.findMaxPositionUnderParent(parentLevel.getNode());
        if (maxPosition == null) {
            maxPosition = 0;
        }

        List<ArrNode> nodes = createNodes(version.getFund(), change, count, uuids);
        
        // create levels
        List<ArrLevel> levels = createLevels(change, parentLevel.getNode(), 1 + maxPosition, nodes);
        return levels;
    }

    private List<ArrNode> createNodes(ArrFund fund, ArrChange change, int count,
                                      @Nullable List<String> uuids) {
        Iterator<String> uuidsIter;
        if(uuids!=null) {
            uuidsIter = uuids.iterator();
        } else {
            uuidsIter = null;
        }

        // create nodes
        List<ArrNode> nodes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String uuid;
            if (uuidsIter != null && uuidsIter.hasNext()) {
                uuid = uuidsIter.next();
            } else {
                uuid = null;
            }
            ArrNode node = arrangementService.createNodeObject(fund, uuid, change);
            nodes.add(node);
        }
        nodeRepository.saveAll(nodes);
        nodeRepository.flush();
        nodeCacheService.createEmptyNodes(nodes);
        return nodes;
    }

    /**
     * Zkontroluje, pře přesouvaný uzel není rodičem cílového uzlu.
     *
     * @param movedNode  přesouvaný uzel
     * @param targetNode cílový statický uzel
     */
    private void checkCycle(ArrLevel movedNode, ArrLevel targetNode) {
        Assert.notNull(movedNode, "Level musí být vyplněn");
        Assert.notNull(targetNode, "Level musí být vyplněn");

        if (targetNode.getNodeParent() == null) {
            return;
        }

        if (movedNode.getNode().equals(targetNode.getNodeParent())) {
            throw new SystemException("Přesouvaný uzel je rodičem cílového uzlu. Přesun nelze provést.");
        }

        ArrLevel parentNode = levelRepository.findByNodeAndDeleteChangeIsNull(targetNode.getNodeParent());
        checkCycle(movedNode, parentNode);
    }


    /**
     * Smaže uzel (uzamkne) a vytvoří jeho kopii.
     * 
     * Předchozí verze je ihned uložena do DB. Nová verze není uložena.
     *
     * @param prevLevel
     *            platná verze
     * @param change
     *            změna smazání
     * @return nový level
     */
    private ArrLevel createNewLevelVersion(ArrLevel prevLevel, ArrChange change) {
        Validate.notNull(prevLevel, "JP musí být vyplněna");
        Validate.notNull(change, "Změna musí být vyplněna");
        Validate.isTrue(prevLevel.getDeleteChange() == null, "Předchozí verze musí být platná");

        ArrLevel newNode = copyLevelData(prevLevel);
        newNode.setCreateChange(change);

        prevLevel.setDeleteChange(change);
        levelRepository.saveAndFlush(prevLevel);
        return newNode;
    }

    /**
     * Vytvoří kopii levelu.
     *
     * @param level level
     * @return kopie
     */
    private ArrLevel copyLevelData(ArrLevel level) {
        Assert.notNull(level, "Level musí být vyplněn");

        ArrLevel newNode = new ArrLevel();
        newNode.setNode(level.getNode());
        newNode.setNodeParent(level.getNodeParent());
        newNode.setPosition(level.getPosition());

        return newNode;
    }

    /**
     * Provede posunutí uzlů ve stejném rodiči na jinou pozici.
     *
     * @param nodesToShift  uzly k přesunutí
     * @param change        změna smazání uzlů
     * @param firstPosition pozice prvního přesouvaného uzlu
     */
    public List<ArrLevel> shiftNodes(Collection<ArrLevel> nodesToShift, ArrChange change, final int firstPosition) {
        Assert.notNull(nodesToShift, "Level k posunu musí být vyplněny");
        Assert.notNull(change, "Změna musí být vyplněna");

        int position = firstPosition + nodesToShift.size() - 1;

        List<ArrLevel> nodesToShiftList = new ArrayList<>(nodesToShift);
        nodesToShiftList.sort((o1, o2) -> new CompareToBuilder()
                .append(o2.getPosition(), o1.getPosition())
                .toComparison());

        List<ArrLevel> updatedLevels = new ArrayList<>();

        for (ArrLevel node : nodesToShiftList) {
            ArrLevel newNode = createNewLevelVersion(node, change);
            newNode.setPosition(position--);
            updatedLevels.add(newNode);
        }

        return updatedLevels;
    }


    /**
     * Najde seznam uzlů k přesunutí.
     *
     * @param movedLevel uzel
     * @return seznam uzlů se stejným rodičem a vyšší pozicí
     */
    public List<ArrLevel> nodesToShift(ArrLevel movedLevel) {
        Assert.notNull(movedLevel, "Level k posunu musí být vyplněn");

        return levelRepository.findByParentNodeAndPositionGreaterThanOrderByPositionAsc(movedLevel.getNodeParent(),
                movedLevel.getPosition());
    }

    /**
     * Změní pořadí levelů na pořadí v jakém jsou předané. Předpokládané použití je pro všechny levely jednoho rodiče.
     * @param levels všechny levely jednoho rodiče
     * @param change změna
     */
    public void changeLevelsPosition(List<ArrLevel> levels, ArrChange change) {
        Assert.notNull(change, "Změna musí být vyplněna");

        if (CollectionUtils.isEmpty(levels)) {
            return;
        }

        List<ArrLevel> updatedLevels = new ArrayList<>(levels.size());
        for (int i = 1; i <= levels.size(); i++) {
            ArrLevel level = levels.get(i - 1);

            ArrLevel newLevel = createNewLevelVersion(level, change);
            newLevel.setPosition(i);
            updatedLevels.add(newLevel);
        }
        levelRepository.saveAll(updatedLevels);
        levelRepository.flush();
    }

    /**
     * Směr přidání nového uzlu.
     */
    public enum AddLevelDirection {
        BEFORE(DirectionLevel.BEFORE, EventType.ADD_LEVEL_BEFORE),
        AFTER(DirectionLevel.AFTER, EventType.ADD_LEVEL_AFTER),
        CHILD(DirectionLevel.CHILD, EventType.ADD_LEVEL_UNDER);

        private DirectionLevel directionLevel;
        private EventType eventType;

        AddLevelDirection(final DirectionLevel directionLevel,
                          final EventType eventType) {
            this.directionLevel = directionLevel;
            this.eventType = eventType;
        }

        public DirectionLevel getDirectionLevel() {
            return directionLevel;
        }

        public EventType getEventType() {
            return eventType;
        }
    }

    /**
     * Add new level to the existing node
     * 
     * This method can be used only if there is no other active level for the node
     * 
     * @param fundVersion
     * @param parentLevel
     * @param change 
     * @param linkNode
     * @param descItemProvider
     * @return
     */
	public ArrLevel addNewLevelForNode(ArrFundVersion fundVersion,
			ArrLevel parentLevel,
			ArrChange change, ArrNode linkNode,
			DaoDesctItemProvider descItemProvider) {
		
		// TODO: Check that no items exists for node
		
        Validate.notNull(fundVersion, "Verze AS musí být vyplněna");
        Validate.notNull(parentLevel, "Rodičovká JP musí být vyplněna");
        Validate.notNull(change, "Change musí existovat");

        arrangementService.isValidAndOpenVersion(fundVersion);

        Integer maxPosition = levelRepository.findMaxPositionUnderParent(parentLevel.getNode());
        if (maxPosition == null) {
            maxPosition = 0;
        }

        ArrLevel newLevel = createLevel(change, parentLevel.getNode(), 
        		maxPosition + 1, linkNode);

        // create/update node cache
        nodeCacheService.syncNodes(Collections.singletonList(linkNode.getNodeId()));
		
        createItemsForNewLevel(fundVersion, parentLevel, newLevel, change, null, null, descItemProvider);

        // send notification about new level
        eventNotificationService.publishEvent(EventFactory.createAddNodeEvent(EventType.ADD_LEVEL_UNDER, fundVersion,
                                                                              parentLevel, newLevel));
    	
		return newLevel;
	}
}
