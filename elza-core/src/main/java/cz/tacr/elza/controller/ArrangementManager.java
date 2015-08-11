package cz.tacr.elza.controller;

import java.time.LocalDateTime;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
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
public class ArrangementManager {

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
    private FindingAid createFindingAid(@RequestParam(value = "name") final String name) {
        Assert.hasText(name);

        FindingAid findingAid = new FindingAid();
        findingAid.setCreateDate(LocalDateTime.now());
        findingAid.setName(name);
        findingAidRepository.save(findingAid);

        return findingAid;
    }

    /**
     * Vytvoří novou archivní pomůcku se zadaným názvem. Jako datum založení vyplní aktuální datum a čas.
     *
     * @param name              název archivní pomůcky
     * @param arrangementTypeId id typu výstupu
     * @param ruleSetId         id pravidel podle kterých se vytváří popis
     * @return nová archivní pomůcka
     */
    @RequestMapping(value = "/createFindingAid", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE,
            params = {"name", "arrangementTypeId", "ruleSetId"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
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

    private FaLevel createLevel(final FaChange createChange, final FaLevel parent) {
        FaLevel level = new FaLevel();
        level.setPosition(1);
        level.setCreateChange(createChange);

        if (parent != null) {
            level.setParentNode(parent);
        }

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

        List<FaLevel> levelsToShift = levelRepository.findByParentNodeAndPositionGreaterThanOrderByPositionAsc(level.getParentNode(), level.getPosition());
        for (FaLevel node : levelsToShift) {
            FaLevel newNode = createNewLevelVersion(node, change);
            newNode.setPosition(node.getPosition() + 1);
            levelRepository.save(newNode);
        }

        return createLevel(change, level.getParentNode(), level.getPosition() + 1);
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
        newNode.setParentNode(node.getParentNode());
        newNode.setPosition(node.getPosition());

        return newNode;
    }

    private FaLevel createLastInLevel(FaChange createChange, FaLevel parent) {
        Assert.notNull(createChange);
        Assert.notNull(parent);

        Integer maxPosition = levelRepository.findMaxPositionUnderParent(parent);
        if (maxPosition == null) {
            maxPosition = 0;
        }

        return createLevel(createChange, parent, maxPosition + 1);
    }

    private FaLevel createLevel(final FaChange createChange, final FaLevel parent, final Integer position) {
        Assert.notNull(createChange);
        Assert.notNull(parent);

        FaLevel level = new FaLevel();
        level.setPosition(position);
        level.setCreateChange(createChange);

        if (parent != null) {
            level.setParentNode(parent);
        }

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

    /**
     * Smaže archivní pomůcku se zadaným id. Maže kompletní strukturu se všemi závislostmi.
     *
     * @param findingAidId id archivní pomůcky
     */
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
        levelRepository.findByParentNodeOrderByPositionAsc(rootNode).forEach((node) -> {removeTree(node);});

        levelRepository.delete(rootNode);
    }

    /**
     * Vrátí všechny archivní pomůcky.
     *
     * @return všechny archivní pomůcky
     */
    @RequestMapping(value = "/getFindingAids", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<FindingAid> getFindingAids() {
        return findingAidRepository.findAll();
    }

    /**
     * Aktualizace názvu archivní pomůcky.
     *
     * @param findingAidId id archivní pomůcky
     * @param name         název arhivní pomůcky
     * @return aktualizovaná archivní pomůcka
     */
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

    /**
     * Vrátí všechny typy výstupu.
     *
     * @return všechny typy výstupu
     */
    @RequestMapping(value = "/getArrangementTypes", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ArrangementType> getArrangementTypes() {
        return arrangementTypeRepository.findAll();
    }

    /**
     * Vrátí seznam verzí pro danou archivní pomůcku seřazený od nejnovější k nejstarší.
     *
     * @param findingAidId id archivní pomůcky
     * @return seznam verzí pro danou archivní pomůcku seřazený od nejnovější k nejstarší
     */
    @RequestMapping(value = "/getFindingAidVersions", method = RequestMethod.GET, params = {"findingAidId"}, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FaVersion> getFindingAidVersions(@RequestParam(value = "findingAidId") final Integer findingAidId) {
        Assert.notNull(findingAidId);

        return versionRepository.findVersionsByFindingAidIdOrderByCreateDateAsc(findingAidId);
    }

    /**
     * Vrátí archivní pomůcku.
     *
     * @param findingAidId id archivní pomůcky
     * @return archivní pomůcka
     */
    @RequestMapping(value = "/getFindingAid", method = RequestMethod.GET, params = {"findingAidId"}, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public FindingAid getFindingAid(final Integer findingAidId) {
        Assert.notNull(findingAidId);
        return findingAidRepository.getOne(findingAidId);
    }

    /**
     * Schválí otevřenou verzi archivní pomůcky a otevře novou verzi.
     *
     * @param findingAidId id archivní pomůcky
     * @param arrangementTypeId id typu výstupu nové verze
     * @param ruleSetId         id pravidel podle kterých se vytváří popis v nové verzi
     * @return nová verze archivní pomůcky
     */
    @Transactional
    @RequestMapping(value = "/approveVersion", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE,
    params = {"findingAidId", "arrangementTypeId", "ruleSetId"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public FaVersion approveVersion(final Integer findingAidId, final Integer arrangementTypeId, final Integer ruleSetId) {
        Assert.notNull(findingAidId);
        Assert.notNull(arrangementTypeId);
        Assert.notNull(ruleSetId);

        FindingAid findingAid = findingAidRepository.findOne(findingAidId);
        FaVersion version = versionRepository.findByFindingAidAndLockChangeIsNull(findingAid);

        FaChange change = createChange();
        version.setLockChange(change);

        ArrangementType arrangementType = arrangementTypeRepository.findOne(arrangementTypeId);
        RuleSet ruleSet = ruleSetRepository.findOne(ruleSetId);

        return createVersion(change, findingAid, arrangementType, ruleSet, version.getRootNode());
    }

    /**
     * Přidá záznam do poslední (otevřené) verze archivní položky
     *
     * @param findingAid    archivní pomůcka
     * @return              nový záznam z archivný pomůcky
     */
    @Transactional
    @RequestMapping(value = "/addFaLevel", method = RequestMethod.PUT)
    public FaLevel addFaLevel(@RequestBody FindingAid findingAid) {
        Assert.notNull(findingAid);

        FaVersion lastVersion = versionRepository.findByFindingAidAndLockChangeIsNull(findingAid);
        FaChange change = createChange();
        return createLastInLevel(change, lastVersion.getRootNode());
    }

    // TODO: dopsat testy
    @Transactional
    @RequestMapping(value = "/addFaLevelAfter", method = RequestMethod.PUT)
    public FaLevel addFaLevelAfter(@RequestBody FaLevel faLevel) {
        Assert.notNull(faLevel);

        FaChange change = createChange();
        return createAfterInLevel(change, faLevel);
    }

    // TODO: dopsat testy
    @Transactional
    @RequestMapping(value = "/addFaLevelChild", method = RequestMethod.PUT)
    public FaLevel addFaLevelChild(@RequestBody FaLevel faLevel) {
        Assert.notNull(faLevel);

        FaChange change = createChange();
        return createLastInLevel(change, faLevel);
    }

    // TODO: dopsat testy
    // TODO: otestovat, zda-li to vůbec funguje
    @Transactional
    @RequestMapping(value = "/moveFaLevelUnder", method = RequestMethod.PUT)
    public FaLevel moveFaLevelUnder(@RequestBody FaLevel[] faLevels) {
        Assert.notNull(faLevels);
        Assert.isTrue(faLevels.length == 2);
        for(int i = 0; i < faLevels.length; i++) {
            Assert.notNull(faLevels[i]);
        }

        FaLevel faLevel = faLevels[0];
        FaLevel parent = faLevels[1];
        Assert.state(faLevel != parent, "Nelze vložit sama do sebe");


        FaChange change = createChange();
        shiftNodesUp(faLevel, change);
        FaLevel newLevel = createNewLevelVersion(faLevel, change);

        return addLastInLevel(newLevel, parent);
    }

    private FaLevel addLastInLevel(FaLevel level, FaLevel parent) {
        Assert.notNull(level);
        Assert.notNull(parent);

        Integer maxPosition = levelRepository.findMaxPositionUnderParent(parent);
        if (maxPosition == null) {
            maxPosition = 0;
        }
        level.setPosition(maxPosition + 1);
        level.setParentNode(parent);

        return levelRepository.save(level);
    }

    // TODO: dopsat testy
    // TODO: otestovat, zda-li to vůbec funguje
    @RequestMapping(value = "/moveFaLevelAfter", method = RequestMethod.PUT)
    public FaLevel moveFaLevelAfter(@RequestBody FaLevel[] faLevels) {
        Assert.notNull(faLevels);
        Assert.isTrue(faLevels.length == 2);
        for(int i = 0; i < faLevels.length; i++) {
            Assert.notNull(faLevels[i]);
        }

        FaLevel faLevel = faLevels[0];
        FaLevel predecessor = faLevels[1];
        Assert.state(faLevel != predecessor, "Nelze vložit sama za sebe");

        FaChange change = createChange();
        shiftNodesUp(faLevel, change);
        FaLevel newLevel = createNewLevelVersion(faLevel, change);

        return addAfterInLevel(newLevel, predecessor, change);
    }

    private void shiftNodesDown(FaLevel movedLevel, FaChange change) {
        Assert.notNull(movedLevel);
        Assert.notNull(change);

        List<FaLevel> levelsToShift = levelRepository.findByParentNodeAndPositionGreaterThanOrderByPositionAsc(movedLevel.getParentNode(), movedLevel.getPosition());
        for (FaLevel node : levelsToShift) {
            FaLevel newNode = createNewLevelVersion(node, change);
            newNode.setPosition(node.getPosition() + 1);
            levelRepository.save(newNode);
        }
    }

    private void shiftNodesUp(FaLevel movedLevel, FaChange change) {
        Assert.notNull(movedLevel);
        Assert.notNull(change);

        List<FaLevel> levelsToShift = levelRepository.findByParentNodeAndPositionGreaterThanOrderByPositionAsc(movedLevel.getParentNode(), movedLevel.getPosition());
        for (FaLevel node : levelsToShift) {
            FaLevel newNode = createNewLevelVersion(node, change);
            newNode.setPosition(node.getPosition() - 1);
            levelRepository.save(newNode);
        }
    }

    private FaLevel addAfterInLevel(FaLevel level, FaLevel predecessor, FaChange change) {
        Assert.notNull(level);
        Assert.notNull(predecessor);

        shiftNodesDown(predecessor, change);

        level.setParentNode(predecessor.getParentNode());
        level.setPosition(predecessor.getPosition() + 1);
        return levelRepository.save(level);
    }

    // TODO: dopsat testy
    @Transactional
    @RequestMapping(value = "/deleteFaLevel", method = RequestMethod.DELETE)
    public FaLevel deleteFaLevel(@RequestBody FaLevel level) {
        Assert.notNull(level);

        level.setDeleteChange(createChange());

        return levelRepository.save(level);
    }

    // TODO: přepsat, dopsat testy
    @RequestMapping(value = "/getOneFaLevelByNodeIdAndDeleteChangeIsNull/{nodeId}", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public FaLevel getOneFaLevelByNodeIdAndDeleteChangeIsNull(@RequestParam("nodeId")Integer nodeId) {
        Assert.notNull(nodeId);
        return levelRepository.findTopByNodeIdAndDeleteChangeIsNull(nodeId);
    }

    @RequestMapping(value = "/getOneFaVersionByFindingAid", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public FaVersion getOneFaVersionByFindingAid(@RequestParam(value = "findingAidId") Integer findingAidId) {
        Assert.notNull(findingAidId);
        List<FaVersion> resultList = versionRepository.findByFindingAidIdAndLockChangeIsNull(findingAidId);
        if (resultList.size() != 1) {
            throw new RuntimeException("Nenalezen jeden záznam. Nalezeno " + resultList.size());
        }
        return resultList.get(0);
    }

    @RequestMapping(value = "/getFaVersionById", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public FaVersion getFaVersionById(@RequestParam("versionId") final Integer versionId) {
        Assert.notNull(versionId);
        return versionRepository.getOne(versionId);
    }

    @RequestMapping(value = "/findFaLevelByParentNodeOrderByPositionAsc", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FaLevel> findFaLevelByParentNodeOrderByPositionAsc(@RequestParam(value = "faLevelId") Integer faLevelId,
            @RequestParam(value = "faChangeId")  Integer faChangeId) {
        Assert.notNull(faLevelId);

        if (faChangeId == null) {
            return levelRepository.findByParentNodeOrderByPositionAsc(faLevelId);
        }
        FaChange faChange = faChangeRepository.getOne(faChangeId);
        return levelRepository.findByParentNodeOrderByPositionAsc(faLevelId, faChange);
    }

    // TODO: přepsat, dopsat testy
    @RequestMapping(value = "/getOneFaLevelByNodeIdOrderByPositionAsc/{nodeId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FaLevel> getOneFaLevelByNodeIdOrderByPositionAsc(@RequestParam("nodeId")Integer nodeId) {
        Assert.notNull(nodeId);
        return levelRepository.findByNodeIdAndDeleteChangeIsNullOrderByPositionAsc(nodeId);
    }

    // TODO: přepsat, dopsat testy
    @RequestMapping(value = "/findFaLevelByParentNodeInOrderByPositionAsc", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FaLevel> findFaLevelByParentNodeInOrderByPositionAsc(@RequestBody List<FaLevel> faLevelList) {
        Assert.notNull(faLevelList);
        return levelRepository.findByParentNodeInDeleteChangeIsNullOrderByPositionAsc(faLevelList);
    }

    // TODO: přepsat, dopsat testy
    @RequestMapping(value = "/findFaLevelsByNodeIdAndDeleteChangeIsNullOrderByPositionAsc{nodeId}", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FaLevel> findFaLevelsByNodeIdAndDeleteChangeIsNullOrderByPositionAsc(@RequestParam("nodeId")Integer nodeId) {
        Assert.notNull(nodeId);
        return levelRepository.findByNodeIdAndDeleteChangeIsNullOrderByPositionAsc(nodeId);
    }
}
