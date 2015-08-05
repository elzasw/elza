package cz.tacr.elza.controller;

import java.time.LocalDateTime;
import java.util.List;

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
import cz.tacr.elza.domain.FindingAid;
import cz.tacr.elza.domain.RuleSet;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.RuleSetRepository;

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
    private LocalDateTime initDate = null;

    @Autowired
    private ArrangementManager arrangementManager;
    @Autowired
    private ArrangementTypeRepository arrangementTypeRepository;
    @Autowired
    private RuleSetRepository ruleSetRepository;

    @Before
    public void setUp() {
        // nastavi default port pro REST-assured
        RestAssured.port = port;

        // nastavi default URI pro REST-assured. Nejcasteni localhost
        RestAssured.baseURI = RestAssured.DEFAULT_URI;

        initDate = LocalDateTime.now();
    }

    @After
    public void setDown() {
        List<FindingAid> findingAids = arrangementManager.getFindingAids();
        for (FindingAid findingAid : findingAids) {
            if ((findingAid.getName().equals(TEST_NAME) || findingAid.getName().equals(TEST_UPDATE_NAME))
                    && isAfterOrEqual(findingAid.getCreateDate(), initDate)) {
                arrangementManager.deleteFindingAid(findingAid.getFindigAidId());
            }
        }

        List<ArrangementType> arrangementTypes = arrangementManager.getArrangementTypes();
        for (ArrangementType arrangementType : arrangementTypes) {
            if (arrangementType.getName().equals(TEST_NAME) || arrangementType.getName().equals(TEST_UPDATE_NAME)) {
                arrangementTypeRepository.delete(arrangementType);
            }
        }

        List<RuleSet> ruleSets = ruleSetRepository.findAll();
        for (RuleSet ruleSet : ruleSets) {
            if (ruleSet.getName().equals(TEST_NAME)|| ruleSet.getName().equals(TEST_UPDATE_NAME)) {
                ruleSetRepository.delete(ruleSet);
            }
        }
    }

    private boolean isAfterOrEqual(final LocalDateTime testDate, final LocalDateTime initDate) {
        if (testDate == null) {
            return false;
        }
        if (initDate == null) {
            return false;
        }
        return testDate.isAfter(initDate) || testDate.isEqual(initDate);
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
}
