package cz.tacr.elza.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.api.vo.NodeTypeOperation;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.drools.DirectionLevel;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.FindingAidVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventDeleteNode;
import cz.tacr.elza.service.eventnotification.events.EventType;


/**
 * Servisní třída pro přesuny uzlů ve stromu.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 18.01.2016
 */
@Service
public class ArrMoveLevelService {

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private LevelRepository levelRepository;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private ArrangementService arrangementService;
    @Autowired
    private RuleService ruleService;
    @Autowired
    private FindingAidVersionRepository findingAidVersionRepository;
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
    public void moveLevelsBefore(final ArrFindingAidVersion version,
                                 final ArrNode staticNode,
                                 final ArrNode staticParentNode,
                                 final Collection<ArrNode> transportNodes,
                                 final ArrNode transportParentNode) {

        Assert.notEmpty(transportNodes);

        ArrLevel staticLevelParent = arrangementService.lockNode(staticParentNode, version);
        ArrLevel transportLevelParent = transportParentNode.equals(staticParentNode)
                                        ? staticLevelParent: arrangementService.lockNode(transportParentNode, version);
        ArrLevel staticLevel = levelRepository
                .findNodeInRootTreeByNodeId(staticNode, version.getRootLevel().getNode(), version.getLockChange());
        Assert.notNull(staticLevel);


        List<ArrLevel> transportLevels = new ArrayList<>(transportNodes.size());
        for (ArrNode transportNode : transportNodes) {
            ArrLevel transportLevel = transportNode.equals(staticParentNode)
                                      ? staticLevelParent : arrangementService.lockNode(transportNode, version);

            if (!transportLevel.getNodeParent().equals(transportParentNode)) {
                throw new IllegalStateException("Všechny přesouvané uzly musejí mít stejného rodiče.");
            }


            if (transportLevel.equals(staticLevel)) {
                throw new IllegalStateException("Nelze vložit záznam na stejné místo ve stromu");
            }

            transportLevels.add(transportLevel);
        }

        // vkládaný nesmí být rodičem uzlu za který ho vkládám
        checkCycle(transportLevels.get(0), staticLevel);

        moveLevelBefore(version, staticLevel, transportLevels, transportLevelParent);
    }

    /**
     * Provede přesunutí uzlů. Všechny nutné uzly musejí být již uzamčeny, zde neprobíhá kontrola zámků, pouze přesuny.
     *
     * @param version              verze stromu
     * @param staticLevel          statický uzel (za, před, pod který přesouváme)
     * @param transportLevels      seznam uzlů, které přesouváme
     * @param transportLevelParent rodič přesouvaných uzlů
     */
    private void moveLevelBefore(final ArrFindingAidVersion version,
                                 final ArrLevel staticLevel,
                                 final List<ArrLevel> transportLevels,
                                 final ArrLevel transportLevelParent) {

        Integer versionId = version.getFindingAidVersionId();
        arrangementService.isValidAndOpenVersion(version);

        Set<Integer> transportNodeIds = new HashSet<>();
        transportLevels.forEach((t) -> transportNodeIds.add(t.getNode().getNodeId()));

        ArrChange change = arrangementService.createChange();

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


        arrangementService.saveLastChangeFaVersion(change, versionId);

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


    /**
     * Přesunutí uzlů za jiný.
     *
     * @param version             verze
     * @param staticNode          statický uzel (za, před, pod který přesouváme)
     * @param staticParentNode    rodič statického uzlu
     * @param transportNodes      seznam uzlů, které přesouváme
     * @param transportParentNode rodič přesouvaných uzlů
     */
    public void moveLevelsAfter(final ArrFindingAidVersion version,
                                final ArrNode staticNode,
                                final ArrNode staticParentNode,
                                final List<ArrNode> transportNodes,
                                final ArrNode transportParentNode) {
        Assert.notEmpty(transportNodes);

        ArrLevel staticLevelParent = arrangementService.lockNode(staticParentNode, version);
        ArrLevel transportLevelParent = transportParentNode.equals(staticParentNode)
                                        ? staticLevelParent : arrangementService.lockNode(transportParentNode, version);
        ArrLevel staticLevel = levelRepository
                .findNodeInRootTreeByNodeId(staticNode, version.getRootLevel().getNode(), version.getLockChange());
        Assert.notNull(staticLevel);


        List<ArrLevel> transportLevels = new ArrayList<>(transportNodes.size());
        for (ArrNode transportNode : transportNodes) {
            ArrLevel transportLevel = transportNode.equals(staticParentNode)
                                      ? staticLevelParent : arrangementService.lockNode(transportNode, version);

            if (!transportLevel.getNodeParent().equals(transportParentNode)) {
                throw new IllegalStateException("Všechny přesouvané uzly musejí mít stejného rodiče.");
            }


            if (transportLevel.equals(staticLevel)) {
                throw new IllegalStateException("Nelze vložit záznam na stejné místo ve stromu");
            }

            transportLevels.add(transportLevel);
        }

        // vkládaný nesmí být rodičem uzlu za který ho vkládám
        checkCycle(transportLevels.get(0), staticLevel);

        moveLevelAfter(version, staticLevel, transportLevels, transportLevelParent);
    }


    /**
     * Provede přesunutí uzlů. Všechny nutné uzly musejí být již uzamčeny, zde neprobíhá kontrola zámků, pouze přesuny.
     *
     * @param version              verze stromu
     * @param staticLevel          statický uzel (za, před, pod který přesouváme)
     * @param transportLevels      seznam uzlů, které přesouváme
     * @param transportLevelParent rodič přesouvaných uzlů
     */
    private void moveLevelAfter(final ArrFindingAidVersion version,
                                final ArrLevel staticLevel,
                                final List<ArrLevel> transportLevels,
                                final ArrLevel transportLevelParent) {

        Integer versionId = version.getFindingAidVersionId();
        arrangementService.isValidAndOpenVersion(version);

        Set<Integer> transportNodeIds = new HashSet<>();
        transportLevels.forEach((t) -> transportNodeIds.add(t.getNode().getNodeId()));


        ArrChange change = arrangementService.createChange();

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


        arrangementService.saveLastChangeFaVersion(change, versionId);

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
    public void moveLevelsUnder(final ArrFindingAidVersion version,
                                final ArrNode staticNode,
                                final Collection<ArrNode> transportNodes,
                                final ArrNode transportParentNode) {
        ArrLevel staticLevel = arrangementService.lockNode(staticNode, version);
        ArrLevel transportLevelParent = staticNode.equals(transportParentNode)
                                        ? staticLevel : arrangementService.lockNode(transportParentNode, version);

        List<ArrLevel> transportLevels = new ArrayList<>(transportNodes.size());

        for (ArrNode transportNode : transportNodes) {
            if (transportNode.equals(staticNode)) {
                throw new IllegalStateException("Nelze vložit záznam na stejné místo ve stromu");
            }

            ArrLevel transportLevel = arrangementService.lockNode(transportNode, version);

            if (!transportLevel.getNodeParent().equals(transportParentNode)) {
                throw new IllegalStateException("Všechny přesouvané uzly musejí mít stejného rodiče.");
            }

            transportLevels.add(transportLevel);

        }

        // vkládaný nesmí být rodičem uzlu za který ho vkládám
        checkCycle(transportLevels.get(0), staticLevel);


        moveLevelUnder(version, staticLevel, transportLevels);
    }


    /**
     * Provede přesunutí uzlů. Všechny nutné uzly musejí být již uzamčeny, zde neprobíhá kontrola zámků, pouze přesuny.
     *
     * @param version         verze stromu
     * @param staticLevel     statický uzel (za, před, pod který přesouváme)
     * @param transportLevels seznam uzlů, které přesouváme
     */
    private void moveLevelUnder(final ArrFindingAidVersion version,
                                final ArrLevel staticLevel,
                                final List<ArrLevel> transportLevels) {

        Integer versionId = version.getFindingAidVersionId();
        arrangementService.isValidAndOpenVersion(version);

        Set<Integer> transportNodeIds = new HashSet<>();
        transportLevels.forEach((t) -> transportNodeIds.add(t.getNode().getNodeId()));

        ruleService.conformityInfo(versionId, transportNodeIds, NodeTypeOperation.DISCONNECT_NODE,
                null, null, null);

        ArrChange change = arrangementService.createChange();


        //zbydou pouze ty, které jsou pod přesouvanými
        List<ArrLevel> nodesToShiftUp = nodesToShift(transportLevels.get(0));
        nodesToShiftUp.removeAll(transportLevels);

        shiftNodes(nodesToShiftUp, change, transportLevels.get(0).getPosition());


        Integer maxPosition = levelRepository.findMaxPositionUnderParent(staticLevel.getNode());
        if (maxPosition == null) {
            maxPosition = 0;
        }

        placeLevels(transportLevels, staticLevel.getNode(), change, maxPosition + 1);


        arrangementService.saveLastChangeFaVersion(change, versionId);

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
     */
    public ArrLevel deleteLevel(final ArrFindingAidVersion version,
                            final ArrNode deleteNode,
                            final ArrNode deleteNodeParent) {
        Assert.notNull(version);
        Assert.notNull(deleteNode);

        ArrLevel deleteLevel = arrangementService.lockNode(deleteNode, version);
        if (deleteNodeParent != null) {
            arrangementService.lockNode(deleteNodeParent, version);

            if(!ObjectUtils.equals(deleteLevel.getNodeParent(), deleteNodeParent)){
                throw new IllegalArgumentException(
                        "Uzel " + deleteNode.getNodeId() + " nemá rodiče s id " + deleteNodeParent.getNodeId());
            }
        }




        ruleService.conformityInfo(version.getFindingAidVersionId(), Arrays.asList(deleteNode.getNodeId()),
                NodeTypeOperation.DELETE_NODE, null, null, null);

        ArrChange change = arrangementService.createChange();
        shiftNodes(nodesToShift(deleteLevel), change, deleteLevel.getPosition());

        ArrLevel level = arrangementService.deleteLevelCascade(deleteLevel, change);

        eventNotificationService.publishEvent(new EventDeleteNode(EventType.DELETE_LEVEL,
                version.getFindingAidVersionId(),
                deleteNode.getNodeId(),deleteNodeParent.getNodeId()));

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
    public void shiftNodesWithCollection(final List<ArrLevel> shiftNodes,
                                         final List<ArrLevel> transferCollection,
                                         final int firstPosition,
                                         final ArrNode parentNode,
                                         final ArrChange change,
                                         @Nullable final ArrLevel beforeLevel,
                                         @Nullable final ArrLevel afterLevel) {
        Assert.isTrue((beforeLevel == null && afterLevel != null) || (beforeLevel != null && afterLevel == null));

        boolean needInsert = true;
        int position = firstPosition;
        for (ArrLevel shiftNode : shiftNodes) {
            if (needInsert && beforeLevel != null && beforeLevel.equals(shiftNode)) {
                needInsert = false;
                position = placeLevels(transferCollection, parentNode, change, position);
            }

            ArrLevel newNode = createNewLevelVersion(shiftNode, change);
            newNode.setPosition(position++);
            levelRepository.save(newNode);

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
    public int placeLevels(final List<ArrLevel> transportLevels, final ArrNode parentNode,
                           final ArrChange change, final int firstPosition) {
        int position = firstPosition;

        for (ArrLevel transportLevel : transportLevels) {
            ArrLevel newLevel = createNewLevelVersion(transportLevel, change);
            newLevel.setNodeParent(parentNode);
            newLevel.setPosition(position++);
            levelRepository.save(newLevel);
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
     * @param descItemCopyTypes id typů atributl, které budou zkopírovány z uzlu přímo nadřazeným nad přidaným uzlem
     *                          (jeho mladší sourozenec).
     */
    public ArrLevel addNewLevel(final ArrFindingAidVersion version,
                                final ArrNode staticNode,
                                final ArrNode staticNodeParent,
                                final AddLevelDirection direction,
                                @Nullable final String scenarionName,
                                @Nullable final Set<RulDescItemType> descItemCopyTypes) {

        Assert.notNull(staticNode);
        Assert.notNull(staticNodeParent);
        final ArrLevel staticLevel = levelRepository
                .findNodeInRootTreeByNodeId(staticNode, version.getRootLevel().getNode(), version.getLockChange());


        ArrLevel newLevel;
        switch (direction){
            case CHILD:
                newLevel = addLevelUnder(version, staticNode);
                break;
            case BEFORE:
            case AFTER:
                newLevel = addLevelBeforeAfter(version, staticNode, staticNodeParent, direction);
                break;
            default:
                throw new IllegalStateException("Neznámý typ směru přidání uzlu " + direction.name());
        }

        Assert.notNull(newLevel);
        ArrChange change = newLevel.getCreateChange();

        Map<Integer, RulDescItemType> descItemTypeCopyMap = ElzaTools
                .createEntityMap(descItemCopyTypes, t -> t.getDescItemTypeId());

        if (StringUtils.isNotBlank(scenarionName)) {
            ScenarioOfNewLevel scenario = descriptionItemService
                    .getDescriptionItamsOfScenario(scenarionName, staticLevel, direction.getDirectionLevel(), version);

            for (ArrDescItem descItem : scenario.getDescItems()) {
                //pokud se má typ kopírovat z předchozího uzlu, nebudeme ho vkládat ze scénáře
                if (descItem.getDescItemType() == null || descItemTypeCopyMap
                        .containsKey(descItem.getDescItemType().getDescItemTypeId())) {
                    continue;
                }

                descItem.setNode(newLevel.getNode());
                descriptionItemService.createDescriptionItem(descItem, newLevel.getNode(), version, change);
            }
        }


        ArrLevel olderSibling = levelRepository.findOlderSibling(newLevel, version.getLockChange());
        if(olderSibling != null && !descItemCopyTypes.isEmpty()){
            List<ArrDescItem> siblingDescItems = descItemRepository
                    .findOpenByNodeAndTypes(olderSibling.getNode(), descItemCopyTypes);
            descriptionItemService.copyDescItemWithDataToNode(newLevel.getNode(), siblingDescItems, change, version);
        }


        arrangementService.saveLastChangeFaVersion(change, version.getFindingAidVersionId());

        ruleService.conformityInfo(version.getFindingAidVersionId(), Arrays.asList(newLevel.getNode().getNodeId()),
                NodeTypeOperation.CREATE_NODE, null, null, null);

        entityManager.flush(); //aktualizace verzí v nodech
        eventNotificationService
                .publishEvent(EventFactory.createAddNodeEvent(direction.getEventType(), version, staticLevel, newLevel));

        return newLevel;
    }

    /**
     * Vloží nový uzel před nebo za statický uzel.
     *
     * @param version          verze stormu
     * @param staticNode       statický uzel (před/za který přidáváme)
     * @param staticNodeParent rodič statického uzlu
     * @param direction        směr přidání uzlu
     * @return přidaný uzel
     */
    private ArrLevel addLevelBeforeAfter(final ArrFindingAidVersion version,
                                         final ArrNode staticNode,
                                         final ArrNode staticNodeParent,
                                         final AddLevelDirection direction) {
        Assert.notNull(version);
        Assert.notNull(staticNode);
        Assert.notNull(staticNodeParent);


        arrangementService.isValidAndOpenVersion(version);

        final ArrLevel staticLevelParent = arrangementService.lockNode(staticNodeParent, version);
        Assert.notNull(staticLevelParent);

        final ArrLevel staticLevel = levelRepository
                .findNodeInRootTreeByNodeId(staticNode, version.getRootLevel().getNode(), version.getLockChange());


        int newLevelPosition = direction.equals(AddLevelDirection.AFTER) ? staticLevel.getPosition() + 1
                                                                         : staticLevel.getPosition();
        List<ArrLevel> nodesToShift = nodesToShift(staticLevel);
        if (direction.equals(AddLevelDirection.BEFORE)) {
            nodesToShift.add(0, staticLevel);
        }

        ArrChange change = arrangementService.createChange();
        shiftNodes(nodesToShift, change, newLevelPosition + 1);
        ArrLevel newLevel = arrangementService.createLevel(change, staticLevelParent.getNode(), newLevelPosition);

        return newLevel;
    }


    /**
     * Vloží nový uzel pod statický uzel na poslední pozici.
     *
     * @param version    verze stromu
     * @param staticNode statický uzel (pod který přidáváme)
     * @return přidaný uzel
     */
    private ArrLevel addLevelUnder(final ArrFindingAidVersion version,
                                   final ArrNode staticNode) {
        Assert.notNull(version);
        Assert.notNull(staticNode);


        arrangementService.isValidAndOpenVersion(version);

        final ArrLevel staticLevel = arrangementService.lockNode(staticNode, version);
        Assert.notNull(staticLevel);


        Integer maxPosition = levelRepository.findMaxPositionUnderParent(staticLevel.getNode());
        if (maxPosition == null) {
            maxPosition = 0;
        }

        ArrChange change = arrangementService.createChange();
        ArrLevel newLevel = arrangementService.createLevel(change, staticLevel.getNode(), maxPosition + 1);

        return newLevel;
    }


    /**
     * Zkontroluje, pře přesouvaný uzel není rodičem cílového uzlu.
     *
     * @param movedNode  přesouvaný uzel
     * @param targetNode cílový statický uzel
     */
    private void checkCycle(ArrLevel movedNode, ArrLevel targetNode) {
        Assert.notNull(movedNode);
        Assert.notNull(targetNode);

        if (targetNode.getNodeParent() == null) {
            return;
        }

        if (movedNode.getNode().equals(targetNode.getNodeParent())) {
            throw new IllegalStateException("Přesouvaný uzel je rodičem cílového uzlu. Přesun nelze provést.");
        }

        List<ArrLevel> parentNodes = levelRepository.findByNodeAndDeleteChangeIsNull(targetNode.getNodeParent());
        for (ArrLevel parentNode : parentNodes) {
            checkCycle(movedNode, parentNode);
        }
    }


    /**
     * Smaže uzel (uzamkne) a vytvoří jeho kopii.
     *
     * @param node   uzel
     * @param change změna smazání
     * @return nový level
     */
    private ArrLevel createNewLevelVersion(ArrLevel node, ArrChange change) {
        Assert.notNull(node);
        Assert.notNull(change);

        ArrLevel newNode = copyLevelData(node);
        newNode.setCreateChange(change);

        node.setDeleteChange(change);
        levelRepository.save(node);

        return newNode;
    }

    /**
     * Vytvoří kopii levelu.
     *
     * @param level level
     * @return kopie
     */
    private ArrLevel copyLevelData(ArrLevel level) {
        Assert.notNull(level);

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
    private void shiftNodes(Collection<ArrLevel> nodesToShift, ArrChange change, final int firstPosition) {
        Assert.notNull(nodesToShift);
        Assert.notNull(change);

        int position = firstPosition;
        for (ArrLevel node : nodesToShift) {
            ArrLevel newNode = createNewLevelVersion(node, change);
            newNode.setPosition(position++);
            levelRepository.save(newNode);
        }
    }


    /**
     * Najde seznam uzlů k přesunutí.
     *
     * @param movedLevel uzel
     * @return seznam uzlů se stejným rodičem a vyšší pozicí
     */
    private List<ArrLevel> nodesToShift(ArrLevel movedLevel) {
        Assert.notNull(movedLevel);

        return levelRepository.findByParentNodeAndPositionGreaterThanOrderByPositionAsc(movedLevel.getNodeParent(),
                movedLevel.getPosition());
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
