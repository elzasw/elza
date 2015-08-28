package cz.tacr.elza.controller;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import cz.tacr.elza.domain.ArrArrangementType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataDatace;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataReference;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrDescItemExt;
import cz.tacr.elza.domain.ArrFaChange;
import cz.tacr.elza.domain.ArrFaLevel;
import cz.tacr.elza.domain.ArrFaLevelExt;
import cz.tacr.elza.domain.ArrFaVersion;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.RulDescItemConstraint;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.vo.ArrDescItemSavePack;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.DataDataceRepository;
import cz.tacr.elza.repository.DataIntegerRepository;
import cz.tacr.elza.repository.DataReferenceRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DataStringRepository;
import cz.tacr.elza.repository.DataTextRepository;
import cz.tacr.elza.repository.DescItemConstraintRepository;
import cz.tacr.elza.repository.DescItemRepository;
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
public class ArrangementManager implements cz.tacr.elza.api.controller.ArrangementManager<ArrDescItemExt, ArrDescItemSavePack> {

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
    private DataDataceRepository dataDataceRepository;

    @Autowired
    private DataReferenceRepository dataReferenceRepository;

    @Autowired
    private RuleManager ruleManager;

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
    @RequestMapping(value = "/createFindingAid", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE,
            params = {"name", "arrangementTypeId", "ruleSetId"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrFindingAid createFindingAid(@RequestParam(value = "name") final String name,
                                          @RequestParam(value = "arrangementTypeId") final Integer arrangementTypeId,
                                          @RequestParam(value = "ruleSetId") final Integer ruleSetId) {
        Assert.hasText(name);
        Assert.notNull(arrangementTypeId);
        Assert.notNull(ruleSetId);

        ArrFindingAid findingAid = createFindingAid(name);

        ArrArrangementType arrangementType = arrangementTypeRepository.getOne(arrangementTypeId);
        RulRuleSet ruleSet = ruleSetRepository.getOne(ruleSetId);

        ArrFaChange change = createChange();

        ArrFaLevel rootNode = createLevel(change, null);
        createVersion(change, findingAid, arrangementType, ruleSet, rootNode);

        return findingAid;
    }

    private ArrFaLevel createLevel(final ArrFaChange createChange, final Integer parentNodeId) {
        ArrFaLevel level = new ArrFaLevel();
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


    private ArrFaLevel createAfterInLevel(ArrFaChange change, ArrFaLevel level) {
        Assert.notNull(change);
        Assert.notNull(level);

        List<ArrFaLevel> levelsToShift = levelRepository.findByParentNodeAndPositionGreaterThanOrderByPositionAsc(
                level.getParentNodeId(), level.getPosition());
        shiftNodesDown(levelsToShift, change);

        return createLevel(change, level.getParentNodeId(), level.getPosition() + 1);
    }

    private ArrFaLevel createBeforeInLevel(final ArrFaChange change, final ArrFaLevel level){
        Assert.notNull(change);
        Assert.notNull(level);


        List<ArrFaLevel> levelsToShift = levelRepository.findByParentNodeAndPositionGreaterThanOrderByPositionAsc(
                level.getParentNodeId(), level.getPosition() - 1);
        shiftNodesDown(levelsToShift, change);

        return createLevel(change, level.getParentNodeId(), level.getPosition());
    }

    private ArrFaLevel createNewLevelVersion(ArrFaLevel node, ArrFaChange change) {
        Assert.notNull(node);
        Assert.notNull(change);

        ArrFaLevel newNode = copyLevel(node);
        newNode.setCreateChange(change);

        node.setDeleteChange(change);
        levelRepository.save(node);

        return newNode;
    }

    private ArrFaLevel copyLevel(ArrFaLevel node) {
        Assert.notNull(node);

        ArrFaLevel newNode = new ArrFaLevel();
        newNode.setNodeId(node.getNodeId());
        newNode.setParentNodeId(node.getParentNodeId());
        newNode.setPosition(node.getPosition());

        return newNode;
    }

    private ArrFaLevel createLastInLevel(ArrFaChange createChange, ArrFaLevel parent) {
        Assert.notNull(createChange);
        Assert.notNull(parent);

        Integer maxPosition = levelRepository.findMaxPositionUnderParent(parent.getNodeId());
        if (maxPosition == null) {
            maxPosition = 0;
        }

        return createLevel(createChange, parent.getNodeId(), maxPosition + 1);
    }

    private ArrFaLevel createLevel(final ArrFaChange createChange, final Integer parentNodeId, final Integer position) {
        Assert.notNull(createChange);

        ArrFaLevel level = new ArrFaLevel();
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

    private ArrFaVersion createVersion(final ArrFaChange createChange, final ArrFindingAid findingAid,
                                       final ArrArrangementType arrangementType, final RulRuleSet ruleSet, final ArrFaLevel rootNode) {
        ArrFaVersion version = new ArrFaVersion();
        version.setCreateChange(createChange);
        version.setArrangementType(arrangementType);
        version.setFindingAid(findingAid);
        version.setRuleSet(ruleSet);
        version.setRootNode(rootNode);
        return versionRepository.save(version);
    }

    private ArrFaChange createChange() {
        ArrFaChange change = new ArrFaChange();
        change.setChangeDate(LocalDateTime.now());
        return faChangeRepository.save(change);
    }


    @Override
    @RequestMapping(value = "/deleteFindingAid", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, params = {"findingAidId"})
    @Transactional
    public void deleteFindingAid(@RequestParam(value = "findingAidId") final Integer findingAidId) {
        Assert.notNull(findingAidId);

        versionRepository.findVersionsByFindingAidIdOrderByCreateDateAsc(findingAidId).forEach((version) -> {
            ArrFaLevel rootNode = version.getRootNode();
            versionRepository.delete(version);
            removeTree(rootNode);
        });

        findingAidRepository.delete(findingAidId);
    }

    private void removeTree(ArrFaLevel rootNode) {
        levelRepository.findByParentNodeIdAndDeleteChangeIsNullOrderByPositionAsc(rootNode.getNodeId()).forEach((node) -> {removeTree(node);});

        levelRepository.delete(rootNode);
    }


    @Override
    @RequestMapping(value = "/getFindingAids", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<ArrFindingAid> getFindingAids() {
        return findingAidRepository.findAll();
    }

    @Override
    @RequestMapping(value = "/updateFindingAid", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, params = {"findingAidId", "name"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ArrFindingAid updateFindingAid(@RequestParam(value = "findingAidId") final Integer findingAidId, @RequestParam(value = "name") final String name) {
        Assert.notNull(findingAidId);
        Assert.hasText(name);

        ArrFindingAid findingAid = findingAidRepository.getOne(findingAidId);
        findingAid.setName(name);
        findingAidRepository.save(findingAid);

        return findingAid;
    }

    @Override
    @RequestMapping(value = "/getFindingAidVersions", method = RequestMethod.GET, params = {"findingAidId"}, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ArrFaVersion> getFindingAidVersions(@RequestParam(value = "findingAidId") final Integer findingAidId) {
        Assert.notNull(findingAidId);

        return versionRepository.findVersionsByFindingAidIdOrderByCreateDateAsc(findingAidId);
    }

    @Override
    @RequestMapping(value = "/getFindingAid", method = RequestMethod.GET, params = {"findingAidId"}, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrFindingAid getFindingAid(final Integer findingAidId) {
        Assert.notNull(findingAidId);
        return findingAidRepository.getOne(findingAidId);
    }

    @Override
    @Transactional
    @RequestMapping(value = "/approveVersion", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE,
            params = {"findingAidId", "arrangementTypeId", "ruleSetId"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrFaVersion approveVersion(final Integer findingAidId, final Integer arrangementTypeId, final Integer ruleSetId) {
        Assert.notNull(findingAidId);
        Assert.notNull(arrangementTypeId);
        Assert.notNull(ruleSetId);

        ArrFindingAid findingAid = findingAidRepository.findOne(findingAidId);
        ArrFaVersion version = versionRepository.findByFindingAidIdAndLockChangeIsNull(findingAidId);

        ArrFaChange change = createChange();
        version.setLockChange(change);

        ArrArrangementType arrangementType = arrangementTypeRepository.findOne(arrangementTypeId);
        RulRuleSet ruleSet = ruleSetRepository.findOne(ruleSetId);

        return createVersion(change, findingAid, arrangementType, ruleSet, version.getRootNode());
    }

    @Override
    @Transactional
    @RequestMapping(value = "/addLevelBefore", method = RequestMethod.PUT, params = {"nodeId"})
    public ArrFaLevel addLevelBefore(@RequestParam("nodeId") Integer nodeId){
        Assert.notNull(nodeId);

        ArrFaLevel faLevel = levelRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
        ArrFaChange change = createChange();

        return createBeforeInLevel(change, faLevel);
    }

    @Override
    @Transactional
    @RequestMapping(value = "/addLevel", method = RequestMethod.PUT, params = {"findingAidId"})
    public ArrFaLevel addLevel(@RequestParam("findingAidId") Integer findingAidId) {
        Assert.notNull(findingAidId);

        ArrFaVersion lastVersion = versionRepository.findByFindingAidIdAndLockChangeIsNull(findingAidId);
        ArrFaChange change = createChange();
        return createLastInLevel(change, lastVersion.getRootNode());
    }

    @Override
    @Transactional
    @RequestMapping(value = "/addLevelAfter", method = RequestMethod.PUT, params = {"nodeId"})
    public ArrFaLevel addLevelAfter(@RequestParam("nodeId") Integer nodeId) {
        Assert.notNull(nodeId);

        ArrFaLevel faLevel = levelRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
        ArrFaChange change = createChange();
        return createAfterInLevel(change, faLevel);
    }

    @Override
    @Transactional
    @RequestMapping(value = "/addLevelChild", method = RequestMethod.PUT, params = {"nodeId"})
    public ArrFaLevel addLevelChild(@RequestParam("nodeId") Integer nodeId) {
        Assert.notNull(nodeId);

        ArrFaLevel faLevel = levelRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
        ArrFaChange change = createChange();
        return createLastInLevel(change, faLevel);
    }

    @Override
    @Transactional
    @RequestMapping(value = "/moveLevelBefore", method = RequestMethod.PUT, params = {"nodeId", "followerNodeId"})
    public ArrFaLevel moveLevelBefore(@RequestParam("nodeId") Integer nodeId,
                                      @RequestParam("followerNodeId") Integer followerNodeId) {

        Assert.notNull(nodeId);
        Assert.notNull(followerNodeId);

        ArrFaLevel faLevel = levelRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
        ArrFaLevel follower = levelRepository.findByNodeIdAndDeleteChangeIsNull(followerNodeId);

        if(faLevel == null || follower == null){
            throw new IllegalArgumentException("Přesun se nezdařil. Záznam byl pravděpodobně smazán jiným uživatelem. Aktualizujte stránku");
        }
        if(faLevel.equals(follower)){
            throw new IllegalStateException("Nelze vložit záznam na stejné místo ve stromu");
        }

        checkCycle(faLevel, follower);

        ArrFaChange change = createChange();
        List<ArrFaLevel> nodesToShiftUp = nodesToShift(faLevel);
        List<ArrFaLevel> nodesToShiftDown = nodesToShift(follower);
        nodesToShiftDown.add(follower);

        Integer position;
        if (faLevel.getParentNodeId().equals(follower.getParentNodeId())) {
            Collection<ArrFaLevel> nodesToShift = CollectionUtils.disjunction(nodesToShiftDown, nodesToShiftUp);
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

        ArrFaLevel newLevel = createNewLevelVersion(faLevel, change);
        return addInLevel(newLevel, follower.getParentNodeId(), position);
    }

    @Override
    @Transactional
    @RequestMapping(value = "/moveLevelUnder", method = RequestMethod.PUT, params = {"nodeId", "parentNodeId"})
    public ArrFaLevel moveLevelUnder(@RequestParam("nodeId") Integer nodeId, @RequestParam("parentNodeId") Integer parentNodeId) {
        Assert.notNull(nodeId);
        Assert.notNull(parentNodeId);

        ArrFaLevel faLevel = levelRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
        ArrFaLevel parent = levelRepository.findByNodeIdAndDeleteChangeIsNull(parentNodeId);
        if(faLevel == null || parent == null){
            throw new IllegalArgumentException("Přesun se nezdařil. Záznam byl pravděpodobně smazán jiným uživatelem. Aktualizujte stránku");
        }

        if(faLevel.equals(parent)){
            throw new IllegalStateException("Nelze vložit záznam sám do sebe");
        }

        // vkládaný nesmí být rodičem uzlu pod který ho vkládám
        checkCycle(faLevel, parent);

        ArrFaChange change = createChange();
        shiftNodesUp(nodesToShift(faLevel), change);
        ArrFaLevel newLevel = createNewLevelVersion(faLevel, change);

        return addLastInLevel(newLevel, parent.getNodeId());
    }

    @Override
    @Transactional
    @RequestMapping(value = "/moveLevelAfter", method = RequestMethod.PUT, params = {"nodeId", "predecessorNodeId"})
    public ArrFaLevel moveLevelAfter(@RequestParam("nodeId") Integer nodeId, @RequestParam("predecessorNodeId") Integer predecessorNodeId) {
        Assert.notNull(nodeId);
        Assert.notNull(predecessorNodeId);

        ArrFaLevel faLevel = levelRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
        ArrFaLevel predecessor = levelRepository.findByNodeIdAndDeleteChangeIsNull(predecessorNodeId);
        if(faLevel == null || predecessor == null){
            throw new IllegalArgumentException("Přesun se nezdařil. Záznam byl pravděpodobně smazán jiným uživatelem. Aktualizujte stránku");
        }
        if(faLevel.equals(predecessor)){
            throw new IllegalStateException("Nelze vložit záznam na stejné místo ve stromu");
        }
        // vkládaný nesmí být rodičem uzlu za který ho vkládám
        checkCycle(faLevel, predecessor);

        ArrFaChange change = createChange();
        List<ArrFaLevel> nodesToShiftUp = nodesToShift(faLevel);
        List<ArrFaLevel> nodesToShiftDown = nodesToShift(predecessor);
        Integer position;
        if (faLevel.getParentNodeId().equals(predecessor.getParentNodeId())) {
            Collection<ArrFaLevel> nodesToShift = CollectionUtils.disjunction(nodesToShiftDown, nodesToShiftUp);
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


        ArrFaLevel newLevel = createNewLevelVersion(faLevel, change);

        return addInLevel(newLevel, predecessor.getParentNodeId(), position);
    }

    private void checkCycle(ArrFaLevel movedNode, ArrFaLevel targetNode) {
        Assert.notNull(movedNode);
        Assert.notNull(targetNode);

        ArrFaLevel node = targetNode;
        while (node.getParentNodeId() != null) {
            if (movedNode.getNodeId().equals(node.getParentNodeId())) {
                throw new IllegalStateException("Přesouvaný uzel je rodičem cílového uzlu. Přesun nelze provést.");
            }
            node = levelRepository.findByNodeIdAndDeleteChangeIsNull(node.getParentNodeId());
        }
    }

    private ArrFaLevel addLastInLevel(ArrFaLevel level, Integer parentNodeId) {
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

    private void shiftNodesDown(Collection<ArrFaLevel> nodesToShift, ArrFaChange change) {
        Assert.notNull(nodesToShift);
        Assert.notNull(change);

        for (ArrFaLevel node : nodesToShift) {
            ArrFaLevel newNode = createNewLevelVersion(node, change);
            newNode.setPosition(node.getPosition() + 1);
            levelRepository.save(newNode);
        }
    }

    private void shiftNodesUp(Collection<ArrFaLevel> nodesToShift, ArrFaChange change) {
        Assert.notNull(nodesToShift);
        Assert.notNull(change);

        for (ArrFaLevel node : nodesToShift) {
            ArrFaLevel newNode = createNewLevelVersion(node, change);
            newNode.setPosition(node.getPosition() - 1);
            levelRepository.save(newNode);
        }
    }
    private List<ArrFaLevel> nodesToShift(ArrFaLevel movedLevel) {
        Assert.notNull(movedLevel);

        return levelRepository.findByParentNodeAndPositionGreaterThanOrderByPositionAsc(movedLevel.getParentNodeId(),
                movedLevel.getPosition());
    }

    private ArrFaLevel addInLevel(ArrFaLevel level, Integer parentNodeId, Integer position) {
        Assert.notNull(level);
        Assert.notNull(position);

        level.setParentNodeId(parentNodeId);
        level.setPosition(position);
        return levelRepository.save(level);
    }

    @Override
    @Transactional
    @RequestMapping(value = "/deleteLevel", method = RequestMethod.PUT)
    public ArrFaLevel deleteLevel(@RequestParam("nodeId") Integer nodeId) {
        Assert.notNull(nodeId);

        ArrFaLevel level = levelRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
        ArrFaChange change = createChange();
        level.setDeleteChange(change);
        shiftNodesUp(nodesToShift(level), change);

        return levelRepository.save(level);
    }

    @Override
    @RequestMapping(value = "/findLevelByNodeId", method = RequestMethod.GET)
    public ArrFaLevel findLevelByNodeId(@RequestParam("nodeId")Integer nodeId) {
        Assert.notNull(nodeId);
        return  levelRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
    }

    @Override
    @RequestMapping(value = "/getOpenVersionByFindingAidId", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrFaVersion getOpenVersionByFindingAidId(@RequestParam(value = "findingAidId") Integer findingAidId) {
        Assert.notNull(findingAidId);
        ArrFaVersion faVersion = versionRepository.findByFindingAidIdAndLockChangeIsNull(findingAidId);

        return faVersion;
    }

    @Override
    @RequestMapping(value = "/getVersion", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrFaVersion getFaVersionById(@RequestParam("versionId") final Integer versionId) {
        Assert.notNull(versionId);
        return versionRepository.findOne(versionId);
    }

    @Override
    @RequestMapping(value = "/findSubLevels", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ArrFaLevelExt> findSubLevels(@RequestParam(value = "nodeId") Integer nodeId,
                                             @RequestParam(value = "versionId", required = false)  Integer versionId,
                                             @RequestParam(value = "formatData", required = false)  String formatData,
                                             @RequestParam(value = "descItemTypeIds", required = false) Integer[] descItemTypeIds) {
        Assert.notNull(nodeId);

        ArrFaChange change = null;
        if (versionId != null) {
            ArrFaVersion version = versionRepository.findOne(versionId);
            change = version.getLockChange();
        }
        final List<ArrFaLevel> levelList;
        if (change == null) {
            levelList = levelRepository.findByParentNodeIdAndDeleteChangeIsNullOrderByPositionAsc(nodeId);
        } else {
            levelList = levelRepository.findByParentNodeOrderByPositionAsc(nodeId, change);
        }

        Set<Integer> nodeIdSet = new HashSet<>();
        for (ArrFaLevel arrFaLevel : levelList) {
            nodeIdSet.add(arrFaLevel.getNodeId());
        }

        final List<ArrData> dataList;
        if (nodeIdSet == null || nodeIdSet.isEmpty()) {
            dataList = new LinkedList<>();
        } else if (change == null) {
            dataList = arrDataRepository.findByNodeIdsAndDeleteChangeIsNull(nodeIdSet);
        } else {
            dataList = arrDataRepository.findByNodeIdsAndChange(nodeIdSet, change);
        }
        Map<Integer, List<ArrData>> dataMap =
                ElzaTools.createGroupMap(dataList, p -> p.getDescItem().getNodeId());

        Set<Integer> idItemTypeSet = createItemTypeSet(descItemTypeIds);
        final List<ArrFaLevelExt> resultList = new LinkedList<ArrFaLevelExt>();
        for (ArrFaLevel arrFaLevel : levelList) {
            ArrFaLevelExt levelExt = new ArrFaLevelExt();
            BeanUtils.copyProperties(arrFaLevel, levelExt);
            List<ArrData> dataNodeList = dataMap.get(arrFaLevel.getNodeId());
            readItemData(levelExt, dataNodeList, idItemTypeSet, formatData);
            resultList.add(levelExt);
        }
        return resultList;
    }

    @RequestMapping(value = "/findLevels", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ArrFaLevel> findLevels(@RequestParam(value = "nodeId") Integer nodeId) {
        Assert.notNull(nodeId);
        return levelRepository.findByNodeIdOrderByCreateChangeAsc(nodeId);
    }

    @Override
    @RequestMapping(value = "/getLevel", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrFaLevelExt getLevel(@RequestParam(value = "nodeId") Integer nodeId,
                                  @RequestParam(value = "versionId", required = false) Integer versionId,
                                  @RequestParam(value = "descItemTypeIds", required = false) Integer[] descItemTypeIds) {
        Assert.notNull(nodeId);
        ArrFaChange change = null;
        if (versionId != null) {
            ArrFaVersion version = versionRepository.findOne(versionId);
            change = version.getLockChange();
        }

        final List<ArrFaLevel> levelList;
        final List<ArrData> dataList;
        if (change == null) {
            levelList = levelRepository.findByNodeIdOrderByCreateChangeAsc(nodeId);
            dataList = arrDataRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
        } else {
            levelList = levelRepository.findByNodeOrderByPositionAsc(nodeId, change);
            dataList = arrDataRepository.findByNodeIdAndChange(nodeId, change);
        }

        if (levelList.isEmpty()) {
            throw new IllegalStateException("Nebyl nalezen záznam podle nodId " + nodeId + " a versionId " + versionId);
        } else if (levelList.size() > 1) {
            throw new IllegalStateException("Bylo nalezeno více záznamů (" + levelList.size()
                    + ") podle nodId " + nodeId + " a versionId " + versionId);
        }
        ArrFaLevel arrFaLevel = levelList.get(0);
        Set<Integer> idItemTypeSet = createItemTypeSet(descItemTypeIds);

        ArrFaLevelExt levelExt = new ArrFaLevelExt();
        BeanUtils.copyProperties(arrFaLevel, levelExt);
        readItemData(levelExt, dataList, idItemTypeSet, null);
        return levelExt;
    }

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

    private void readItemData(final ArrFaLevelExt levelExt, final List<ArrData> dataList,
                              final Set<Integer> idItemTypeSet, final String formatData) {
        if (dataList == null) {
            return;
        }
        for (ArrData arrData : dataList) {
            Integer idItemType = arrData.getDescItem().getDescItemType().getId();
            if (idItemTypeSet != null && (!idItemTypeSet.contains(idItemType))) {
                continue;
            }
            ArrDescItemExt arrDescItemExt = new ArrDescItemExt();
            BeanUtils.copyProperties(arrData.getDescItem(), arrDescItemExt);
            if (arrData instanceof ArrDataString) {
                ArrDataString stringData = (ArrDataString) arrData;
                String stringValue = stringData.getValue();
                if (stringValue != null && stringValue.length() > 250 && formatData != null && FORMAT_ATTRIBUTE_SHORT.equals(formatData)) {
                    stringValue = stringValue.substring(0, 250);
                }
                arrDescItemExt.setData(stringValue);
            } else if (arrData instanceof ArrDataInteger) {
                ArrDataInteger stringData = (ArrDataInteger) arrData;
                arrDescItemExt.setData(stringData.getValue().toString());
            } if (arrData instanceof ArrDataText) {
                ArrDataText stringData = (ArrDataText) arrData;
                String stringValue = stringData.getValue();
                if (stringValue != null && stringValue.length() > 250 && formatData != null && FORMAT_ATTRIBUTE_SHORT.equals(formatData)) {
                    stringValue = stringValue.substring(0, 250);
                }
                arrDescItemExt.setData(stringValue);
            }
            levelExt.getDescItemList().add(arrDescItemExt);
        }
    }

    @Override
    @RequestMapping(value = "/createDescriptionItem/{faVersionId}", method = RequestMethod.POST)
    @Transactional
    public ArrDescItemExt createDescriptionItem(@RequestBody ArrDescItemExt descItemExt,
                                             @PathVariable(value = "faVersionId") Integer faVersionId) {
        Assert.notNull(descItemExt);
        Assert.notNull(faVersionId);

        Integer nodeId = descItemExt.getNodeId();
        Assert.notNull(nodeId);

        List<RulDescItemTypeExt> rulDescItemTypes = ruleManager.getDescriptionItemTypesForNodeId(faVersionId, nodeId, false);

        RulDescItemType rulDescItemType = descItemExt.getDescItemType();
        Assert.notNull(rulDescItemType);

        String data = descItemExt.getData();
        Assert.notNull(data);

        RulDescItemSpec rulDescItemSpec = descItemExt.getDescItemSpec();

        validateAllowedItemType(rulDescItemTypes, rulDescItemType);
        validateAllItemConstraintsBySpec(nodeId, rulDescItemType, data, rulDescItemSpec, null);
        validateAllItemConstraintsByType(nodeId, rulDescItemType, data, null);
        validateUniqueness();

        // uložení

        ArrDescItem descItem = new ArrDescItem();
        BeanUtils.copyProperties(descItemExt, descItem);

        ArrFaChange arrFaChange = createChange();
        descItem.setDeleteChange(null);
        descItem.setCreateChange(arrFaChange);
        descItem.setDescItemObjectId(getNextDescItemObjectId());


        Integer position;
        Integer maxPosition = descItemRepository.findMaxPositionByNodeIdAndDescItemTypeIdAndDeleteChangeIsNull(nodeId, rulDescItemType.getId());
        if (maxPosition == null) {
            position = 1; // ještě žádný neexistuje
        } else {
            position = maxPosition + 1;
        }

        Integer positionUI = descItemExt.getPosition();
        // je definovaná pozice u UI
        if (positionUI != null) {
            if(positionUI < 1) {
                throw new IllegalArgumentException("Pozice nemůže být menší než 1 (" + positionUI + ")");
            } else if (positionUI < position) { // pokud existují nejaké položky k posunutí
                position = positionUI;
                updatePositionsAfter(position, nodeId, arrFaChange, descItem);
            }
        }

        descItem.setPosition(position);

        descItemRepository.save(descItem);

        saveNewDataValue(rulDescItemType, data, descItem);

        BeanUtils.copyProperties(descItem, descItemExt);
        return descItemExt;
    }

    @Override
    @RequestMapping(value = "/updateDescriptionItem/{faVersionId},{createNewVersion}", method = RequestMethod.POST)
    @Transactional
    public ArrDescItemExt updateDescriptionItem(@RequestBody ArrDescItemExt descItemExt,
                                             @PathVariable(value = "faVersionId") Integer faVersionId,
                                             @PathVariable(value = "createNewVersion") Boolean createNewVersion) {
        Assert.notNull(descItemExt);
        Assert.notNull(faVersionId);

        ArrDescItem descItem = descItemRepository.findOne(descItemExt.getId());

        Assert.notNull(descItem);

        Integer nodeId = descItem.getNodeId();

        List<RulDescItemTypeExt> rulDescItemTypes = ruleManager.getDescriptionItemTypesForNodeId(faVersionId, nodeId, false);

        RulDescItemType rulDescItemType = descItem.getDescItemType();

        String data = descItemExt.getData();
        Assert.notNull(data);

        RulDescItemSpec rulDescItemSpec = descItemExt.getDescItemSpec();

        validateAllowedItemType(rulDescItemTypes, rulDescItemType);
        validateAllItemConstraintsBySpec(nodeId, rulDescItemType, data, rulDescItemSpec, descItem);
        validateAllItemConstraintsByType(nodeId, rulDescItemType, data, descItem);
        validateUniqueness();

        // uložení

        Integer position = descItem.getPosition();
        Integer positionUI = descItemExt.getPosition();

        if (createNewVersion) {

            ArrFaChange arrFaChange = createChange();
            descItem.setDeleteChange(arrFaChange);
            descItemRepository.save(descItem);

            ArrDescItem descItemNew = new ArrDescItem();
            descItemNew.setCreateChange(arrFaChange);
            descItemNew.setDeleteChange(null);
            descItemNew.setDescItemObjectId(descItem.getDescItemObjectId());
            descItemNew.setDescItemType(rulDescItemType);
            descItemNew.setDescItemSpec(rulDescItemSpec);
            descItemNew.setNodeId(descItem.getNodeId());

            Integer maxPosition = descItemRepository.findMaxPositionByNodeIdAndDescItemTypeIdAndDeleteChangeIsNull(nodeId, rulDescItemType.getId());

            // provedla se změna pozice
            if (positionUI != position) {

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
                    updatePositionsBetween(position, positionUI, nodeId, arrFaChange, descItem);
                } else {
                    // posun výš
                    updatePositionsBefore(position, nodeId, arrFaChange, descItem);
                }

                descItemNew.setPosition(positionUI);
            } else {
                descItemNew.setPosition(descItem.getPosition());
            }

            descItemRepository.save(descItemNew);
            descItem = descItemNew;

            saveNewDataValue(rulDescItemType, data, descItem);

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

            saveUpdateDataValue(rulDescItemType, data, arrData);

            descItem.setDescItemSpec(rulDescItemSpec);
            descItemRepository.save(descItem);
        }

        BeanUtils.copyProperties(descItem, descItemExt);
        return descItemExt;
    }

    @Override
    @RequestMapping(value = "/deleteDescriptionItem/{descItemObjectId}", method = RequestMethod.DELETE)
    @Transactional
    public ArrDescItemExt deleteDescriptionItem(@PathVariable(value = "descItemObjectId") Integer descItemObjectId) {
        Assert.notNull(descItemObjectId);
        List<ArrDescItem> descItems = descItemRepository.findByDescItemObjectIdAndDeleteChangeIsNull(descItemObjectId);

        // musí být právě jeden
        if (descItems.size() != 1) {
            throw new IllegalArgumentException("Neplatný počet záznamů (" + descItems.size() + ")");
        }

        ArrDescItem descItem = descItems.get(0);

        ArrFaChange arrFaChange = createChange();
        descItem.setDeleteChange(arrFaChange);
        descItemRepository.save(descItem);

        // TODO: je třeba ověřit, jestli není potřeba přečíslovat atributy, protože teď může být mezera v position

        ArrDescItemExt descItemExt = new ArrDescItemExt();
        BeanUtils.copyProperties(descItem, descItemExt);
        return descItemExt;
    }

    @Override
    @RequestMapping(value = "/saveDescriptionItems", method = RequestMethod.POST)
    @Transactional
    public List<ArrDescItemExt> saveDescriptionItems(@RequestBody ArrDescItemSavePack descItemSavePack) {
        Assert.notNull(descItemSavePack);
        // TODO: dopsat implementaci
        return null;
    }

    /**
     * Pokud má typ atributu vyplněný constraint, který má repeatable false, tak je potřeba zkontrolovat, jestli pro daný node_id už neexistuje jiná hodnota stejného typu atributu
     *
     * @param nodeId                Identifikátor uzlu
     * @param rulDescItemType       Typ atributu
     * @param rulDescItemConstraint Podmínka
     */
    private void validateRepeatableType(Integer nodeId, RulDescItemType rulDescItemType, RulDescItemConstraint rulDescItemConstraint, ArrDescItem descItem) {
        if (rulDescItemConstraint.getRepeatable() != null && !rulDescItemConstraint.getRepeatable()) {
            List<ArrDescItem> arrDescItems = descItemRepository.findByNodeIdAndDeleteChangeIsNullAndDescItemTypeId(nodeId, rulDescItemType.getId());
            arrDescItems.remove(descItem); // odstraníme ten, co přidáváme / upravujeme
            if (arrDescItems.size() > 0) {
                throw new IllegalArgumentException("Pro daný node_id už existuje jiná hodnota stejného typu atributu");
            }
        }
    }

    /**
     * Pokud má specifikace typu atributu vyplněný constraint, který má repeatable false, tak je potřeba zkontrolovat, jestli pro daný node_id a specifikaci už neexistuje jiná
     * hodnota stejného typu atributu
     *
     * @param nodeId                Identifikátor uzlu
     * @param rulDescItemType       Typ atributu
     * @param rulDescItemSpec       Specifický typ atributu
     * @param rulDescItemConstraint Podmínka
     */
    private void validateRepeatableSpec(Integer nodeId,
                                        RulDescItemType rulDescItemType,
                                        RulDescItemSpec rulDescItemSpec,
                                        RulDescItemConstraint rulDescItemConstraint,
                                        ArrDescItem descItem) {
        if (rulDescItemConstraint.getRepeatable() != null && !rulDescItemConstraint.getRepeatable()) {
            List<ArrDescItem> arrDescItems = descItemRepository.findByNodeIdAndDeleteChangeIsNullAndDescItemTypeIdAndSpecItemTypeId(nodeId, rulDescItemType.getId(),
                    rulDescItemSpec.getId());
            arrDescItems.remove(descItem); // odstraníme ten, co přidáváme / upravujeme
            if (arrDescItems.size() > 0) {
                throw new IllegalArgumentException("Pro daný node_id už existuje jiná hodnota stejného typu atributu");
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
        if (textLenghtLimit != null) {
            if (data.length() > textLenghtLimit) {
                throw new IllegalStateException("Hodnota je příliš dlouhá - " + data.length() + "/" + textLenghtLimit);
            }
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
        if (regexp != null) {
            if (!data.matches(regexp)) {
                throw new IllegalStateException("Hodnota '" + data + "' neodpovídá výrazu " + regexp);
            }
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
     * Specifikace (pokud není NULL) musí patřit k typu atributu, který přidávám
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
     * Kontroluje data vůči podmínkám specifického typu atributu
     *
     * @param nodeId          Identifikátor uzlu
     * @param rulDescItemType Typ atributu
     * @param data            Kontrolovaná data
     * @param rulDescItemSpec Specifický typ atributu
     */
    private void validateAllItemConstraintsBySpec(Integer nodeId, RulDescItemType rulDescItemType, String data, RulDescItemSpec rulDescItemSpec, ArrDescItem descItem) {
        if (rulDescItemSpec != null) {
            validateSpecificationAttribute(rulDescItemType, rulDescItemSpec);
            List<RulDescItemConstraint> rulDescItemConstraints = descItemConstraintRepository.findByDescItemSpec(rulDescItemSpec);
            for (RulDescItemConstraint rulDescItemConstraint : rulDescItemConstraints) {
                validateRepeatableSpec(nodeId, rulDescItemType, rulDescItemSpec, rulDescItemConstraint, descItem);
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
     * Kontroluje data vůči podmínkám typu atributu
     *
     * @param nodeId          Identifikátor uzlu
     * @param rulDescItemType Typ atributu
     * @param data            Kontrolovaná data
     */
    private void validateAllItemConstraintsByType(Integer nodeId, RulDescItemType rulDescItemType, String data, ArrDescItem descItem) {
        List<RulDescItemConstraint> rulDescItemConstraints = descItemConstraintRepository.findByDescItemType(rulDescItemType);
        for (RulDescItemConstraint rulDescItemConstraint : rulDescItemConstraints) {
            validateRepeatableType(nodeId, rulDescItemType, rulDescItemConstraint, descItem);
            validateDataDescItemConstraintTextLenghtLimit(data, rulDescItemConstraint);
            validateDataDescItemConstraintRegexp(data, rulDescItemConstraint);
        }
    }

    /**
     * TODO: kontrolovat unikátnost
     */
    private void validateUniqueness(/* TODO */) {

    }

    /**
     * Uloží novou hodnotu attributu do tabulky podle jeho typu
     *
     * @param rulDescItemType Typ atributu
     * @param data            Hodnota attributu
     * @param descItem        Spjatý objekt attributu
     */
    private void saveNewDataValue(RulDescItemType rulDescItemType, String data, ArrDescItem descItem) {
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
            case "TEXT":
                ArrDataText valueText = new ArrDataText();
                valueText.setDataType(rulDescItemType.getDataType());
                valueText.setDescItem(descItem);
                valueText.setValue(data);
                dataTextRepository.save(valueText);
                break;

            case "DATACE":
                ArrDataDatace valueDatace = new ArrDataDatace();
                valueDatace.setDataType(rulDescItemType.getDataType());
                valueDatace.setDescItem(descItem);
                valueDatace.setValue(data);
                dataDataceRepository.save(valueDatace);
                break;

            case "REF":
                ArrDataReference valueReference = new ArrDataReference();
                valueReference.setDataType(rulDescItemType.getDataType());
                valueReference.setDescItem(descItem);
                valueReference.setValue(data);
                dataReferenceRepository.save(valueReference);
                break;

            default:
                throw new IllegalStateException("Datový typ hodnoty není implementován");
        }
    }

    /**
     * Uloží upravenout hodnotu attributu do tabulky podle jeho typu
     *
     * @param rulDescItemType Typ atributu
     * @param data            Hodnota attributu
     * @param arrData         Upravovaná položka hodnoty attributu
     */
    private void saveUpdateDataValue(RulDescItemType rulDescItemType, String data, ArrData arrData) {
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
            case "TEXT":
                ArrDataText valueText = (ArrDataText) arrData;
                valueText.setValue(data);
                dataTextRepository.save(valueText);
                break;

            case "DATACE":
                ArrDataDatace valueDatace = (ArrDataDatace) arrData;
                valueDatace.setValue(data);
                dataDataceRepository.save(valueDatace);
                break;

            case "REF":
                ArrDataReference valueReference = (ArrDataReference) arrData;
                valueReference.setValue(data);
                dataReferenceRepository.save(valueReference);
                break;

            default:
                throw new IllegalStateException("Datový typ hodnoty není implementován");
        }
    }


    private void updatePositionsBetween(Integer position, Integer position2, Integer nodeId, ArrFaChange arrFaChange, ArrDescItem descItem) {
        List<ArrDescItem> descItemListForUpdate = descItemRepository.findByNodeIdAndDescItemTypeIdAndDeleteChangeIsNullBetweenPositions(position, position2, nodeId, descItem.getDescItemType().getDescItemTypeId());
        updatePositionsRaw(arrFaChange, descItemListForUpdate, -1);
    }

    private void updatePositionsAfter(Integer position, Integer nodeId, ArrFaChange arrFaChange, ArrDescItem descItem) {
        List<ArrDescItem> descItemListForUpdate = descItemRepository.findByNodeIdAndDescItemTypeIdAndDeleteChangeIsNullAfterPosistion(position, nodeId, descItem.getDescItemType().getDescItemTypeId());
        updatePositionsRaw(arrFaChange, descItemListForUpdate, 1);
    }

    private void updatePositionsBefore(Integer position, Integer nodeId, ArrFaChange arrFaChange, ArrDescItem descItem) {
        List<ArrDescItem> descItemListForUpdate = descItemRepository.findByNodeIdAndDescItemTypeIdAndDeleteChangeIsNullBeforePosistion(position, nodeId, descItem.getDescItemType().getDescItemTypeId());
        updatePositionsRaw(arrFaChange, descItemListForUpdate, 1);
    }

    /**
     * Upraví pozici s kopií dat u všech položek ze seznamu
     * @param arrFaChange   Změna
     * @param descItemListForUpdate Seznam upravovaných položek
     * @param diff  Číselná změna (posun)
     */
    private void updatePositionsRaw(ArrFaChange arrFaChange, List<ArrDescItem> descItemListForUpdate, int diff) {
        for(ArrDescItem descItemUpdate : descItemListForUpdate) {
            descItemUpdate.setDeleteChange(arrFaChange);

            ArrDescItem descItemNew = new ArrDescItem();
            descItemNew.setCreateChange(arrFaChange);
            descItemNew.setDeleteChange(null);
            descItemNew.setDescItemObjectId(descItemUpdate.getDescItemObjectId());
            descItemNew.setDescItemType(descItemUpdate.getDescItemType());
            descItemNew.setDescItemSpec(descItemUpdate.getDescItemSpec());
            descItemNew.setNodeId(descItemUpdate.getNodeId());
            descItemNew.setPosition(descItemUpdate.getPosition() + diff);

            descItemRepository.save(descItemUpdate);
            descItemRepository.save(descItemNew);

            copyDataValue(descItemUpdate, descItemNew);
        }
    }

    /**
     * Vytvoří kopie hodnot pro novou verzi hodnoty atributu
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
            case "TEXT":
                ArrDataText valueText = (ArrDataText) arrData;
                ArrDataText valueTextNew = new ArrDataText();
                valueTextNew.setDataType(arrData.getDataType());
                valueTextNew.setValue(valueText.getValue());
                valueTextNew.setDescItem(descItemNew);
                dataTextRepository.save(valueTextNew);
                break;

            case "DATACE":
                ArrDataDatace valueDatace = (ArrDataDatace) arrData;
                ArrDataDatace valueDataceNew = new ArrDataDatace();
                valueDataceNew.setDataType(arrData.getDataType());
                valueDataceNew.setValue(valueDatace.getValue());
                valueDataceNew.setDescItem(descItemNew);
                dataDataceRepository.save(valueDataceNew);
                break;

            case "REF":
                ArrDataReference valueReference = (ArrDataReference) arrData;
                ArrDataReference valueReferenceNew = new ArrDataReference();
                valueReferenceNew.setDataType(arrData.getDataType());
                valueReferenceNew.setValue(valueReference.getValue());
                valueReferenceNew.setDescItem(descItemNew);
                dataReferenceRepository.save(valueReferenceNew);
                break;

            default:
                throw new IllegalStateException("Datový typ hodnoty není implementován");
        }

    }

}
