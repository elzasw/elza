package cz.tacr.elza.controller;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.EncoderConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import cz.tacr.elza.ElzaCore;
import cz.tacr.elza.domain.ArrArrangementType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFaChange;
import cz.tacr.elza.domain.ArrFaLevel;
import cz.tacr.elza.domain.ArrFaLevelExt;
import cz.tacr.elza.domain.ArrFaVersion;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ParAbstractParty;
import cz.tacr.elza.domain.ParPartySubtype;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemConstraint;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulFaView;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.repository.AbstractPartyRepository;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DataStringRepository;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.repository.DescItemConstraintRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;
import cz.tacr.elza.repository.ExternalSourceRepository;
import cz.tacr.elza.repository.FaViewRepository;
import cz.tacr.elza.repository.FindingAidRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.PartySubtypeRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.VariantRecordRepository;
import cz.tacr.elza.repository.VersionRepository;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static com.jayway.restassured.RestAssured.given;

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


    private static final RestAssuredConfig UTF8_ENCODER_CONFIG = RestAssuredConfig.newConfig().encoderConfig(EncoderConfig.encoderConfig().defaultContentCharset("UTF-8"));

    private static final Logger logger = LoggerFactory.getLogger(ArrangementManagerTest.class);

    protected static final String ARRANGEMENT_MANAGER_URL = "/api/arrangementManager";
    protected static final String RULE_MANAGER_URL = "/api/ruleSetManager";
    protected static final String REGISTRY_MANAGER_URL = "/api/registryManager";
    protected static final String PARTY_MANAGER_URL = "/api/partyManager";

    protected static final String TEST_CODE = "Tcode";
    protected static final String TEST_NAME = "Test name";
    protected static final String TEST_UPDATE_NAME = "Update name";

    protected static final String DATA_TYP_STRING = "STR";
    protected static final String DATA_TYP_RECORD = "REC";

    protected static final String CONTENT_TYPE_HEADER = "content-type";
    protected static final String JSON_CONTENT_TYPE = "application/json";
    private static final Header JSON_CT_HEADER = new Header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE);

    // REGISTRY MANAGER CONSTANTS
    protected static final String GET_REGISTER_TYPES_URL = REGISTRY_MANAGER_URL + "/getRegisterTypes";
    protected static final String CREATE_RECORD_URL = REGISTRY_MANAGER_URL + "/createRecord";
    protected static final String CREATE_VARIANT_RECORD_URL = REGISTRY_MANAGER_URL + "/createVariantRecord";
    protected static final String FIND_RECORD_URL = REGISTRY_MANAGER_URL + "/findRecord";
    protected static final String FIND_RECORD_COUNT_URL = REGISTRY_MANAGER_URL + "/findRecordCount";
    protected static final String UPDATE_RECORD_URL = REGISTRY_MANAGER_URL + "/updateRecord";
    protected static final String DELETE_RECORD_URL = REGISTRY_MANAGER_URL + "/deleteRecord";
    protected static final String UPDATE_VARIANT_RECORD_URL = REGISTRY_MANAGER_URL + "/updateVariantRecord";
    protected static final String DELETE_VARIANT_RECORD_URL = REGISTRY_MANAGER_URL + "/deleteVariantRecord";
    protected static final String GET_RECORD_URL = REGISTRY_MANAGER_URL + "/getRecord";

    protected static final String RECORD_ID_ATT = "recordId";
    protected static final String VARIANT_RECORD_ID_ATT = "variantRecordId";
    protected static final String SEARCH_ATT = "search";
    protected static final String FROM_ATT = "from";
    protected static final String COUNT_ATT = "count";
    protected static final String REGISTER_TYPE_ID_ATT = "registerTypeId";

    // END - REGISTRY MANAGER CONSTANTS

    // PARTY MANAGER CONSTANTS
    protected static final String GET_PARTY_TYPES = PARTY_MANAGER_URL + "/getPartyTypes";
    protected static final String INSERT_ABSTRACT_PARTY = PARTY_MANAGER_URL + "/insertAbstractParty";
    protected static final String FIND_ABSTRACT_PARTY = PARTY_MANAGER_URL + "/findAbstractParty";
    protected static final String FIND_ABSTRACT_PARTY_COUNT = PARTY_MANAGER_URL + "/findAbstractPartyCount";
    protected static final String UPDATE_ABSTRACT_PARTY = PARTY_MANAGER_URL + "/updateAbstractParty";
    protected static final String DELETE_ABSTRACT_PARTY = PARTY_MANAGER_URL + "/deleteAbstractParty";
    protected static final String GET_ABSTRACT_PARTY = PARTY_MANAGER_URL + "/getAbstractParty";

    protected static final String PARTY_TYPE_ID_ATT = "partyTypeId";
    protected static final String ORIGINATOR_ATT = "originator";
    protected static final String ABSTRACT_PARTY_ID_ATT = "abstractPartyId";

    // END - PARTY MANAGER CONSTANTS

    // ARRANGEMENT MANAGER CONSTANTS
    protected static final String CREATE_FA_URL = ARRANGEMENT_MANAGER_URL + "/createFindingAid";
    protected static final String UPDATE_FA_URL = ARRANGEMENT_MANAGER_URL + "/updateFindingAid";
    protected static final String DELETE_FA_URL = ARRANGEMENT_MANAGER_URL + "/deleteFindingAid";
    protected static final String GET_FA_URL = ARRANGEMENT_MANAGER_URL + "/getFindingAids";
    protected static final String GET_FA_ONE_URL = ARRANGEMENT_MANAGER_URL + "/getFindingAid";
    protected static final String GET_ARRANGEMENT_TYPES_URL = RULE_MANAGER_URL + "/getArrangementTypes";
    protected static final String GET_FINDING_AID_VERSIONS_URL = ARRANGEMENT_MANAGER_URL + "/getFindingAidVersions";
    protected static final String APPROVE_VERSION_URL = ARRANGEMENT_MANAGER_URL + "/approveVersion";
    protected static final String GET_VERSION_ID_URL = ARRANGEMENT_MANAGER_URL + "/getVersion";
    protected static final String GET_OPEN_VERSION_BY_FA_ID_URL = ARRANGEMENT_MANAGER_URL + "/getOpenVersionByFindingAidId";
    protected static final String FIND_SUB_LEVELS_EXT_URL = ARRANGEMENT_MANAGER_URL + "/findSubLevelsExt";
    protected static final String FIND_SUB_LEVELS_URL = ARRANGEMENT_MANAGER_URL + "/findSubLevels";

    protected static final String ADD_LEVEL_URL = ARRANGEMENT_MANAGER_URL + "/addLevel";
    protected static final String ADD_LEVEL_BEFORE_URL = ARRANGEMENT_MANAGER_URL + "/addLevelBefore";
    protected static final String ADD_LEVEL_AFTER_URL = ARRANGEMENT_MANAGER_URL + "/addLevelAfter";
    protected static final String ADD_LEVEL_CHILD_URL = ARRANGEMENT_MANAGER_URL + "/addLevelChild";
    protected static final String MOVE_LEVEL_BEFORE_URL = ARRANGEMENT_MANAGER_URL + "/moveLevelBefore";
    protected static final String MOVE_LEVEL_UNDER_URL = ARRANGEMENT_MANAGER_URL + "/moveLevelUnder";
    protected static final String MOVE_LEVEL_AFTER_URL = ARRANGEMENT_MANAGER_URL + "/moveLevelAfter";
    protected static final String DELETE_LEVEL_URL = ARRANGEMENT_MANAGER_URL + "/deleteLevel";
    protected static final String FIND_LEVEL_BY_NODE_ID_URL = ARRANGEMENT_MANAGER_URL + "/findLevelByNodeId";
    protected static final String GET_LEVEL_URL = ARRANGEMENT_MANAGER_URL + "/getLevel";

    protected static final String FA_NAME_ATT = "name";
    protected static final String FA_ID_ATT = "findingAidId";
    protected static final String ARRANGEMENT_TYPE_ID_ATT = "arrangementTypeId";
    protected static final String RULE_SET_ID_ATT = "ruleSetId";
    protected static final String NODE_ID_ATT = "nodeId";
    protected static final String PARENT_NODE_ID_ATT = "parentNodeId";
    protected static final String FOLLOWER_NODE_ID_ATT = "followerNodeId";
    protected static final String PREDECESSOR_NODE_ID_ATT = "predecessorNodeId";
    protected static final String VERSION_ID_ATT = "versionId";

    protected static final Integer DATA_TYPE_INTEGER = 1;
    protected static final Integer DATA_TYPE_STRING = 2;
    protected static final Integer DATA_TYPE_TEXT = 3;
    protected static final Integer DATA_TYPE_DATACE = 4;
    protected static final Integer DATA_TYPE_REF = 5;
    // END ARRANGEMENT MANAGER CONSTANTS
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
    private DescItemConstraintRepository descItemConstraintRepository;
    @Autowired
    private DataTypeRepository dataTypeRepository;
    @Autowired
    private FaViewRepository faViewRepository;
    @Autowired
    private DataStringRepository arrDataStringRepository;
    @Autowired
    private DataRepository arrDataRepository;
    @Autowired
    private RegisterTypeRepository registerTypeRepository;
    @Autowired
    private ExternalSourceRepository externalSourceRepository;
    @Autowired
    protected AbstractPartyRepository abstractPartyRepository;
    @Autowired
    private VariantRecordRepository variantRecordRepository;
    @Autowired
    private RegRecordRepository recordRepository;
    @Autowired
    private PartySubtypeRepository partySubtypeRepository;
    @Autowired
    private PartyTypeRepository partyTypeRepository;
    @Autowired
    private DataRecordRefRepository dataRecordRefRepository;
    @Autowired
    private NodeRepository nodeRepository;

    @Before
    public void setUp() {
        // nastavi default port pro REST-assured
        RestAssured.port = port;

        // nastavi default URI pro REST-assured. Nejcasteni localhost
        RestAssured.baseURI = RestAssured.DEFAULT_URI;

        // potřebné delete, jen data, ne číselníky
        abstractPartyRepository.deleteAll();
        variantRecordRepository.deleteAll();
        arrDataRepository.deleteAll();
        recordRepository.deleteAll();

        descItemConstraintRepository.deleteAll();
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
        nodeRepository.deleteAll();
        changeRepository.deleteAll();
    }

    @After
    public void setDown() {
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

        return arrangementManager.createFindingAid(name, arrangementType.getArrangementTypeId(), ruleSet.getRuleSetId());
    }

    protected ArrFaVersion createFindingAidVersion(final ArrFindingAid findingAid, boolean isLock, ArrFaChange createChange) {
        ArrFaLevel root = levelRepository.findAll().iterator().next();
        return createFindingAidVersion(findingAid, root, isLock, createChange);
    }

    protected ArrFaChange createFaChange(final LocalDateTime changeDate) {
        ArrFaChange resultChange = new ArrFaChange();
        resultChange.setChangeDate(changeDate);
        changeRepository.save(resultChange);
        return resultChange;
    }

    protected ArrFaVersion createFindingAidVersion(final ArrFindingAid findingAid, final ArrFaLevel root, boolean isLock, ArrFaChange createChange) {
        RulRuleSet ruleSet = ruleSetRepository.findAll().iterator().next();
        ArrArrangementType arrangementType = arrangementTypeRepository.findAll().iterator().next();

        return createFindingAidVersion(findingAid, root, ruleSet, arrangementType, isLock, createChange);
    }

    protected ArrFaVersion createFindingAidVersion(final ArrFindingAid findingAid, final ArrFaLevel root,
                                                   RulRuleSet ruleSet, ArrArrangementType arrangementType, boolean isLock, ArrFaChange createChange) {

        if (createChange == null) {
            createChange = createFaChange(LocalDateTime.now());
        }

        ArrFaChange lockChange = null;
        if (isLock) {
            lockChange = createFaChange(LocalDateTime.now());
        }

        ArrFaVersion version = new ArrFaVersion();
        version.setArrangementType(arrangementType);
        version.setCreateChange(createChange);
        version.setLockChange(lockChange);
        version.setFindingAid(findingAid);
        version.setRootFaLevel(root);
        version.setRuleSet(ruleSet);

        return versionRepository.save(version);
    }

    protected ArrFaLevel createLevel(final Integer position, final ArrFaLevel parent, final ArrFaChange change) {
        ArrFaLevel level = new ArrFaLevel();
        level.setPosition(position);
        if (parent != null) {
            level.setParentNode(parent.getNode());
        }
        level.setCreateChange(change);
        level.setNode(createNode());
        return levelRepository.save(level);
    }

    protected ArrNode createNode() {
        ArrNode node = new ArrNode();
        node.setLastUpdate(LocalDateTime.now());
        return nodeRepository.save(node);
    }

    @Transactional
    protected ArrDescItem createAttributs(final ArrNode node, final Integer position,
                                          final ArrFaChange change, final int index, final String typ) {
        RulDescItemType descItemType = createDescItemType(index);
        RulDescItemSpec rulDescItemSpec = createDescItemSpec(descItemType, index);

        ArrDescItem item = new ArrDescItem();
        item.setNode(node);
        item.setPosition(position);
        item.setCreateChange(change);
        item.setDescItemObjectId(1);
        item.setDescItemType(descItemType);
        item.setDescItemSpec(rulDescItemSpec);
        descItemRepository.save(item);
        createData(item, index, typ);
        return item;
    }

    @Transactional
    protected RulDescItemConstraint createConstrain(final int index) {
        RulDescItemType descItemType = createDescItemType(index);
        RulDescItemSpec rulDescItemSpec = createDescItemSpec(descItemType, index);
        RulDescItemConstraint itemConstraint = createDescItemConstrain(descItemType, rulDescItemSpec, index);
        return itemConstraint;
    }

    private ArrData createData(final ArrDescItem item, final int index, final String typ) {
        if (typ == null || DATA_TYP_STRING.equalsIgnoreCase(typ)) {
            ArrDataString dataStr = new ArrDataString();
            dataStr.setDescItem(item);
            RulDataType dataType = dataTypeRepository.getOne(2);
            dataStr.setDataType(dataType);
            dataStr.setValue(TEST_NAME + index);
            arrDataStringRepository.save(dataStr);
            return dataStr;
        } else if (DATA_TYP_RECORD.equalsIgnoreCase(typ)) {
            ArrDataRecordRef dataStr = new ArrDataRecordRef();
            RegRecord record = createRecord();
            dataStr.setDescItem(item);
            RulDataType dataType = dataTypeRepository.getOne(2);
            dataStr.setDataType(dataType);
            dataStr.setRecordId(record.getRecordId());
            dataRecordRefRepository.save(dataStr);
            return dataStr;
        }
        return null;
    }

    protected RulDescItemType createDescItemType(final int index) {
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

    private RulDescItemConstraint createDescItemConstrain(final RulDescItemType itemType,
                                                          RulDescItemSpec rulDescItemSpec, final int index) {
        RulDescItemConstraint itemConstrain = new RulDescItemConstraint();
        itemConstrain.setDescItemSpec(rulDescItemSpec);
        itemConstrain.setDescItemType(itemType);
        descItemConstraintRepository.save(itemConstrain);
        return itemConstrain;
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

    protected RulDescItemType createDescItemType(RulDataType rulDataType, Boolean sys, String code, String name, String shortcut, String description, Boolean isValueUnique, Boolean canBeOrdered, Boolean useSpecification, Integer viewOrder) {
        RulDescItemType dataTypeItem = new RulDescItemType();
        dataTypeItem.setDataType(rulDataType);
        dataTypeItem.setSys(sys);
        dataTypeItem.setCode(code);
        dataTypeItem.setName(name);
        dataTypeItem.setShortcut(shortcut);
        dataTypeItem.setDescription(description);
        dataTypeItem.setIsValueUnique(isValueUnique);
        dataTypeItem.setCanBeOrdered(canBeOrdered);
        dataTypeItem.setUseSpecification(useSpecification);
        dataTypeItem.setViewOrder(viewOrder);
        descItemTypeRepository.save(dataTypeItem);
        return dataTypeItem;
    }

    protected RulDataType createDataType(String code, String name, String description, Boolean regexUse, Boolean textLenghtLimitUse, String storageTable) {
        RulDataType dataType = new RulDataType();
        dataType.setCode(code);
        dataType.setName(name);
        dataType.setDescription(description);
        dataType.setRegexpUse(regexUse);
        dataType.setTextLenghtLimitUse(textLenghtLimitUse);
        dataType.setStorageTable(storageTable);
        dataTypeRepository.save(dataType);
        return dataType;
    }

    protected RulDataType getDataType(Integer dataTypeId) {
        return dataTypeRepository.findOne(dataTypeId);
    }

    protected RulDescItemSpec createDescItemSpec(RulDescItemType rulDescItemType, String code, String name, String shortcut, String description, Integer viewOrder) {
        RulDescItemSpec dataSpecItem = new RulDescItemSpec();
        dataSpecItem.setDescItemType(rulDescItemType);
        dataSpecItem.setCode(code);
        dataSpecItem.setName(name);
        dataSpecItem.setShortcut(shortcut);
        dataSpecItem.setDescription(description);
        dataSpecItem.setViewOrder(viewOrder);
        descItemSpecRepository.save(dataSpecItem);
        return dataSpecItem;
    }

    protected RulDescItemConstraint createDescItemConstrain(RulDescItemType rulDescItemType, RulDescItemSpec rulDescItemSpec, ArrFaVersion faVersion, Boolean repeatable, String regexp, Integer textLengthLimit) {
        RulDescItemConstraint itemConstraint = new RulDescItemConstraint();
        itemConstraint.setDescItemType(rulDescItemType);
        itemConstraint.setDescItemSpec(rulDescItemSpec);
        itemConstraint.setVersion(faVersion);
        itemConstraint.setRepeatable(repeatable);
        itemConstraint.setRegexp(regexp);
        itemConstraint.setTextLenghtLimit(textLengthLimit);
        descItemConstraintRepository.save(itemConstraint);
        return itemConstraint;
    }

    protected ArrDescItem createArrDescItem(ArrFaChange createFaChange, ArrFaChange deleteFaChange, Integer descItemObjectId, RulDescItemType rulDescItemType, RulDescItemSpec rulDescItemSpec, ArrNode node, Integer position) {
        ArrDescItem descItem = new ArrDescItem();
        descItem.setCreateChange(createFaChange);
        descItem.setDeleteChange(deleteFaChange);

        // pokud není vyplněno, vybere další možné
        if(descItemObjectId == null) {
            Integer maxDescItemObjectId = descItemRepository.findMaxDescItemObjectId();
            if (maxDescItemObjectId == null) {
                maxDescItemObjectId = 0;
            }
            descItemObjectId = maxDescItemObjectId+1;
        }

        descItem.setDescItemObjectId(descItemObjectId);
        descItem.setDescItemType(rulDescItemType);
        descItem.setDescItemSpec(rulDescItemSpec);
        descItem.setNode(node);
        descItem.setPosition(position);
        descItemRepository.save(descItem);
        return descItem;
    }

    protected RegRecord createRecord(int index) {
        RegRegisterType registerType = new RegRegisterType();
        registerType.setCode("RT" + index);
        registerType.setName("Reg type " + index);
        registerTypeRepository.save(registerType);
        RegRecord record = new RegRecord();
        record.setCharacteristics(" dobrovolny hasicsky sbor");
        record.setLocal(Boolean.TRUE);
        record.setRegisterType(registerType);
        record.setRecord("Sbor dobrovolnych hasicu Topol");
        recordRepository.save(record);
        return record;
    }

    protected ParPartySubtype findPartySubtype() {
        return partySubtypeRepository.findOne(5);
    }

    protected ParAbstractParty createParAbstractParty() {
        final ParPartySubtype partySubtype = findPartySubtype();
//        final ParPartyType partyType = partyTypeRepository.findOne(partySubtype.getPartyType().getPartyTypeId());
        partySubtype.setPartyType(null);
        final RegRecord record = createRecord(1);
        return createParAbstractParty(partySubtype, record);
    }

    protected ParAbstractParty createParAbstractParty(final ParPartySubtype partySubtype, final RegRecord record) {
        ParAbstractParty party = new ParAbstractParty();
        party.setPartySubtype(partySubtype);
        party.setRecord(record);
        abstractPartyRepository.save(party);
        return party;
    }

    /**
     * Vytvoření jednoho typu rejstříku.
     * @return  vytvořený objekt, zapsaný do db
     */
    protected RegRegisterType createRegisterType() {
        RegRegisterType regRegisterType = new RegRegisterType();
        regRegisterType.setCode(TEST_CODE);
        regRegisterType.setName(TEST_NAME);
        registerTypeRepository.save(regRegisterType);
        return regRegisterType;
    }

    /**
     * Vytvoření jednoho záznamu rejstříku defaultního typu.
     * @return  vytvořený objekt, zapsaný do db
     */
    protected RegRecord createRecord() {
        RegRecord regRecord = new RegRecord();
        regRecord.setRecord(TEST_NAME);
        regRecord.setCharacteristics("CHARACTERISTICS");
        regRecord.setLocal(false);
        regRecord.setRegisterType(createRegisterType());

        return recordRepository.save(regRecord);
    }

    /**
     * Vytvoří variantní záznam rejstříku
     *
     * @param obsah     textový obsah záznamu
     * @param record    záznam rejstříku ke kterému patří
     * @return          vytvořený objekt
     */
    protected RegVariantRecord createVariantRecord(final String obsah, final RegRecord record) {
        RegVariantRecord regVariantRecord = new RegVariantRecord();
        regVariantRecord.setRecord(obsah);
        regVariantRecord.setRegRecord(record);

        return variantRecordRepository.save(regVariantRecord);
    }

    @Transactional
    protected ParAbstractParty createParty(String obsah) {
        final RegRecord record = createRecord();
        final ParPartySubtype partySubtype = findPartySubtype();
        createVariantRecord(obsah, record);

        ParAbstractParty party = new ParAbstractParty();
        party.setRecord(record);
        party.setPartySubtype(partySubtype);
        abstractPartyRepository.save(party);
        return party;
    }

    protected RulFaView createFaView(RulRuleSet ruleSet, ArrArrangementType arrangementType, Integer[] ids) {
        RulFaView view = new RulFaView();
        view.setArrangementType(arrangementType);
        view.setRuleSet(ruleSet);
        String specification = null;
        for (Integer id : ids) {
            if (specification == null) {
                specification = id.toString();
            } else {
                specification += "|" + id.toString();
            }
        }
        view.setViewSpecification(specification);

        return faViewRepository.save(view);
    }


    public static Response post(Function<RequestSpecification, RequestSpecification> params, String url) {
        return httpMethod(params, url, HttpMethod.POST, HttpStatus.OK);
    }

    public static Response put(Function<RequestSpecification, RequestSpecification> params, String url) {
        return httpMethod(params, url, HttpMethod.PUT, HttpStatus.OK);
    }

    public static Response putError(Function<RequestSpecification, RequestSpecification> params, String url) {
        return httpMethod(params, url, HttpMethod.PUT, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static Response put(Function<RequestSpecification, RequestSpecification> params, String url, HttpStatus status) {
        return httpMethod(params, url, HttpMethod.PUT, status);
    }

    public static Response get(Function<RequestSpecification, RequestSpecification> params, String url) {
        return httpMethod(params, url, HttpMethod.GET, HttpStatus.OK);
    }

    public static Response getError(Function<RequestSpecification, RequestSpecification> params, String url) {
        return httpMethod(params, url, HttpMethod.GET, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static Response get(String url) {
        return httpMethod((spec) -> spec, url, HttpMethod.GET, HttpStatus.OK);
    }

    public static Response httpMethod(Function<RequestSpecification, RequestSpecification> params, String url, HttpMethod method, HttpStatus status) {
        Assert.assertNotNull(params);
        Assert.assertNotNull(url);
        Assert.assertNotNull(method);

        RequestSpecification requestSpecification = params.apply(given());

        requestSpecification.header(JSON_CT_HEADER).log().all().config(UTF8_ENCODER_CONFIG);

        Response response = null;
        switch (method) {
            case GET:
                response = requestSpecification.get(url);
                break;
            case PUT:
                response = requestSpecification.put(url);
                break;
            case DELETE:
                response = requestSpecification.delete(url);
                break;
            case HEAD:
                response = requestSpecification.head(url);
                break;
            case OPTIONS:
                response = requestSpecification.options(url);
                break;
            case PATCH:
                response = requestSpecification.patch(url);
                break;
            case POST:
                response = requestSpecification.post(url);
                break;
            default:
                throw new IllegalStateException("Nedefinovaný stav " + method + ".");
        }

        logger.info("Response status: " + response.statusLine() + ", response body:");
        response.prettyPrint();
        Assert.assertEquals(status.value(), response.statusCode());

        return response;
    }

    /**
     * @return  nastavení češtiny pro testy
     */
    protected static RestAssuredConfig getUtf8Config() {
        return UTF8_ENCODER_CONFIG;
    }

    /**
     * Načte archivní pomůcku přes REST volání.
     *
     * @return archivní pomůcka
     */
    protected ArrFindingAid getFindingAid(final Integer findingAidId) {
        Response response = get(spec -> spec.parameter(FA_ID_ATT, findingAidId), GET_FA_ONE_URL);
        return response.getBody().as(ArrFindingAid.class);
    }

    /**
     * Načte archivní pomůcky přes REST volání.
     *
     * @return archivní pomůcky
     */
    protected List<ArrFindingAid> getFindingAids() {
        Response response = get(GET_FA_URL);

        List<ArrFindingAid> findingAids = Arrays.asList(response.getBody().as(ArrFindingAid[].class));
        return findingAids;
    }

    /**
     * Načte otevřenou verzi archivní pomůcky přes REST volání.
     *
     * @param findingAid archivní pomůcka
     *
     * @return otevřená verze archivní pomůcky
     */
    protected ArrFaVersion getFindingAidOpenVersion(ArrFindingAid findingAid) {
        Response response = get(spec -> spec.parameter(FA_ID_ATT, findingAid.getFindingAidId()),
                GET_OPEN_VERSION_BY_FA_ID_URL);

        return response.getBody().as(ArrFaVersion.class);
    }

    /**
     * Načte level v otevřené verzi podle nodeId.
     *
     * @param nodeId id uzlu
     *
     * @return level
     */
    protected ArrFaLevelExt getLevelByNodeId(Integer nodeId) {
        Response response = get(spec -> spec.parameter(NODE_ID_ATT, nodeId), GET_LEVEL_URL);

        return response.getBody().as(ArrFaLevelExt.class);
    }
}
