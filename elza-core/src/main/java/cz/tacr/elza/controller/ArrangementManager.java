package cz.tacr.elza.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrDescItemExt;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrLevelExt;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.RulDescItemConstraint;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.vo.ArrDescItemSavePack;
import cz.tacr.elza.domain.vo.ArrLevelWithExtraNode;
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
    ArrDescItemExt, ArrDescItemSavePack, ArrLevel, ArrLevelWithExtraNode, ArrNode> {

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
     * @param node kořen
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
     * @param node  Kontrolovaný uzel
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
     * @param node  Kontrolovaný uzel
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
     * @param level testovaný level.
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
            ArrDescItemExt arrDescItemExt = new ArrDescItemExt();
            BeanUtils.copyProperties(arrData.getDescItem(), arrDescItemExt);
            if (arrData instanceof ArrDataString) {
                ArrDataString stringData = (ArrDataString) arrData;
                String stringValue = createFormatString(stringData.getValue(), formatData);
                arrDescItemExt.setData(stringValue);
            } else if (arrData instanceof ArrDataInteger) {
                ArrDataInteger stringData = (ArrDataInteger) arrData;
                if (stringData.getValue() != null) {
                    arrDescItemExt.setData(stringData.getValue().toString());
                }
            } else if (arrData instanceof ArrDataText) {
                ArrDataText stringData = (ArrDataText) arrData;
                String stringValue = createFormatString(stringData.getValue(), formatData);
                arrDescItemExt.setData(stringValue);
            } else if (arrData instanceof ArrDataCoordinates) {
                ArrDataCoordinates stringData = (ArrDataCoordinates) arrData;
                arrDescItemExt.setData(stringData.getValue());
            } else if (arrData instanceof ArrDataPartyRef) {
                ArrDataPartyRef stringData = (ArrDataPartyRef) arrData;
                Integer idPartyId = stringData.getPartyId();
                if (idPartyId != null) {
                    ParParty party = partyRepository.getOne(idPartyId);
                    arrDescItemExt.setParty(party);
                    if (party.getRecord() != null) {
                        party.getRecord().getVariantRecordList().forEach((variantRecord) -> {
                            variantRecord.setRegRecord(null);
                        });
                        String stringValue = createFormatString(party.getRecord().getRecord(), formatData);
                        arrDescItemExt.setData(stringValue);
                    }
                }
            } else if (arrData instanceof ArrDataRecordRef) {
                ArrDataRecordRef stringData = (ArrDataRecordRef) arrData;
                Integer recordId = stringData.getRecordId();
                if (recordId != null) {
                    RegRecord record = recordRepository.getOne(recordId);
                    record.getVariantRecordList().forEach((variantRecord) -> {
                        variantRecord.setRegRecord(null);
                    });
                    String stringValue = createFormatString(record.getRecord(), formatData);
                    arrDescItemExt.setRecord(record);
                    arrDescItemExt.setData(stringValue);
                }
            } else if (arrData instanceof ArrDataUnitdate) {
                ArrDataUnitdate stringData = (ArrDataUnitdate) arrData;
                String stringValue = createFormatString(stringData.getValue(), formatData);
                arrDescItemExt.setData(stringValue);
            } else if (arrData instanceof ArrDataUnitid) {
                ArrDataUnitid stringData = (ArrDataUnitid) arrData;
                String stringValue = createFormatString(stringData.getValue(), formatData);
                arrDescItemExt.setData(stringValue);
            }

            levelExt.getDescItemList().add(arrDescItemExt);
        }
    }

    /**
     * formátuje text pro prezentaci atributů. V současné době pouze upraví délku na 250 znaků.
     * @param value text pro formátování
     * @param formatData typ formátu textu.
     * @return naformátovaný text
     */
    private String createFormatString(final String value,  final String formatData) {
        String stringValue = value;
        if (stringValue != null && stringValue.length() > 250 && formatData != null && FORMAT_ATTRIBUTE_SHORT.equals(formatData)) {
            stringValue = stringValue.substring(0, 250);
        }
        return stringValue;
    }

    @Override
    @RequestMapping(value = "/createDescriptionItem/{versionId}", method = RequestMethod.POST)
    @Transactional
    public ArrDescItemExt createDescriptionItem(@RequestBody ArrDescItemExt descItemExt,
                                                @PathVariable(value = "versionId") Integer versionId) {
        Assert.notNull(descItemExt);
        Assert.notNull(versionId);
        ArrChange arrFaChange = createChange();
        return createDescriptionItemRaw(descItemExt, versionId, arrFaChange, true);
    }

    @Override
    @RequestMapping(value = "/updateDescriptionItem/{versionId}/{createNewVersion}", method = RequestMethod.POST)
    @Transactional
    public ArrDescItemExt updateDescriptionItem(@RequestBody ArrDescItemExt descItemExt,
                                                @PathVariable(value = "versionId") Integer versionId,
                                                @PathVariable(value = "createNewVersion") Boolean createNewVersion) {
        Assert.notNull(descItemExt);
        Assert.notNull(versionId);
        Assert.notNull(createNewVersion);

        ArrChange arrFaChange = null;
        if (createNewVersion) {
            arrFaChange = createChange();
        }

        return updateDescriptionItemRaw(descItemExt, versionId, createNewVersion, arrFaChange, true);
    }

    @Override
    @RequestMapping(value = "/deleteDescriptionItem/{versionId}", method = RequestMethod.DELETE)
    @Transactional
    public ArrDescItemExt deleteDescriptionItem(@RequestBody ArrDescItemExt descItemExt, @PathVariable(value = "versionId") Integer versionId) {
        Assert.notNull(descItemExt);
        ArrChange arrFaChange = createChange();
        return deleteDescriptionsItemRaw(descItemExt, versionId, arrFaChange, true);
    }

    @Override
    @RequestMapping(value = "/saveDescriptionItems", method = RequestMethod.POST)
    @Transactional
    public List<ArrDescItemExt> saveDescriptionItems(@RequestBody ArrDescItemSavePack descItemSavePack) {
        Assert.notNull(descItemSavePack);

        List<ArrDescItemExt> deleteDescItems = descItemSavePack.getDeleteDescItems();
        Assert.notNull(deleteDescItems);

        List<ArrDescItemExt> descItems = descItemSavePack.getDescItems();
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

        List<ArrDescItemExt> descItemsRet = new ArrayList<>();

        // analýza vstupních dat, roztřídění

        List<ArrDescItemExt> createDescItems = new ArrayList<>();
        List<ArrDescItemExt> updateDescItems = new ArrayList<>();

        // pouze informativní kvůli logice
        List<ArrDescItemExt> updatePositionDescItems = new ArrayList<>();

        // rozřazení
        categorizeDescItems(descItems, createDescItems, updateDescItems, updatePositionDescItems);

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
            for (ArrDescItemExt descItem : deleteDescItems) {
                // smazání jedné hodnoty atributu
                // přidání výsledného objektu do navratového seznamu
                descItemsRet.add(deleteDescriptionsItemRaw(descItem, versionId, arrFaChange, false));
            }

            // vytvoření
            for (ArrDescItemExt descItem : createDescItems) {
                // vytvoření jedné hodnoty atributu
                // přidání výsledného objektu do navratového seznamu
                descItemsRet.add(createDescriptionItemRaw(descItem, versionId, arrFaChange, false));
            }

            // úpravy s verzováním
            for (ArrDescItemExt descItem : updateDescItems) {
                // úprava jedné hodnoty atributu
                // přidání výsledného objektu do navratového seznamu
                descItemsRet.add(updateDescriptionItemRaw(descItem, versionId, true, arrFaChange, false));
            }

        } else {
            // úpravy bez verzování
            for (ArrDescItemExt descItem : updateDescItems) {
                // úprava jedné hodnoty atributu
                // přidání výsledného objektu do navratového seznamu
                descItemsRet.add(updateDescriptionItemRaw(descItem, versionId, false, null, false));
            }
        }

        // nastavení správného nodu (optimistické zámky)
        for (ArrDescItemExt descItemExt : descItemsRet) {
            descItemExt.setNode(node);
        }

        return descItemsRet;
    }

    /**
     * Rozdělí hodnoty atributu podle typu úpravy.
     *
     * @param descItems Seznam hodnot atributu
     * @param createDescItems   Hodnoty atributu pro vytvoření
     * @param updateDescItems   Hodnoty atributu pro úpravu
     * @param updatePositionDescItems   Hodnoty atributu pro pro úpravu se změnou pozice (pouze informativní)
     */
    private void categorizeDescItems(List<ArrDescItemExt> descItems,
                                     List<ArrDescItemExt> createDescItems,
                                     List<ArrDescItemExt> updateDescItems,
                                     List<ArrDescItemExt> updatePositionDescItems) {
        for (ArrDescItemExt descItem : descItems) {
            Integer descItemObjectId = descItem.getDescItemObjectId();
            if (descItemObjectId != null) {
                updateDescItems.add(descItem);

                List<ArrDescItem> descItemsOrig = descItemRepository.findByDescItemObjectIdAndDeleteChangeIsNull(descItemObjectId);

                // musí být právě jeden
                if (descItemsOrig.size() != 1) {
                    throw new IllegalArgumentException("Neplatný počet záznamů (" + descItems.size() + ")");
                }

                ArrDescItem descItemOrig = descItemsOrig.get(0);

                if (!descItemOrig.getPosition().equals(descItem.getPosition())) {
                    updatePositionDescItems.add(descItem);
                }

            } else {
                createDescItems.add(descItem);
            }
        }
    }

    /**
     * Přidá atribut archivního popisu včetně hodnoty k existující jednotce archivního popisu.
     *
     * @param descItemExt vytvářená položka
     * @param faVersionId identifikátor verze
     * @param arrFaChange společná změna
     * @return výsledný(vytvořený) attribut
     */
    private ArrDescItemExt createDescriptionItemRaw(ArrDescItemExt descItemExt, Integer faVersionId, ArrChange arrFaChange, boolean saveNode) {
        Assert.notNull(descItemExt);
        Assert.notNull(faVersionId);
        Assert.notNull(arrFaChange);

        validateLockVersion(faVersionId);

        ArrNode node = descItemExt.getNode();
        Assert.notNull(node);

        List<RulDescItemTypeExt> rulDescItemTypes = ruleManager.getDescriptionItemTypesForNodeId(faVersionId, node.getNodeId(), null);

        RulDescItemType rulDescItemType = descItemTypeRepository.findOne(descItemExt.getDescItemType().getDescItemTypeId());
        Assert.notNull(rulDescItemType);

        String data = descItemExt.getData();
        Assert.notNull(data, "Není vyplněna hodnota");

        RulDescItemSpec rulDescItemSpec = (descItemExt.getDescItemSpec() != null) ? descItemSpecRepository.findOne(descItemExt.getDescItemSpec().getDescItemSpecId()) : null;

        validateAllowedItemType(rulDescItemTypes, rulDescItemType);
        validateAllItemConstraintsBySpec(node, rulDescItemType, data, rulDescItemSpec, null);
        validateAllItemConstraintsByType(node, rulDescItemType, data, null);

        // uložení

        ArrDescItem descItem = new ArrDescItem();
        BeanUtils.copyProperties(descItemExt, descItem);

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

        descItemRepository.save(descItem);

        saveNewDataValue(rulDescItemType, data, descItem, descItemExt);

        ArrDescItemExt descItemRet = new ArrDescItemExt();
        BeanUtils.copyProperties(descItem, descItemRet);
        if (saveNode) {
            node.setLastUpdate(LocalDateTime.now());
            descItemRet.setNode(nodeRepository.save(node));
        }
        descItemRet.setData(descItemExt.getData());
        return descItemRet;
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
    private ArrDescItemExt updateDescriptionItemRaw(ArrDescItemExt descItemExt, Integer faVersionId, Boolean createNewVersion, ArrChange arrFaChange, boolean saveNode) {
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

        ArrNode node = descItem.getNode();
        Assert.notNull(node);

        List<RulDescItemTypeExt> rulDescItemTypes = ruleManager.getDescriptionItemTypesForNodeId(faVersionId, node.getNodeId(), null);

        RulDescItemType rulDescItemType = descItem.getDescItemType();

        String data = descItemExt.getData();
        Assert.notNull(data);

        RulDescItemSpec rulDescItemSpec = (descItemExt.getDescItemSpec() != null) ? descItemSpecRepository.findOne(descItemExt.getDescItemSpec().getDescItemSpecId()) : null;

        validateAllowedItemType(rulDescItemTypes, rulDescItemType);
        validateAllItemConstraintsBySpec(node, rulDescItemType, data, rulDescItemSpec, descItem);
        validateAllItemConstraintsByType(node, rulDescItemType, data, descItem);

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
            descItem = descItemNew;

            saveNewDataValue(rulDescItemType, data, descItem, descItemExt);

        } else {

            // provedla se změna pozice
            if (positionUI != position) {
                // při změně pozice musí být vytvářená nová verze
                throw new IllegalArgumentException("Při změně pozice musí být vytvořena nová verze");
            }

            List<ArrData> arrDataList = arrDataRepository.findByDescItem(descItem);

            // musí být právě jeden
            if (arrDataList.size() != 1) {
                throw new IllegalStateException("Neplatný počet záznamů");
            }

            ArrData arrData = arrDataList.get(0);

            saveUpdateDataValue(rulDescItemType, data, arrData, descItemExt);

            descItem.setDescItemSpec(rulDescItemSpec);
            descItemRepository.save(descItem);
        }

        ArrDescItemExt descItemRet = new ArrDescItemExt();
        BeanUtils.copyProperties(descItem, descItemRet);
        descItemRet.setData(descItemExt.getData());
        if (saveNode) {
            node.setLastUpdate(LocalDateTime.now());
            descItemRet.setNode(nodeRepository.save(node));
        }
        descItemRet.setData(descItemExt.getData());
        return descItemRet;
    }

    /**
     * Vymaže atribut archivního popisu.
     *
     * @param descItemExt       objekt attributu
     * @param arrFaChange      společná změna
     * @param versionId         id verze
     * @return výsledný(smazaný) attribut
     */
    private ArrDescItemExt deleteDescriptionsItemRaw(ArrDescItemExt descItemExt, Integer versionId, ArrChange arrFaChange, boolean saveNode) {
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

        ArrDescItem descItem = descItems.get(0);

        deleteDescItemInner(descItem, arrFaChange);

        Integer position = descItem.getPosition();
        ArrNode node = descItem.getNode();

        // position+1 protože nechci upravovat position u smazané položky
        updatePositionsAfter(position + 1, node, arrFaChange, descItem, -1);

        ArrDescItemExt descItemRet = new ArrDescItemExt();
        BeanUtils.copyProperties(descItem, descItemRet);
        if (saveNode) {
            node.setLastUpdate(LocalDateTime.now());
            descItemRet.setNode(nodeRepository.save(node));
        }
        descItemRet.setData(descItemExt.getData());
        return descItemRet;
    }

    private void deleteDescItemInner(final ArrDescItem descItem, final ArrChange deleteChange) {
        Assert.notNull(descItem);

        descItem.setDeleteChange(deleteChange);
        descItemRepository.save(descItem);
    }

    /**
     * Pokud má typ atributu vyplněný constraint, který má repeatable false, tak je potřeba zkontrolovat, jestli pro daný node_id už neexistuje jiná hodnota stejného typu atributu
     *
     * @param node                Uzel
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
     * @param node                Uzel
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
     * @param data                  Kontrolovaná data
     * @param rulDescItemConstraint Podmínka
     */
    private void validateDataDescItemConstraintTextLenghtLimit(String data, RulDescItemConstraint rulDescItemConstraint) {
        Integer textLenghtLimit = rulDescItemConstraint.getTextLenghtLimit();
        if (textLenghtLimit != null && data.length() > textLenghtLimit) {
            throw new IllegalStateException("Hodnota je příliš dlouhá - " + data.length() + "/" + textLenghtLimit);
        }
    }

    /**
     * Pokud má typ atributu vyplněný constraint na regulární výraz, tak je potřeba hodnotu ověřit předaným regulárním výrazem
     *
     * @param data                  Kontrolovaná data
     * @param rulDescItemConstraint Podmínka
     */
    private void validateDataDescItemConstraintRegexp(String data, RulDescItemConstraint rulDescItemConstraint) {
        String regexp = rulDescItemConstraint.getRegexp();
        if (regexp != null && !data.matches(regexp)) {
            throw new IllegalStateException("Hodnota '" + data + "' neodpovídá výrazu " + regexp);
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
     * @param node          Uzel
     * @param rulDescItemType Typ atributu
     * @param data            Kontrolovaná data
     * @param rulDescItemSpec Specifický typ atributu
     */
    private void validateAllItemConstraintsBySpec(ArrNode node, RulDescItemType rulDescItemType, String data, RulDescItemSpec rulDescItemSpec, ArrDescItem descItem) {
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
     * @param node          Uzel
     * @param rulDescItemType Typ atributu
     * @param data            Kontrolovaná data
     */
    private void validateAllItemConstraintsByType(ArrNode node, RulDescItemType rulDescItemType, String data, ArrDescItem descItem) {
        List<RulDescItemConstraint> rulDescItemConstraints = descItemConstraintRepository.findByDescItemType(rulDescItemType);
        for (RulDescItemConstraint rulDescItemConstraint : rulDescItemConstraints) {
            validateRepeatableType(node, rulDescItemType, rulDescItemConstraint, descItem);
            validateDataDescItemConstraintTextLenghtLimit(data, rulDescItemConstraint);
            validateDataDescItemConstraintRegexp(data, rulDescItemConstraint);
        }
    }


    /**
     * Uloží novou hodnotu attributu do tabulky podle jeho typu.
     *  @param rulDescItemType Typ atributu
     * @param data            Hodnota attributu
     * @param descItem        Spjatý objekt attributu
     * @param descItemExt
     */
    private void saveNewDataValue(RulDescItemType rulDescItemType,
                                  String data,
                                  ArrDescItem descItem,
                                  final ArrDescItemExt descItemExt) {
        switch (rulDescItemType.getDataType().getCode()) {
            case "INT":
                ArrDataInteger valueInt = new ArrDataInteger();
                valueInt.setDataType(rulDescItemType.getDataType());
                valueInt.setDescItem(descItem);
                try {
                    valueInt.setValue(Integer.valueOf(data));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Hodnota neodpovídá datovému typu atributu");
                }
                dataIntegerRepository.save(valueInt);
                break;

            case "STRING":
                ArrDataString valueString = new ArrDataString();
                valueString.setDataType(rulDescItemType.getDataType());
                valueString.setDescItem(descItem);
                valueString.setValue(data);
                dataStringRepository.save(valueString);
                break;

            case "FORMATTED_TEXT":
            case "TEXT":
                ArrDataText valueText = new ArrDataText();
                valueText.setDataType(rulDescItemType.getDataType());
                valueText.setDescItem(descItem);
                valueText.setValue(data);
                dataTextRepository.save(valueText);
                break;

            case "UNITDATE":
                ArrDataUnitdate valueUnitdateNew = new ArrDataUnitdate();
                valueUnitdateNew.setDataType(rulDescItemType.getDataType());
                valueUnitdateNew.setDescItem(descItem);
                valueUnitdateNew.setValue(data);
                dataUnitdateRepository.save(valueUnitdateNew);
                break;

            case "UNITID":
                ArrDataUnitid valueUnitid = new ArrDataUnitid();
                valueUnitid.setDataType(rulDescItemType.getDataType());
                valueUnitid.setDescItem(descItem);
                valueUnitid.setValue(data);
                dataUnitidRepository.save(valueUnitid);
                break;

            case "PARTY_REF":
                ArrDataPartyRef valuePartyRef = new ArrDataPartyRef();
                valuePartyRef.setDataType(rulDescItemType.getDataType());
                valuePartyRef.setDescItem(descItem);
                try {
                    Integer partyId = descItemExt.getParty() == null
                                              ? null : descItemExt.getParty().getPartyId();
                    if (partyId == null || partyRepository.findOne(partyId) == null) {
                        throw new IllegalArgumentException("Neplatný odkaz do tabulky");
                    }

                    valuePartyRef.setPartyId(partyId);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Hodnota neodpovídá datovému typu atributu (" + data + ")");
                }
                dataPartyRefRepository.save(valuePartyRef);
                break;

            case "RECORD_REF":
                ArrDataRecordRef valueRecordRef = new ArrDataRecordRef();
                valueRecordRef.setDataType(rulDescItemType.getDataType());
                valueRecordRef.setDescItem(descItem);
                try {
                    Integer recordId = descItemExt.getRecord() == null ? null : descItemExt.getRecord().getRecordId();
                    if (recordId == null || recordRepository.findOne(recordId) == null) {
                        throw new IllegalArgumentException("Neplatný odkaz do tabulky");

                    }
                    valueRecordRef.setRecordId(recordId);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Hodnota neodpovídá datovému typu atributu (" + data + ")");
                }
                dataRecordRefRepository.save(valueRecordRef);
                break;

            case "COORDINATES":
                ArrDataCoordinates valueCoordinates = new ArrDataCoordinates();
                valueCoordinates.setDataType(rulDescItemType.getDataType());
                valueCoordinates.setDescItem(descItem);
                valueCoordinates.setValue(data);
                dataCoordinatesRepository.save(valueCoordinates);
                break;


            default:
                throw new IllegalStateException("Datový typ hodnoty není implementován");
        }
    }

    /**
     * Uloží upravenout hodnotu attributu do tabulky podle jeho typu.
     * @param rulDescItemType Typ atributu
     * @param data            Hodnota attributu
     * @param arrData         Upravovaná položka hodnoty attributu
     * @param descItemExt upravená hodnota atributu.
     */
    private void saveUpdateDataValue(RulDescItemType rulDescItemType,
                                     String data,
                                     ArrData arrData,
                                     final ArrDescItemExt descItemExt) {
        switch (rulDescItemType.getDataType().getCode()) {
            case "INT":
                ArrDataInteger valueInt = (ArrDataInteger) arrData;
                try {
                    valueInt.setValue(Integer.valueOf(data));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Hodnota neodpovídá datovému typu atributu");
                }
                dataIntegerRepository.save(valueInt);
                break;

            case "STRING":
                ArrDataString valueString = (ArrDataString) arrData;
                valueString.setValue(data);
                dataStringRepository.save(valueString);
                break;

            case "FORMATTED_TEXT":
            case "TEXT":
                ArrDataText valueText = (ArrDataText) arrData;
                valueText.setValue(data);
                dataTextRepository.save(valueText);
                break;

            case "UNITDATE":
                ArrDataUnitdate valueUnitdate = (ArrDataUnitdate) arrData;
                valueUnitdate.setValue(data);
                dataUnitdateRepository.save(valueUnitdate);
                break;

            case "UNITID":
                ArrDataUnitid valueUnitid = (ArrDataUnitid) arrData;
                valueUnitid.setValue(data);
                dataUnitidRepository.save(valueUnitid);
                break;

            case "PARTY_REF":
                ArrDataPartyRef valuePartyRef = (ArrDataPartyRef) arrData;
                try {
                    Integer partyId = descItemExt.getParty() == null
                                              ? null : descItemExt.getParty().getPartyId();
                    if (partyId == null || partyRepository.findOne(partyId) == null) {
                        throw new IllegalArgumentException("Neplatný odkaz do tabulky");
                    }

                    valuePartyRef.setPartyId(partyId);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Hodnota neodpovídá datovému typu atributu (" + data + ")");
                }
                dataPartyRefRepository.save(valuePartyRef);
                break;

            case "RECORD_REF":
                ArrDataRecordRef valueRecordRef = (ArrDataRecordRef) arrData;
                try {
                    Integer recordId = descItemExt.getRecord() == null ? null : descItemExt.getRecord().getRecordId();
                    if (recordId == null || recordRepository.findOne(recordId) == null) {
                        throw new IllegalArgumentException("Neplatný odkaz do tabulky");

                    }
                    valueRecordRef.setRecordId(recordId);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Hodnota neodpovídá datovému typu atributu (" + data + ")");
                }
                dataRecordRefRepository.save(valueRecordRef);
                break;

            case "COORDINATES":
                ArrDataCoordinates valueCoordinates = (ArrDataCoordinates) arrData;
                valueCoordinates.setValue(data);
                dataCoordinatesRepository.save(valueCoordinates);
                break;

            default:
                throw new IllegalStateException("Datový typ hodnoty není implementován");
        }
    }


    /**
     * Provede upravení pozic attribut/hodnot v zadaném intervalu.
     *
     * @param position      začáteční pozice pro změnu
     * @param position2     koncová pozice pro změnu
     * @param node          uzel
     * @param arrFaChange   společná změna
     * @param descItem      spjatý objekt attributu
     */
    private void updatePositionsBetween(Integer position, Integer position2, ArrNode node, ArrChange arrFaChange, ArrDescItem descItem) {
        List<ArrDescItem> descItemListForUpdate = descItemRepository
                .findByNodeAndDescItemTypeIdAndDeleteChangeIsNullBetweenPositions(position, position2, node, descItem.getDescItemType().getDescItemTypeId());
        updatePositionsRaw(arrFaChange, descItemListForUpdate, -1);
    }

    /**
     * Provede upravení pozic následujících attribut/hodnot po zvolené pozici.
     *
     * @param position      začáteční pozice pro změnu
     * @param node          uzel
     * @param arrFaChange   společná změna
     * @param descItem      spjatý objekt attributu
     * @param diff          rozdíl pozice
     */
    private void updatePositionsAfter(Integer position, ArrNode node, ArrChange arrFaChange, ArrDescItem descItem, int diff) {
        List<ArrDescItem> descItemListForUpdate = descItemRepository
                .findByNodeAndDescItemTypeIdAndDeleteChangeIsNullAfterPosistion(position, node, descItem.getDescItemType().getDescItemTypeId());
        updatePositionsRaw(arrFaChange, descItemListForUpdate, diff);
    }

    /**
     * Provede upravení pozic předchozích attribut/hodnot před zvolenou pozicí.
     *
     * @param position      koncová pozice pro změnu
     * @param node          uzel
     * @param arrFaChange   společná změna
     * @param descItem      spjatý objekt attributu
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
     * @param arrFaChange   Změna
     * @param descItemListForUpdate Seznam upravovaných položek
     * @param diff  Číselná změna (posun)
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

            copyDataValue(descItemUpdate, descItemNew);
        }
    }

    /**
     * Vytvoří kopie hodnot pro novou verzi hodnoty atributu.
     *
     * @param descItemUpdate    Původní hodnota attributu
     * @param descItemNew       Nová hodnota attributu
     */
    private void copyDataValue(ArrDescItem descItemUpdate, ArrDescItem descItemNew) {
        List<ArrData> arrDataList = arrDataRepository.findByDescItem(descItemUpdate);
        if (arrDataList.size() != 1) {
            throw new IllegalStateException("Neplatný počet záznamů");
        }

        ArrData arrData = arrDataList.get(0);

        switch (arrData.getDataType().getCode()) {
            case "INT":
                ArrDataInteger valueInt = (ArrDataInteger) arrData;
                ArrDataInteger valueIntNew = new ArrDataInteger();
                valueIntNew.setDataType(arrData.getDataType());
                valueIntNew.setValue(valueInt.getValue());
                valueIntNew.setDescItem(descItemNew);
                dataIntegerRepository.save(valueIntNew);
                break;

            case "STRING":
                ArrDataString valueString = (ArrDataString) arrData;
                ArrDataString valueStringNew = new ArrDataString();
                valueStringNew.setDataType(arrData.getDataType());
                valueStringNew.setValue(valueString.getValue());
                valueStringNew.setDescItem(descItemNew);
                dataStringRepository.save(valueStringNew);
                break;

            case "FORMATTED_TEXT":
            case "TEXT":
                ArrDataText valueText = (ArrDataText) arrData;
                ArrDataText valueTextNew = new ArrDataText();
                valueTextNew.setDataType(arrData.getDataType());
                valueTextNew.setValue(valueText.getValue());
                valueTextNew.setDescItem(descItemNew);
                dataTextRepository.save(valueTextNew);
                break;

            case "UNITDATE":
                ArrDataUnitdate valueUnitdate = (ArrDataUnitdate) arrData;
                ArrDataUnitdate valueUnitdateNew = new ArrDataUnitdate();
                valueUnitdateNew.setDataType(arrData.getDataType());
                valueUnitdateNew.setValue(valueUnitdate.getValue());
                valueUnitdateNew.setDescItem(descItemNew);
                dataUnitdateRepository.save(valueUnitdateNew);
                break;

            case "UNITID":
                ArrDataUnitid valueUnitid = (ArrDataUnitid) arrData;
                ArrDataUnitid valueUnitidNew = new ArrDataUnitid();
                valueUnitidNew.setDataType(arrData.getDataType());
                valueUnitidNew.setValue(valueUnitid.getValue());
                valueUnitidNew.setDescItem(descItemNew);
                dataUnitidRepository.save(valueUnitidNew);
                break;

            case "PARTY_REF":
                ArrDataPartyRef valuePartyRef = (ArrDataPartyRef) arrData;
                ArrDataPartyRef valuePartyRefNew = new ArrDataPartyRef();
                valuePartyRefNew.setDataType(arrData.getDataType());
                valuePartyRefNew.setPosition(valuePartyRef.getPosition());
                valuePartyRefNew.setPartyId(valuePartyRef.getPartyId());
                valuePartyRefNew.setDescItem(descItemNew);
                dataPartyRefRepository.save(valuePartyRefNew);
                break;

            case "RECORD_REF":
                ArrDataRecordRef valueRecordRef = (ArrDataRecordRef) arrData;
                ArrDataRecordRef valueRecordRefNew = new ArrDataRecordRef();
                valueRecordRefNew.setDataType(arrData.getDataType());
                valueRecordRefNew.setRecordId(valueRecordRef.getRecordId());
                valueRecordRefNew.setDescItem(descItemNew);
                dataRecordRefRepository.save(valueRecordRefNew);
                break;

            case "COORDINATES":
                ArrDataCoordinates valueCoordinates = (ArrDataCoordinates) arrData;
                ArrDataCoordinates valueCoordinatesNew = new ArrDataCoordinates();
                valueCoordinatesNew.setDataType(arrData.getDataType());
                valueCoordinatesNew.setValue(valueCoordinates.getValue());
                valueCoordinatesNew.setDescItem(descItemNew);
                dataCoordinatesRepository.save(valueCoordinatesNew);
                break;

            default:
                throw new IllegalStateException("Datový typ hodnoty není implementován");
        }
    }

    @Override
    @RequestMapping(value = "/getDescriptionItemsForAttribute", method = RequestMethod.GET)
    public List<ArrDescItemExt> getDescriptionItemsForAttribute(
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
        return createItemExt(itemList);
    }

    /**
     * rozšíří level o atributy. Vytvoří z {@link ArrDescItem} rozšíření {@link ArrDescItemExt}.
     * @param itemList
     * @return
     */
    private List<ArrDescItemExt> createItemExt(List<ArrDescItem> itemList) {
        List<ArrDescItemExt> descItemList = new LinkedList<>();
        if (itemList.isEmpty()) {
            return descItemList;
        }

        for (ArrDescItem descItem : itemList) {
            ArrDescItemExt descItemExt = new ArrDescItemExt();

            BeanUtils.copyProperties(descItem, descItemExt);

            List<ArrData> dataList = dataRepository.findByDescItem(descItem);

            if (dataList.size() != 1) {
                throw new IllegalStateException("Neplatný počet odkazujících dat (" + dataList.size() + ")");
            }

            ArrData data = dataList.get(0);
            descItemExt.setData(data.getData());
            descItemList.add(descItemExt);

            if (data instanceof ArrDataPartyRef) {
                ArrDataPartyRef partyRef = (ArrDataPartyRef) data;
                descItemExt.setParty(partyRepository.findOne(partyRef.getPartyId()));
            }  else if (data instanceof ArrDataRecordRef) {
                ArrDataRecordRef recordRef = (ArrDataRecordRef) data;
                descItemExt.setRecord(regRecordRepository.findOne(recordRef.getRecordId()));
            }
        }

        return descItemList;
    }
}
