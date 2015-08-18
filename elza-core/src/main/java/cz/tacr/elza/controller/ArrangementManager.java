package cz.tacr.elza.controller;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import javax.naming.OperationNotSupportedException;
import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.domain.ArrangementType;
import cz.tacr.elza.domain.FaChange;
import cz.tacr.elza.domain.FaLevel;
import cz.tacr.elza.domain.FaVersion;
import cz.tacr.elza.domain.FindingAid;
import cz.tacr.elza.domain.RuleSet;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.FindingAidRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.VersionRepository;


/**
 * API pro pořádání.
 *
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@RestController
@RequestMapping("/api/arrangementManager")
public class ArrangementManager /*implements cz.tacr.elza.api.controller.ArrangementManager*/ {

    @Autowired
    private FindingAidRepository findingAidRepository;

    @Autowired
    private ArrangementTypeRepository arrangementTypeRepository;

    @Autowired
    private VersionRepository versionRepository;

    @Autowired
    private RuleSetRepository ruleSetRepository;

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private ChangeRepository faChangeRepository;

    /**
     * Vytvoří novou archivní pomůcku se zadaným názvem. Jako datum založení vyplní aktuální datum a čas.
     *
     * @param name název archivní pomůcky
     * @return nová archivní pomůcka
     */
    private FindingAid createFindingAid(final String name) {
        Assert.hasText(name);

        FindingAid findingAid = new FindingAid();
        findingAid.setCreateDate(LocalDateTime.now());
        findingAid.setName(name);
        findingAidRepository.save(findingAid);

        return findingAid;
    }

    @Transactional
    @RequestMapping(value = "/createFindingAid", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE,
    params = {"name", "arrangementTypeId", "ruleSetId"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public FindingAid createFindingAid(@RequestParam(value = "name") final String name,
            @RequestParam(value = "arrangementTypeId") final Integer arrangementTypeId,
            @RequestParam(value = "ruleSetId") final Integer ruleSetId) {
        Assert.hasText(name);
        Assert.notNull(arrangementTypeId);
        Assert.notNull(ruleSetId);

        FindingAid findingAid = createFindingAid(name);

        ArrangementType arrangementType = arrangementTypeRepository.getOne(arrangementTypeId);
        RuleSet ruleSet = ruleSetRepository.getOne(ruleSetId);

        FaChange change = createChange();

        FaLevel rootNode = createLevel(change, null);
        createVersion(change, findingAid, arrangementType, ruleSet, rootNode);

        return findingAid;
    }

    private FaLevel createLevel(final FaChange createChange, final Integer parentNodeId) {
        FaLevel level = new FaLevel();
        level.setPosition(1);
        level.setCreateChange(createChange);
        level.setParentNodeId(parentNodeId);

        Integer maxNodeId = levelRepository.findMaxNodeId();
        if (maxNodeId == null) {
            maxNodeId = 0;
        }
        level.setNodeId(maxNodeId + 1);
        return levelRepository.save(level);
    }


    private FaLevel createAfterInLevel(FaChange change, FaLevel level) {
        Assert.notNull(change);
        Assert.notNull(level);

        List<FaLevel> levelsToShift = levelRepository.findByParentNodeAndPositionGreaterThanOrderByPositionAsc(
                level.getParentNodeId(), level.getPosition());
        shiftNodesDown(levelsToShift, change);

        return createLevel(change, level.getParentNodeId(), level.getPosition() + 1);
    }

    private FaLevel createBeforeInLevel(final FaChange change, final FaLevel level){
        Assert.notNull(change);
        Assert.notNull(level);


        List<FaLevel> levelsToShift = levelRepository.findByParentNodeAndPositionGreaterThanOrderByPositionAsc(
                level.getParentNodeId(), level.getPosition() - 1);
        shiftNodesDown(levelsToShift, change);

        return createLevel(change, level.getParentNodeId(), level.getPosition());
    }

    private FaLevel createNewLevelVersion(FaLevel node, FaChange change) {
        Assert.notNull(node);
        Assert.notNull(change);

        FaLevel newNode = copyLevel(node);
        newNode.setCreateChange(change);

        node.setDeleteChange(change);
        levelRepository.save(node);

        return newNode;
    }

    private FaLevel copyLevel(FaLevel node) {
        Assert.notNull(node);

        FaLevel newNode = new FaLevel();
        newNode.setNodeId(node.getNodeId());
        newNode.setParentNodeId(node.getParentNodeId());
        newNode.setPosition(node.getPosition());

        return newNode;
    }

    private FaLevel createLastInLevel(FaChange createChange, FaLevel parent) {
        Assert.notNull(createChange);
        Assert.notNull(parent);

        Integer maxPosition = levelRepository.findMaxPositionUnderParent(parent.getNodeId());
        if (maxPosition == null) {
            maxPosition = 0;
        }

        return createLevel(createChange, parent.getNodeId(), maxPosition + 1);
    }

    private FaLevel createLevel(final FaChange createChange, final Integer parentNodeId, final Integer position) {
        Assert.notNull(createChange);

        FaLevel level = new FaLevel();
        level.setPosition(position);
        level.setCreateChange(createChange);
        level.setParentNodeId(parentNodeId);

        Integer maxNodeId = levelRepository.findMaxNodeId();
        if (maxNodeId == null) {
            maxNodeId = 0;
        }
        level.setNodeId(maxNodeId + 1);
        return levelRepository.save(level);
    }

    private FaVersion createVersion(final FaChange createChange, final FindingAid findingAid,
            final ArrangementType arrangementType, final RuleSet ruleSet, final FaLevel rootNode) {
        FaVersion version = new FaVersion();
        version.setCreateChange(createChange);
        version.setArrangementType(arrangementType);
        version.setFindingAid(findingAid);
        version.setRuleSet(ruleSet);
        version.setRootNode(rootNode);
        return versionRepository.save(version);
    }

    private FaChange createChange() {
        FaChange change = new FaChange();
        change.setChangeDate(LocalDateTime.now());
        return faChangeRepository.save(change);
    }


    @RequestMapping(value = "/deleteFindingAid", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, params = {"findingAidId"})
    @Transactional
    public void deleteFindingAid(@RequestParam(value = "findingAidId") final Integer findingAidId) {
        Assert.notNull(findingAidId);

        versionRepository.findVersionsByFindingAidIdOrderByCreateDateAsc(findingAidId).forEach((version) -> {
            FaLevel rootNode = version.getRootNode();
            versionRepository.delete(version);
            removeTree(rootNode);
        });

        findingAidRepository.delete(findingAidId);
    }

    private void removeTree(FaLevel rootNode) {
        levelRepository.findByParentNodeIdAndDeleteChangeIsNullOrderByPositionAsc(rootNode.getNodeId()).forEach((node) -> {removeTree(node);});

        levelRepository.delete(rootNode);
    }


    @RequestMapping(value = "/getFindingAids", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<FindingAid> getFindingAids() {
        return findingAidRepository.findAll();
    }

    @RequestMapping(value = "/updateFindingAid", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, params = {"findingAidId", "name"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public FindingAid updateFindingAid(@RequestParam(value = "findingAidId") final Integer findingAidId, @RequestParam(value = "name") final String name) {
        Assert.notNull(findingAidId);
        Assert.hasText(name);

        FindingAid findingAid = findingAidRepository.getOne(findingAidId);
        findingAid.setName(name);
        findingAidRepository.save(findingAid);

        return findingAid;
    }

    @RequestMapping(value = "/getArrangementTypes", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ArrangementType> getArrangementTypes() {
        return arrangementTypeRepository.findAll();
    }

    @RequestMapping(value = "/getFindingAidVersions", method = RequestMethod.GET, params = {"findingAidId"}, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FaVersion> getFindingAidVersions(@RequestParam(value = "findingAidId") final Integer findingAidId) {
        Assert.notNull(findingAidId);

        return versionRepository.findVersionsByFindingAidIdOrderByCreateDateAsc(findingAidId);
    }

    @RequestMapping(value = "/getFindingAid", method = RequestMethod.GET, params = {"findingAidId"}, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public FindingAid getFindingAid(final Integer findingAidId) {
        Assert.notNull(findingAidId);
        return findingAidRepository.getOne(findingAidId);
    }

    @Transactional
    @RequestMapping(value = "/approveVersion", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE,
    params = {"findingAidId", "arrangementTypeId", "ruleSetId"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public FaVersion approveVersion(final Integer findingAidId, final Integer arrangementTypeId, final Integer ruleSetId) {
        Assert.notNull(findingAidId);
        Assert.notNull(arrangementTypeId);
        Assert.notNull(ruleSetId);

        FindingAid findingAid = findingAidRepository.findOne(findingAidId);
        FaVersion version = versionRepository.findByFindingAidIdAndLockChangeIsNull(findingAidId);

        FaChange change = createChange();
        version.setLockChange(change);

        ArrangementType arrangementType = arrangementTypeRepository.findOne(arrangementTypeId);
        RuleSet ruleSet = ruleSetRepository.findOne(ruleSetId);

        return createVersion(change, findingAid, arrangementType, ruleSet, version.getRootNode());
    }

    @Transactional
    @RequestMapping(value = "/addLevelBefore", method = RequestMethod.PUT, params = {"nodeId"})
    public FaLevel addLevelBefore(@RequestParam("nodeId") Integer nodeId){
        Assert.notNull(nodeId);

        FaLevel faLevel = levelRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
        FaChange change = createChange();

        return createBeforeInLevel(change, faLevel);
    }

    @Transactional
    @RequestMapping(value = "/addLevel", method = RequestMethod.PUT, params = {"findingAidId"})
    public FaLevel addLevel(@RequestParam("findingAidId") Integer findingAidId) {
        Assert.notNull(findingAidId);

        FaVersion lastVersion = versionRepository.findByFindingAidIdAndLockChangeIsNull(findingAidId);
        FaChange change = createChange();
        return createLastInLevel(change, lastVersion.getRootNode());
    }



    @Transactional
    @RequestMapping(value = "/addLevelAfter", method = RequestMethod.PUT, params = {"nodeId"})
    public FaLevel addLevelAfter(@RequestParam("nodeId") Integer nodeId) {
        Assert.notNull(nodeId);

        FaLevel faLevel = levelRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
        FaChange change = createChange();
        return createAfterInLevel(change, faLevel);
    }

    @Transactional
    @RequestMapping(value = "/addLevelChild", method = RequestMethod.PUT, params = {"nodeId"})
    public FaLevel addLevelChild(@RequestParam("nodeId") Integer nodeId) {
        Assert.notNull(nodeId);

        FaLevel faLevel = levelRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
        FaChange change = createChange();
        return createLastInLevel(change, faLevel);
    }


    @Transactional
    @RequestMapping(value = "/moveLevelBefore", method = RequestMethod.PUT, params = {"nodeId", "followerNodeId"})
    public FaLevel moveLevelBefore(@RequestParam("nodeId") Integer nodeId,
                                   @RequestParam("followerNodeId") Integer followerNodeId) {

        Assert.notNull(nodeId);
        Assert.notNull(followerNodeId);

        FaLevel faLevel = levelRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
        FaLevel follower = levelRepository.findByNodeIdAndDeleteChangeIsNull(followerNodeId);

        if(faLevel == null || follower == null){
            throw new IllegalArgumentException("Přesun se nezdařil. Záznam byl pravděpodobně smazán jiným uživatelem. Aktualizujte stránku");
        }
        if(faLevel.equals(follower)){
            throw new IllegalStateException("Nelze vložit záznam na stejné místo ve stromu");
        }

        checkCycle(faLevel, follower);

        FaChange change = createChange();
        List<FaLevel> nodesToShiftUp = nodesToShift(faLevel);
        List<FaLevel> nodesToShiftDown = nodesToShift(follower);
        nodesToShiftDown.add(follower);

        Integer position;
        if (faLevel.getParentNodeId().equals(follower.getParentNodeId())) {
            Collection<FaLevel> nodesToShift = CollectionUtils.disjunction(nodesToShiftDown, nodesToShiftUp);
            if (faLevel.getPosition() > follower.getPosition()) {
                nodesToShift.remove(faLevel);
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

        FaLevel newLevel = createNewLevelVersion(faLevel, change);
        return addInLevel(newLevel, follower.getParentNodeId(), position);
    }

    @Transactional
    @RequestMapping(value = "/moveLevelUnder", method = RequestMethod.PUT, params = {"nodeId", "parentNodeId"})
    public FaLevel moveLevelUnder(@RequestParam("nodeId") Integer nodeId, @RequestParam("parentNodeId") Integer parentNodeId) {
        Assert.notNull(nodeId);
        Assert.notNull(parentNodeId);

        FaLevel faLevel = levelRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
        FaLevel parent = levelRepository.findByNodeIdAndDeleteChangeIsNull(parentNodeId);
        if(faLevel == null || parent == null){
            throw new IllegalArgumentException("Přesun se nezdařil. Záznam byl pravděpodobně smazán jiným uživatelem. Aktualizujte stránku");
        }

        if(faLevel.equals(parent)){
            throw new IllegalStateException("Nelze vložit záznam sám do sebe");
        }

        // vkládaný nesmí být rodičem uzlu pod který ho vkládám
        checkCycle(faLevel, parent);

        FaChange change = createChange();
        shiftNodesUp(nodesToShift(faLevel), change);
        FaLevel newLevel = createNewLevelVersion(faLevel, change);

        return addLastInLevel(newLevel, parent.getNodeId());
    }

    @Transactional
    @RequestMapping(value = "/moveLevelAfter", method = RequestMethod.PUT, params = {"nodeId", "predecessorNodeId"})
    public FaLevel moveLevelAfter(@RequestParam("nodeId") Integer nodeId, @RequestParam("predecessorNodeId") Integer predecessorNodeId) {
        Assert.notNull(nodeId);
        Assert.notNull(predecessorNodeId);

        FaLevel faLevel = levelRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
        FaLevel predecessor = levelRepository.findByNodeIdAndDeleteChangeIsNull(predecessorNodeId);
        if(faLevel == null || predecessor == null){
            throw new IllegalArgumentException("Přesun se nezdařil. Záznam byl pravděpodobně smazán jiným uživatelem. Aktualizujte stránku");
        }
        if(faLevel.equals(predecessor)){
            throw new IllegalStateException("Nelze vložit záznam na stejné místo ve stromu");
        }
        // vkládaný nesmí být rodičem uzlu za který ho vkládám
        checkCycle(faLevel, predecessor);

        FaChange change = createChange();
        List<FaLevel> nodesToShiftUp = nodesToShift(faLevel);
        List<FaLevel> nodesToShiftDown = nodesToShift(predecessor);
        Integer position;
        if (faLevel.getParentNodeId().equals(predecessor.getParentNodeId())) {
            Collection<FaLevel> nodesToShift = CollectionUtils.disjunction(nodesToShiftDown, nodesToShiftUp);
            if (faLevel.getPosition() > predecessor.getPosition()) {
                nodesToShift.remove(faLevel);
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


        FaLevel newLevel = createNewLevelVersion(faLevel, change);

        return addInLevel(newLevel, predecessor.getParentNodeId(), position);
    }

    private void checkCycle(FaLevel movedNode, FaLevel targetNode) {
        Assert.notNull(movedNode);
        Assert.notNull(targetNode);

        FaLevel node = targetNode;
        while (node.getParentNodeId() != null) {
            if (movedNode.getNodeId().equals(node.getParentNodeId())) {
                throw new IllegalStateException("Přesouvaný uzel je rodičem cílového uzlu. Přesun nelze provést.");
            }
            node = levelRepository.findByNodeIdAndDeleteChangeIsNull(node.getParentNodeId());
        }
    }

    private FaLevel addLastInLevel(FaLevel level, Integer parentNodeId) {
        Assert.notNull(level);
        Assert.notNull(parentNodeId);

        Integer maxPosition = levelRepository.findMaxPositionUnderParent(parentNodeId);
        if (maxPosition == null) {
            maxPosition = 0;
        }
        level.setPosition(maxPosition + 1);
        level.setParentNodeId(parentNodeId);

        return levelRepository.save(level);
    }

    private void shiftNodesDown(Collection<FaLevel> nodesToShift, FaChange change) {
        Assert.notNull(nodesToShift);
        Assert.notNull(change);

        for (FaLevel node : nodesToShift) {
            FaLevel newNode = createNewLevelVersion(node, change);
            newNode.setPosition(node.getPosition() + 1);
            levelRepository.save(newNode);
        }
    }

    private void shiftNodesUp(Collection<FaLevel> nodesToShift, FaChange change) {
        Assert.notNull(nodesToShift);
        Assert.notNull(change);

        for (FaLevel node : nodesToShift) {
            FaLevel newNode = createNewLevelVersion(node, change);
            newNode.setPosition(node.getPosition() - 1);
            levelRepository.save(newNode);
        }
    }
    private List<FaLevel> nodesToShift(FaLevel movedLevel) {
        Assert.notNull(movedLevel);

        return levelRepository.findByParentNodeAndPositionGreaterThanOrderByPositionAsc(movedLevel.getParentNodeId(),
                movedLevel.getPosition());
    }

    private FaLevel addInLevel(FaLevel level, Integer parentNodeId, Integer position) {
        Assert.notNull(level);
        Assert.notNull(position);

        level.setParentNodeId(parentNodeId);
        level.setPosition(position);
        return levelRepository.save(level);
    }

    @Transactional
    @RequestMapping(value = "/deleteLevel", method = RequestMethod.PUT)
    public FaLevel deleteLevel(@RequestParam("nodeId") Integer nodeId) {
        Assert.notNull(nodeId);

        FaLevel level = levelRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
        FaChange change = createChange();
        level.setDeleteChange(change);
        shiftNodesUp(nodesToShift(level), change);

        return levelRepository.save(level);
    }

    @RequestMapping(value = "/findLevelByNodeId", method = RequestMethod.GET)
    public FaLevel findLevelByNodeId(@RequestParam("nodeId")Integer nodeId) {
        Assert.notNull(nodeId);
        return levelRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
    }

    @RequestMapping(value = "/getOpenVersionByFindingAidId", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public FaVersion getOpenVersionByFindingAidId(@RequestParam(value = "findingAidId") Integer findingAidId) {
        Assert.notNull(findingAidId);
        FaVersion faVersion = versionRepository.findByFindingAidIdAndLockChangeIsNull(findingAidId);

        return faVersion;
    }

    @RequestMapping(value = "/getVersion", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public FaVersion getFaVersionById(@RequestParam("versionId") final Integer versionId) {
        Assert.notNull(versionId);
        return versionRepository.findOne(versionId);
    }

    @RequestMapping(value = "/findSubLevels", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FaLevel> findSubLevels(@RequestParam(value = "nodeId") Integer nodeId,
            @RequestParam(value = "versionId", required = false)  Integer versionId) {
        Assert.notNull(nodeId);

        FaChange change = null;
        if (versionId != null) {
            FaVersion version = versionRepository.findOne(versionId);
            change = version.getLockChange();
        }
        if (change == null) {
            return levelRepository.findByParentNodeIdAndDeleteChangeIsNullOrderByPositionAsc(nodeId);
        }
        return levelRepository.findByParentNodeOrderByPositionAsc(nodeId, change);
    }

    @RequestMapping(value = "/findLevels", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FaLevel> findLevels(@RequestParam(value = "nodeId") Integer nodeId) {
        Assert.notNull(nodeId);
        return levelRepository.findByNodeIdOrderByCreateChangeAsc(nodeId);
    }
}
