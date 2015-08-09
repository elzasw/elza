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
    private ChangeRepository changeRepository;
    @Autowired
    private LevelRepository levelRepository;
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

    protected ArrangementType createArrangementType() {
        ArrangementType arrangementType = new ArrangementType();
        arrangementType.setName(TEST_NAME);
        arrangementType.setCode(TEST_CODE);
        arrangementTypeRepository.save(arrangementType);
        return arrangementType;
    }

    protected RuleSet createRuleSet() {
        RuleSet ruleSet = new RuleSet();
        ruleSet.setName(TEST_NAME);
        ruleSet.setCode(TEST_CODE);
        ruleSetRepository.save(ruleSet);
        return ruleSet;
    }

    protected FindingAid createFindingAid(final String name) {
        RuleSet ruleSet = createRuleSet();
        ArrangementType arrangementType = createArrangementType();

        return arrangementManager.createFindingAid(name, arrangementType.getId(), ruleSet.getId());
    }

    protected FaVersion createFindingAidVersion(final FindingAid findingAid) {
        RuleSet ruleSet = ruleSetRepository.findAll().iterator().next();
        ArrangementType arrangementType = arrangementTypeRepository.findAll().iterator().next();
        FaChange change = new FaChange();
        change.setChangeDate(LocalDateTime.now());
        changeRepository.save(change);
        FaLevel root = levelRepository.findAll().iterator().next();

        FaVersion version = new FaVersion();
        version.setArrangementType(arrangementType);
        version.setCreateChange(change);
        version.setFindingAid(findingAid);
        version.setRootNode(root);
        version.setRuleSet(ruleSet);

        return versionRepository.save(version);
    }
}
