package cz.tacr.elza.controller;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.domain.ArrangementType;
import cz.tacr.elza.domain.FindingAid;
import cz.tacr.elza.domain.Level;
import cz.tacr.elza.domain.RuleSet;
import cz.tacr.elza.domain.Version;
import cz.tacr.elza.domain.VersionLevel;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.FindingAidRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.VersionLevelRepository;
import cz.tacr.elza.repository.VersionRepository;

/**
 * API pro pořádání.
 *
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@RestController
@RequestMapping("/arrangementManager")
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
    private VersionLevelRepository versionLevelRepository;

    /**
     * Vytvoří novou archivní pomůcku se zadaným názvem. Jako datum založení vyplní aktuální datum a čas.
     *
     * @param name název archivní pomůcky
     *
     * @return nová archivní pomůcka
     */
    private FindingAid createFindingAid(@RequestParam(value="name") final String name) {
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
     * @param name název archivní pomůcky
     * @param arrangementTypeId id typu výstupu
     * @param ruleSetId id pravidel podle kterých se vytváří popis
     *
     * @return nová archivní pomůcka
     */
    @RequestMapping(value = "/createFindingAid", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE,
            params = {"name", "arrangementTypeId", "ruleSetId"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public FindingAid createFindingAid(@RequestParam(value="name") final String name,
            @RequestParam(value="arrangementTypeId") final Integer arrangementTypeId,
            @RequestParam(value="ruleSetId") final Integer ruleSetId) {
        Assert.hasText(name);
        Assert.notNull(arrangementTypeId);
        Assert.notNull(ruleSetId);

        FindingAid findingAid = createFindingAid(name);

        ArrangementType arrangementType = arrangementTypeRepository.getOne(arrangementTypeId);
        RuleSet ruleSet = ruleSetRepository.getOne(ruleSetId);

        Version version = createVersion(findingAid, arrangementType, ruleSet);
        Level level = createLevel();
        createVersionLevel(version, level);

        return findingAid;
    }

    private VersionLevel createVersionLevel(final Version version, final Level level) {
        VersionLevel versionLevel = new VersionLevel();
        versionLevel.setLevel(level);
        versionLevel.setVersion(version);
        return versionLevelRepository.save(versionLevel);
    }

    private Level createLevel() {
        Level level = new Level();
        level.setPosition(1);

        Integer maxTreeId = levelRepository.findMaxTreeId();
        if (maxTreeId == null) {
            maxTreeId = 0;
        }
        level.setTreeId(maxTreeId + 1);
        return levelRepository.save(level);
    }

    private Version createVersion(final FindingAid findingAid, final ArrangementType arrangementType, final RuleSet ruleSet) {
        Version version = new Version();
        version.setArrangementType(arrangementType);
        version.setCreateDate(findingAid.getCreateDate());
        version.setFindingAid(findingAid);
        version.setRuleSet(ruleSet);
        return versionRepository.save(version);
    }

    /**
     * Smaže archivní pomůcku se zadaným id. Maže kompletní strukturu se všemi závislostmi.
     *
     * @param idArchivniPomucka id archivní pomůcky
     */
    @RequestMapping(value = "/deleteFindingAid", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, params = {"findingAidId"})
    public void deleteFindingAid(@RequestParam(value="findingAidId") final Integer findingAidId) {
        Assert.notNull(findingAidId);

        List<Version> versions = versionRepository.findByFindingAidId(findingAidId);

        List<VersionLevel> versionLevels = versionLevelRepository.findByVersion(versions);

        List<Integer> levelIds = new LinkedList<Integer>();
        for (VersionLevel versionLevel : versionLevels) {
            levelIds.add(versionLevel.getLevelId());
        }
        List<Level> levels = levelRepository.findByLevelId(levelIds);

        versionLevelRepository.deleteInBatch(versionLevels);
        versionRepository.deleteInBatch(versions);
        levelRepository.deleteInBatch(levels);

        findingAidRepository.delete(findingAidId);
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
     * @param name název arhivní pomůcky
     *
     * @return aktualizovaná archivní pomůcka
     */
    @RequestMapping(value = "/updateFindingAid", method = RequestMethod.GET, consumes = MediaType.APPLICATION_JSON_VALUE, params = {"findingAidId", "name"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public FindingAid updateFindingAid(@RequestParam(value="findingAidId") final Integer findingAidId, @RequestParam(value="name") final String name) {
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
}
