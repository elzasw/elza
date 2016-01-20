package cz.tacr.elza.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.api.vo.NodeTypeOperation;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.repository.FindingAidVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.eventnotification.EventFactory;
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
    private IEventNotificationService eventNotificationService;


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

        ArrLevel staticLevelParent = lockNode(staticParentNode, version);
        ArrLevel transportLevelParent = transportParentNode.equals(staticParentNode) ? staticLevelParent
                                                                                     : lockNode(transportParentNode,
                                                                                             version);
        ArrLevel staticLevel = levelRepository
                .findNodeInRootTreeByNodeId(staticNode, version.getRootLevel().getNode(), version.getLockChange());
        Assert.notNull(staticLevel);


        List<ArrLevel> transportLevels = new ArrayList<>(transportNodes.size());
        for (ArrNode transportNode : transportNodes) {
            ArrLevel transportLevel = transportNode.equals(staticParentNode)
                                      ? staticLevelParent : lockNode(transportNode, version);

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
        isValidAndOpenVersion(versionId);

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

        ArrLevel staticLevelParent = lockNode(staticParentNode, version);
        ArrLevel transportLevelParent = transportParentNode.equals(staticParentNode) ? staticLevelParent
                                                                                     : lockNode(transportParentNode,
                                                                                             version);
        ArrLevel staticLevel = levelRepository
                .findNodeInRootTreeByNodeId(staticNode, version.getRootLevel().getNode(), version.getLockChange());
        Assert.notNull(staticLevel);


        List<ArrLevel> transportLevels = new ArrayList<>(transportNodes.size());
        for (ArrNode transportNode : transportNodes) {
            ArrLevel transportLevel = transportNode.equals(staticParentNode)
                                      ? staticLevelParent : lockNode(transportNode, version);

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
        isValidAndOpenVersion(versionId);

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
        ArrLevel staticLevel = lockNode(staticNode, version);
        ArrLevel transportLevelParent = staticNode.equals(transportParentNode) ? staticLevel
                                                                               : lockNode(transportParentNode, version);

        List<ArrLevel> transportLevels = new ArrayList<>(transportNodes.size());

        for (ArrNode transportNode : transportNodes) {
            if (transportNode.equals(staticNode)) {
                throw new IllegalStateException("Nelze vložit záznam na stejné místo ve stromu");
            }

            ArrLevel transportLevel = lockNode(transportNode, version);

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
        isValidAndOpenVersion(versionId);

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
                EventFactory.createMoveEvent(EventType.MOVE_LEVEL_BEFORE, staticLevel, transportLevels, version));
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
            addInLevel(newLevel, parentNode, position++);
        }
        return position;
    }


    /**
     * Provede uzamčení nodu (zvýšení verze uzlu)
     *
     * @param lockNode uzamykaný node
     * @param version  verze stromu, do které patří uzel
     * @return level nodu
     */
    private ArrLevel lockNode(final ArrNode lockNode, final ArrFindingAidVersion version) {
        ArrLevel lockLevel = levelRepository
                .findNodeInRootTreeByNodeId(lockNode, version.getRootLevel().getNode(), version.getLockChange());
        Assert.notNull(lockLevel);
        ArrNode staticNodeDb = lockLevel.getNode();

        lockNode.setUuid(staticNodeDb.getUuid());
        lockNode.setLastUpdate(LocalDateTime.now());
        nodeRepository.save(lockNode);

        return lockLevel;
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
     * Kontrola verze, že existuje v DB a není uzavřena.
     *
     * @param versionId Identifikátor verze
     * @return verze archivni pomucky
     */
    private ArrFindingAidVersion isValidAndOpenVersion(Integer versionId) {
        Assert.notNull(versionId);
        ArrFindingAidVersion version = findingAidVersionRepository.findOne(versionId);
        if (version == null) {
            throw new IllegalArgumentException("Verze neexistuje");
        }
        if (version.getLockChange() != null) {
            throw new IllegalArgumentException("Aktuální verze je zamčená");
        }
        return version;
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

        ArrLevel newNode = copyLevel(node);
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
    private ArrLevel copyLevel(ArrLevel level) {
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
     * Vloží level do rodiče na konkrétní pozici.
     *
     * @param level      level
     * @param parentNode rodič
     * @param position   pozice
     * @return level
     */
    private ArrLevel addInLevel(ArrLevel level, ArrNode parentNode, Integer position) {
        Assert.notNull(level);
        Assert.notNull(position);

        level.setNodeParent(parentNode);
        level.setPosition(position);
        return levelRepository.save(level);
    }
}
