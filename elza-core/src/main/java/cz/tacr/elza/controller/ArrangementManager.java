package cz.tacr.elza.controller;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.domain.ArrArrangementType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDescItemExt;
import cz.tacr.elza.domain.ArrFaChange;
import cz.tacr.elza.domain.ArrFaLevel;
import cz.tacr.elza.domain.ArrFaLevelExt;
import cz.tacr.elza.domain.ArrFaVersion;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.DataRepository;
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

    @Autowired
    private DataRepository arrDataRepository;

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


    @RequestMapping(value = "/getFindingAids", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<ArrFindingAid> getFindingAids() {
        return findingAidRepository.findAll();
    }

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

    @RequestMapping(value = "/getArrangementTypes", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ArrArrangementType> getArrangementTypes() {
        return arrangementTypeRepository.findAll();
    }

    @RequestMapping(value = "/getFindingAidVersions", method = RequestMethod.GET, params = {"findingAidId"}, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ArrFaVersion> getFindingAidVersions(@RequestParam(value = "findingAidId") final Integer findingAidId) {
        Assert.notNull(findingAidId);

        return versionRepository.findVersionsByFindingAidIdOrderByCreateDateAsc(findingAidId);
    }

    @RequestMapping(value = "/getFindingAid", method = RequestMethod.GET, params = {"findingAidId"}, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrFindingAid getFindingAid(final Integer findingAidId) {
        Assert.notNull(findingAidId);
        return findingAidRepository.getOne(findingAidId);
    }

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

    @Transactional
    @RequestMapping(value = "/addLevelBefore", method = RequestMethod.PUT, params = {"nodeId"})
    public ArrFaLevel addLevelBefore(@RequestParam("nodeId") Integer nodeId){
        Assert.notNull(nodeId);

        ArrFaLevel faLevel = levelRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
        ArrFaChange change = createChange();

        return createBeforeInLevel(change, faLevel);
    }

    @Transactional
    @RequestMapping(value = "/addLevel", method = RequestMethod.PUT, params = {"findingAidId"})
    public ArrFaLevel addLevel(@RequestParam("findingAidId") Integer findingAidId) {
        Assert.notNull(findingAidId);

        ArrFaVersion lastVersion = versionRepository.findByFindingAidIdAndLockChangeIsNull(findingAidId);
        ArrFaChange change = createChange();
        return createLastInLevel(change, lastVersion.getRootNode());
    }



    @Transactional
    @RequestMapping(value = "/addLevelAfter", method = RequestMethod.PUT, params = {"nodeId"})
    public ArrFaLevel addLevelAfter(@RequestParam("nodeId") Integer nodeId) {
        Assert.notNull(nodeId);

        ArrFaLevel faLevel = levelRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
        ArrFaChange change = createChange();
        return createAfterInLevel(change, faLevel);
    }

    @Transactional
    @RequestMapping(value = "/addLevelChild", method = RequestMethod.PUT, params = {"nodeId"})
    public ArrFaLevel addLevelChild(@RequestParam("nodeId") Integer nodeId) {
        Assert.notNull(nodeId);

        ArrFaLevel faLevel = levelRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
        ArrFaChange change = createChange();
        return createLastInLevel(change, faLevel);
    }


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

    @RequestMapping(value = "/findLevelByNodeId", method = RequestMethod.GET)
    public ArrFaLevelExt findLevelByNodeId(@RequestParam("nodeId")Integer nodeId, @RequestParam(value = "descItemTypeIds", required = false) Integer[] descItemTypeIds) {
        Assert.notNull(nodeId);
        Set<Integer> idItemTypeSet = createItemTypeSet(descItemTypeIds);
        ArrFaLevel level =  levelRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
        final List<ArrData> dataList = arrDataRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
        ArrFaLevelExt levelExt = new ArrFaLevelExt();
        BeanUtils.copyProperties(level, levelExt);
        readItemData(levelExt, dataList, idItemTypeSet);
        return levelExt;
    }

    @RequestMapping(value = "/getOpenVersionByFindingAidId", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrFaVersion getOpenVersionByFindingAidId(@RequestParam(value = "findingAidId") Integer findingAidId) {
        Assert.notNull(findingAidId);
        ArrFaVersion faVersion = versionRepository.findByFindingAidIdAndLockChangeIsNull(findingAidId);

        return faVersion;
    }

    @RequestMapping(value = "/getVersion", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrFaVersion getFaVersionById(@RequestParam("versionId") final Integer versionId) {
        Assert.notNull(versionId);
        return versionRepository.findOne(versionId);
    }

    @RequestMapping(value = "/findSubLevels", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ArrFaLevel> findSubLevels(@RequestParam(value = "nodeId") Integer nodeId,
            @RequestParam(value = "versionId", required = false)  Integer versionId) {
        Assert.notNull(nodeId);

        ArrFaChange change = null;
        if (versionId != null) {
            ArrFaVersion version = versionRepository.findOne(versionId);
            change = version.getLockChange();
        }
        if (change == null) {
            return levelRepository.findByParentNodeIdAndDeleteChangeIsNullOrderByPositionAsc(nodeId);
        }
        return levelRepository.findByParentNodeOrderByPositionAsc(nodeId, change);
    }

    @RequestMapping(value = "/findLevels", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ArrFaLevel> findLevels(@RequestParam(value = "nodeId") Integer nodeId) {
        Assert.notNull(nodeId);
        return levelRepository.findByNodeIdOrderByCreateChangeAsc(nodeId);
    }

    @RequestMapping(value = "/getLevel", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ArrFaLevelExt> getLevel(@RequestParam(value = "nodeId") Integer nodeId,
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

        Set<Integer> idItemTypeSet = createItemTypeSet(descItemTypeIds);

        List<ArrFaLevelExt> resultList = new LinkedList<>();
        for (ArrFaLevel arrFaLevel : levelList) {
            ArrFaLevelExt levelExt = new ArrFaLevelExt();
            BeanUtils.copyProperties(arrFaLevel, levelExt);
            readItemData(levelExt, dataList, idItemTypeSet);
            resultList.add(levelExt);
        }
        return resultList;
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

    private void readItemData(final ArrFaLevelExt levelExt, final List<ArrData> dataList, final Set<Integer> idItemTypeSet) {
        for (ArrData arrData : dataList) {
            Integer idItemType = arrData.getDescItem().getDescItemType().getId();
            if (idItemTypeSet != null && (!idItemTypeSet.contains(idItemType))) {
                continue;
            }
            ArrDescItemExt arrDescItemExt = new ArrDescItemExt();
            BeanUtils.copyProperties(arrData.getDescItem(), arrDescItemExt);
            if (arrData instanceof ArrDataString) {
                ArrDataString stringData = (ArrDataString) arrData;
                arrDescItemExt.setData(stringData.getValue());
            } else if (arrData instanceof ArrDataInteger) {
                ArrDataInteger stringData = (ArrDataInteger) arrData;
                arrDescItemExt.setData(stringData.getValue().toString());
            } if (arrData instanceof ArrDataText) {
                ArrDataText stringData = (ArrDataText) arrData;
                arrDescItemExt.setData(stringData.getValue());
            }
            levelExt.getDescItemList().add(arrDescItemExt);
        }
    }
}
