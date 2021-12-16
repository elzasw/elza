package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDescItem;
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
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.service.arrangement.DesctItemProvider;
import cz.tacr.elza.service.arrangement.MultiplItemChangeContext;
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
    private LevelRepository levelRepository;
    
    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private ArrangementInternalService arrangementInternalService;

    @Autowired
    private RuleService ruleService;
    
    @Autowired
    private DescItemRepository descItemRepository;
    
    @Autowired
    private IEventNotificationService eventNotificationService;
    
    @Autowired
    private DescriptionItemService descriptionItemService;

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

            if (!transportLevel.getNodeParent().equals(transportParentNode)) {
                throw new SystemException("Všechny přesouvané uzly musejí mít stejného rodiče.");
            }


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

        Integer position;
        if (transportLevelParent.getNode().equals(staticLevel.getNodeParent())) {
            ruleService.conformityInfo(versionId, transportNodeIds, NodeTypeOperation.DISCONNECT_NODE_LOCAL,
                    null, null, null);

            if (transportLevels.get(0).getPosition() > staticLevel.getPosition()) {
                position = placeLevels(transportLevels, staticLevel.getNodeParent(), change,
                        staticLevel.getPosition());
                // TODO Lebeda - tady spadne na: Violation of UNIQUE KEY constraint 'u_arr_level_ppd'. Cannot insert duplicate key in object 'dbo.arr_level'.
                placeLevels(nodesToShiftDown, staticLevel.getNodeParent(), change, position);
            } else {
                //posun up
                shiftNodesWithCollection(nodesToShiftUp, transportLevels, transportLevels.get(0).getPosition(),
                        staticLevel.getNodeParent(), change, staticLevel, null);
            }
        } else {
            ruleService.conformityInfo(versionId, transportNodeIds, NodeTypeOperation.DISCONNECT_NODE,
                    null, null, null);

            shiftNodes(nodesToShiftUp, change, transportLevels.get(0).getPosition());
            position = placeLevels(transportLevels, staticLevel.getNodeParent(), change, staticLevel.getPosition());
            shiftNodes(nodesToShiftDown, change, position);
        }


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

            if (!transportLevel.getNodeParent().equals(transportParentNode)) {
                throw new SystemException("Všechny přesouvané uzly musejí mít stejného rodiče.");
            }


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

        Integer position;
        if (transportLevelParent.getNode().equals(staticLevel.getNodeParent())) {
            ruleService.conformityInfo(versionId, transportNodeIds, NodeTypeOperation.DISCONNECT_NODE_LOCAL,
                    null, null, null);

            if (transportLevels.get(0).getPosition() > staticLevel.getPosition()) {
                position = placeLevels(transportLevels, staticLevel.getNodeParent(), change,
                        staticLevel.getPosition() + 1);
                placeLevels(nodesToShiftDown, staticLevel.getNodeParent(), change, position);
            } else {
                //posun up
                shiftNodesWithCollection(nodesToShiftUp, transportLevels, transportLevels.get(0).getPosition(),
                        staticLevel.getNodeParent(), change, null, staticLevel);
            }
        } else {
            ruleService.conformityInfo(versionId, transportNodeIds, NodeTypeOperation.DISCONNECT_NODE,
                    null, null, null);

            shiftNodes(nodesToShiftUp, change, transportLevels.get(0).getPosition());
            position = placeLevels(transportLevels, staticLevel.getNodeParent(), change, staticLevel.getPosition() + 1);
            shiftNodes(nodesToShiftDown, change, position);
        }


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

            if (!transportLevel.getNodeParent().equals(transportParentNode)) {
                throw new SystemException("Všechny přesouvané uzly musejí mít stejného rodiče.");
            }

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

        Set<Integer> transportNodeIds = new HashSet<>();
        transportLevels.forEach((t) -> transportNodeIds.add(t.getNode().getNodeId()));

        ruleService.conformityInfo(versionId, transportNodeIds, NodeTypeOperation.DISCONNECT_NODE,
                null, null, null);


        //zbydou pouze ty, které jsou pod přesouvanými
        List<ArrLevel> nodesToShiftUp = nodesToShift(transportLevels.get(0));
        nodesToShiftUp.removeAll(transportLevels);

        shiftNodes(nodesToShiftUp, change, transportLevels.get(0).getPosition());


        Integer maxPosition = levelRepository.findMaxPositionUnderParent(staticLevel.getNode());
        if (maxPosition == null) {
            maxPosition = 0;
        }

        placeLevels(transportLevels, staticLevel.getNode(), change, maxPosition + 1);


        ruleService.conformityInfo(versionId, transportNodeIds, NodeTypeOperation.CONNECT_NODE, null, null, null);


        entityManager.flush(); //aktualizace verzí v nodech
        eventNotificationService.publishEvent(
                EventFactory.createMoveEvent(EventType.MOVE_LEVEL_UNDER, staticLevel, transportLevels, version));
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

            if(!ObjectUtils.equals(deleteLevel.getNodeParent(), deleteNodeParent)){
                throw new SystemException(
                        "Uzel " + deleteNode.getNodeId() + " nemá rodiče s id " + deleteNodeParent.getNodeId());
            }
        }

        ruleService.conformityInfo(version.getFundVersionId(), Arrays.asList(deleteNode.getNodeId()),
                NodeTypeOperation.DELETE_NODE, null, null, null);

        shiftNodes(nodesToShift(deleteLevel), change, deleteLevel.getPosition());

        ArrLevel level = arrangementService.deleteLevelCascade(deleteLevel, change,
                levelRepository.findLevelsByDirection(deleteLevel, version, RelatedNodeDirection.DESCENDANTS),
                deleteLevelsWithAttachedDao);

        eventNotificationService.publishEvent(new EventDeleteNode(EventType.DELETE_LEVEL,
                version.getFundVersionId(),
                deleteNode.getNodeId(),
                (deleteNodeParent != null) ? deleteNodeParent.getNodeId() : null));

        return level;
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
    private void shiftNodesWithCollection(final List<ArrLevel> shiftNodes,
                                         final List<ArrLevel> transferCollection,
                                         final int firstPosition,
                                         final ArrNode parentNode,
                                         final ArrChange change,
                                         @Nullable final ArrLevel beforeLevel,
                                         @Nullable final ArrLevel afterLevel) {
        Assert.isTrue((beforeLevel == null && afterLevel != null) || (beforeLevel != null && afterLevel == null), "Musí být platné");

        boolean needInsert = true;
        int position = firstPosition;
        for (ArrLevel shiftNode : shiftNodes) {
            if (needInsert && beforeLevel != null && beforeLevel.equals(shiftNode)) {
                needInsert = false;
                position = placeLevels(transferCollection, parentNode, change, position);
            }

            ArrLevel newNode = createNewLevelVersion(shiftNode, change);
            newNode.setPosition(position++);
            levelRepository.saveAndFlush(newNode);


            if (needInsert && afterLevel != null && afterLevel.equals(shiftNode)) {
                needInsert = false;
                position = placeLevels(transferCollection, parentNode, change, position);
            }
        }

        if (needInsert) {
            placeLevels(transferCollection, parentNode, change, position);
        }
    }

    /**
     * Vloží do rodiče seznam uzlů na danou pozici
     *
     * @param transportLevels seznam přesouvaných uzlů
     * @param parentNode      nadřazený uzel
     * @param change          změna uzamčení
     * @param firstPosition   pozice prvního posouvaného uzlu
     */
    private int placeLevels(final List<ArrLevel> transportLevels, final ArrNode parentNode,
                           final ArrChange change, final int firstPosition) {
        int position = firstPosition;

        for (ArrLevel transportLevel : transportLevels) {
            ArrLevel newLevel = createNewLevelVersion(transportLevel, change);
            newLevel.setNodeParent(parentNode);
            newLevel.setPosition(position++);
            levelRepository.saveAndFlush(newLevel);
        }

        return position;
    }


    /**
     * Vloží nový uzel do stromu. Podle směru zjistí pozici, posune případné sourozence a vloží uzel.
     *
     * @param version           verze stromu
     * @param staticNode        Statický uzel (za/před/pod který přidáváme)
     * @param staticNodeParent  Rodič statického uzlu (za/před/pod který přidáváme)
     * @param direction         směr přidávání
     * @param scenarionName     Název scénáře, ze kterého se mají převzít výchozí hodnoty atributů.
     * @param descItemCopyTypes id typů atributl, které budou zkopírovány z uzlu přímo nadřazeným nad přidaným uzlem (jeho mladší sourozenec).
     * @param count             počet přidaných úrovní (pokud je null, přidáme jeden)
     * @param uuids             seznam UUID pro nové uzly, může být null
     */
    @Transactional(value = TxType.MANDATORY)
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR, UsrPermission.Permission.FUND_ARR_NODE})
    public List<ArrLevel> addNewLevel(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion version,
                                      final ArrNode baseNode,
                                @AuthParam(type = AuthParam.Type.NODE) final ArrNode staticNodeParent,
                                final AddLevelDirection direction,
                                @Nullable final String scenarionName,
                                final Set<RulItemType> descItemCopyTypes,
                                @Nullable final DesctItemProvider desctItemProvider,
                                @Nullable final Integer countNewLevel,
                                @Nullable final List<String> uuids) {

        Assert.notNull(baseNode, "Refereční JP musí být vyplněna");
        Assert.notNull(staticNodeParent, "Rodič JP musí být vyplněn");

        ArrLevel baseLevel = levelRepository.findByNode(baseNode, version.getLockChange());
        int count = countNewLevel == null? 1 : countNewLevel;

        List<ArrLevel> levels = new ArrayList<>(count);
        switch (direction){
            case CHILD:
                levels = addLevelUnder(version, baseNode, count, uuids);
                break;
            case BEFORE:
            case AFTER:
                levels = addLevelBeforeAfter(version, baseNode, staticNodeParent, direction, count);
                break;
            default:
                throw new IllegalStateException("Neznámý typ směru přidání uzlu " + direction.name());
        }

        Assert.notEmpty(levels, "Level musí být vyplněn");

        for (ArrLevel newLevel : levels) {
            ArrChange change = newLevel.getCreateChange();

            Map<Integer, RulItemType> descItemTypeCopyMap = ElzaTools.createEntityMap(descItemCopyTypes, t -> t.getItemTypeId());

            MultiplItemChangeContext changeContext = descriptionItemService.createChangeContext(version.getFundVersionId());

            if (StringUtils.isNotBlank(scenarionName)) {
                ScenarioOfNewLevel scenario = descriptionItemService
                        .getDescriptionItamsOfScenario(scenarionName, baseLevel, direction.getDirectionLevel(),
                                                       version);
                for (ArrDescItem descItem : scenario.getDescItems()) {
                    //pokud se má typ kopírovat z předchozího uzlu, nebudeme ho vkládat ze scénáře
                    if (descItem.getItemType() == null || descItemTypeCopyMap
                            .containsKey(descItem.getItemType().getItemTypeId())) {
                        continue;
                    }

                    descItem.setNode(newLevel.getNode());
                    descriptionItemService
                        .createDescriptionItemInBatch(descItem, newLevel.getNode(), version, change, changeContext);
                }
            }

            ArrLevel olderSibling = levelRepository.findOlderSibling(newLevel, version.getLockChange());
            if (olderSibling != null && descItemCopyTypes != null && !descItemCopyTypes.isEmpty()) {
                List<ArrDescItem> siblingDescItems = descItemRepository
                        .findOpenByNodeAndTypes(olderSibling.getNode(), descItemCopyTypes);
                descriptionItemService.copyDescItemWithDataToNode(newLevel.getNode(),
                                                                  siblingDescItems, change, version,
                                                                  changeContext);
            }
            if (desctItemProvider != null) {
                desctItemProvider.provide(newLevel, change, version, changeContext);
            }

            changeContext.flush();

            ruleService.conformityInfo(version.getFundVersionId(),
                                       Arrays.asList(newLevel.getNode().getNodeId()),
                                       NodeTypeOperation.CREATE_NODE, null, null, null);

            entityManager.flush(); //aktualizace verzí v nodech
            eventNotificationService.publishEvent(EventFactory.createAddNodeEvent(direction.getEventType(), version,
                                                                                  baseLevel, newLevel));

            // při přidání AFTER by se měla změnit aktuální ArrLevel na předchozí
            if (direction == AddLevelDirection.AFTER) {
                baseLevel = newLevel;
            }

        }

        return levels;
    }

    /**
     * Vloží nový uzel před nebo za statický uzel.
     *
     * @param version          verze stormu
     * @param staticNode       statický uzel (před/za který přidáváme)
     * @param staticNodeParent rodič statického uzlu
     * @param direction        směr přidání uzlu
     * @param count            počet přidaných úrovní
     * @return přidaný uzel
     */
    private List<ArrLevel> addLevelBeforeAfter(final ArrFundVersion version,
                                         final ArrNode staticNode,
                                         final ArrNode staticNodeParent,
                                         final AddLevelDirection direction,
                                         int count) {
        Assert.notNull(version, "Verze AS musí být vyplněna");
        Assert.notNull(staticNode, "Refereční JP musí být vyplněna");
        Assert.notNull(staticNodeParent, "Rodič JP musí být vyplněn");
        Validate.isTrue(count > 0, "Počet uzlů musí být větší než 0", count);

        arrangementService.isValidAndOpenVersion(version);

        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.ADD_LEVEL, staticNodeParent);

        final ArrLevel staticLevelParent = arrangementService.lockNode(staticNodeParent, version, change);
        Assert.notNull(staticLevelParent, "Rodič levelu musí být vyplněn");

        final ArrLevel staticLevel = levelRepository.findByNode(staticNode, version.getLockChange());

        int newLevelPosition = direction.equals(AddLevelDirection.AFTER) ? staticLevel.getPosition() + 1
                                                                         : staticLevel.getPosition();
        List<ArrLevel> nodesToShift = nodesToShift(staticLevel);
        if (direction.equals(AddLevelDirection.BEFORE)) {
            nodesToShift.add(0, staticLevel);
        }

        List<ArrLevel> levels = new ArrayList<>(count);
        shiftNodes(nodesToShift, change, newLevelPosition + count);
        for (int i = 0; i < count; i++) {
            levels.add(arrangementService.createLevel(change, staticLevelParent.getNode(),
                                                      newLevelPosition + i,
                                                      null,
                                                      version.getFund()));
        }

        return levels;
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
                                  final ArrNode staticNode, 
                                  int count,
                                  List<String> uuids) {
        Validate.notNull(version, "Verze AS musí být vyplněna");
        Validate.notNull(staticNode, "Refereční JP musí být vyplněna");
        Validate.isTrue(count>0, "Level count has to be greater then zero, %d", count);

        arrangementService.isValidAndOpenVersion(version);

        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.ADD_LEVEL, staticNode);
        final ArrLevel staticLevel = arrangementService.lockNode(staticNode, version, change);
        Assert.notNull(staticLevel, "Referenční level musí být vyplněn");

        Integer maxPosition = levelRepository.findMaxPositionUnderParent(staticLevel.getNode());
        if (maxPosition == null) {
            maxPosition = 0;
        }

        
        Iterator<String> uuidsIter; 
        if(uuids!=null) {
        	uuidsIter = uuids.iterator();
        } else {
        	uuidsIter = null;
        }
        
        List<ArrLevel> levels = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
        	String uuid;
        	if(uuidsIter!=null&&uuidsIter.hasNext()) {
        		uuid = uuidsIter.next(); 
        	} else {
        		uuid = null;
        	}
            ArrLevel level = arrangementService.createLevel(change, staticLevel.getNode(),
                                                            maxPosition + i, uuid,
                                                            version.getFund());
            levels.add(level);
        }
        
        return levels;
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
     * @param node   uzel
     * @param change změna smazání
     * @return nový level
     */
    private ArrLevel createNewLevelVersion(ArrLevel node, ArrChange change) {
        Assert.notNull(node, "JP musí být vyplněna");
        Assert.notNull(change, "Změna musí být vyplněna");

        ArrLevel newNode = copyLevelData(node);
        newNode.setCreateChange(change);

        node.setDeleteChange(change);
        levelRepository.saveAndFlush(node);
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
    public void shiftNodes(Collection<ArrLevel> nodesToShift, ArrChange change, final int firstPosition) {
        Assert.notNull(nodesToShift, "Level k posunu musí být vyplněny");
        Assert.notNull(change, "Změna musí být vyplněna");

        int position = firstPosition + nodesToShift.size() - 1;

        List<ArrLevel> nodesToShiftList = new ArrayList<>(nodesToShift);
        nodesToShiftList.sort((o1, o2) -> new CompareToBuilder()
                .append(o2.getPosition(), o1.getPosition())
                .toComparison());

        for (ArrLevel node : nodesToShiftList) {
            ArrLevel newNode = createNewLevelVersion(node, change);
            newNode.setPosition(position--);
            levelRepository.saveAndFlush(newNode);
        }
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

        for (int i = 1; i <= levels.size(); i++) {
            ArrLevel level = levels.get(i - 1);

            ArrLevel newLevel = createNewLevelVersion(level, change);
            newLevel.setPosition(i);
            levelRepository.saveAndFlush(newLevel);
        }
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
}
