package cz.tacr.elza.controller;

import java.time.LocalDateTime;

import javax.transaction.Transactional;

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
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.ArrArrangementType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFaLevel;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DataStringRepository;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;
import cz.tacr.elza.repository.FaViewRepository;
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
    @Autowired
    protected DescItemRepository descItemRepository;
    @Autowired
    private DescItemTypeRepository descItemTypeRepository;
    @Autowired
    private DescItemSpecRepository descItemSpecRepository;
    @Autowired
    private DataTypeRepository dataTypeRepository;
    @Autowired
    private FaViewRepository faViewRepository;
    @Autowired
    private DataStringRepository arrDataStringRepository;
    @Autowired
    private DataRepository arrDataRepository;

    @Before
    public void setUp() {
        // nastavi default port pro REST-assured
        RestAssured.port = port;

        // nastavi default URI pro REST-assured. Nejcasteni localhost
        RestAssured.baseURI = RestAssured.DEFAULT_URI;
    }

    @After
    public void setDown() {
        faViewRepository.deleteAll();
        versionRepository.deleteAll();
        arrangementTypeRepository.deleteAll();
        ruleSetRepository.deleteAll();
        findingAidRepository.deleteAll();
        levelRepository.deleteAll();
        arrDataRepository.deleteAll();
        arrDataStringRepository.deleteAll();
        descItemRepository.deleteAll();
        descItemSpecRepository.deleteAll();
        descItemTypeRepository.deleteAll();
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

    @Transactional
    protected ArrDescItem createAttributs(final Integer nodeId, final Integer position,
                                          final ArrFaChange change, final int index) {
        RulDescItemType descItemType = createDescItemType(index);
        RulDescItemSpec rulDescItemSpec = createDescItemSpec(descItemType, index);

        ArrDescItem item = new ArrDescItem();
        item.setNodeId(nodeId);
        item.setPosition(position);
        item.setCreateChange(change);
        item.setDescItemObjectId(1);
        item.setDescItemType(descItemType);
        item.setDescItemSpec(rulDescItemSpec);
        descItemRepository.save(item);
        createData(item, index);
        return item;
    }

    private ArrData createData(final ArrDescItem item, final int index) {
        ArrDataString dataStr = new ArrDataString();
        dataStr.setDescItem(item);
        RulDataType dataType = dataTypeRepository.getOne(2);
        dataStr.setDataType(dataType);
        dataStr.setValue("str data " + index);
        arrDataStringRepository.save(dataStr);
        return dataStr;
    }

    private RulDescItemType createDescItemType(final int index) {
        RulDescItemType itemType = new RulDescItemType();
        RulDataType dataType = createDataType(index);
        itemType.setSys(true);
        itemType.setDataType(dataType);
        itemType.setCode("DI" + index);
        itemType.setName("Desc Item " + index);
        itemType.setShortcut("DItem " + index);
        itemType.setDescription("popis");
        itemType.setCanBeOrdered(false);
        itemType.setIsValueUnique(false);
        itemType.setUseSpecification(false);
        itemType.setViewOrder(index);
        descItemTypeRepository.save(itemType);
        return itemType;
    }

    private RulDescItemSpec createDescItemSpec(final RulDescItemType itemType, final int index) {
        RulDescItemSpec rulDescItemSpec = new RulDescItemSpec();
        rulDescItemSpec.setCode("IS" + index);
        rulDescItemSpec.setDescItemType(itemType);
        rulDescItemSpec.setName("Item Spec " + index);
        rulDescItemSpec.setShortcut("ISpec " + index);
        rulDescItemSpec.setDescription("popis");
        rulDescItemSpec.setViewOrder(index);
        descItemSpecRepository.save(rulDescItemSpec);
        return rulDescItemSpec;
    }

    private RulDataType createDataType(final int index) {
        RulDataType dataType = new RulDataType();
        dataType.setCode("DT" + index);
        dataType.setName("Data type " + index);
        dataType.setRegexpUse(false);
        dataType.setTextLenghtLimitUse((index > 1) ? true : false);
        dataType.setDescription("popis");
        dataType.setStorageTable("arr_data_integer");
        dataTypeRepository.save(dataType);
        return dataType;
    }
}
