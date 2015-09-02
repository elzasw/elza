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
import cz.tacr.elza.domain.ArrArrangementType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataDatace;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataReference;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUnitid;
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
import cz.tacr.elza.repository.DataCoordinatesRepository;
import cz.tacr.elza.repository.DataDataceRepository;
import cz.tacr.elza.repository.DataIntegerRepository;
import cz.tacr.elza.repository.DataPartyRefRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.DataReferenceRepository;
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
public class ArrangementManager implements cz.tacr.elza.api.controller.ArrangementManager<ArrFindingAid, ArrFaVersion, ArrDescItemExt, ArrDescItemSavePack, ArrFaLevel> {

    @PersistenceContext
    private EntityManager entityManager;

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
    private DataReferenceRepository dataReferenceRepository;

    @Autowired
    private RuleManager ruleManager;

    @Autowired
    private DescItemTypeRepository descItemTypeRepository;

    @Autowired
    private DescItemSpecRepository descItemSpecRepository;

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
        newNode.setVersion(node.getVersion());

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

    @RequestMapping(value = "/updateFindingAid", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Override
    @Transactional
    public ArrFindingAid updateFindingAid(@RequestBody ArrFindingAid findingAid) {
        Assert.notNull(findingAid);

        return findingAidRepository.save(findingAid);
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
    @RequestMapping(value = "/approveVersion", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrFaVersion approveVersion(@RequestBody final ArrFaVersion version, @RequestParam("arrangementTypeId") final Integer arrangementTypeId, @RequestParam("ruleSetId") final Integer ruleSetId) {
        Assert.notNull(version);
        Assert.notNull(arrangementTypeId);
        Assert.notNull(ruleSetId);

        ArrFindingAid findingAid = version.getFindingAid();

        ArrFaChange change = createChange();
        version.setLockChange(change);
        versionRepository.save(version);

        ArrArrangementType arrangementType = arrangementTypeRepository.findOne(arrangementTypeId);
        RulRuleSet ruleSet = ruleSetRepository.findOne(ruleSetId);

        return createVersion(change, findingAid, arrangementType, ruleSet, version.getRootNode());
    }

    @Override
    @Transactional
    @RequestMapping(value = "/addLevelBefore", method = RequestMethod.PUT)
    public ArrFaLevel addLevelBefore(@RequestBody ArrFaLevel node){
        Assert.notNull(node);

        ArrFaChange change = createChange();

        return createBeforeInLevel(change, node);
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
    @RequestMapping(value = "/addLevelAfter", method = RequestMethod.PUT)
    public ArrFaLevel addLevelAfter(@RequestBody ArrFaLevel node) {
        Assert.notNull(node);

        ArrFaChange change = createChange();
        return createAfterInLevel(change, node);
    }

    @Override
    @Transactional
    @RequestMapping(value = "/addLevelChild", method = RequestMethod.PUT)
    public ArrFaLevel addLevelChild(@RequestBody ArrFaLevel node) {
        Assert.notNull(node);

        ArrFaChange change = createChange();
        return createLastInLevel(change, node);
    }

    @Override
    @Transactional
    @RequestMapping(value = "/moveLevelBefore", method = RequestMethod.PUT)
    public ArrFaLevel moveLevelBefore(@RequestBody ArrFaLevel node, @RequestParam("followerNodeId") Integer followerNodeId) {
        Assert.notNull(node);
        Assert.notNull(followerNodeId);

        ArrFaLevel follower = levelRepository.findByNodeIdAndDeleteChangeIsNull(followerNodeId);

        if(node == null || follower == null){
            throw new IllegalArgumentException("Přesun se nezdařil. Záznam byl pravděpodobně smazán jiným uživatelem. Aktualizujte stránku");
        }
        if(node.equals(follower)){
            throw new IllegalStateException("Nelze vložit záznam na stejné místo ve stromu");
        }

        checkCycle(node, follower);

        ArrFaChange change = createChange();
        List<ArrFaLevel> nodesToShiftUp = nodesToShift(node);
        List<ArrFaLevel> nodesToShiftDown = nodesToShift(follower);
        nodesToShiftDown.add(follower);

        Integer position;
        if (node.getParentNodeId().equals(follower.getParentNodeId())) {
            Collection<ArrFaLevel> nodesToShift = CollectionUtils.disjunction(nodesToShiftDown, nodesToShiftUp);
            if (node.getPosition() > follower.getPosition()) {
                nodesToShift.remove(node);
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

        ArrFaLevel newLevel = createNewLevelVersion(node, change);
        return addInLevel(newLevel, follower.getParentNodeId(), position);
    }

    @Override
    @Transactional
    @RequestMapping(value = "/moveLevelUnder", method = RequestMethod.PUT)
    public ArrFaLevel moveLevelUnder(@RequestBody ArrFaLevel node, @RequestParam("parentNodeId") Integer parentNodeId) {
        Assert.notNull(node);
        Assert.notNull(parentNodeId);

        ArrFaLevel parent = levelRepository.findByNodeIdAndDeleteChangeIsNull(parentNodeId);
        if(node == null || parent == null){
            throw new IllegalArgumentException("Přesun se nezdařil. Záznam byl pravděpodobně smazán jiným uživatelem. Aktualizujte stránku");
        }

        if(node.equals(parent)){
            throw new IllegalStateException("Nelze vložit záznam sám do sebe");
        }

        // vkládaný nesmí být rodičem uzlu pod který ho vkládám
        checkCycle(node, parent);

        ArrFaChange change = createChange();
        shiftNodesUp(nodesToShift(node), change);
        ArrFaLevel newLevel = createNewLevelVersion(node, change);

        return addLastInLevel(newLevel, parent.getNodeId());
    }

    @Override
    @Transactional
    @RequestMapping(value = "/moveLevelAfter", method = RequestMethod.PUT)
    public ArrFaLevel moveLevelAfter(@RequestBody ArrFaLevel node, @RequestParam("predecessorNodeId") Integer predecessorNodeId) {
        Assert.notNull(node);
        Assert.notNull(predecessorNodeId);

        ArrFaLevel predecessor = levelRepository.findByNodeIdAndDeleteChangeIsNull(predecessorNodeId);
        if(node == null || predecessor == null){
            throw new IllegalArgumentException("Přesun se nezdařil. Záznam byl pravděpodobně smazán jiným uživatelem. Aktualizujte stránku");
        }
        if(node.equals(predecessor)){
            throw new IllegalStateException("Nelze vložit záznam na stejné místo ve stromu");
        }
        // vkládaný nesmí být rodičem uzlu za který ho vkládám
        checkCycle(node, predecessor);

        ArrFaChange change = createChange();
        List<ArrFaLevel> nodesToShiftUp = nodesToShift(node);
        List<ArrFaLevel> nodesToShiftDown = nodesToShift(predecessor);
        Integer position;
        if (node.getParentNodeId().equals(predecessor.getParentNodeId())) {
            Collection<ArrFaLevel> nodesToShift = CollectionUtils.disjunction(nodesToShiftDown, nodesToShiftUp);
            if (node.getPosition() > predecessor.getPosition()) {
                nodesToShift.remove(node);
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


        ArrFaLevel newLevel = createNewLevelVersion(node, change);

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
    public ArrFaLevel findLevelByNodeId(@RequestParam("nodeId") Integer nodeId, @RequestParam("versionId") Integer versionId) {
        Assert.notNull(nodeId);

        ArrFaChange change = null;
        if (versionId != null) {
            ArrFaVersion version = versionRepository.findOne(versionId);
            change = version.getLockChange();
        }

        final ArrFaLevel level;
        if (change == null) {
            level = levelRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
        } else {
            level = levelRepository.findByNodeOrderByPositionAsc(nodeId, change);
        }

        if (level == null) {
            throw new IllegalStateException("Nebyl nalezen záznam podle nodId " + nodeId + " a versionId " + versionId);
        }

        return level;
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
    @RequestMapping(value = "/findSubLevelsExt", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
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

    @Override
    @RequestMapping(value = "/findSubLevels", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ArrFaLevel> findSubLevels(@RequestParam(value = "nodeId") Integer nodeId,
            @RequestParam(value = "versionId", required = false)  Integer versionId) {
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

        return levelList;
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

        final ArrFaLevel level;
        final List<ArrData> dataList;
        if (change == null) {
            level = levelRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
            dataList = arrDataRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
        } else {
            level = levelRepository.findByNodeOrderByPositionAsc(nodeId, change);
            dataList = arrDataRepository.findByNodeIdAndChange(nodeId, change);
        }

        if (level == null) {
            throw new IllegalStateException("Nebyl nalezen záznam podle nodId " + nodeId + " a versionId " + versionId);
        }
        Set<Integer> idItemTypeSet = createItemTypeSet(descItemTypeIds);

        ArrFaLevelExt levelExt = new ArrFaLevelExt();
        BeanUtils.copyProperties(level, levelExt);
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
        ArrFaChange arrFaChange = createChange();
        return createDescriptionItemRaw(descItemExt, faVersionId, arrFaChange);
    }

    @Override
    @RequestMapping(value = "/updateDescriptionItem/{faVersionId},{createNewVersion}", method = RequestMethod.POST)
    @Transactional
    public ArrDescItemExt updateDescriptionItem(@RequestBody ArrDescItemExt descItemExt,
                                                @PathVariable(value = "faVersionId") Integer faVersionId,
                                                @PathVariable(value = "createNewVersion") Boolean createNewVersion) {
        Assert.notNull(descItemExt);
        Assert.notNull(faVersionId);
        Assert.notNull(createNewVersion);

        ArrFaChange arrFaChange = null;
        if (createNewVersion) {
            arrFaChange = createChange();
        }

        return updateDescriptionItemRaw(descItemExt, faVersionId, createNewVersion, arrFaChange);
    }

    @Override
    @RequestMapping(value = "/deleteDescriptionItem/{descItemObjectId}", method = RequestMethod.DELETE)
    @Transactional
    public ArrDescItemExt deleteDescriptionItem(@PathVariable(value = "descItemObjectId") Integer descItemObjectId) {
        Assert.notNull(descItemObjectId);
        ArrFaChange arrFaChange = createChange();
        return deleteDescriptionsItemRaw(descItemObjectId, arrFaChange);
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

        Integer versionId = descItemSavePack.getFaVersionId();
        Assert.notNull(versionId);

        Boolean createNewVersion = descItemSavePack.getCreateNewVersion();
        Assert.notNull(createNewVersion);

        List<ArrDescItemExt> descItemRet = new ArrayList<>();

        // analýza vstupních dat, roztřídění

        List<ArrDescItemExt> createDescItems = new ArrayList<>();
        List<ArrDescItemExt> updateDescItems = new ArrayList<>();
        List<ArrDescItemExt> updatePositionDescItems = new ArrayList<>();

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

        ArrFaChange arrFaChange = null;

        // provedení akcí

        if (createNewVersion) {
            arrFaChange = createChange();

            // mazání
            for (ArrDescItemExt descItem : deleteDescItems) {
                descItemRet.add(deleteDescriptionsItemRaw(descItem.getDescItemObjectId(), arrFaChange));
            }

            // vytvoření
            for (ArrDescItemExt descItem : createDescItems) {
                descItemRet.add(createDescriptionItemRaw(descItem, versionId, arrFaChange));
            }

            // úpravy s verzováním
            for (ArrDescItemExt descItem : updateDescItems) {
                descItemRet.add(updateDescriptionItemRaw(descItem, versionId, true, arrFaChange));
            }

        } else {
            // úpravy bez verzování
            for (ArrDescItemExt descItem : updateDescItems) {
                descItemRet.add(updateDescriptionItemRaw(descItem, versionId, false, null));
            }
        }

        return descItemRet;
    }

    private void refreshDescItem(ArrDescItemExt descItem) {
        List<ArrDescItem> descItems = descItemRepository.findByDescItemObjectIdAndDeleteChangeIsNull(descItem.getDescItemObjectId());
        // musí být právě jeden
        if (descItems.size() != 1) {
            throw new IllegalArgumentException("Neplatný počet záznamů (" + descItems.size() + ")");
        }
        BeanUtils.copyProperties(descItems.get(0), descItem);
    }

    /**
     * Přidá atribut archivního popisu včetně hodnoty k existující jednotce archivního popisu.
     *
     * @param descItemExt vytvářená položka
     * @param faVersionId identifikátor verze
     * @param arrFaChange společná změna
     * @return výsledný(vytvořený) attribut
     */
    private ArrDescItemExt createDescriptionItemRaw(ArrDescItemExt descItemExt, Integer faVersionId, ArrFaChange arrFaChange) {
        Assert.notNull(descItemExt);
        Assert.notNull(faVersionId);
        Assert.notNull(arrFaChange);

        Integer nodeId = descItemExt.getNodeId();
        Assert.notNull(nodeId);

        List<RulDescItemTypeExt> rulDescItemTypes = ruleManager.getDescriptionItemTypesForNodeId(faVersionId, nodeId, false);

        RulDescItemType rulDescItemType = descItemTypeRepository.findOne(descItemExt.getDescItemType().getDescItemTypeId());
        Assert.notNull(rulDescItemType);

        String data = descItemExt.getData();
        Assert.notNull(data, "Není vyplněna hodnota");

        RulDescItemSpec rulDescItemSpec = (descItemExt.getDescItemSpec() != null) ? descItemSpecRepository.findOne(descItemExt.getDescItemSpec().getDescItemSpecId()) : null;

        validateAllowedItemType(rulDescItemTypes, rulDescItemType);
        validateAllItemConstraintsBySpec(nodeId, rulDescItemType, data, rulDescItemSpec, null);
        validateAllItemConstraintsByType(nodeId, rulDescItemType, data, null);

        // uložení

        ArrDescItem descItem = new ArrDescItem();
        BeanUtils.copyProperties(descItemExt, descItem);

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
            if (positionUI < 1) {
                throw new IllegalArgumentException("Pozice nemůže být menší než 1 (" + positionUI + ")");
            } else if (positionUI < position) { // pokud existují nejaké položky k posunutí
                position = positionUI;
                updatePositionsAfter(position, nodeId, arrFaChange, descItem, 1);
            }
        }

        descItem.setPosition(position);

        descItemRepository.save(descItem);

        saveNewDataValue(rulDescItemType, data, descItem);

        ArrDescItemExt descItemRet = new ArrDescItemExt();
        BeanUtils.copyProperties(descItem, descItemRet);
        descItemRet.setData(descItemExt.getData());
        return descItemRet;
    }

    /**
     * Upraví hodnotu existujícího atributu archivního popisu.
     *
     * @param descItemExt upravovaná položka
     * @param faVersionId identifikátor verze
     * @param arrFaChange společná změna
     * @return výsledný(upravený) attribut
     */
    private ArrDescItemExt updateDescriptionItemRaw(ArrDescItemExt descItemExt, Integer faVersionId, Boolean createNewVersion, ArrFaChange arrFaChange) {
        Assert.notNull(descItemExt);
        Assert.notNull(faVersionId);
        Assert.notNull(createNewVersion);

        if (createNewVersion ^ arrFaChange != null) {
            throw new IllegalArgumentException("Pokud vytvářím novou verzi, musí být předaná reference změny. Pokud verzi nevytvářím, musí být reference změny null.");
        }

        List<ArrDescItem> descItems = descItemRepository.findByDescItemObjectIdAndDeleteChangeIsNull(descItemExt.getDescItemObjectId());
        // musí být právě jeden
        if (descItems.size() != 1) {
            throw new IllegalArgumentException("Neplatný počet záznamů (" + descItems.size() + ")");
        }
        ArrDescItem descItem = descItems.get(0);

        Assert.notNull(descItem);

        Integer nodeId = descItem.getNodeId();

        List<RulDescItemTypeExt> rulDescItemTypes = ruleManager.getDescriptionItemTypesForNodeId(faVersionId, nodeId, false);

        RulDescItemType rulDescItemType = descItem.getDescItemType();

        String data = descItemExt.getData();
        Assert.notNull(data);

        RulDescItemSpec rulDescItemSpec = (descItemExt.getDescItemSpec() != null) ? descItemSpecRepository.findOne(descItemExt.getDescItemSpec().getDescItemSpecId()) : null;

        validateAllowedItemType(rulDescItemTypes, rulDescItemType);
        validateAllItemConstraintsBySpec(nodeId, rulDescItemType, data, rulDescItemSpec, descItem);
        validateAllItemConstraintsByType(nodeId, rulDescItemType, data, descItem);

        // uložení

        Integer position = descItem.getPosition();
        Integer positionUI = descItemExt.getPosition();

        if (createNewVersion) {

            Integer maxPosition = descItemRepository.findMaxPositionByNodeIdAndDescItemTypeIdAndDeleteChangeIsNull(nodeId, rulDescItemType.getId());

            descItem.setDeleteChange(arrFaChange);
            descItemRepository.save(descItem);

            ArrDescItem descItemNew = new ArrDescItem();
            descItemNew.setCreateChange(arrFaChange);
            descItemNew.setDeleteChange(null);
            descItemNew.setDescItemObjectId(descItem.getDescItemObjectId());
            descItemNew.setDescItemType(rulDescItemType);
            descItemNew.setDescItemSpec(rulDescItemSpec);
            descItemNew.setNodeId(descItem.getNodeId());

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

        ArrDescItemExt descItemRet = new ArrDescItemExt();
        BeanUtils.copyProperties(descItem, descItemRet);
        descItemRet.setData(descItemExt.getData());
        return descItemRet;
    }

    /**
     * Vymaže atribut archivního popisu.
     *
     * @param descItemObjectId identifikátor objektu attributu
     * @param arrFaChange      společná změna
     * @return výsledný(smazaný) attribut
     */
    private ArrDescItemExt deleteDescriptionsItemRaw(Integer descItemObjectId, ArrFaChange arrFaChange) {
        Assert.notNull(descItemObjectId);
        Assert.notNull(arrFaChange);

        List<ArrDescItem> descItems = descItemRepository.findByDescItemObjectIdAndDeleteChangeIsNull(descItemObjectId);

        // musí být právě jeden
        if (descItems.size() != 1) {
            throw new IllegalArgumentException("Neplatný počet záznamů (" + descItems.size() + ")");
        }

        ArrDescItem descItem = descItems.get(0);

        descItem.setDeleteChange(arrFaChange);
        descItemRepository.save(descItem);

        Integer position = descItem.getPosition();
        Integer nodeId = descItem.getNodeId();

        // position+1 protože nechci upravovat position u smazané položky
        updatePositionsAfter(position + 1, nodeId, arrFaChange, descItem, -1);

        ArrDescItemExt descItemExt = new ArrDescItemExt();
        BeanUtils.copyProperties(descItem, descItemExt);
        return descItemExt;
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
                throw new IllegalArgumentException("Pro daný uzel již existuje jiná hodnota stejného typu atributu");
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
     * Kontroluje data vůči podmínkám typu atributu.
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
     * Uloží novou hodnotu attributu do tabulky podle jeho typu.
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

            case "FORMATTED_TEXT":
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
                    String[] dataArray = data.split(",");
                    valuePartyRef.setPosition(Integer.valueOf(dataArray[0]));
                    valuePartyRef.setAbstractPartyId(Integer.valueOf(dataArray[1]));
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
                    String[] dataArray = data.split(",");
                    valueRecordRef.setPosition(Integer.valueOf(dataArray[0]));
                    valueRecordRef.setRecordId(Integer.valueOf(dataArray[1]));
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

            case "FORMATTED_TEXT":
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
                    String[] dataArray = data.split(",");
                    valuePartyRef.setPosition(Integer.valueOf(dataArray[0]));
                    valuePartyRef.setAbstractPartyId(Integer.valueOf(dataArray[1]));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Hodnota neodpovídá datovému typu atributu (" + data + ")");
                }
                dataPartyRefRepository.save(valuePartyRef);
                break;

            case "RECORD_REF":
                ArrDataRecordRef valueRecordRef = (ArrDataRecordRef) arrData;
                try {
                    String[] dataArray = data.split(",");
                    valueRecordRef.setPosition(Integer.valueOf(dataArray[0]));
                    valueRecordRef.setRecordId(Integer.valueOf(dataArray[1]));
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
     * @param nodeId        identifikátor uzlu
     * @param arrFaChange   společná změna
     * @param descItem      spjatý objekt attributu
     */
    private void updatePositionsBetween(Integer position, Integer position2, Integer nodeId, ArrFaChange arrFaChange, ArrDescItem descItem) {
        List<ArrDescItem> descItemListForUpdate = descItemRepository
                .findByNodeIdAndDescItemTypeIdAndDeleteChangeIsNullBetweenPositions(position, position2, nodeId, descItem.getDescItemType().getDescItemTypeId());
        updatePositionsRaw(arrFaChange, descItemListForUpdate, -1);
    }

    /**
     * Provede upravení pozic následujících attribut/hodnot po zvolené pozici.
     *
     * @param position      začáteční pozice pro změnu
     * @param nodeId        identifikátor uzlu
     * @param arrFaChange   společná změna
     * @param descItem      spjatý objekt attributu
     * @param diff          rozdíl pozice
     */
    @Transactional
    private void updatePositionsAfter(Integer position, Integer nodeId, ArrFaChange arrFaChange, ArrDescItem descItem, int diff) {
        List<ArrDescItem> descItemListForUpdate = descItemRepository
                .findByNodeIdAndDescItemTypeIdAndDeleteChangeIsNullAfterPosistion(position, nodeId, descItem.getDescItemType().getDescItemTypeId());
        updatePositionsRaw(arrFaChange, descItemListForUpdate, diff);
    }

    /**
     * Provede upravení pozic předchozích attribut/hodnot před zvolenou pozicí.
     *
     * @param position      koncová pozice pro změnu
     * @param nodeId        identifikátor uzlu
     * @param arrFaChange   společná změna
     * @param descItem      spjatý objekt attributu
     */
    private void updatePositionsBefore(Integer position, Integer nodeId, ArrFaChange arrFaChange, ArrDescItem descItem) {
        List<ArrDescItem> descItemListForUpdate = descItemRepository
                .findByNodeIdAndDescItemTypeIdAndDeleteChangeIsNullBeforePosistion(position, nodeId, descItem.getDescItemType().getDescItemTypeId());
        updatePositionsRaw(arrFaChange, descItemListForUpdate, 1);
    }

    /**
     * Upraví pozici s kopií dat u všech položek ze seznamu
     *
     * @param arrFaChange   Změna
     * @param descItemListForUpdate Seznam upravovaných položek
     * @param diff  Číselná změna (posun)
     */
    private void updatePositionsRaw(ArrFaChange arrFaChange, List<ArrDescItem> descItemListForUpdate, int diff) {
        for (ArrDescItem descItemUpdate : descItemListForUpdate) {
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
                valuePartyRefNew.setAbstractPartyId(valuePartyRef.getAbstractPartyId());
                valuePartyRefNew.setDescItem(descItemNew);
                dataPartyRefRepository.save(valuePartyRefNew);
                break;

            case "RECORD_REF":
                ArrDataRecordRef valueRecordRef = (ArrDataRecordRef) arrData;
                ArrDataRecordRef valueRecordRefNew = new ArrDataRecordRef();
                valueRecordRefNew.setDataType(arrData.getDataType());
                valueRecordRefNew.setPosition(valueRecordRef.getPosition());
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

}
