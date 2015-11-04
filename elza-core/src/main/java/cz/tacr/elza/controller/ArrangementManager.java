package cz.tacr.elza.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.api.exception.ConcurrentUpdateException;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrDescItemPartyRef;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrLevelExt;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.ArrPacketType;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.RulDescItemConstraint;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.domain.vo.ArrCalendarTypes;
import cz.tacr.elza.domain.vo.ArrDescItemSavePack;
import cz.tacr.elza.domain.vo.ArrDescItems;
import cz.tacr.elza.domain.vo.ArrLevelWithExtraNode;
import cz.tacr.elza.domain.vo.ArrNodeHistoryItem;
import cz.tacr.elza.domain.vo.ArrNodeHistoryPack;
import cz.tacr.elza.domain.vo.ArrNodeRegisterPack;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.CalendarTypeRepository;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DescItemConstraintRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;
import cz.tacr.elza.repository.FindingAidRepository;
import cz.tacr.elza.repository.FindingAidVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRegisterRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.PacketRepository;
import cz.tacr.elza.repository.PacketTypeRepository;
import cz.tacr.elza.repository.RuleSetRepository;


/**
 * Implementace API pro archivní pomůcku a hierarchický přehled včetně atributů.
 *
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@RestController
@RequestMapping("/api/arrangementManager")
public class ArrangementManager implements cz.tacr.elza.api.controller.ArrangementManager<ArrFindingAid, ArrFindingAidVersion,
    ArrDescItem, ArrDescItemSavePack, ArrLevel, ArrLevelWithExtraNode, ArrNode, ArrDescItems, ArrNodeHistoryPack,
    ArrCalendarTypes, ArrNodeRegister, ArrNodeRegisterPack, ArrPacket> {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private FindingAidRepository findingAidRepository;

    @Autowired
    private ArrangementTypeRepository arrangementTypeRepository;

    @Autowired
    private FindingAidVersionRepository findingAidVersionRepository;

    @Autowired
    private RuleSetRepository ruleSetRepository;

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private ChangeRepository faChangeRepository;

    @Autowired
    private DataRepository arrDataRepository;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private DescItemConstraintRepository descItemConstraintRepository;

    @Autowired
    private RuleManager ruleManager;

    @Autowired
    private DescItemTypeRepository descItemTypeRepository;

    @Autowired
    private DescItemSpecRepository descItemSpecRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private CalendarTypeRepository calendarTypeRepository;

    @Autowired
    private DescItemFactory descItemFactory;

    @Autowired
    private NodeRegisterRepository nodeRegisterRepository;

    @Autowired
    private PacketRepository packetRepository;

    @Autowired
    private PacketTypeRepository packetTypeRepository;


    /**
     * Vytvoří novou archivní pomůcku se zadaným názvem. Jako datum založení vyplní aktuální datum a čas.
     *
     * @param name název archivní pomůcky
     * @return nová archivní pomůcka
     */
    private ArrFindingAid createFindingAid(final String name) {
        Assert.hasText(name);

        ArrFindingAid findingAid = new ArrFindingAid();
        findingAid.setCreateDate(LocalDateTime.now());
        findingAid.setName(name);
        findingAidRepository.save(findingAid);

        return findingAid;
    }

    @Override
    @Transactional
    @RequestMapping(value = "/createFindingAid", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE,
            params = {"name", "arrangementTypeId", "ruleSetId"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrFindingAid createFindingAid(@RequestParam(value = "name") final String name,
                                          @RequestParam(value = "arrangementTypeId") final Integer arrangementTypeId,
                                          @RequestParam(value = "ruleSetId") final Integer ruleSetId) {
        Assert.hasText(name);
        Assert.notNull(arrangementTypeId);
        Assert.notNull(ruleSetId);

        ArrFindingAid findingAid = createFindingAid(name);

        RulArrangementType arrangementType = arrangementTypeRepository.getOne(arrangementTypeId);
        RulRuleSet ruleSet = ruleSetRepository.getOne(ruleSetId);

        Assert.isTrue(ruleSet.equals(arrangementType.getRuleSet()));

        ArrChange change = createChange();

        ArrLevel rootNode = createLevel(change, null);
        createVersion(change, findingAid, arrangementType, ruleSet, rootNode);

        return findingAid;
    }

    private ArrLevel createLevel(final ArrChange createChange, final ArrNode parentNode) {
        ArrLevel level = new ArrLevel();
        level.setPosition(1);
        level.setCreateChange(createChange);
        level.setNodeParent(parentNode);
        level.setNode(createNode());
        return levelRepository.save(level);
    }

    private ArrNode createNode() {
        ArrNode node = new ArrNode();
        node.setLastUpdate(LocalDateTime.now());
        return nodeRepository.save(node);
    }

    private ArrLevel createAfterInLevel(ArrChange change, ArrLevel level) {
        Assert.notNull(change);
        Assert.notNull(level);

        List<ArrLevel> levelsToShift = levelRepository.findByParentNodeAndPositionGreaterThanOrderByPositionAsc(
                level.getNodeParent(), level.getPosition());
        shiftNodesDown(levelsToShift, change);

        return createLevel(change, level.getNodeParent(), level.getPosition() + 1);
    }

    private ArrLevel createBeforeInLevel(final ArrChange change, final ArrLevel level) {
        Assert.notNull(change);
        Assert.notNull(level);


        List<ArrLevel> levelsToShift = levelRepository.findByParentNodeAndPositionGreaterThanOrderByPositionAsc(
                level.getNodeParent(), level.getPosition() - 1);
        shiftNodesDown(levelsToShift, change);

        return createLevel(change, level.getNodeParent(), level.getPosition());
    }

    private ArrLevel createNewLevelVersion(ArrLevel node, ArrChange change) {
        Assert.notNull(node);
        Assert.notNull(change);

        ArrLevel newNode = copyLevel(node);
        newNode.setCreateChange(change);

        node.setDeleteChange(change);
        levelRepository.save(node);

        return newNode;
    }

    private ArrLevel copyLevel(ArrLevel node) {
        Assert.notNull(node);

        ArrLevel newNode = new ArrLevel();
        newNode.setNode(node.getNode());
        newNode.setNodeParent(node.getNodeParent());
        newNode.setPosition(node.getPosition());

        return newNode;
    }

    /**
     * vytvoří level jako posledního potomka zadaného kořenového levlu.
     * @param createChange datum vytvoření
     * @param node         kořen
     * @return vytvořený level.
     */
    private ArrLevel createLastInLevel(ArrChange createChange, ArrLevel node) {
        Assert.notNull(createChange);
        Assert.notNull(node);

        Integer maxPosition = levelRepository.findMaxPositionUnderParent(node.getNode());
        if (maxPosition == null) {
            maxPosition = 0;
        }

        return createLevel(createChange, node.getNode(), maxPosition + 1);
    }

    private ArrLevel createLevel(final ArrChange createChange, final ArrNode parentNode, final Integer position) {
        Assert.notNull(createChange);

        ArrLevel level = new ArrLevel();
        level.setPosition(position);
        level.setCreateChange(createChange);
        level.setNodeParent(parentNode);
        level.setNode(createNode());
        return levelRepository.save(level);
    }

    private ArrFindingAidVersion createVersion(final ArrChange createChange, final ArrFindingAid findingAid,
            final RulArrangementType arrangementType, final RulRuleSet ruleSet, final ArrLevel rootNode) {
        ArrFindingAidVersion version = new ArrFindingAidVersion();
        version.setCreateChange(createChange);
        version.setArrangementType(arrangementType);
        version.setFindingAid(findingAid);
        version.setRuleSet(ruleSet);
        version.setRootLevel(rootNode);
        return findingAidVersionRepository.save(version);
    }

    private ArrChange createChange() {
        ArrChange change = new ArrChange();
        change.setChangeDate(LocalDateTime.now());
        return faChangeRepository.save(change);
    }


    @Override
    @RequestMapping(value = "/deleteFindingAid", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, params = {"findingAidId"})
    @Transactional
    public void deleteFindingAid(@RequestParam(value = "findingAidId") final Integer findingAidId) {
        Assert.notNull(findingAidId);

        if (!findingAidRepository.exists(findingAidId)) {
            return;
        }

        ArrFindingAidVersion version = getOpenVersionByFindingAidId(findingAidId);

        ArrChange change = createChange();


        deleteLevelCascade(version.getRootLevel(), change);

        for (ArrFindingAidVersion deleteVersion : findingAidVersionRepository
                .findVersionsByFindingAidIdOrderByCreateDateAsc(findingAidId)) {
            findingAidVersionRepository.delete(deleteVersion);
        }
        findingAidRepository.delete(findingAidId);
    }


    @Override
    @RequestMapping(value = "/getFindingAids", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<ArrFindingAid> getFindingAids() {
        return findingAidRepository.findAll();
    }

    @RequestMapping(value = "/updateFindingAid", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Override
    @Transactional
    public ArrFindingAid updateFindingAid(@RequestBody ArrFindingAid findingAid) {
        Assert.notNull(findingAid);

        Integer findingAidId = findingAid.getFindingAidId();
        if (!findingAidRepository.exists(findingAidId)) {
            throw new ConcurrentUpdateException("Archivní pomůcka s identifikátorem " + findingAidId + " již neexistuje.");
        }

        return findingAidRepository.save(findingAid);
    }

    @Override
    @RequestMapping(value = "/getFindingAidVersions", method = RequestMethod.GET, params = {"findingAidId"}, consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ArrFindingAidVersion> getFindingAidVersions(@RequestParam(value = "findingAidId") final Integer findingAidId) {
        Assert.notNull(findingAidId);

        return findingAidVersionRepository.findVersionsByFindingAidIdOrderByCreateDateAsc(findingAidId);
    }

    @Override
    @RequestMapping(value = "/getFindingAid", method = RequestMethod.GET, params = {"findingAidId"}, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrFindingAid getFindingAid(final Integer findingAidId) {
        Assert.notNull(findingAidId);
        return findingAidRepository.findOne(findingAidId);
    }

    @Override
    @Transactional
    @RequestMapping(value = "/approveVersion", method = RequestMethod.PUT)
    public ArrFindingAidVersion approveVersion(@RequestBody final ArrFindingAidVersion version,
            @RequestParam("arrangementTypeId") final Integer arrangementTypeId,
            @RequestParam("ruleSetId") final Integer ruleSetId) {
        Assert.notNull(version);
        Assert.notNull(arrangementTypeId);
        Assert.notNull(ruleSetId);

        ArrFindingAid findingAid = version.getFindingAid();
        Integer findingAidId = findingAid.getFindingAidId();
        if (!findingAidRepository.exists(findingAidId)) {
            throw new ConcurrentUpdateException("Archivní pomůcka s identifikátorem " + findingAidId + " již neexistuje.");
        }

        if (version.getLockChange() != null) {
            throw new ConcurrentUpdateException("Verze byla již uzavřena");
        }

        ArrChange change = createChange();
        version.setLockChange(change);
        findingAidVersionRepository.save(version);

        RulArrangementType arrangementType = arrangementTypeRepository.findOne(arrangementTypeId);
        RulRuleSet ruleSet = ruleSetRepository.findOne(ruleSetId);

        Assert.isTrue(ruleSet.equals(arrangementType.getRuleSet()));

        return createVersion(change, findingAid, arrangementType, ruleSet, version.getRootLevel());
    }


    @Override
    @Transactional
    @RequestMapping(value = "/addLevelBefore", method = RequestMethod.PUT)
    public ArrLevelWithExtraNode addLevelBefore(@RequestBody ArrLevelWithExtraNode levelWithParentNode) {
        Assert.notNull(levelWithParentNode);
        ArrLevel node = levelWithParentNode.getLevel();
        ArrNode parentNode = levelWithParentNode.getExtraNode();
        Integer versionId = levelWithParentNode.getFaVersionId();

        isValidAndOpenVersion(versionId);
        isValidArrFaLevel(node);
        isValidNode(parentNode);

        ArrChange change = createChange();
        ArrLevel faLevelRet = createBeforeInLevel(change, node);
        parentNode.setLastUpdate(LocalDateTime.now());
        parentNode = nodeRepository.save(parentNode);

        entityManager.flush();
        entityManager.refresh(faLevelRet);

        ArrLevelWithExtraNode levelWithParentNodeRet = new ArrLevelWithExtraNode();
        levelWithParentNodeRet.setLevel(faLevelRet);
        levelWithParentNodeRet.setExtraNode(parentNode);

        return levelWithParentNodeRet;
    }

    @Override
    @Transactional
    @RequestMapping(value = "/addLevelAfter", method = RequestMethod.PUT)
    public ArrLevelWithExtraNode addLevelAfter(@RequestBody ArrLevelWithExtraNode levelWithParentNode) {
        Assert.notNull(levelWithParentNode);
        ArrLevel node = levelWithParentNode.getLevel();
        ArrNode parentNode = levelWithParentNode.getExtraNode();
        Integer versionId = levelWithParentNode.getFaVersionId();

        isValidAndOpenVersion(versionId);
        isValidArrFaLevel(node);
        isValidNode(parentNode);

        ArrChange change = createChange();
        ArrLevel faLevelRet = createAfterInLevel(change, node);

        parentNode.setLastUpdate(LocalDateTime.now());
        parentNode = nodeRepository.save(parentNode);

        entityManager.flush();
        entityManager.refresh(faLevelRet);

        ArrLevelWithExtraNode levelWithParentNodeRet = new ArrLevelWithExtraNode();
        levelWithParentNodeRet.setLevel(faLevelRet);
        levelWithParentNodeRet.setExtraNode(parentNode);

        return levelWithParentNodeRet;
    }

    @Override
    @Transactional
    @RequestMapping(value = "/addLevelChild", method = RequestMethod.PUT)
    public ArrLevelWithExtraNode addLevelChild(@RequestBody ArrLevelWithExtraNode levelWithParentNode) {
        Assert.notNull(levelWithParentNode);

        ArrLevel node = levelWithParentNode.getLevel();
        Integer versionId = levelWithParentNode.getFaVersionId();

        isValidAndOpenVersion(versionId);
        isValidArrFaLevel(node);

        ArrChange change = createChange();
        ArrLevel faLevelRet = createLastInLevel(change, node);

        node.getNode().setLastUpdate(LocalDateTime.now());
        node.setNode(nodeRepository.save(node.getNode()));

        ArrLevelWithExtraNode levelWithParentNodeRet = new ArrLevelWithExtraNode();
        levelWithParentNodeRet.setLevel(faLevelRet);
        levelWithParentNodeRet.setExtraNode(node.getNode());

        return levelWithParentNodeRet;
    }

    @Override
    @Transactional
    @RequestMapping(value = "/moveLevelBefore", method = RequestMethod.PUT)
    public ArrLevelWithExtraNode moveLevelBefore(@RequestBody ArrLevelWithExtraNode levelWithFollowerNode) {
        Assert.notNull(levelWithFollowerNode);

        ArrLevel level = levelWithFollowerNode.getLevel();
        ArrLevel targetLevel = levelWithFollowerNode.getLevelTarget();
        ArrNode targetNode = targetLevel.getNode();
        Integer versionId = levelWithFollowerNode.getFaVersionId();

        isValidAndOpenVersion(versionId);
        isValidArrFaLevel(level);
        isValidArrFaLevel(targetLevel);
        isValidNode(targetNode);

        if (targetNode == null || targetLevel == null) {
            throw new IllegalArgumentException("Přesun se nezdařil. Záznam byl pravděpodobně smazán jiným uživatelem. Aktualizujte stránku");
        }

        ArrLevel follower = findNodeInRootTreeByNodeId(targetNode, levelWithFollowerNode.getRootNode());

        if (level == null || follower == null) {
            throw new IllegalArgumentException("Přesun se nezdařil. Záznam byl pravděpodobně smazán jiným uživatelem. Aktualizujte stránku");
        }
        if (level.equals(follower)) {
            throw new IllegalStateException("Nelze vložit záznam na stejné místo ve stromu");
        }

        level.getNodeParent().setLastUpdate(LocalDateTime.now());
        nodeRepository.save(level.getNodeParent());

        level.getNode().setLastUpdate(LocalDateTime.now());
        nodeRepository.save(level.getNode());

        targetLevel.getNodeParent().setLastUpdate(LocalDateTime.now());
        targetLevel.setNodeParent(nodeRepository.save(targetLevel.getNodeParent()));

        targetNode.setLastUpdate(LocalDateTime.now());
        nodeRepository.save(targetNode);

        checkCycle(level, follower);

        ArrChange change = createChange();
        List<ArrLevel> nodesToShiftUp = nodesToShift(level);
        List<ArrLevel> nodesToShiftDown = nodesToShift(follower);
        nodesToShiftDown.add(follower);

        Integer position;
        if (level.getNodeParent().equals(follower.getNodeParent())) {
            Collection<ArrLevel> nodesToShift = CollectionUtils.disjunction(nodesToShiftDown, nodesToShiftUp);
            if (level.getPosition() > follower.getPosition()) {
                nodesToShift.remove(level);
                shiftNodesDown(nodesToShift, change);
                position = follower.getPosition();
            } else {
                shiftNodesUp(nodesToShift, change);
                position = follower.getPosition() - 1;
            }
        } else {
            shiftNodesDown(nodesToShiftDown, change);
            shiftNodesUp(nodesToShiftUp, change);
            position = follower.getPosition();
        }

        ArrLevel newLevel = createNewLevelVersion(level, change);
        ArrLevel levelRet = addInLevel(newLevel, follower.getNodeParent(), position);

        ArrLevelWithExtraNode levelWithFollowerNodeRet = new ArrLevelWithExtraNode();
        levelWithFollowerNodeRet.setLevel(levelRet);
        levelWithFollowerNodeRet.setLevelTarget(targetLevel);

        return levelWithFollowerNodeRet;
    }

    @Override
    @Transactional
    @RequestMapping(value = "/moveLevelUnder", method = RequestMethod.PUT)
    public ArrLevelWithExtraNode moveLevelUnder(@RequestBody ArrLevelWithExtraNode levelWithUnderNode) {
        Assert.notNull(levelWithUnderNode);

        ArrLevel level = levelWithUnderNode.getLevel();
        ArrNode parentNode = levelWithUnderNode.getExtraNode();
        Integer versionId = levelWithUnderNode.getFaVersionId();

        isValidAndOpenVersion(versionId);
        isValidArrFaLevel(level);
        isValidNode(parentNode);

        if (parentNode == null) {
            throw new IllegalArgumentException("Přesun se nezdařil. Záznam byl pravděpodobně smazán jiným uživatelem. Aktualizujte stránku");
        }

        ArrLevel parent = findNodeInRootTreeByNodeId(parentNode, levelWithUnderNode.getRootNode());
        if (level == null || parent == null) {
            throw new IllegalArgumentException("Přesun se nezdařil. Záznam byl pravděpodobně smazán jiným uživatelem. Aktualizujte stránku");
        }

        if (level.equals(parent)) {
            throw new IllegalStateException("Nelze vložit záznam sám do sebe");
        }

        level.getNode().setLastUpdate(LocalDateTime.now());
        level.setNode(nodeRepository.save(level.getNode()));

        // vkládaný nesmí být rodičem uzlu pod který ho vkládám
        checkCycle(level, parent);

        ArrChange change = createChange();
        shiftNodesUp(nodesToShift(level), change);
        ArrLevel newLevel = createNewLevelVersion(level, change);

        ArrLevel faLevelRet = addLastInLevel(newLevel, parent.getNode());

        parentNode.setLastUpdate(LocalDateTime.now());
        parentNode = nodeRepository.save(parentNode);

        ArrLevelWithExtraNode levelWithPredecessorNodeRet = new ArrLevelWithExtraNode();
        levelWithPredecessorNodeRet.setLevel(faLevelRet);
        levelWithPredecessorNodeRet.setExtraNode(parentNode);

        return levelWithPredecessorNodeRet;
    }

    @Override
    @Transactional
    @RequestMapping(value = "/moveLevelAfter", method = RequestMethod.PUT)
    public ArrLevelWithExtraNode moveLevelAfter(@RequestBody ArrLevelWithExtraNode levelWithPredecessorNode) {
        Assert.notNull(levelWithPredecessorNode);

        ArrLevel level = levelWithPredecessorNode.getLevel();
        ArrLevel predecessorLevel = levelWithPredecessorNode.getLevelTarget();
        ArrNode predecessorNode = predecessorLevel.getNode();
        Integer versionId = levelWithPredecessorNode.getFaVersionId();

        isValidAndOpenVersion(versionId);
        isValidArrFaLevel(level);
        isValidArrFaLevel(predecessorLevel);
        isValidNode(predecessorNode);

        if (predecessorNode == null) {
            throw new IllegalArgumentException("Přesun se nezdařil. Záznam byl pravděpodobně smazán jiným uživatelem. Aktualizujte stránku");
        }

        ArrLevel predecessor = findNodeInRootTreeByNodeId(predecessorNode, levelWithPredecessorNode.getRootNode());
        if (level == null || predecessor == null) {
            throw new IllegalArgumentException("Přesun se nezdařil. Záznam byl pravděpodobně smazán jiným uživatelem. Aktualizujte stránku");
        }
        if (level.equals(predecessor)) {
            throw new IllegalStateException("Nelze vložit záznam na stejné místo ve stromu");
        }

        level.getNodeParent().setLastUpdate(LocalDateTime.now());
        nodeRepository.save(level.getNodeParent());

        level.getNode().setLastUpdate(LocalDateTime.now());
        nodeRepository.save(level.getNode());

        predecessorLevel.getNodeParent().setLastUpdate(LocalDateTime.now());
        predecessorLevel.setNodeParent(nodeRepository.save(predecessorLevel.getNodeParent()));

        predecessorNode.setLastUpdate(LocalDateTime.now());
        nodeRepository.save(predecessorNode);

        // vkládaný nesmí být rodičem uzlu za který ho vkládám
        checkCycle(level, predecessor);

        ArrChange change = createChange();
        List<ArrLevel> nodesToShiftUp = nodesToShift(level);
        List<ArrLevel> nodesToShiftDown = nodesToShift(predecessor);
        Integer position;
        if (level.getNodeParent().equals(predecessor.getNodeParent())) {
            Collection<ArrLevel> nodesToShift = CollectionUtils.disjunction(nodesToShiftDown, nodesToShiftUp);
            if (level.getPosition() > predecessor.getPosition()) {
                nodesToShift.remove(level);
                shiftNodesDown(nodesToShift, change);
                position = predecessor.getPosition() + 1;
            } else {
                shiftNodesUp(nodesToShift, change);
                position = predecessor.getPosition();
            }
        } else {
            shiftNodesDown(nodesToShiftDown, change);
            shiftNodesUp(nodesToShiftUp, change);
            position = predecessor.getPosition() + 1;
        }


        ArrLevel newLevel = createNewLevelVersion(level, change);

        ArrLevel faLevelRet = addInLevel(newLevel, predecessor.getNodeParent(), position);

        ArrLevelWithExtraNode levelWithPredecessorNodeRet = new ArrLevelWithExtraNode();
        levelWithPredecessorNodeRet.setLevel(faLevelRet);
        levelWithPredecessorNodeRet.setLevelTarget(predecessorLevel);

        return levelWithPredecessorNodeRet;
    }

    /**
     * Kontrola uzlu, že existuje v DB.
     * @param node Kontrolovaný uzel
     */
    private void isValidNode(final ArrNode node) {
        Assert.notNull(node);
        ArrNode dbNode = nodeRepository.findOne(node.getNodeId());
        if (dbNode == null) {
            throw new IllegalArgumentException("Neplatný uzel");
        }
    }

    /**
     * Kontrola uzlu, že existuje v DB a není smazán.
     * @param node Kontrolovaný uzel
     */
    private void isValidArrFaLevel(final ArrLevel node) {
        Assert.notNull(node);
        ArrLevel dbNode = levelRepository.findOne(node.getLevelId());
        if (dbNode == null) {
            throw new IllegalArgumentException("Neplatný uzel");
        }
    }

    /**
     * Kontrola verze, že existuje v DB a není uzavřena.
     * @param versionId Identifikátor verze
     */
    private void isValidAndOpenVersion(Integer versionId) {
        Assert.notNull(versionId);
        ArrFindingAidVersion version = findingAidVersionRepository.findOne(versionId);
        if (version == null) {
            throw new IllegalArgumentException("Verze neexistuje");
        }
        if (version.getLockChange() != null) {
            throw new IllegalArgumentException("Aktuální verze je zamčená");
        }
    }

    /**
     * Zjistí, jestli je daný node ve stejném stromu, jako je daný kořen. Pokud máme dva nody se stejným nodeId v
     * různých stromech, je potřeba najít tu entitu pro konkrétní strom.
     *
     * @param node     id nodu
     * @param rootNode id kořenu
     * @return nalezený level pro daný strom nebo null, pokud nebyl nalezen
     */
    private ArrLevel findNodeInRootTreeByNodeId(final ArrNode node, final ArrNode rootNode) {
        List<ArrLevel> levelsByNode = levelRepository.findByNodeAndDeleteChangeIsNull(node);

        if (levelsByNode.isEmpty()) {
            throw new IllegalArgumentException("Entita byla změněna nebo odstraněna. Načtěte znovu entitu a opakujte akci.");
        } else if (levelsByNode.size() == 1) {
            return levelsByNode.iterator().next();
        }


        for (ArrLevel arrFaLevel : levelsByNode) {
            if (isLevelInRootTree(arrFaLevel, rootNode)) {
                return arrFaLevel;
            }
        }

        return null;
    }

    /**
     * zjistí zda je level v zadané hierarchické struktuře.
     * @param level    testovaný level.
     * @param rootNode kořen zadané hierarchické struktury.
     * @return true pokud je level v zadané hierarchické struktuře.
     */
    private boolean isLevelInRootTree(final ArrLevel level, final ArrNode rootNode) {
        if (level.getNode().equals(rootNode) || rootNode.equals(level.getNodeParent())) {
            return true;
        }

        List<ArrLevel> levelsByNode = levelRepository.findByNodeAndDeleteChangeIsNull(level.getNodeParent());

        boolean result = false;
        for (ArrLevel parentLevel : levelsByNode) {
            result = result || isLevelInRootTree(parentLevel, rootNode);
        }

        return result;
    }


    private void checkCycle(ArrLevel movedNode, ArrLevel targetNode) {
        Assert.notNull(movedNode);
        Assert.notNull(targetNode);

        ArrLevel node = targetNode;
        if (node.getNodeParent() == null) {
            return;
        }

        if (movedNode.getNode().equals(node.getNodeParent())) {
            throw new IllegalStateException("Přesouvaný uzel je rodičem cílového uzlu. Přesun nelze provést.");
        }

        List<ArrLevel> parentNodes = levelRepository.findByNodeAndDeleteChangeIsNull(node.getNodeParent());
        for (ArrLevel parentNode : parentNodes) {
            checkCycle(movedNode, parentNode);
        }
    }

    private ArrLevel addLastInLevel(ArrLevel level, ArrNode parentNode) {
        Assert.notNull(level);
        Assert.notNull(parentNode);

        Integer maxPosition = levelRepository.findMaxPositionUnderParent(parentNode);
        if (maxPosition == null) {
            maxPosition = 0;
        }
        level.setPosition(maxPosition + 1);
        level.setNodeParent(parentNode);

        return levelRepository.save(level);
    }

    private void shiftNodesDown(Collection<ArrLevel> nodesToShift, ArrChange change) {
        Assert.notNull(nodesToShift);
        Assert.notNull(change);

        for (ArrLevel node : nodesToShift) {
            ArrLevel newNode = createNewLevelVersion(node, change);
            newNode.setPosition(node.getPosition() + 1);
            levelRepository.save(newNode);
        }
    }

    private void shiftNodesUp(Collection<ArrLevel> nodesToShift, ArrChange change) {
        Assert.notNull(nodesToShift);
        Assert.notNull(change);

        for (ArrLevel node : nodesToShift) {
            ArrLevel newNode = createNewLevelVersion(node, change);
            newNode.setPosition(node.getPosition() - 1);
            levelRepository.save(newNode);
        }
    }

    private List<ArrLevel> nodesToShift(ArrLevel movedLevel) {
        Assert.notNull(movedLevel);

        return levelRepository.findByParentNodeAndPositionGreaterThanOrderByPositionAsc(movedLevel.getNodeParent(),
                movedLevel.getPosition());
    }

    private ArrLevel addInLevel(ArrLevel level, ArrNode parentNode, Integer position) {
        Assert.notNull(level);
        Assert.notNull(position);

        level.setNodeParent(parentNode);
        level.setPosition(position);
        return levelRepository.save(level);
    }

    @Override
    @Transactional
    @RequestMapping(value = "/deleteLevel", method = RequestMethod.PUT)
    public ArrLevelWithExtraNode deleteLevel(@RequestBody ArrLevelWithExtraNode levelWithParentNode) {
        Assert.notNull(levelWithParentNode);

        ArrLevel faLevel = levelWithParentNode.getLevel();
        ArrNode node = faLevel.getNode();
        ArrNode parentNode = levelWithParentNode.getExtraNode();
        Integer versionId = levelWithParentNode.getFaVersionId();

        isValidAndOpenVersion(versionId);
        ArrLevel level = findNodeInRootTreeByNodeId(faLevel.getNode(), levelWithParentNode.getRootNode());
        if (level == null || level.getDeleteChange() != null) {
            throw new IllegalArgumentException("Záznam již byl smazán");
        }

        if (node == null) {
            throw new IllegalArgumentException("Záznam neexistuje");
        }

        ArrChange change = createChange();

        level = deleteLevelCascade(level, change);
        shiftNodesUp(nodesToShift(level), change);


        ArrLevelWithExtraNode levelWithParentNodeRet = new ArrLevelWithExtraNode();

        // zámky
        parentNode.setLastUpdate(LocalDateTime.now());
        parentNode = nodeRepository.save(parentNode);

        levelWithParentNodeRet.setLevel(level);
        levelWithParentNodeRet.setExtraNode(parentNode);

        return levelWithParentNodeRet;
    }

    public ArrLevel deleteLevelCascade(final ArrLevel level, final ArrChange deleteChange) {

        //pokud je level sdílený, smažeme pouze entitu, atributy ponecháme
        if (isLevelShared(level)) {
            return deleteLevelInner(level, deleteChange);
        }


        for (ArrLevel childLevel : levelRepository
                .findByNodeParentAndDeleteChangeIsNullOrderByPositionAsc(level.getNode())) {
            deleteLevelCascade(childLevel, deleteChange);
        }

        for (ArrDescItem descItem : descItemRepository.findByNodeAndDeleteChangeIsNull(level.getNode())) {
            deleteDescItemInner(descItem, deleteChange);
        }

        return deleteLevelInner(level, deleteChange);
    }

    private ArrLevel deleteLevelInner(final ArrLevel level, final ArrChange deleteChange) {
        Assert.notNull(level);

        ArrNode node = level.getNode();
        node.setLastUpdate(LocalDateTime.now());
        nodeRepository.save(node);

        level.setDeleteChange(deleteChange);
        return levelRepository.save(level);
    }


    private boolean isLevelShared(final ArrLevel level) {
        Assert.notNull(level);

        return levelRepository.countByNode(level.getNode()) > 1;
    }

    @Override
    @RequestMapping(value = "/getOpenVersionByFindingAidId", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrFindingAidVersion getOpenVersionByFindingAidId(@RequestParam(value = "findingAidId") Integer findingAidId) {
        Assert.notNull(findingAidId);
        ArrFindingAidVersion faVersion = findingAidVersionRepository.findByFindingAidIdAndLockChangeIsNull(findingAidId);

        return faVersion;
    }

    @Override
    @RequestMapping(value = "/getVersion", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrFindingAidVersion getFaVersionById(@RequestParam("versionId") final Integer versionId) {
        Assert.notNull(versionId);
        return findingAidVersionRepository.findOne(versionId);
    }

    @Override
    @RequestMapping(value = "/findSubLevelsExt", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public List<ArrLevelExt> findSubLevels(@RequestParam(value = "nodeId") Integer nodeId,
            @RequestParam(value = "versionId", required = false)  Integer versionId,
            @RequestParam(value = "formatData", required = false)  String formatData,
            @RequestParam(value = "descItemTypeIds", required = false) Integer[] descItemTypeIds) {
        Assert.notNull(nodeId);

        ArrNode node = nodeRepository.findOne(nodeId);

        if (node == null) {
            throw new IllegalArgumentException("Záznam neexistuje");
        }

        ArrChange change = null;
        if (versionId != null) {
            ArrFindingAidVersion version = findingAidVersionRepository.findOne(versionId);
            change = version.getLockChange();
        }
        final List<ArrLevel> levelList;
        if (change == null) {
            levelList = levelRepository.findByNodeParentAndDeleteChangeIsNullOrderByPositionAsc(node);
        } else {
            levelList = levelRepository.findByParentNodeOrderByPositionAsc(node, change);
        }

        Set<ArrNode> nodes = new HashSet<>();
        for (ArrLevel arrFaLevel : levelList) {
            nodes.add(arrFaLevel.getNode());
        }

        final List<ArrData> dataList;
        if (nodes == null || nodes.isEmpty()) {
            dataList = new LinkedList<>();
        } else if (change == null) {
            dataList = arrDataRepository.findByNodesAndDeleteChangeIsNull(nodes);
        } else {
            dataList = arrDataRepository.findByNodesAndChange(nodes, change);
        }
        Map<Integer, List<ArrData>> dataMap =
                ElzaTools.createGroupMap(dataList, p -> p.getDescItem().getNode().getNodeId());

        Set<Integer> idItemTypeSet = createItemTypeSet(descItemTypeIds);
        final List<ArrLevelExt> resultList = new LinkedList<ArrLevelExt>();
        for (ArrLevel arrFaLevel : levelList) {
            ArrLevelExt levelExt = new ArrLevelExt();
            BeanUtils.copyProperties(arrFaLevel, levelExt);
            List<ArrData> dataNodeList = dataMap.get(arrFaLevel.getNode().getNodeId());
            readItemData(levelExt, dataNodeList, idItemTypeSet, formatData);
            resultList.add(levelExt);
        }
        return resultList;
    }

    @Override
    @RequestMapping(value = "/findSubLevels", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ArrLevel> findSubLevels(@RequestParam(value = "nodeId") Integer nodeId,
            @RequestParam(value = "versionId", required = false)  Integer versionId) {
        Assert.notNull(nodeId);

        ArrNode node = nodeRepository.findOne(nodeId);

        if (node == null) {
            throw new IllegalArgumentException("Záznam neexistuje");
        }

        ArrChange change = null;
        if (versionId != null) {
            ArrFindingAidVersion version = findingAidVersionRepository.findOne(versionId);
            change = version.getLockChange();
        }
        final List<ArrLevel> levelList;
        if (change == null) {
            levelList = levelRepository.findByNodeParentAndDeleteChangeIsNullOrderByPositionAsc(node);
        } else {
            levelList = levelRepository.findByParentNodeOrderByPositionAsc(node, change);
        }


//        for (ArrLevel faLevel : levelList) {
//            entityManager.refresh(faLevel);
//        }

        return levelList;
    }

    @RequestMapping(value = "/findLevels", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ArrLevel> findLevels(@RequestParam(value = "nodeId") Integer nodeId) {
        Assert.notNull(nodeId);

        ArrNode node = nodeRepository.findOne(nodeId);

        if (node == null) {
            throw new IllegalArgumentException("Záznam neexistuje");
        }

        return levelRepository.findByNodeOrderByCreateChangeAsc(node);
    }

    @Override
    @RequestMapping(value = "/getLevel", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrLevelExt getLevel(@RequestParam(value = "nodeId") Integer nodeId,
            @RequestParam(value = "versionId", required = false) Integer versionId,
            @RequestParam(value = "descItemTypeIds", required = false) Integer[] descItemTypeIds) {
        Assert.notNull(nodeId);

        ArrNode node = nodeRepository.findOne(nodeId);

        if (node == null) {
            throw new IllegalArgumentException("Záznam neexistuje");
        }

        ArrChange change = null;
        ArrFindingAidVersion version = null;
        if (versionId != null) {
            version = findingAidVersionRepository.findOne(versionId);
            change = version.getLockChange();
        }

        final ArrLevel level;
        final List<ArrData> dataList;
        if (change == null) {
            level = version == null ? levelRepository.findFirstByNodeAndDeleteChangeIsNull(node)
                                    : findNodeInRootTreeByNodeId(node, version.getRootLevel().getNode());
            dataList = arrDataRepository.findByNodeAndDeleteChangeIsNull(node);
        } else {
            level = levelRepository.findByNodeOrderByPositionAsc(node, change);
            dataList = arrDataRepository.findByNodeAndChange(node, change);
        }

        if (level == null) {
            throw new IllegalStateException("Nebyl nalezen záznam podle nodId " + nodeId + " a versionId " + versionId);
        }
        Set<Integer> idItemTypeSet = createItemTypeSet(descItemTypeIds);

        for (ArrData data : dataList) {
            entityManager.refresh(data);
        }

        ArrLevelExt levelExt = new ArrLevelExt();
        BeanUtils.copyProperties(level, levelExt);
        readItemData(levelExt, dataList, idItemTypeSet, null);

        for (ArrDescItem descItem : levelExt.getDescItemList()) {
            if (descItem instanceof ArrDescItemPartyRef) {
                ArrDescItemPartyRef partyRef = (ArrDescItemPartyRef) descItem;
                partyRef.getParty().getPreferredName().setParty(null);
            }
        }

        return levelExt;
    }

    /**
     * převod pole na {@link Set}.
     * @param descItemTypeIds
     * @return
     */
    private Set<Integer> createItemTypeSet(final Integer[] descItemTypeIds) {
        Set<Integer> idItemTypeSet = null;
        if (descItemTypeIds != null && descItemTypeIds.length > 0) {
            idItemTypeSet = new HashSet<>();
            for (Integer idItemType : descItemTypeIds) {
                idItemTypeSet.add(idItemType);
            }
        }
        return idItemTypeSet;
    }

    /**
     * doplní do nodu jeho atributy. Pokud je potřeba dodělá formátování.
     * @param levelExt nod (level) do kterého se budou přidávat atributy.
     * @param dataList seznam atributů
     * @param idItemTypeSet omezení na typ
     * @param formatData typ formátu pro text
     */
    private void readItemData(final ArrLevelExt levelExt, final List<ArrData> dataList,
            final Set<Integer> idItemTypeSet, final String formatData) {
        if (dataList == null) {
            return;
        }
        for (ArrData arrData : dataList) {
            Integer idItemType = arrData.getDescItem().getDescItemType().getDescItemTypeId();
            if (idItemTypeSet != null && !idItemTypeSet.contains(idItemType)) {
                continue;
            }

            ArrDescItem descItem = arrData.getDescItem();
            descItem = descItemFactory.getDescItem(descItem, formatData);
            levelExt.getDescItemList().add(descItem);

        }
    }

    @Override
    @RequestMapping(value = "/createDescriptionItem/{versionId}", method = RequestMethod.POST)
    @Transactional
    public ArrDescItem createDescriptionItem(@RequestBody ArrDescItem descItem,
                                                @PathVariable(value = "versionId") Integer versionId) {
        Assert.notNull(descItem);
        Assert.notNull(versionId);
        ArrChange change = createChange();
        Integer objectId = getNextDescItemObjectId();
        Map<RulDescItemType, Map<RulDescItemSpec, List<ArrDescItem>>> mapDescItems = new HashMap<>();
        ArrDescItem descItemRet = createDescriptionItemRaw(descItem, versionId, change, true, mapDescItems, objectId);
        saveChanges(mapDescItems, null, true);
        return descItemRet;
    }

    @Override
    @RequestMapping(value = "/updateDescriptionItem/{versionId}/{createNewVersion}", method = RequestMethod.POST)
    @Transactional
    public ArrDescItem updateDescriptionItem(@RequestBody ArrDescItem descItem,
                                                @PathVariable(value = "versionId") Integer versionId,
                                                @PathVariable(value = "createNewVersion") Boolean createNewVersion) {
        Assert.notNull(descItem);
        Assert.notNull(versionId);
        Assert.notNull(createNewVersion);

        ArrChange change = null;
        if (createNewVersion) {
            change = createChange();
        }
        Map<RulDescItemType, Map<RulDescItemSpec, List<ArrDescItem>>> mapDescItems = new HashMap<>();
        ArrDescItem descItemRet = updateDescriptionItemRaw(descItem, versionId, change, true, createNewVersion, mapDescItems);
        List<ArrDescItem> descItems = new ArrayList<>();
        descItems.add(descItemRet);
        saveChanges(mapDescItems, descItems, createNewVersion);
        return descItems.get(0);
    }

    @Override
    @RequestMapping(value = "/deleteDescriptionItem/{versionId}", method = RequestMethod.DELETE)
    @Transactional
    public ArrDescItem deleteDescriptionItem(@RequestBody ArrDescItem descItem, @PathVariable(value = "versionId") Integer versionId) {
        Assert.notNull(descItem);
        ArrChange change = createChange();
        Map<RulDescItemType, Map<RulDescItemSpec, List<ArrDescItem>>> mapDescItems = new HashMap<>();
        ArrDescItem descItemRet = deleteDescriptionItemRaw(descItem, versionId, change, true, mapDescItems);
        saveChanges(mapDescItems, null, true);
        return descItemRet;
    }

    @Override
    @RequestMapping(value = "/saveDescriptionItems", method = RequestMethod.POST)
    @Transactional
    public ArrDescItems saveDescriptionItems(@RequestBody ArrDescItemSavePack descItemSavePack) {
        Assert.notNull(descItemSavePack);

        List<ArrDescItem> deleteDescItems = descItemSavePack.getDeleteDescItems();
        Assert.notNull(deleteDescItems);

        List<ArrDescItem> descItems = descItemSavePack.getDescItems();
        Assert.notNull(descItems);

        // seřazení podle position
        descItems.sort((o1, o2) -> {
            Integer pos1 = o1.getPosition();
            Integer pos2 = o2.getPosition();
            if (pos1 == null && pos2 == null) {
                return 0;
            } else
            if (pos1 == null) {
                return -1;
            } else if (pos2 == null) {
                return 1;
            } else {
                return pos1.compareTo(pos2);
            }
        });

        Integer versionId = descItemSavePack.getFaVersionId();
        Assert.notNull(versionId);

        Boolean createNewVersion = descItemSavePack.getCreateNewVersion();
        Assert.notNull(createNewVersion);

        ArrNode node = descItemSavePack.getNode();
        Assert.notNull(node);
        node.setLastUpdate(LocalDateTime.now());
        nodeRepository.save(node);

        ArrayList<ArrDescItem> descItemsRet = new ArrayList<>();

        // analýza vstupních dat, roztřídění

        List<ArrDescItem> createDescItems = new ArrayList<>();
        List<ArrDescItem> updateDescItems = new ArrayList<>();

        // pouze informativní kvůli logice
        List<ArrDescItem> updatePositionDescItems = new ArrayList<>();

        for (ArrDescItem descItem : descItems) {
            Integer descItemObjectId = descItem.getDescItemObjectId();
            if (descItemObjectId != null) {
                updateDescItems.add(descItem);

                List<ArrDescItem> descItemsOrig = descItemRepository.findByDescItemObjectIdAndDeleteChangeIsNull(descItemObjectId);

                // musí být právě jeden
                if (descItemsOrig.size() != 1) {
                    throw new IllegalArgumentException("Neplatný počet záznamů (" + descItemsOrig.size() + ")");
                }

                ArrDescItem descItemOrig = descItemsOrig.get(0);

                if (!descItemOrig.getPosition().equals(descItem.getPosition())) {
                    updatePositionDescItems.add(descItem);
                }

            } else {
                createDescItems.add(descItem);
            }
        }

        // zakladni validace

        if (deleteDescItems.size() + updateDescItems.size() + createDescItems.size() == 0) {
            throw new IllegalArgumentException("Žádné položky k vytvoření/smazání/změně");
        }

        if ((deleteDescItems.size() > 0 || createDescItems.size() > 0) && createNewVersion == false) {
            throw new IllegalArgumentException("Při mazání/vytváření hodnoty atributu musí být nastavená hodnota o verzování na true");
        }

        if (updatePositionDescItems.size() > 0 && createNewVersion == false) {
            throw new IllegalArgumentException("Při změně pozice atributu musí být nastavená hodnota o verzování na true");
        }

        ArrChange change;

        // provedení akcí

        // mapa dat, funguje jako cache při hromadných úpravách
        Map<RulDescItemType, Map<RulDescItemSpec, List<ArrDescItem>>> mapDescItems = new HashMap<>();

        try {

            if (createNewVersion) {

                change = createChange();

                Integer objectId = getNextDescItemObjectId();

                for (ArrDescItem deleteDescItem : deleteDescItems) {
                    ArrDescItem deleteDescItemRet = deleteDescriptionItemRaw(deleteDescItem, versionId, change, false, mapDescItems);
                    descItemsRet.add(deleteDescItemRet);
                }

                for (ArrDescItem createDescItem : createDescItems) {
                    ArrDescItem createDescItemRet = createDescriptionItemRaw(createDescItem, versionId, change, false, mapDescItems, objectId);
                    objectId++;
                    descItemsRet.add(createDescItemRet);
                }

                for (ArrDescItem updateDescItem : updateDescItems) {
                    ArrDescItem updateDescItemRet = updateDescriptionItemRaw(updateDescItem, versionId, change, false, true, mapDescItems);
                    descItemsRet.add(updateDescItemRet);
                }

                saveChanges(mapDescItems, descItemsRet, true);

            } else {
                // úpravy bez verzování
                for (ArrDescItem descItem : updateDescItems) {
                    descItemsRet.add(updateDescriptionItemRaw(descItem, versionId, null, false, false, mapDescItems));
                }
            }

        } catch (Exception e) {
            rollbackDescItems(deleteDescItems, createDescItems, updateDescItems);
            throw e;
        }

        ArrDescItems descItemsContainer = new ArrDescItems();
        descItemsContainer.setDescItems(descItemsRet);
        return descItemsContainer;
    }

    /**
     * Vrácení změn u objektů.
     *
     * @param deleteDescItems seznam smazaných
     * @param createDescItems seznam vytvořených
     * @param updateDescItems seznam upravovaných
     */
    private void rollbackDescItems(List<ArrDescItem> deleteDescItems, List<ArrDescItem> createDescItems, List<ArrDescItem> updateDescItems) {
        for (ArrDescItem deleteDescItem : deleteDescItems) {
            deleteDescItem.setDeleteChange(null);
        }
        for (ArrDescItem createDescItem : createDescItems) {
            createDescItem.setCreateChange(null);
            createDescItem.setDescItemObjectId(null);
            createDescItem.setDescItemObjectId(null);
        }
        for (ArrDescItem updateDescItem : updateDescItems) {
            updateDescItem.setDeleteChange(null);
        }
    }

    /**
     * Upraví hodnotu existujícího atributu archivního popisu.
     *
     * @param updateDescItem    upravovaná položka
     * @param versionId         identifikátor verze
     * @param change            změna
     * @param saveNode          ukládat uzel? (optimictické zámky)
     * @param createNewVersion  vytvořit novou verzi?
     * @param mapDescItems      mapa cachovaných položek
     * @return                  upravená položka
     */
    private ArrDescItem updateDescriptionItemRaw(ArrDescItem updateDescItem,
                                                 Integer versionId,
                                                 ArrChange change,
                                                 boolean saveNode,
                                                 boolean createNewVersion,
                                                 Map<RulDescItemType, Map<RulDescItemSpec, List<ArrDescItem>>> mapDescItems) {

        if (createNewVersion ^ change != null) {
            throw new IllegalArgumentException("Pokud vytvářím novou verzi, musí být předaná reference změny. Pokud verzi nevytvářím, musí být reference změny null.");
        }

        ArrFindingAidVersion version = findingAidVersionRepository.findOne(versionId);

        Assert.notNull(version);
        if (createNewVersion && version.getLockChange() != null) {
            throw new IllegalArgumentException("Nelze provést verzovanou změnu v uzavřené verzi.");
        }

        List<ArrDescItem> descItemsGroup = getDescItemByTypeAndSpec(mapDescItems, updateDescItem.getDescItemType(), updateDescItem.getDescItemSpec(), updateDescItem.getNode());
        Integer maxPosition = getMaxPositionInDescItems(descItemsGroup);

        if (updateDescItem.getPosition() == null || updateDescItem.getPosition() > maxPosition) {
            updateDescItem.setPosition(maxPosition+1);
        }

        ArrDescItem updateDescItemRet;

        if (existDescItemByObjectId(descItemsGroup, updateDescItem) || findAndMoveDescItemByObjectId(mapDescItems, updateDescItem)) {
            ArrDescItem updateDescItemOrig = getDescItemByObjectId(descItemsGroup, updateDescItem);

            Integer positionNew = updateDescItem.getPosition();
            Integer positionOrig = updateDescItemOrig.getPosition();

            deleteDescItemByObjectId(descItemsGroup, updateDescItem);

            ArrNode nodeDescItem = updateDescItemOrig.getNode();
            validationDescItem(versionId, nodeDescItem, mapDescItems, updateDescItem);

            if (createNewVersion) {

                if (updateDescItemOrig.getClass().equals(ArrDescItem.class)) {
                    updateDescItemOrig.setDeleteChange(change);
                    updateDescItemRet = descItemFactory.saveDescItem(updateDescItemOrig);
                } else {
                    updateDescItemRet = updateDescItem;
                }

                updateDescItem.setDescItemId(null);
                updateDescItem.setCreateChange(change);
                updateDescItem.setDeleteChange(null);

                ArrNode node = updateDescItem.getNode();
                if (saveNode) {
                    node.setLastUpdate(LocalDateTime.now());
                    updateDescItem.setNode(nodeRepository.save(node));
                }

                List<ArrDescItem> descItemsToChange;

                Integer diff;

                if (positionNew < positionOrig) {
                    diff = 1;
                    descItemsToChange = findDescItemsBetweenPosition(descItemsGroup, positionNew, positionOrig);
                } else {
                    diff = -1;
                    descItemsToChange = findDescItemsBetweenPosition(descItemsGroup, positionOrig, positionNew);
                }

                // úpravení pozic, popřípadné vytvoření nových verzí
                for (ArrDescItem descItem : descItemsToChange) {
                    if (descItem.getClass().equals(ArrDescItem.class)) {
                        descItemsGroup.remove(descItem);
                        descItem.setDeleteChange(change);
                        descItemFactory.saveDescItem(descItem);

                        descItem = descItemFactory.getDescItem(descItem);
                        descItem.setDescItemId(null);
                        descItem.setCreateChange(change);
                        descItem.setDeleteChange(null);
                        descItem.setPosition(descItem.getPosition() + diff);
                        descItemsGroup.add(descItem);
                    } else {
                        descItem.setPosition(descItem.getPosition() + diff);
                    }
                }

                descItemsGroup.add(updateDescItem);

            } else {

                // provedla se změna pozice
                if (positionNew != positionOrig) {
                    // při změně pozice musí být vytvářená nová verze
                    throw new IllegalArgumentException("Při změně pozice musí být vytvořena nová verze");
                }

                updateDescItemOrig = descItemFactory.getDescItem(updateDescItemOrig);
                BeanUtils.copyProperties(updateDescItem, updateDescItemOrig);
                updateDescItemRet = descItemFactory.saveDescItemWithData(updateDescItemOrig, false);

                descItemsGroup.add(updateDescItemRet);
            }
        } else {
            throw new IllegalStateException("Neplatny stav pro upraveni polozky. " + updateDescItem);
        }

        return updateDescItemRet;
    }

    /**
     * Vytvoření atributu - pro použití jádra.
     *
     * @param createDescItem    vytvářená položka
     * @param version           verze archivní pomůcky
     * @param change            změna
     * @param saveNode          ukládat uzel? (optimictické zámky)
     * @return                  vytvořená položka
     */
    public ArrDescItem createDescriptionItem(ArrDescItem createDescItem,
                                             ArrFindingAidVersion version,
                                             ArrChange change,
                                             boolean saveNode) {
        Map<RulDescItemType, Map<RulDescItemSpec, List<ArrDescItem>>> mapDescItems = new HashMap<>();

        ArrDescItem descItemRet = createDescriptionItemRaw(createDescItem, version.getFindingAidVersionId(), change, saveNode, mapDescItems, getNextDescItemObjectId());
        saveChanges(mapDescItems, null, true);
        return descItemRet;
    }

    /**
     * Úprava atributu - pro použití jádra.
     *
     * @param descItem          upravovaná položka
     * @param version           verze archivní pomůcky
     * @param createNewVersion  vytvořit novou verzi?
     * @param change            změna
     * @return                  upravená položka
     */
    public ArrDescItem updateDescriptionItem(ArrDescItem descItem, ArrFindingAidVersion version, Boolean createNewVersion, ArrChange change) {
        Map<RulDescItemType, Map<RulDescItemSpec, List<ArrDescItem>>> mapDescItems = new HashMap<>();
        ArrDescItem descItemRet = updateDescriptionItemRaw(descItem, version.getFindingAidVersionId(), change, true, createNewVersion, mapDescItems);
        List<ArrDescItem> descItems = new ArrayList<>();
        descItems.add(descItemRet);
        saveChanges(mapDescItems, descItems, createNewVersion);
        return descItems.get(0);
    }

    /**
     * Smazání atrubutu - pro použití jádra.
     *
     * @param descItem      mazaná položka
     * @param version       verze archivní pomůcky
     * @param change        změna
     * @return              smazaná položka
     */
    public ArrDescItem deleteDescriptionItem(ArrDescItem descItem, ArrFindingAidVersion version, ArrChange change) {
        Map<RulDescItemType, Map<RulDescItemSpec, List<ArrDescItem>>> mapDescItems = new HashMap<>();
        ArrDescItem descItemRet = deleteDescriptionItemRaw(descItem, version.getFindingAidVersionId(), change, true, mapDescItems);
        saveChanges(mapDescItems, null, true);
        return descItemRet;
    }

    /**
     * Vytvoří hodnotu atributu archivního popisu.
     *
     * @param createDescItem    vytvářená položka
     * @param versionId         identifikátor verze
     * @param change            změna
     * @param saveNode          ukládat uzel? (optimictické zámky)
     * @param mapDescItems      mapa cachovaných položek
     * @param objectId          identifikátor objektu
     * @return                  vytvořená položka
     */
    private ArrDescItem createDescriptionItemRaw(ArrDescItem createDescItem,
                                                 Integer versionId,
                                                 ArrChange change,
                                                 boolean saveNode,
                                                 Map<RulDescItemType, Map<RulDescItemSpec, List<ArrDescItem>>> mapDescItems,
                                                 Integer objectId) {
        List<ArrDescItem> descItemsGroup = getDescItemByTypeAndSpec(mapDescItems, createDescItem.getDescItemType(), createDescItem.getDescItemSpec(), createDescItem.getNode());
        Integer maxPosition = getMaxPositionInDescItems(descItemsGroup);

        ArrNode nodeDescItem = createDescItem.getNode();
        validationDescItem(versionId, nodeDescItem, mapDescItems, createDescItem);

        if (createDescItem.getPosition() == null || createDescItem.getPosition() > maxPosition) {
            createDescItem.setPosition(maxPosition+1);
        }

        ArrDescItem createDescItemRet;

        createDescItem.setDeleteChange(null);
        createDescItem.setCreateChange(change);
        createDescItem.setDescItemObjectId(objectId);

        ArrNode node = createDescItem.getNode();
        if (saveNode) {
            node.setLastUpdate(LocalDateTime.now());
            createDescItem.setNode(nodeRepository.save(node));
        }

        createDescItemRet = descItemFactory.saveDescItem(createDescItem);

        List<ArrDescItem> descItemsToChange = findDescItemsAfterPosition(descItemsGroup, createDescItem.getPosition()-1);
        for (ArrDescItem descItem : descItemsToChange) {
            if (descItem.getClass().equals(ArrDescItem.class)) {
                descItemsGroup.remove(descItem);
                descItem.setDeleteChange(change);
                descItemFactory.saveDescItem(descItem);

                descItem = descItemFactory.getDescItem(descItem);
                descItem.setDescItemId(null);
                descItem.setCreateChange(change);
                descItem.setDeleteChange(null);
                descItem.setPosition(descItem.getPosition()+1);
                descItemsGroup.add(descItem);
            } else {
                descItem.setPosition(descItem.getPosition()+1);
            }
        }

        descItemsGroup.add(createDescItem);
        return createDescItemRet;
    }

    /**
     * Smaže hodnotu atributu archivního popisu.
     *
     * @param deleteDescItem    smazávaná položka
     * @param versionId         identifikátor verze
     * @param change            změna
     * @param saveNode          ukládat uzel? (optimictické zámky)
     * @param mapDescItems      mapa cachovaných položek
     * @return                  smazaná položka
     */
    private ArrDescItem deleteDescriptionItemRaw(ArrDescItem deleteDescItem,
                                                 Integer versionId,
                                                 ArrChange change,
                                                 boolean saveNode,
                                                 Map<RulDescItemType, Map<RulDescItemSpec, List<ArrDescItem>>> mapDescItems) {
        Assert.notNull(deleteDescItem);
        Assert.notNull(versionId);
        Assert.notNull(change);
        Assert.notNull(mapDescItems);

        validateLockVersion(versionId);

        ArrDescItem deleteDescItemRet;

        List<ArrDescItem> descItemsGroup = getDescItemByTypeAndSpec(mapDescItems, deleteDescItem.getDescItemType(), deleteDescItem.getDescItemSpec(), deleteDescItem.getNode());
        if (existDescItemByObjectId(descItemsGroup, deleteDescItem)) {
            deleteDescItemByObjectId(descItemsGroup, deleteDescItem);

            deleteDescItem.setDeleteChange(change);

            ArrNode node = deleteDescItem.getNode();
            if (saveNode) {
                node.setLastUpdate(LocalDateTime.now());
                deleteDescItem.setNode(nodeRepository.save(node));
            }

            deleteDescItemRet = descItemFactory.saveDescItem(deleteDescItem);

            List<ArrDescItem> descItemsToChange = findDescItemsAfterPosition(descItemsGroup, deleteDescItem.getPosition());
            for (ArrDescItem descItem : descItemsToChange) {
                if (descItem.getClass().equals(ArrDescItem.class)) {
                    descItemsGroup.remove(descItem);
                    descItem.setDeleteChange(change);
                    descItemFactory.saveDescItem(descItem);

                    descItem = descItemFactory.getDescItem(descItem);
                    descItem.setDescItemId(null);
                    descItem.setCreateChange(change);
                    descItem.setDeleteChange(null);
                    descItem.setPosition(descItem.getPosition() - 1);

                    descItemsGroup.add(descItem);
                } else {
                    descItem.setPosition(descItem.getPosition() - 1);
                }
            }
        } else {
            throw new IllegalStateException("Neplatny stav pro smazani polozky. " + deleteDescItem);
        }

        return deleteDescItemRet;
    }

    /**
     * Uložení z cache změn do databáze.
     *
     * @param mapDescItems      mapa cachovaných položek
     * @param descItemsRet      seznam návratových položek - provede refresh nově uložených položek
     * @param createNewVersion  vytvořit novou verzi?
     */
    private void saveChanges(Map<RulDescItemType, Map<RulDescItemSpec, List<ArrDescItem>>> mapDescItems, List<ArrDescItem> descItemsRet, boolean createNewVersion) {
        for (Map<RulDescItemSpec, List<ArrDescItem>> descItemSpecsMap : mapDescItems.values()) {
            for (List<ArrDescItem> descItemList : descItemSpecsMap.values()) {
                for (ArrDescItem descItem : descItemList) {
                    if (!(descItem.getClass().equals(ArrDescItem.class))) {
                        boolean added = false;
                        if (descItemsRet != null) {
                            // vyhledá podle object id
                            for (ArrDescItem item : descItemsRet) {
                                if (item.getDescItemObjectId().equals(descItem.getDescItemObjectId())) {
                                    descItemsRet.remove(item);
                                    added = true;
                                    break;
                                }
                            }
                        }

                        ArrDescItem ret = descItemFactory.saveDescItemWithData(descItem, createNewVersion);

                        if (descItemsRet != null && added)
                            descItemsRet.add(ret);
                    }
                }
            }
        }
    }

    /**
     * Provede validaci předená položky.
     *
     * @param versionId     identifikátor verze
     * @param node          uzel
     * @param mapDescItems  mapa cachovaných položek
     * @param descItem      položka
     */
    private void validationDescItem(Integer versionId, ArrNode node, Map<RulDescItemType, Map<RulDescItemSpec, List<ArrDescItem>>> mapDescItems, ArrDescItem descItem) {
        List<RulDescItemTypeExt> rulDescItemTypes = ruleManager.getDescriptionItemTypesForNodeId(versionId, node.getNodeId(), null);

        RulDescItemType rulDescItemType = descItemTypeRepository.findOne(descItem.getDescItemType().getDescItemTypeId());
        Assert.notNull(rulDescItemType);

        String data = descItem.toString();
        Assert.notNull(data, "Není vyplněna hodnota");
        if (data.length() == 0) {
            throw new IllegalArgumentException("Není vyplněna hodnota");
        }

        RulDescItemSpec rulDescItemSpec = (descItem.getDescItemSpec() != null) ? descItemSpecRepository.findOne(descItem.getDescItemSpec().getDescItemSpecId()) : null;

        validateAllowedItemType(rulDescItemTypes, rulDescItemType);
        validateAllItemConstraintsBySpec(rulDescItemType, descItem, rulDescItemSpec, mapDescItems);
        validateAllItemConstraintsByType(rulDescItemType, descItem, mapDescItems);
    }

    /**
     * Provedení validace položky nad podmínkama.
     *
     * @param rulDescItemType   kontrolovaný typ
     * @param data              položka
     * @param mapDescItems      mapa cachovaných položek
     */
    private void validateAllItemConstraintsByType(RulDescItemType rulDescItemType,
                                                  ArrDescItem data,
                                                  Map<RulDescItemType, Map<RulDescItemSpec, List<ArrDescItem>>> mapDescItems) {
        List<RulDescItemConstraint> rulDescItemConstraints = descItemConstraintRepository.findByDescItemType(rulDescItemType);
        for (RulDescItemConstraint rulDescItemConstraint : rulDescItemConstraints) {
            validateRepeatableType(rulDescItemType, rulDescItemConstraint, mapDescItems);
            validateDataDescItemConstraintTextLenghtLimit(data, rulDescItemConstraint);
            validateDataDescItemConstraintRegexp(data, rulDescItemConstraint);
        }
    }

    /**
     * Provede validaci opakovatelnosti.
     *
     * @param rulDescItemType           kontrolovaný typ
     * @param rulDescItemConstraint     podmínka
     * @param mapDescItems              mapa cachovaných položek
     */
    private void validateRepeatableType(RulDescItemType rulDescItemType,
                                        RulDescItemConstraint rulDescItemConstraint,
                                        Map<RulDescItemType, Map<RulDescItemSpec, List<ArrDescItem>>> mapDescItems) {
        if (rulDescItemConstraint.getRepeatable() != null && !rulDescItemConstraint.getRepeatable()) {
            List<ArrDescItem> arrDescItems = mapDescItems.get(rulDescItemType).get(null);
            if (arrDescItems != null && arrDescItems.size() > 0) {
                throw new IllegalArgumentException("Pro daný uzel již existuje jiná hodnota stejného typu atributu");
            }
        }
    }

    /**
     * Provede validaci podle specifikace.
     *
     * @param rulDescItemType   kontrolovaný typ
     * @param data              položka
     * @param rulDescItemSpec   specifický typ atributu
     * @param mapDescItems      mapa cachovaných položek
     */
    private void validateAllItemConstraintsBySpec(RulDescItemType rulDescItemType,
                                                  ArrDescItem data,
                                                  RulDescItemSpec rulDescItemSpec,
                                                  Map<RulDescItemType, Map<RulDescItemSpec, List<ArrDescItem>>> mapDescItems) {
        if (rulDescItemSpec != null) {
            validateSpecificationAttribute(rulDescItemType, rulDescItemSpec);
            List<RulDescItemConstraint> rulDescItemConstraints = descItemConstraintRepository.findByDescItemSpec(rulDescItemSpec);
            for (RulDescItemConstraint rulDescItemConstraint : rulDescItemConstraints) {
                validateRepeatableSpec(rulDescItemType, rulDescItemSpec, rulDescItemConstraint, mapDescItems);
                validateDataDescItemConstraintTextLenghtLimit(data, rulDescItemConstraint);
                validateDataDescItemConstraintRegexp(data, rulDescItemConstraint);
            }
        } else
            // Specifikace musí být vyplněna, pokud typ atributu má vyplněno use_specification na true
            if (rulDescItemType.getUseSpecification()) {
                throw new IllegalArgumentException("Specifikace musí být vyplněna, pokud typ atributu má nastaveno use_specification na true");
            }
    }

    /**
     * Provede validaci opakovatelnosti podle specifikace.
     *
     * @param rulDescItemType       kontrolovaný typ
     * @param rulDescItemSpec       specifický typ atributu
     * @param rulDescItemConstraint podmínka
     * @param mapDescItems          mapa cachovaných položek
     */
    private void validateRepeatableSpec(RulDescItemType rulDescItemType,
                                        RulDescItemSpec rulDescItemSpec,
                                        RulDescItemConstraint rulDescItemConstraint,
                                        Map<RulDescItemType, Map<RulDescItemSpec, List<ArrDescItem>>> mapDescItems) {
        if (rulDescItemConstraint.getRepeatable() != null && !rulDescItemConstraint.getRepeatable()) {
            List<ArrDescItem> arrDescItems = mapDescItems.get(rulDescItemType).get(rulDescItemSpec);
            if (arrDescItems.size() > 0) {
                throw new IllegalArgumentException("Pro daný uzel již existuje jiná hodnota stejného typu atributu");
            }
        }
    }

    /**
     * Vyhledá položky v seznam podle pozice.
     *
     * @param descItems     seznam hodnot atributů
     * @param positionFrom  minimální pozice
     * @param positionTo    maximální pozice
     * @return              seznam nalezený položek
     */
    private List<ArrDescItem> findDescItemsBetweenPosition(List<ArrDescItem> descItems, Integer positionFrom, Integer positionTo) {
        List<ArrDescItem> findDescItems = new ArrayList<>();
        for (ArrDescItem descItem : descItems) {
            if (descItem.getPosition() >= positionFrom && descItem.getPosition() <= positionTo) {
                findDescItems.add(descItem);
            }
        }
        return findDescItems;
    }

    /**
     * Vyhledá položku v seznamu podle object id.
     *
     * @param descItems seznam hodnot atributů
     * @param descItem  hledaná položka
     * @return          nalezená položka
     */
    private ArrDescItem getDescItemByObjectId(List<ArrDescItem> descItems, ArrDescItem descItem) {
        for (ArrDescItem item : descItems) {
            if (item.getDescItemObjectId().equals(descItem.getDescItemObjectId())) {
                return item;
            }
        }
        return null;
    }

    /**
     * Smaze položku v seznamu podle object id.
     *
     * @param descItems seznam hodnot atributů
     * @param descItem  smazávaná položka
     */
    private void deleteDescItemByObjectId(List<ArrDescItem> descItems, ArrDescItem descItem) {
        for (ArrDescItem item : descItems) {
            if (item.getDescItemObjectId().equals(descItem.getDescItemObjectId())) {
                descItems.remove(item);
                break;
            }
        }
    }

    /**
     * Existuje položka v seznamu? (podle object id)
     * @param descItems seznam hodnot atributů
     * @param descItem  hledaná položka
     * @return
     */
    private boolean existDescItemByObjectId(List<ArrDescItem> descItems, ArrDescItem descItem) {
        for (ArrDescItem item : descItems) {
            if (item.getDescItemObjectId().equals(descItem.getDescItemObjectId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Pomocná metoda při úpravě specifikace atributu. Přesouvá existující (se starou specifikací) do nové specifikace.
     *
     * @param mapDescItems      mapa cachovaných položek
     * @param updateDescItem    upravovaná položka
     * @return                  přesunuto
     */
    private boolean findAndMoveDescItemByObjectId(Map<RulDescItemType, Map<RulDescItemSpec, List<ArrDescItem>>> mapDescItems, ArrDescItem updateDescItem) {

        List<ArrDescItem> descItems = descItemRepository.findByDescItemObjectIdAndDeleteChangeIsNull(updateDescItem.getDescItemObjectId());
        if (descItems.size() != 1) {
            throw new IllegalStateException("Hodnota musí být právě jedna");
        }
        ArrDescItem descItem = descItems.get(0);

        List<ArrDescItem> descItemsMove = getDescItemByTypeAndSpec(mapDescItems, updateDescItem.getDescItemType(), updateDescItem.getDescItemSpec(), updateDescItem.getNode());
        descItemsMove.add(descItem);
        return true;
    }

    /**
     * Vyhledá nejvyšší pozici v předaném seznamu.
     *
     * @param descItems seznam hodnot atributů
     * @return  maximální pozice
     */
    private Integer getMaxPositionInDescItems(List<ArrDescItem> descItems) {
        Integer maxPosition = 0;
        for (ArrDescItem descItem : descItems) {
            if (descItem.getPosition() > maxPosition) {
                maxPosition = descItem.getPosition();
            }
        }
        return maxPosition;
    }

    /**
     * Vyhledání hodnot atributů podle typu a specifikace. Pokud není nalezena v cache, je dotaženo z DB.
     *
     * @param mapDescItems  mapa cachovaných položek
     * @param type          typ hodnoty atributu
     * @param spec          specifikace hodnoty atributu
     * @param node          uzel
     * @return              seznam nalezený položek
     */
    private List<ArrDescItem> getDescItemByTypeAndSpec(Map<RulDescItemType, Map<RulDescItemSpec, List<ArrDescItem>>> mapDescItems, RulDescItemType type, RulDescItemSpec spec, ArrNode node) {
        List<ArrDescItem> descItems;
        Map<RulDescItemSpec, List<ArrDescItem>> map = mapDescItems.get(type);
        if (map == null) {
            map = new HashMap<>();
            if (spec == null) {
                descItems = descItemRepository.findByNodeAndDeleteChangeIsNullAndDescItemTypeAndSpecItemTypeIsNull(node, type);
            } else {
                descItems = descItemRepository.findByNodeAndDeleteChangeIsNullAndDescItemTypeAndSpecItemType(node, type, spec);
            }
            map.put(spec, descItems);
            mapDescItems.put(type, map);
        } else {
            descItems = map.get(spec);
            if (descItems == null) {
                if (spec == null) {
                    descItems = descItemRepository.findByNodeAndDeleteChangeIsNullAndDescItemTypeAndSpecItemTypeIsNull(node, type);
                } else {
                    descItems = descItemRepository.findByNodeAndDeleteChangeIsNullAndDescItemTypeAndSpecItemType(node, type, spec);
                }
                map.put(spec, descItems);
            }
        }
        return descItems;
    }

    /**
     * Vyhledá položky s pozici vyšší nez zadaná hodnota.
     *
     * @param descItems seznam hodnot atributů
     * @param position  zadaná podnota pozice
     * @return          seznam nalezených položek
     */
    private List<ArrDescItem> findDescItemsAfterPosition(List<ArrDescItem> descItems, Integer position) {
        List<ArrDescItem> findDescItems = new ArrayList<>();
        for (ArrDescItem descItem : descItems) {
            if (descItem.getPosition() > position) {
                findDescItems.add(descItem);
            }
        }
        return findDescItems;
    }

    /**
     * Kontroluje, že verze není zamčená.
     *
     * @param versionId id verze
     */
    private void validateLockVersion(Integer versionId) {
        Assert.notNull(versionId);
        ArrFindingAidVersion version = findingAidVersionRepository.findOne(versionId);

        Assert.notNull(version);
        if (version.getLockChange() != null) {
            throw new IllegalArgumentException("Nelze provést verzovanou změnu v uzavřené verzi.");
        }
    }

    private void deleteDescItemInner(final ArrDescItem descItem, final ArrChange deleteChange) {
        Assert.notNull(descItem);

        descItem.setDeleteChange(deleteChange);
        ArrDescItem descItemTmp = new ArrDescItem();
        BeanUtils.copyProperties(descItem, descItemTmp);
        descItemRepository.save(descItemTmp);
    }

    /**
     * Pokud má typ atributu vyplněný constraint na délku textového řetězce, tak je potřeba zkontrolovat délku hodnoty
     *
     * @param descItem                  Kontrolovaná data
     * @param rulDescItemConstraint Podmínka
     */
    private void validateDataDescItemConstraintTextLenghtLimit(ArrDescItem descItem, RulDescItemConstraint rulDescItemConstraint) {
        Integer textLenghtLimit = rulDescItemConstraint.getTextLenghtLimit();
        if (textLenghtLimit != null && descItem.toString().length() > textLenghtLimit) {
            throw new IllegalStateException("Hodnota je příliš dlouhá - " + descItem.toString().length() + "/" + textLenghtLimit);
        }
    }

    /**
     * Pokud má typ atributu vyplněný constraint na regulární výraz, tak je potřeba hodnotu ověřit předaným regulárním výrazem
     *
     * @param descItem                  Kontrolovaná data
     * @param rulDescItemConstraint Podmínka
     */
    private void validateDataDescItemConstraintRegexp(ArrDescItem descItem, RulDescItemConstraint rulDescItemConstraint) {
        String regexp = rulDescItemConstraint.getRegexp();
        if (regexp != null && !descItem.toString().matches(regexp)) {
            throw new IllegalStateException("Hodnota '" + descItem.toString() + "' neodpovídá výrazu " + regexp);
        }
    }

    /**
     * Typ atributu musí být povolený pro nodeId
     *
     * @param rulDescItemTypes Povolené typy
     * @param rulDescItemType  Kontrolovaný typ
     */
    private void validateAllowedItemType(List<RulDescItemTypeExt> rulDescItemTypes, RulDescItemType rulDescItemType) {
        // Typ atributu musí být povolený pro nodeId
        if (!rulDescItemTypes.contains(rulDescItemType)) {
            throw new IllegalArgumentException("Typ atributu není povolený");
        }
    }

    /**
     * Vrací další identifikátor objektu pro atribut (oproti PK se zachovává při nové verzi)
     *
     * TODO:
     * Není dořešené, může dojít k přidělení stejného object_id dvěma různýmhodnotám atributu.
     * Řešit v budoucnu zrušením object_id (pravděpodobně GUID) nebo vytvořením nové entity,
     * kde bude object_id primární klíč a bude tak generován pomocí sekvencí hibernate.
     *
     * @return Identifikátor objektu
     */
    public Integer getNextDescItemObjectId() {
        Integer maxDescItemObjectId = descItemRepository.findMaxDescItemObjectId();
        if (maxDescItemObjectId == null) {
            maxDescItemObjectId = 0;
        }
        return maxDescItemObjectId + 1;
    }

    /**
     * Specifikace (pokud není NULL) musí patřit k typu atributu, který přidávám.
     *
     * @param rulDescItemType Typ atributu
     * @param rulDescItemSpec Specifický typ atributu
     */
    private void validateSpecificationAttribute(RulDescItemType rulDescItemType, RulDescItemSpec rulDescItemSpec) {
        if (!rulDescItemSpec.getDescItemType().equals(rulDescItemType)) {
            throw new IllegalArgumentException("Specifikace musí patřit k typu atributu");
        }
    }

    @Override
    @RequestMapping(value = "/getDescriptionItemsForAttribute", method = RequestMethod.GET)
    public List<ArrDescItem> getDescriptionItemsForAttribute(
            @RequestParam(value = "faVersionId") Integer faVersionId,
            @RequestParam(value = "nodeId") Integer nodeId,
            @RequestParam(value = "rulDescItemTypeId") Integer rulDescItemTypeId) {
        ArrFindingAidVersion version = findingAidVersionRepository.findOne(faVersionId);
        Assert.notNull(version);
        List<ArrDescItem> itemList;
        ArrNode node = nodeRepository.findOne(nodeId);
        Assert.notNull(node);
        if (version.getLockChange() == null) {
            itemList = descItemRepository.findByNodeAndDeleteChangeIsNullAndDescItemTypeId(node, rulDescItemTypeId);
        } else {
            itemList = descItemRepository.findByNodeDescItemTypeIdAndLockChangeId(node, rulDescItemTypeId, version.getLockChange());
        }
        return createItem(itemList);
    }

    /**
     * rozšíří level o atributy. Vytvoří z {@link ArrDescItem} rozšíření {@link ArrDescItem}.
     * @param itemList
     * @return
     */
    private List<ArrDescItem> createItem(List<ArrDescItem> itemList) {
        List<ArrDescItem> descItems = new LinkedList<>();
        if (itemList.isEmpty()) {
            return descItems;
        }
        for (ArrDescItem descItem : itemList) {
            descItems.add(descItemFactory.getDescItem(descItem));
        }
        return descItems;
    }

    @Override
    @RequestMapping(value = "/getHistoryForNode/{findingAidId}/{nodeId}", method = RequestMethod.GET)
    public ArrNodeHistoryPack getHistoryForNode(@PathVariable(value = "nodeId") Integer nodeId,
                                                @PathVariable(value = "findingAidId") Integer findingAidId) {
        Assert.notNull(nodeId);
        Assert.notNull(findingAidId);

        ArrNode node = nodeRepository.findOne(nodeId);
        ArrFindingAid findingAid = findingAidRepository.findOne(findingAidId);

        if (node == null) {
            throw new IllegalArgumentException("Uzel s identifikátorem " + nodeId + " neexistuje");
        }

        if (findingAid == null) {
            throw new IllegalArgumentException("Archivní pomůcka s identifikátorem " + findingAidId + " neexistuje");
        }

        List<ArrLevel> levels = levelRepository.findByNodeOrderByCreateChangeAsc(node);
        List<ArrDescItem> descItems = new ArrayList<>();
        for (ArrDescItem descItem : descItemRepository.findByNodeOrderByCreateChangeAsc(node)) {
            descItems.add(descItemFactory.getDescItem(descItem));
        }

        List<ArrFindingAidVersion> findingAidVersions = findingAidVersionRepository.findVersionsByFindingAidIdOrderByCreateDateAsc(findingAid.getFindingAidId());

        ArrLevel firstLevel = levels.get(0);
        ArrLevel lastLevel = levels.get(levels.size() - 1);

        Map<Integer, ArrDescItem> firstDescItems = new HashMap<>();
        Map<Integer, ArrDescItem> lastDescItems = new HashMap<>();

        for (ArrDescItem descItem : descItems) {
            if (firstDescItems.get(descItem.getDescItemObjectId()) == null) {
                firstDescItems.put(descItem.getDescItemObjectId(), descItem);
                lastDescItems.put(descItem.getDescItemObjectId(), descItem);
            } else {
                lastDescItems.put(descItem.getDescItemObjectId(), descItem);
            }
        }

        ArrFindingAidVersion[] versions = new ArrFindingAidVersion[findingAidVersions.size()];
        Integer[] versionEnds = new Integer[findingAidVersions.size()];

        Map<ArrFindingAidVersion, List<ArrLevel>> versionsLevelsMap = getVersionsLevelsMap(levels, findingAidVersions, versions, versionEnds);
        Map<ArrFindingAidVersion, Map<ArrChange, List<ArrDescItem>>> versionsChangesDescItemsMap = getVersionsDescItemsMap(descItems, findingAidVersions);


        Map<Integer, List<ArrNodeHistoryItem>> items = new HashMap<>();

        for (ArrFindingAidVersion version : findingAidVersions) {
            List<ArrNodeHistoryItem> nodeHistoryItems = new ArrayList<>();
            List<ArrLevel> levelsByVersion = versionsLevelsMap.get(version);
            Map<ArrChange, List<ArrDescItem>> changeDescItemsByVersion = versionsChangesDescItemsMap.get(version);

            if (levelsByVersion != null) {
                for (ArrLevel level : levelsByVersion) {
                    ArrNodeHistoryItem item = new ArrNodeHistoryItem();

                    item.setChange(level.getCreateChange());

                    cz.tacr.elza.api.vo.ArrNodeHistoryItem.Type type = cz.tacr.elza.api.vo.ArrNodeHistoryItem.Type.LEVEL_CHANGE;

                    if (level.equals(lastLevel) && lastLevel.getDeleteChange() != null) {
                        type = cz.tacr.elza.api.vo.ArrNodeHistoryItem.Type.LEVEL_DELETE;
                    }


                    if (level.equals(firstLevel)) {
                        type = cz.tacr.elza.api.vo.ArrNodeHistoryItem.Type.LEVEL_CREATE;
                    }

                    item.setType(type);
                    nodeHistoryItems.add(item);
                }
            }

            if (changeDescItemsByVersion != null) {
                for (ArrChange change : changeDescItemsByVersion.keySet()) {
                    ArrNodeHistoryItem item = new ArrNodeHistoryItem();
                    item.setDescItems(changeDescItemsByVersion.get(change));
                    item.setChange(change);
                    item.setType(cz.tacr.elza.api.vo.ArrNodeHistoryItem.Type.ATTRIBUTE_CHANGE);
                    nodeHistoryItems.add(item);
                }
            }

            items.put(version.getFindingAidVersionId(), nodeHistoryItems);
        }

        ArrNodeHistoryPack nodeHistoryPack = new ArrNodeHistoryPack();
        nodeHistoryPack.setItems(items);
        return nodeHistoryPack;
    }

    /**
     * Rozřazení levelů podle verze archivní pomůcky a změn.
     *
     * @param levels                seznam levelů
     * @param findingAidVersions    seznam verzí archivní pomůcky
     * @param versions              seznam verzí archivní pomůcky indexovaně
     * @param versionEnds           seznam konců verzí archivní pomůcky indexovaně
     * @return                      rozřazené data v mapě
     */
    private Map<ArrFindingAidVersion, List<ArrLevel>> getVersionsLevelsMap(List<ArrLevel> levels,
                                                                            List<ArrFindingAidVersion> findingAidVersions,
                                                                            ArrFindingAidVersion[] versions,
                                                                            Integer[] versionEnds) {
        final Map<ArrFindingAidVersion, List<ArrLevel>> versionLevelsMap = new LinkedHashMap<>();

        int index = 0;
        for (ArrFindingAidVersion faVersion : findingAidVersions) {
            versionEnds[index] = faVersion.getLockChange() == null ? Integer.MAX_VALUE
                                                                   : faVersion.getLockChange().getChangeId();
            versions[index] = faVersion;
            index++;
        }

        boolean firstLevelBool = true;
        for (ArrLevel faLevel : levels) {
            ArrFindingAidVersion version = getVersionLevelByChangeId(firstLevelBool, faLevel, versionEnds, versions);
            firstLevelBool = false;

            List<ArrLevel> levelList = versionLevelsMap.get(version);
            if (levelList == null) {
                levelList = new LinkedList<>();
                versionLevelsMap.put(version, levelList);
            }
            levelList.add(faLevel);
        }
        return versionLevelsMap;
    }

    /**
     * Vyhledání verze archivní pomůcky podle levelu.
     * @param versions  verze archivní pomůcky
     * @param faLevel   level zanorení
     * @param versions              seznam verzí archivní pomůcky indexovaně
     * @param versionEnds           seznam konců verzí archivní pomůcky indexovaně
     * @return  nalezená archivní pomůcka
     */
    private ArrFindingAidVersion getVersionLevelByChangeId(final boolean firstLevel,
                                                           @Nullable final ArrLevel faLevel,
                                                           final Integer[] versionEnds,
                                                           final ArrFindingAidVersion[] versions) {
        Integer deleteId = faLevel.getDeleteChange() == null ? null : faLevel.getDeleteChange().getChangeId();
        if (firstLevel || deleteId == null) {
            Integer createId = faLevel.getCreateChange().getChangeId();

            int index = Arrays.binarySearch(versionEnds, createId);
            if (index < 0) {
                index = -index - 1;
            }
            return versions[index];
        } else {
            int index = Arrays.binarySearch(versionEnds, deleteId);
            if (index < 0) {
                index = -index - 1;
            }
            return versions[index];
        }
    }

    /**
     * Rozřazení hodnot atributů podle verze archivní pomůcky a změn.
     *
     * @param descItems             seznam hodnot atributl
     * @param findingAidVersions    seznam verzí archivní pomůcky
     * @return                      rozřazené data v mapě
     */
    private Map<ArrFindingAidVersion, Map<ArrChange, List<ArrDescItem>>> getVersionsDescItemsMap(List<ArrDescItem> descItems,
                                                                           List<ArrFindingAidVersion> findingAidVersions) {
        Map<ArrFindingAidVersion, Map<ArrChange, List<ArrDescItem>>> versionDescItemsMap = new LinkedHashMap<>();
        Map<ArrChange, List<ArrDescItem>> changeListMap = new HashMap<>();

        // rozřazení hodnot atributů podle změny
        for (ArrDescItem descItem : descItems) {
            if (descItem.getCreateChange() != null) {
                insertDescItemToMapChanges(changeListMap, descItem.getCreateChange(), descItem);
            }
            if (descItem.getDeleteChange() != null) {
                insertDescItemToMapChanges(changeListMap, descItem.getDeleteChange(), descItem);
            }
        }

        // rozřazení změn podle verzí
        for (ArrChange change : changeListMap.keySet()) {
            ArrFindingAidVersion version = getFindingAidVersionByChange(findingAidVersions, change);
            Map<ArrChange, List<ArrDescItem>> changeListMapTmp = versionDescItemsMap.get(version);
            List<ArrDescItem> descItemList = changeListMap.get(change);
            if (changeListMapTmp == null) {
                changeListMapTmp = new HashMap<>();
                changeListMapTmp.put(change, descItemList);
                versionDescItemsMap.put(version, changeListMapTmp);
            } else {
                changeListMapTmp.put(change, descItemList);
            }
        }

        return versionDescItemsMap;
    }

    /**
     * Vyhledání verze archivní pomůcky podle změny.
     * @param versions  verze archivní pomůcky
     * @param change    změna
     * @return  nalezená archivní pomůcka
     */
    private ArrFindingAidVersion getFindingAidVersionByChange(List<ArrFindingAidVersion> versions, ArrChange change) {
        ArrFindingAidVersion versionRet = null;
        for (ArrFindingAidVersion version : versions) {
            LocalDateTime createDateTime = version.getCreateChange().getChangeDate();
            LocalDateTime lockDateTime = (version.getLockChange() == null) ? null : version.getLockChange().getChangeDate();
            if (createDateTime.isBefore(change.getChangeDate()) &&
                    (lockDateTime == null || lockDateTime.isAfter(change.getChangeDate()))) {
                versionRet = version;
            }
        }
        return versionRet;
    }

    /**
     * Vložení hodnoty atributu do mapy podle změny.
     *
     * @param changeListMap mapa změn - hodnoty atributů
     * @param change        změna
     * @param descItem      hodnota atributu
     */
    private void insertDescItemToMapChanges(Map<ArrChange, List<ArrDescItem>> changeListMap, ArrChange change, ArrDescItem descItem) {
        List<ArrDescItem> descItems = changeListMap.get(change);
        if (descItems == null) {
            descItems = new ArrayList<>();
            descItems.add(descItem);
            changeListMap.put(change, descItems);
        } else {
            descItems.add(descItem);
        }
    }

    @Override
    @RequestMapping(value = "/getCalendarTypes", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ArrCalendarTypes getCalendarTypes() {
        ArrCalendarTypes calendarTypes = new ArrCalendarTypes();
        calendarTypes.setCalendarTypes(calendarTypeRepository.findAll());
        return calendarTypes;
    }

    @Override
    @RequestMapping(value = "/findNodeRegisterLinks", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ArrNodeRegister> findNodeRegisterLinks(final @RequestParam(value = "versionId") Integer versionId,
                                                       final @RequestParam(value = "nodeId") Integer nodeId) {
        Assert.notNull(versionId);
        Assert.notNull(nodeId);

        ArrNode node = nodeRepository.getOne(nodeId);

        ArrFindingAidVersion version = getFaVersionById(versionId);
        boolean open = version.getLockChange() == null;

        if (open) {
            return nodeRegisterRepository.findByNodeAndDeleteChangeIsNull(node);
        } else {
            return nodeRegisterRepository.findClosedVersion(node, version.getLockChange().getChangeId());
        }
    }

    @Override
    @Transactional
    @RequestMapping(value = "/modifyArrNodeRegisterLinks", method = RequestMethod.PUT)
    public void modifyArrNodeRegisterLinks(final @RequestBody ArrNodeRegisterPack arrNodeRegisterPack) {

        Assert.notNull(arrNodeRegisterPack);

        if (CollectionUtils.isNotEmpty(arrNodeRegisterPack.getSaveList())) {
            saveNodeRegisterLinks(arrNodeRegisterPack.getSaveList());
        }

        if (CollectionUtils.isNotEmpty(arrNodeRegisterPack.getDeleteList())) {
            delArrNodeRegisterLinks(arrNodeRegisterPack.getDeleteList());
        }
    }

    private void delArrNodeRegisterLinks(final @RequestBody List<ArrNodeRegister> arrNodeRegisterList) {
        Assert.notNull(arrNodeRegisterList);

        ArrChange change = createChange();
        for (final ArrNodeRegister nodeRegister : arrNodeRegisterList) {
            if (nodeRegister.getDeleteChange() != null) {
                throw new IllegalStateException("Nelze vytvářet či modifikovat změnu," +
                        " která již byla smazána (má delete change).");
            }

            ArrNode node = nodeRegister.getNode();
            node.setLastUpdate(LocalDateTime.now());  // change kvůli locking
            nodeRepository.save(node);

            nodeRegister.setDeleteChange(change);
            nodeRegisterRepository.save(nodeRegister);
        }
    }

    /**
     * Create či update vazby heslo na node.
     *
     * @param arrNodeRegisterList   list vazeb ke create či update
     */
    private void saveNodeRegisterLinks(final List<ArrNodeRegister> arrNodeRegisterList) {
        ArrChange change = createChange();
        for (final ArrNodeRegister nodeRegister : arrNodeRegisterList) {

            validateNodeRegisterLink(nodeRegister);

            ArrNode node = nodeRegister.getNode();
            node.setLastUpdate(LocalDateTime.now());  // change kvůli locking
            nodeRepository.save(node);

            nodeRegister.setCreateChange(change);
            nodeRegisterRepository.save(nodeRegister);
        }
    }

    /**
     * Validuje entitu před uložením.
     *
     * @param nodeRegister  entita
     */
    private void validateNodeRegisterLink(final ArrNodeRegister nodeRegister) {
        if (nodeRegister.getDeleteChange() != null) {
            throw new IllegalStateException("Nelze vytvářet či modifikovat změnu," +
                    " která již byla smazána (má delete change).");
        }

        if (nodeRegister.getNode() == null) {
            throw new IllegalArgumentException("Není vyplněn uzel.");
        }
        if (nodeRegister.getRecord() == null) {
            throw new IllegalArgumentException("Není vyplněno rejstříkové heslo.");
        }
    }

    @RequestMapping(value = "/findPacket", method = RequestMethod.GET)
    @Override
    public List<ArrPacket> findPacket(@RequestParam("search") final String search,
                                    @RequestParam("from") final Integer from, @RequestParam("count") final Integer count,
                                    @RequestParam(value = "packetTypeId", required = false) final Integer packetTypeId) {

        List<ArrPacket> resultList = packetRepository.findAll();
//        List<ParParty> resultList = partyRepository
//                .findPartyByTextAndType(search, partyTypeId, from, count, originator);
//        resultList.forEach((party) -> {
//            if (party.getRecord() != null) {
//                party.getRecord().getVariantRecordList().forEach((variantRecord) -> {
//                    variantRecord.setRegRecord(null);
//                });
//            }
//            if (party.getPreferredName() != null) {
//                party.setPreferredName(null);
//            }
//        });
        return resultList;
    }

    @Override
    @RequestMapping(value = "/insertPacket", method = RequestMethod.PUT)
    public ArrPacket insertPacket(@RequestBody final ArrPacket packet) {
        ArrPacket newPacket = new ArrPacket();
        updateParty(packet, newPacket);
        return newPacket;
    }

    @RequestMapping(value = "/updatePacket", method = RequestMethod.PUT)
    @Override
    @Transactional
    public ArrPacket updatePacket(@RequestBody final ArrPacket packet) {
        Integer packetId = packet.getPacketId();
        Assert.notNull(packetId);
        ArrPacket checkPacket = packetRepository.findOne(packetId);
        Assert.notNull(checkPacket, "Nebyla nalezena ArrPacket s id " + packetId);
        updateParty(packet, packet);
        return packet;
    }

    @Transactional
    private void updateParty(final ArrPacket source, final ArrPacket target) {
        Assert.notNull(source.getPacketType(), "Není vyplněné packet type");
        Assert.notNull(source.getFindingAid(), "Není vyplněné finding aid");
        Assert.notNull(source.getStorageNumber(), "Není vyplněné storage number");

        Integer findingAidId = source.getFindingAid().getFindingAidId();
        Integer packetTypeId = source.getPacketType().getPacketTypeId();

        Assert.notNull(packetTypeId, "Není vyplněné packetTypeId");
        Assert.notNull(findingAidId, "Není vyplněné findingAidId");

        final ArrPacketType partyType = packetTypeRepository.findOne(packetTypeId);
        final ArrFindingAid findingAid = findingAidRepository.findOne(findingAidId);

        Assert.notNull(partyType, "Nebyla nalezena ArrPacketType s id " + packetTypeId);
        Assert.notNull(findingAid, "Nebyla nalezena ArrFindingAid s id " + findingAidId);

        target.setPacketType(partyType);
        target.setFindingAid(findingAid);
        target.setInvalidPacket(BooleanUtils.isTrue(source.getInvalidPacket()));
        target.setStorageNumber(source.getStorageNumber());
        packetRepository.save(target);
    }

//    -- vytvorit metody create a update packet
}
