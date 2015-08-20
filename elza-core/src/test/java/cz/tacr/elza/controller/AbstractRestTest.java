package cz.tacr.elza.controller;

import java.time.LocalDateTime;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.jayway.restassured.RestAssured;

import cz.tacr.elza.ElzaCore;
import cz.tacr.elza.domain.ArrFaChange;
import cz.tacr.elza.domain.ArrFaVersion;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrArrangementType;
import cz.tacr.elza.domain.ArrFaLevel;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.FindingAidRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.VersionRepository;

/**
 * Abstraktní předek pro testy. Nastavuje REST prostředí.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 31. 7. 2015
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ElzaCore.class)
@IntegrationTest("server.port:0") // zvoli volny port, lze spustit i s aktivni Elzou
@WebAppConfiguration
public abstract class AbstractRestTest {

    protected static final String ARRANGEMENT_MANAGER_URL = "/api/arrangementManager";
    protected static final String RULE_SET_MANAGER_URL = "/api/ruleSetManager";

    protected static final String TEST_CODE = "Tcode";
    protected static final String TEST_NAME = "Test name";
    protected static final String TEST_UPDATE_NAME = "Update name";

    protected static final String CONTENT_TYPE_HEADER = "content-type";
    protected static final String JSON_CONTENT_TYPE = "application/json";

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ArrangementManager arrangementManager;
    @Autowired
    private ArrangementTypeRepository arrangementTypeRepository;
    @Autowired
    private RuleSetRepository ruleSetRepository;
    @Autowired
    private VersionRepository versionRepository;
    @Autowired
    protected ChangeRepository changeRepository;
    @Autowired
    protected LevelRepository levelRepository;
    @Autowired
    private FindingAidRepository findingAidRepository;

    @Before
    public void setUp() {
        // nastavi default port pro REST-assured
        RestAssured.port = port;

        // nastavi default URI pro REST-assured. Nejcasteni localhost
        RestAssured.baseURI = RestAssured.DEFAULT_URI;
    }

    @After
    public void setDown() {
        versionRepository.deleteAll();
        arrangementTypeRepository.deleteAll();
        ruleSetRepository.deleteAll();
        findingAidRepository.deleteAll();
        levelRepository.deleteAll();
        changeRepository.deleteAll();
    }

    protected ArrArrangementType createArrangementType() {
        ArrArrangementType arrangementType = new ArrArrangementType();
        arrangementType.setName(TEST_NAME);
        arrangementType.setCode(TEST_CODE);
        arrangementTypeRepository.save(arrangementType);
        return arrangementType;
    }

    protected RulRuleSet createRuleSet() {
        RulRuleSet ruleSet = new RulRuleSet();
        ruleSet.setName(TEST_NAME);
        ruleSet.setCode(TEST_CODE);
        ruleSetRepository.save(ruleSet);
        return ruleSet;
    }

    protected ArrFindingAid createFindingAid(final String name) {
        RulRuleSet ruleSet = createRuleSet();
        ArrArrangementType arrangementType = createArrangementType();

        return arrangementManager.createFindingAid(name, arrangementType.getId(), ruleSet.getId());
    }

    protected ArrFaVersion createFindingAidVersion(final ArrFindingAid findingAid, boolean isLock) {
        ArrFaLevel root = levelRepository.findAll().iterator().next();
        return createFindingAidVersion(findingAid, root, isLock);
    }

    protected ArrFaChange createFaChange(final LocalDateTime changeDate) {
        ArrFaChange resultChange = new ArrFaChange();
        resultChange.setChangeDate(changeDate);
        changeRepository.save(resultChange);
        return resultChange;
    }

    protected ArrFaVersion createFindingAidVersion(final ArrFindingAid findingAid, final ArrFaLevel root, boolean isLock) {
        RulRuleSet ruleSet = ruleSetRepository.findAll().iterator().next();
        ArrArrangementType arrangementType = arrangementTypeRepository.findAll().iterator().next();
        ArrFaChange createChange = createFaChange(LocalDateTime.now());

        ArrFaChange lockChange = null;
        if (isLock) {
            lockChange = createFaChange(LocalDateTime.now());
        }

        ArrFaVersion version = new ArrFaVersion();
        version.setArrangementType(arrangementType);
        version.setCreateChange(createChange);
        version.setLockChange(lockChange);
        version.setFindingAid(findingAid);
        version.setRootNode(root);
        version.setRuleSet(ruleSet);

        return versionRepository.save(version);
    }

    protected ArrFaLevel createLevel(final Integer position, final ArrFaLevel parent, final ArrFaChange change) {
        ArrFaLevel level = new ArrFaLevel();
        level.setPosition(position);
        if (parent != null) {
            level.setParentNodeId(parent.getNodeId());
        }
        level.setCreateChange(change);
        Integer maxNodeId = levelRepository.findMaxNodeId();
        if (maxNodeId == null) {
            maxNodeId = 0;
        }
        level.setNodeId(maxNodeId + 1);

        return levelRepository.save(level);
    }
}
