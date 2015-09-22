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
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrLevelExt;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.RulDescItemConstraint;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.domain.vo.ArrDescItemSavePack;
import cz.tacr.elza.domain.vo.ArrDescItems;
import cz.tacr.elza.domain.vo.ArrLevelWithExtraNode;
import cz.tacr.elza.domain.vo.ArrNodeHistoryItem;
import cz.tacr.elza.domain.vo.ArrNodeHistoryPack;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.DataCoordinatesRepository;
import cz.tacr.elza.repository.DataIntegerRepository;
import cz.tacr.elza.repository.DataPartyRefRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DataStringRepository;
import cz.tacr.elza.repository.DataTextRepository;
import cz.tacr.elza.repository.DataUnitdateRepository;
import cz.tacr.elza.repository.DataUnitidRepository;
import cz.tacr.elza.repository.DescItemConstraintRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;
import cz.tacr.elza.repository.FindingAidRepository;
import cz.tacr.elza.repository.FindingAidVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.RegRecordRepository;
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
    ArrDescItem, ArrDescItemSavePack, ArrLevel, ArrLevelWithExtraNode, ArrNode, ArrDescItems, ArrNodeHistoryPack> {

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
    private DataIntegerRepository dataIntegerRepository;

    @Autowired
    private DataStringRepository dataStringRepository;

    @Autowired
    private DataTextRepository dataTextRepository;

    @Autowired
    private DataUnitdateRepository dataUnitdateRepository;

    @Autowired
    private DataUnitidRepository dataUnitidRepository;

    @Autowired
    private DataCoordinatesRepository dataCoordinatesRepository;

    @Autowired
    private DataPartyRefRepository dataPartyRefRepository;

    @Autowired
    private DataRecordRefRepository dataRecordRefRepository;

    @Autowired
    private RuleManager ruleManager;

    @Autowired
    private DescItemTypeRepository descItemTypeRepository;

    @Autowired
    private DescItemSpecRepository descItemSpecRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private RegRecordRepository recordRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private RegRecordRepository regRecordRepository;

    @Autowired
    private DescItemFactory descItemFactory;

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

        ArrLevel node = levelWithUnderNode.getLevel();
        ArrNode parentNode = levelWithUnderNode.getExtraNode();
        Integer versionId = levelWithUnderNode.getFaVersionId();

        isValidAndOpenVersion(versionId);
        isValidArrFaLevel(node);
        isValidNode(parentNode);

        if (parentNode == null) {
            throw new IllegalArgumentException("Přesun se nezdařil. Záznam byl pravděpodobně smazán jiným uživatelem. Aktualizujte stránku");
        }

        ArrLevel parent = findNodeInRootTreeByNodeId(parentNode, levelWithUnderNode.getRootNode());
        if (node == null || parent == null) {
            throw new IllegalArgumentException("Přesun se nezdařil. Záznam byl pravděpodobně smazán jiným uživatelem. Aktualizujte stránku");
        }

        if (node.equals(parent)) {
            throw new IllegalStateException("Nelze vložit záznam sám do sebe");
        }

        node.getNode().setLastUpdate(LocalDateTime.now());
        node.setNode(nodeRepository.save(node.getNode()));

        // vkládaný nesmí být rodičem uzlu pod který ho vkládám
        checkCycle(node, parent);

        ArrChange change = createChange();
        shiftNodesUp(nodesToShift(node), change);
        ArrLevel newLevel = createNewLevelVersion(node, change);

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
        ArrChange arrFaChange = createChange();
        return createDescriptionItemRaw(descItem, versionId, arrFaChange, true);
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

        ArrChange arrFaChange = null;
        if (createNewVersion) {
            arrFaChange = createChange();
        }

        return updateDescriptionItemRaw(descItem, versionId, createNewVersion, arrFaChange, true);
    }

    @Override
    @RequestMapping(value = "/deleteDescriptionItem/{versionId}", method = RequestMethod.DELETE)
    @Transactional
    public ArrDescItem deleteDescriptionItem(@RequestBody ArrDescItem descItem, @PathVariable(value = "versionId") Integer versionId) {
        Assert.notNull(descItem);
        ArrChange arrFaChange = createChange();
        return deleteDescriptionsItemRaw(descItem, versionId, arrFaChange, true);
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

        // validace

        if (deleteDescItems.size() + updateDescItems.size() + createDescItems.size() == 0) {
            throw new IllegalArgumentException("Žádné položky k vytvoření/smazání/změně");
        }

        if ((deleteDescItems.size() > 0 || createDescItems.size() > 0) && createNewVersion == false) {
            throw new IllegalArgumentException("Při mazání/vytváření hodnoty atributu musí být nastavená hodnota o verzování na true");
        }

        if (updatePositionDescItems.size() > 0 && createNewVersion == false) {
            throw new IllegalArgumentException("Při změně pozice atributu musí být nastavená hodnota o verzování na true");
        }

        ArrChange arrFaChange = null;

        // provedení akcí

        if (createNewVersion) {
            arrFaChange = createChange();

            // mazání
            for (ArrDescItem descItem : deleteDescItems) {
                // smazání jedné hodnoty atributu
                // přidání výsledného objektu do navratového seznamu
                descItemsRet.add(deleteDescriptionsItemRaw(descItem, versionId, arrFaChange, false));
            }

            // vytvoření
            for (ArrDescItem descItem : createDescItems) {
                descItemsRet.add(createDescriptionItemRaw(descItem, versionId, arrFaChange, false));
            }

            // úpravy s verzováním
            for (ArrDescItem descItem : updateDescItems) {
                descItemsRet.add(updateDescriptionItemRaw(descItem, versionId, true, arrFaChange, false));
            }

        } else {
            // úpravy bez verzování
            for (ArrDescItem descItem : updateDescItems) {
                descItemsRet.add(updateDescriptionItemRaw(descItem, versionId, false, null, false));
            }
        }

        for (ArrDescItem descItemExt : descItemsRet) {
            descItemExt.setNode(node);
        }

        ArrDescItems descItemsContainer = new ArrDescItems();
        descItemsContainer.setDescItems(descItemsRet);
        return descItemsContainer;
    }

    /**
     * Přidá atribut archivního popisu včetně hodnoty k existující jednotce archivního popisu.
     *
     * @param descItemExt vytvářená položka
     * @param faVersionId identifikátor verze
     * @param arrFaChange společná změna
     * @return výsledný(vytvořený) attribut
     */
    private ArrDescItem createDescriptionItemRaw(ArrDescItem descItemExt, Integer faVersionId, ArrChange arrFaChange, boolean saveNode) {
        Assert.notNull(descItemExt);
        Assert.notNull(faVersionId);
        Assert.notNull(arrFaChange);

        validateLockVersion(faVersionId);

        ArrNode node = descItemExt.getNode();
        Assert.notNull(node);

        List<RulDescItemTypeExt> rulDescItemTypes = ruleManager.getDescriptionItemTypesForNodeId(faVersionId, node.getNodeId(), null);

        RulDescItemType rulDescItemType = descItemTypeRepository.findOne(descItemExt.getDescItemType().getDescItemTypeId());
        Assert.notNull(rulDescItemType);

        String data = descItemExt.toString();
        Assert.notNull(data, "Není vyplněna hodnota");
        if (data.length() == 0) {
            throw new IllegalArgumentException("Není vyplněna hodnota");
        }

        RulDescItemSpec rulDescItemSpec = (descItemExt.getDescItemSpec() != null) ? descItemSpecRepository.findOne(descItemExt.getDescItemSpec().getDescItemSpecId()) : null;

        validateAllowedItemType(rulDescItemTypes, rulDescItemType);
        validateAllItemConstraintsBySpec(node, rulDescItemType, descItemExt, rulDescItemSpec, null);
        validateAllItemConstraintsByType(node, rulDescItemType, descItemExt, null);

        // uložení

        ArrDescItem descItem = descItemExt;

        descItem.setDeleteChange(null);
        descItem.setCreateChange(arrFaChange);
        descItem.setDescItemObjectId(getNextDescItemObjectId());

        Integer position;
        Integer maxPosition = descItemRepository.findMaxPositionByNodeAndDescItemTypeIdAndDeleteChangeIsNull(node, rulDescItemType.getDescItemTypeId());
        if (maxPosition == null) {
            position = 1; // ještě žádný neexistuje
        } else {
            position = maxPosition + 1;
        }

        Integer positionUI = descItemExt.getPosition();
        // je definovaná pozice u UI
        if (positionUI != null) {
            if (positionUI < 1) {
                throw new IllegalArgumentException("Pozice nemůže být menší než 1 (" + positionUI + ")");
            } else if (positionUI < position) { // pokud existují nejaké položky k posunutí
                position = positionUI;
                updatePositionsAfter(position, node, arrFaChange, descItem, 1);
            }
        }

        descItem.setPosition(position);

        descItem = descItemFactory.saveDescItem(descItem, true);

        if (saveNode) {
            node.setLastUpdate(LocalDateTime.now());
            descItem.setNode(nodeRepository.save(node));
        }

        return descItem;
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

    /**
     * Upraví hodnotu existujícího atributu archivního popisu.
     *
     * @param descItemExt upravovaná položka
     * @param faVersionId identifikátor verze
     * @param arrFaChange společná změna
     * @return výsledný(upravený) attribut
     */
    private ArrDescItem updateDescriptionItemRaw(ArrDescItem descItemExt, Integer faVersionId, Boolean createNewVersion, ArrChange arrFaChange, boolean saveNode) {
        Assert.notNull(descItemExt);
        Assert.notNull(faVersionId);
        Assert.notNull(createNewVersion);

        if (createNewVersion ^ arrFaChange != null) {
            throw new IllegalArgumentException("Pokud vytvářím novou verzi, musí být předaná reference změny. Pokud verzi nevytvářím, musí být reference změny null.");
        }

        ArrFindingAidVersion version = findingAidVersionRepository.findOne(faVersionId);

        Assert.notNull(version);
        if (createNewVersion && version.getLockChange() != null) {
            throw new IllegalArgumentException("Nelze provést verzovanou změnu v uzavřené verzi.");
        }

        List<ArrDescItem> descItems;
        if (version.getLockChange() == null) {
            descItems = descItemRepository.findByDescItemObjectIdAndDeleteChangeIsNull(descItemExt.getDescItemObjectId());
        } else {
            descItems = descItemRepository.findByDescItemObjectIdAndLockChangeId(descItemExt.getDescItemObjectId(), version.getLockChange());
        }

        // musí být právě jeden
        if (descItems.size() != 1) {
            throw new IllegalArgumentException("Neplatný počet záznamů (" + descItems.size() + ")");
        }
        ArrDescItem descItem = descItems.get(0);

        Assert.notNull(descItem);

        ArrDescItem descItemExtNew = descItemFactory.getDescItem(descItem);
        BeanUtils.copyProperties(descItemExt, descItemExtNew);

        ArrNode node = descItem.getNode();
        Assert.notNull(node);

        List<RulDescItemTypeExt> rulDescItemTypes = ruleManager.getDescriptionItemTypesForNodeId(faVersionId, node.getNodeId(), null);

        RulDescItemType rulDescItemType = descItem.getDescItemType();

        String data = descItemExt.toString();
        Assert.notNull(data);
        if (data.length() == 0) {
            throw new IllegalArgumentException("Není vyplněna hodnota");
        }

        RulDescItemSpec rulDescItemSpec = (descItemExt.getDescItemSpec() != null) ? descItemSpecRepository.findOne(descItemExt.getDescItemSpec().getDescItemSpecId()) : null;

        validateAllowedItemType(rulDescItemTypes, rulDescItemType);

        validateAllItemConstraintsBySpec(node, rulDescItemType, descItemExt, rulDescItemSpec, descItem);
        validateAllItemConstraintsByType(node, rulDescItemType, descItemExt, descItem);

        // uložení

        Integer position = descItem.getPosition();
        Integer positionUI = descItemExt.getPosition();

        if (createNewVersion) {

            Integer maxPosition = descItemRepository.findMaxPositionByNodeAndDescItemTypeIdAndDeleteChangeIsNull(node, rulDescItemType.getDescItemTypeId());

            descItem.setDeleteChange(arrFaChange);
            descItemRepository.save(descItem);

            ArrDescItem descItemNew = new ArrDescItem();
            descItemNew.setCreateChange(arrFaChange);
            descItemNew.setDeleteChange(null);
            descItemNew.setDescItemObjectId(descItem.getDescItemObjectId());
            descItemNew.setDescItemType(rulDescItemType);
            descItemNew.setDescItemSpec(rulDescItemSpec);
            descItemNew.setNode(descItem.getNode());

            // provedla se změna pozice
            if (positionUI != null && positionUI != position) {

                // kontrola spodní hranice
                if (positionUI < 1) {
                    throw new IllegalArgumentException("Pozice nemůže být menší než 1 (" + positionUI + ")");
                }

                // kontrola horní hranice
                if (positionUI > maxPosition) {
                    positionUI = maxPosition;
                }

                // typ posunu?
                if (position < positionUI) {
                    // posun níž
                    updatePositionsBetween(position, positionUI, node, arrFaChange, descItem);
                } else {
                    // posun výš
                    updatePositionsBefore(position, node, arrFaChange, descItem);
                }

                descItemNew.setPosition(positionUI);
            } else {
                descItemNew.setPosition(descItem.getPosition());
            }

            descItemRepository.save(descItemNew);

            BeanUtils.copyProperties(descItemNew, descItemExtNew);

            descItem = descItemExtNew;

            descItemFactory.saveDescItem(descItem, true);

        } else {

            // provedla se změna pozice
            if (positionUI != position) {
                // při změně pozice musí být vytvářená nová verze
                throw new IllegalArgumentException("Při změně pozice musí být vytvořena nová verze");
            }

            descItem = descItemFactory.getDescItem(descItem);
            BeanUtils.copyProperties(descItemExt, descItem);
            descItemFactory.saveDescItem(descItem, false);

        }

        if (saveNode) {
            node.setLastUpdate(LocalDateTime.now());
            descItem.setNode(nodeRepository.save(node));
        }

        return descItem;
    }

    /**
     * Vymaže atribut archivního popisu.
     *
     * @param descItemExt       objekt attributu
     * @param arrFaChange      společná změna
     * @param versionId         id verze
     * @return výsledný(smazaný) attribut
     */
    private ArrDescItem deleteDescriptionsItemRaw(ArrDescItem descItemExt, Integer versionId, ArrChange arrFaChange, boolean saveNode) {
        Assert.notNull(descItemExt);
        Assert.notNull(arrFaChange);
        Assert.notNull(versionId);

        validateLockVersion(versionId);

        Integer descItemObjectId = descItemExt.getDescItemObjectId();
        Assert.notNull(descItemObjectId);

        List<ArrDescItem> descItems = descItemRepository.findByDescItemObjectIdAndDeleteChangeIsNull(descItemObjectId);

        // musí být právě jeden
        if (descItems.size() != 1) {
            throw new IllegalArgumentException("Neplatný počet záznamů (" + descItems.size() + ")");
        }

        ArrDescItem descItem = descItemFactory.getDescItem(descItems.get(0));

        deleteDescItemInner(descItem, arrFaChange);

        Integer position = descItem.getPosition();
        ArrNode node = descItem.getNode();

        // position+1 protože nechci upravovat position u smazané položky
        updatePositionsAfter(position + 1, node, arrFaChange, descItem, -1);

        if (saveNode) {
            node.setLastUpdate(LocalDateTime.now());
            descItem.setNode(nodeRepository.save(node));
        }

        return descItem;
    }

    private void deleteDescItemInner(final ArrDescItem descItem, final ArrChange deleteChange) {
        Assert.notNull(descItem);

        descItem.setDeleteChange(deleteChange);
        ArrDescItem descItemTmp = new ArrDescItem();
        BeanUtils.copyProperties(descItem, descItemTmp);
        descItemRepository.save(descItemTmp);
    }

    /**
     * Pokud má typ atributu vyplněný constraint, který má repeatable false, tak je potřeba zkontrolovat, jestli pro daný node_id už neexistuje jiná hodnota stejného typu atributu
     *
     * @param node                  Uzel
     * @param rulDescItemType       Typ atributu
     * @param rulDescItemConstraint Podmínka
     */
    private void validateRepeatableType(ArrNode node, RulDescItemType rulDescItemType, RulDescItemConstraint rulDescItemConstraint, ArrDescItem descItem) {
        if (rulDescItemConstraint.getRepeatable() != null && !rulDescItemConstraint.getRepeatable()) {
            List<ArrDescItem> arrDescItems = descItemRepository.findByNodeAndDeleteChangeIsNullAndDescItemTypeId(node, rulDescItemType.getDescItemTypeId());
            arrDescItems.remove(descItem); // odstraníme ten, co přidáváme / upravujeme
            if (arrDescItems.size() > 0) {
                throw new IllegalArgumentException("Pro daný uzel již existuje jiná hodnota stejného typu atributu");
            }
        }
    }

    /**
     * Pokud má specifikace typu atributu vyplněný constraint, který má repeatable false, tak je potřeba zkontrolovat, jestli pro daný node_id a specifikaci už neexistuje jiná
     * hodnota stejného typu atributu
     *
     * @param node                  Uzel
     * @param rulDescItemType       Typ atributu
     * @param rulDescItemSpec       Specifický typ atributu
     * @param rulDescItemConstraint Podmínka
     */
    private void validateRepeatableSpec(ArrNode node,
                                        RulDescItemType rulDescItemType,
                                        RulDescItemSpec rulDescItemSpec,
                                        RulDescItemConstraint rulDescItemConstraint,
                                        ArrDescItem descItem) {
        if (rulDescItemConstraint.getRepeatable() != null && !rulDescItemConstraint.getRepeatable()) {
            List<ArrDescItem> arrDescItems = descItemRepository.findByNodeAndDeleteChangeIsNullAndDescItemTypeIdAndSpecItemTypeId(node, rulDescItemType.getDescItemTypeId(),
                    rulDescItemSpec.getDescItemSpecId());
            arrDescItems.remove(descItem); // odstraníme ten, co přidáváme / upravujeme
            if (arrDescItems.size() > 0) {
                throw new IllegalArgumentException("Pro daný uzel již existuje jiná hodnota stejného typu atributu");
            }
        }
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
    private Integer getNextDescItemObjectId() {
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

    /**
     * Kontroluje data vůči podmínkám specifického typu atributu.
     *
     * @param node            Uzel
     * @param rulDescItemType Typ atributu
     * @param data            Kontrolovaná data
     * @param rulDescItemSpec Specifický typ atributu
     */
    private void validateAllItemConstraintsBySpec(ArrNode node, RulDescItemType rulDescItemType, ArrDescItem data, RulDescItemSpec rulDescItemSpec, ArrDescItem descItem) {
        if (rulDescItemSpec != null) {
            validateSpecificationAttribute(rulDescItemType, rulDescItemSpec);
            List<RulDescItemConstraint> rulDescItemConstraints = descItemConstraintRepository.findByDescItemSpec(rulDescItemSpec);
            for (RulDescItemConstraint rulDescItemConstraint : rulDescItemConstraints) {
                validateRepeatableSpec(node, rulDescItemType, rulDescItemSpec, rulDescItemConstraint, descItem);
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
     * Kontroluje data vůči podmínkám typu atributu.
     *
     * @param node            Uzel
     * @param rulDescItemType Typ atributu
     * @param data            Kontrolovaná data
     */
    private void validateAllItemConstraintsByType(ArrNode node, RulDescItemType rulDescItemType, ArrDescItem data, ArrDescItem descItem) {
        List<RulDescItemConstraint> rulDescItemConstraints = descItemConstraintRepository.findByDescItemType(rulDescItemType);
        for (RulDescItemConstraint rulDescItemConstraint : rulDescItemConstraints) {
            validateRepeatableType(node, rulDescItemType, rulDescItemConstraint, descItem);
            validateDataDescItemConstraintTextLenghtLimit(data, rulDescItemConstraint);
            validateDataDescItemConstraintRegexp(data, rulDescItemConstraint);
        }
    }

    /**
     * Provede upravení pozic attribut/hodnot v zadaném intervalu.
     *
     * @param position    začáteční pozice pro změnu
     * @param position2   koncová pozice pro změnu
     * @param node        uzel
     * @param arrFaChange společná změna
     * @param descItem    spjatý objekt attributu
     */
    private void updatePositionsBetween(Integer position, Integer position2, ArrNode node, ArrChange arrFaChange, ArrDescItem descItem) {
        List<ArrDescItem> descItemListForUpdate = descItemRepository
                .findByNodeAndDescItemTypeIdAndDeleteChangeIsNullBetweenPositions(position, position2, node, descItem.getDescItemType().getDescItemTypeId());
        updatePositionsRaw(arrFaChange, descItemListForUpdate, -1);
    }

    /**
     * Provede upravení pozic následujících attribut/hodnot po zvolené pozici.
     *
     * @param position    začáteční pozice pro změnu
     * @param node        uzel
     * @param arrFaChange společná změna
     * @param descItem    spjatý objekt attributu
     * @param diff        rozdíl pozice
     */
    private void updatePositionsAfter(Integer position, ArrNode node, ArrChange arrFaChange, ArrDescItem descItem, int diff) {
        List<ArrDescItem> descItemListForUpdate = descItemRepository
                .findByNodeAndDescItemTypeIdAndDeleteChangeIsNullAfterPosistion(position, node, descItem.getDescItemType().getDescItemTypeId());
        updatePositionsRaw(arrFaChange, descItemListForUpdate, diff);
    }

    /**
     * Provede upravení pozic předchozích attribut/hodnot před zvolenou pozicí.
     *
     * @param position    koncová pozice pro změnu
     * @param node        uzel
     * @param arrFaChange společná změna
     * @param descItem    spjatý objekt attributu
     */
    private void updatePositionsBefore(Integer position, ArrNode node, ArrChange arrFaChange, ArrDescItem descItem) {
        List<ArrDescItem> descItemListForUpdate = descItemRepository
                .findByNodeAndDescItemTypeIdAndDeleteChangeIsNullBeforePosistion(position, node,
                        descItem.getDescItemType().getDescItemTypeId());
        updatePositionsRaw(arrFaChange, descItemListForUpdate, 1);
    }

    /**
     * Upraví pozici s kopií dat u všech položek ze seznamu.
     *
     * @param arrFaChange           Změna
     * @param descItemListForUpdate Seznam upravovaných položek
     * @param diff                  Číselná změna (posun)
     */
    private void updatePositionsRaw(ArrChange arrFaChange, List<ArrDescItem> descItemListForUpdate, int diff) {
        for (ArrDescItem descItemUpdate : descItemListForUpdate) {
            descItemUpdate.setDeleteChange(arrFaChange);

            ArrDescItem descItemNew = new ArrDescItem();
            descItemNew.setCreateChange(arrFaChange);
            descItemNew.setDeleteChange(null);
            descItemNew.setDescItemObjectId(descItemUpdate.getDescItemObjectId());
            descItemNew.setDescItemType(descItemUpdate.getDescItemType());
            descItemNew.setDescItemSpec(descItemUpdate.getDescItemSpec());
            descItemNew.setNode(descItemUpdate.getNode());
            descItemNew.setPosition(descItemUpdate.getPosition() + diff);

            descItemRepository.save(descItemUpdate);
            descItemRepository.save(descItemNew);

            descItemFactory.copyDescItemValues(descItemUpdate, descItemNew);
            //copyDataValue(descItemUpdate, descItemNew);
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


        Map<ArrFindingAidVersion, List<ArrNodeHistoryItem>> items = new HashMap<>();

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

            items.put(version, nodeHistoryItems);
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

}
