package cz.tacr.elza.controller;

import static com.jayway.restassured.RestAssured.given;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import javax.transaction.Transactional;

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

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.EncoderConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;

import cz.tacr.elza.ElzaCore;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrLevelExt;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartySubtype;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemConstraint;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulFaView;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.vo.ArrDescItemSavePack;
import cz.tacr.elza.domain.vo.ArrDescItems;
import cz.tacr.elza.domain.vo.ArrLevelWithExtraNode;
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
import cz.tacr.elza.repository.FindingAidVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.PartySubtypeRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.VariantRecordRepository;

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
    protected static final String INSERT_ABSTRACT_PARTY = PARTY_MANAGER_URL + "/insertParty";
    protected static final String FIND_ABSTRACT_PARTY = PARTY_MANAGER_URL + "/findParty";
    protected static final String FIND_ABSTRACT_PARTY_COUNT = PARTY_MANAGER_URL + "/findPartyCount";
    protected static final String UPDATE_ABSTRACT_PARTY = PARTY_MANAGER_URL + "/updateParty";
    protected static final String DELETE_ABSTRACT_PARTY = PARTY_MANAGER_URL + "/deleteParty";
    protected static final String GET_ABSTRACT_PARTY = PARTY_MANAGER_URL + "/getParty";

    protected static final String PARTY_TYPE_ID_ATT = "partyTypeId";
    protected static final String ORIGINATOR_ATT = "originator";
    protected static final String ABSTRACT_PARTY_ID_ATT = "partyId";

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
    protected static final String CREATE_DESCRIPTION_ITEM_URL = ARRANGEMENT_MANAGER_URL + "/createDescriptionItem/{versionId}";
    protected static final String UPDATE_DESCRIPTION_ITEM_URL = ARRANGEMENT_MANAGER_URL + "/updateDescriptionItem/{versionId}/{createNewVersion}";
    protected static final String DELETE_DESCRIPTION_ITEM_URL = ARRANGEMENT_MANAGER_URL + "/deleteDescriptionItem";
    protected static final String SAVE_DESCRIPTION_ITEMS_URL = ARRANGEMENT_MANAGER_URL + "/saveDescriptionItems";

    protected static final String FA_NAME_ATT = "name";
    protected static final String FA_ID_ATT = "findingAidId";
    protected static final String ARRANGEMENT_TYPE_ID_ATT = "arrangementTypeId";
    protected static final String RULE_SET_ID_ATT = "ruleSetId";
    protected static final String NODE_ID_ATT = "nodeId";
    protected static final String PARENT_NODE_ID_ATT = "parentNodeId";
    protected static final String FOLLOWER_NODE_ID_ATT = "followerNodeId";
    protected static final String PREDECESSOR_NODE_ID_ATT = "predecessorNodeId";
    protected static final String VERSION_ID_ATT = "versionId";
    protected static final String CREATE_NEW_VERSION_ATT = "createNewVersion";

    protected static final Integer DATA_TYPE_INTEGER = 1;
    protected static final Integer DATA_TYPE_STRING = 2;
    protected static final Integer DATA_TYPE_TEXT = 3;
    protected static final Integer DATA_TYPE_DATACE = 4;
    protected static final Integer DATA_TYPE_REF = 5;
    protected static final Integer DATA_TYPE_FORMATTED_TEXT = 6;
    protected static final Integer DATA_TYPE_COORDINATES = 7;
    protected static final Integer DATA_TYPE_PARTY_REF = 8;
    protected static final Integer DATA_TYPE_RECORD_REF = 9;
    // END ARRANGEMENT MANAGER CONSTANTS

    // RULE MANAGER CONSTANTS
    protected static final String GET_DESC_ITEM_SPEC = RULE_MANAGER_URL + "/getDescItemSpecById";
    protected static final String GET_RS_URL = RULE_MANAGER_URL + "/getRuleSets";
    protected static final String GET_DIT_URL = RULE_MANAGER_URL + "/getDescriptionItemTypes";
    protected static final String GET_DIT_FOR_NODE_ID_URL = RULE_MANAGER_URL + "/getDescriptionItemTypesForNodeId";
    protected static final String GET_FVDIT_URL = RULE_MANAGER_URL + "/getFaViewDescItemTypes";
    protected static final String SAVE_FVDIT_URL = RULE_MANAGER_URL + "/saveFaViewDescItemTypes";

    protected static final String DT_UNITID = "UNITID";
    protected static final String DT_STRING = "STRING";
    protected static final String DT_INT = "INT";
    protected static final String DT_UNITDATE = "UNITDATE";
    protected static final String DT_PARTY_REF = "PARTY_REF";
    protected static final String DT_TEXT = "TEXT";
    protected static final String DT_COORDINATES = "COORDINATES";
    protected static final String DT_FORMATTED_TEXT = "FORMATTED_TEXT";
    protected static final String DT_RECORD_REF = "RECORD_REF";
    // END RULE MANAGER CONSTANTS

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ArrangementManager arrangementManager;
    @Autowired
    private ArrangementTypeRepository arrangementTypeRepository;
    @Autowired
    private RuleSetRepository ruleSetRepository;
    @Autowired
    private FindingAidVersionRepository findingAidVersionRepository;
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
    protected PartyRepository partyRepository;
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
        arrDataRepository.deleteAll();
        partyRepository.deleteAll();
        variantRecordRepository.deleteAll();
        recordRepository.deleteAll();

        descItemConstraintRepository.deleteAll();
        faViewRepository.deleteAll();
        findingAidVersionRepository.deleteAll();
        arrangementTypeRepository.deleteAll();
        ruleSetRepository.deleteAll();
        findingAidRepository.deleteAll();
        levelRepository.deleteAll();
        descItemRepository.deleteAll();
        descItemSpecRepository.deleteAll();
        descItemTypeRepository.deleteAll();
        nodeRepository.deleteAll();
        changeRepository.deleteAll();
    }

    protected RulArrangementType createArrangementType(RulRuleSet ruleSet) {
        RulArrangementType arrangementType = new RulArrangementType();
        arrangementType.setName(TEST_NAME);
        arrangementType.setCode(TEST_CODE);
        arrangementType.setRuleSet(ruleSet);
        return arrangementTypeRepository.save(arrangementType);
    }

    protected RulRuleSet createRuleSet() {
        RulRuleSet ruleSet = new RulRuleSet();
        ruleSet.setName(TEST_NAME);
        ruleSet.setCode(TEST_CODE);
        return ruleSetRepository.save(ruleSet);
    }

    protected ArrFindingAid createFindingAid(final String name) {
        RulRuleSet ruleSet = createRuleSet();
        RulArrangementType arrangementType = createArrangementType(ruleSet);

        return arrangementManager.createFindingAid(name, arrangementType.getArrangementTypeId(), ruleSet.getRuleSetId());
    }

    protected ArrFindingAidVersion createFindingAidVersion(final ArrFindingAid findingAid, boolean isLock, ArrChange createChange) {
        ArrLevel root = levelRepository.findAll().iterator().next();
        return createFindingAidVersion(findingAid, root, isLock, createChange);
    }

    protected ArrChange createFaChange(final LocalDateTime changeDate) {
        ArrChange resultChange = new ArrChange();
        resultChange.setChangeDate(changeDate);
        return changeRepository.save(resultChange);
    }

    protected ArrFindingAidVersion createFindingAidVersion(final ArrFindingAid findingAid, final ArrLevel root, boolean isLock, ArrChange createChange) {
        RulRuleSet ruleSet = ruleSetRepository.findAll().iterator().next();
        RulArrangementType arrangementType = arrangementTypeRepository.findAll().iterator().next();

        return createFindingAidVersion(findingAid, root, ruleSet, arrangementType, isLock, createChange);
    }

    protected ArrFindingAidVersion createFindingAidVersion(final ArrFindingAid findingAid, final ArrLevel root,
                                                   RulRuleSet ruleSet, RulArrangementType arrangementType, boolean isLock, ArrChange createChange) {

        if (createChange == null) {
            createChange = createFaChange(LocalDateTime.now());
        }

        ArrChange lockChange = null;
        if (isLock) {
            lockChange = createFaChange(LocalDateTime.now());
        }

        ArrFindingAidVersion version = new ArrFindingAidVersion();
        version.setArrangementType(arrangementType);
        version.setCreateChange(createChange);
        version.setLockChange(lockChange);
        version.setFindingAid(findingAid);
        version.setRootLevel(root);
        version.setRuleSet(ruleSet);

        return findingAidVersionRepository.save(version);
    }

    protected ArrLevel createLevel(final Integer position, final ArrLevel parent, final ArrChange change) {
        ArrLevel level = new ArrLevel();
        level.setPosition(position);
        if (parent != null) {
            level.setNodeParent(parent.getNode());
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
                                          final ArrChange change, final int index, final int dataTypeId) {
        RulDescItemType descItemType = createDescItemType(index, dataTypeId);
        RulDescItemSpec rulDescItemSpec = createDescItemSpec(descItemType, index);

        ArrDescItem item = new ArrDescItem();
        item.setNode(node);
        item.setPosition(position);
        item.setCreateChange(change);
        item.setDescItemObjectId(1);
        item.setDescItemType(descItemType);
        item.setDescItemSpec(rulDescItemSpec);
        item = descItemRepository.save(item);
        createData(item, index, dataTypeId);
        return item;
    }

    @Transactional
    protected RulDescItemConstraint createConstrain(final int index) {
        RulDescItemType descItemType = createDescItemType(index, DATA_TYPE_INTEGER);
        RulDescItemSpec rulDescItemSpec = createDescItemSpec(descItemType, index);
        RulDescItemConstraint itemConstraint = createDescItemConstrain(descItemType, rulDescItemSpec);
        return itemConstraint;
    }

    private ArrData createData(final ArrDescItem item, final int index, final Integer dataTypeId) {
        if (DATA_TYPE_STRING.equals(dataTypeId)) {
            ArrDataString dataStr = new ArrDataString();
            dataStr.setDescItem(item);
            RulDataType dataType = dataTypeRepository.getOne(dataTypeId);
            dataStr.setDataType(dataType);
            dataStr.setValue(TEST_NAME + index);
            arrDataStringRepository.save(dataStr);
            return dataStr;
        } else if (DATA_TYPE_RECORD_REF.equals(dataTypeId)) {
            ArrDataRecordRef dataStr = new ArrDataRecordRef();
            RegRecord record = createRecord();
            dataStr.setDescItem(item);
            RulDataType dataType = dataTypeRepository.getOne(dataTypeId);
            dataStr.setDataType(dataType);
            dataStr.setRecordId(record.getRecordId());
            dataRecordRefRepository.save(dataStr);
            return dataStr;
        }
        return null;
    }

    protected RulDescItemType createDescItemType(final int index, final int dataTypeId) {
        RulDescItemType itemType = new RulDescItemType();
        RulDataType dataType = getDataType(dataTypeId);
        itemType.setDataType(dataType);
        itemType.setCode("DI" + index);
        itemType.setName("Desc Item " + index);
        itemType.setShortcut("DItem " + index);
        itemType.setDescription("popis");
        itemType.setCanBeOrdered(false);
        itemType.setIsValueUnique(false);
        itemType.setUseSpecification(false);
        itemType.setViewOrder(index);
        return descItemTypeRepository.save(itemType);
    }

    protected RulDescItemConstraint createDescItemConstrain(final RulDescItemType itemType, RulDescItemSpec rulDescItemSpec) {
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
        return descItemSpecRepository.save(rulDescItemSpec);
    }

    protected RulDescItemType createDescItemType(RulDataType rulDataType, String code, String name, String shortcut,
            String description, Boolean isValueUnique, Boolean canBeOrdered, Boolean useSpecification, Integer viewOrder) {
        RulDescItemType dataTypeItem = new RulDescItemType();
        dataTypeItem.setDataType(rulDataType);
        dataTypeItem.setCode(code);
        dataTypeItem.setName(name);
        dataTypeItem.setShortcut(shortcut);
        dataTypeItem.setDescription(description);
        dataTypeItem.setIsValueUnique(isValueUnique);
        dataTypeItem.setCanBeOrdered(canBeOrdered);
        dataTypeItem.setUseSpecification(useSpecification);
        dataTypeItem.setViewOrder(viewOrder);
        return descItemTypeRepository.save(dataTypeItem);
    }

    protected RulDataType createDataType(String code, String name, String description, Boolean regexUse, Boolean textLenghtLimitUse, String storageTable) {
        RulDataType dataType = new RulDataType();
        dataType.setCode(code);
        dataType.setName(name);
        dataType.setDescription(description);
        dataType.setRegexpUse(regexUse);
        dataType.setTextLenghtLimitUse(textLenghtLimitUse);
        dataType.setStorageTable(storageTable);
        return dataTypeRepository.save(dataType);
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
        return descItemSpecRepository.save(dataSpecItem);
    }

    protected RulDescItemConstraint createDescItemConstrain(RulDescItemType rulDescItemType, RulDescItemSpec rulDescItemSpec,
            ArrFindingAidVersion faVersion, Boolean repeatable, String regexp, Integer textLengthLimit) {
        RulDescItemConstraint itemConstraint = new RulDescItemConstraint();
        itemConstraint.setDescItemType(rulDescItemType);
        itemConstraint.setDescItemSpec(rulDescItemSpec);
        itemConstraint.setVersion(faVersion);
        itemConstraint.setRepeatable(repeatable);
        itemConstraint.setRegexp(regexp);
        itemConstraint.setTextLenghtLimit(textLengthLimit);
        return descItemConstraintRepository.save(itemConstraint);
    }

    protected ArrDescItem createArrDescItem(ArrChange createFaChange, ArrChange deleteFaChange, Integer descItemObjectId,
            RulDescItemType rulDescItemType, RulDescItemSpec rulDescItemSpec, ArrNode node, Integer position) {
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
        return descItemRepository.save(descItem);
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
        return recordRepository.save(record);
    }

    protected ParPartySubtype findPartySubtype() {
        return partySubtypeRepository.findOne(5);
    }

    protected ParParty createParParty() {
        final ParPartySubtype partySubtype = findPartySubtype();
//        final ParPartyType partyType = partyTypeRepository.findOne(partySubtype.getPartyType().getPartyTypeId());
        partySubtype.setPartyType(null);
        final RegRecord record = createRecord(1);
        return createParParty(partySubtype, record);
    }

    protected ParParty createParParty(final ParPartySubtype partySubtype, final RegRecord record) {
        ParParty party = new ParParty();
        party.setPartySubtype(partySubtype);
        party.setRecord(record);
        return partyRepository.save(party);
    }

    /**
     * Vytvoření jednoho typu rejstříku.
     * @return  vytvořený objekt, zapsaný do db
     */
    protected RegRegisterType createRegisterType() {
        RegRegisterType regRegisterType = new RegRegisterType();
        regRegisterType.setCode(TEST_CODE);
        regRegisterType.setName(TEST_NAME);
        return registerTypeRepository.save(regRegisterType);
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
    protected ParParty createParty(String obsah) {
        final RegRecord record = createRecord();
        final ParPartySubtype partySubtype = findPartySubtype();
        createVariantRecord(obsah, record);

        ParParty party = new ParParty();
        party.setRecord(record);
        party.setPartySubtype(partySubtype);
        return partyRepository.save(party);
    }

    protected RulFaView createFaView(RulRuleSet ruleSet, RulArrangementType arrangementType, Integer[] ids) {
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


    public static Response delete(Function<RequestSpecification, RequestSpecification> params, String url) {
        return httpMethod(params, url, HttpMethod.DELETE, HttpStatus.OK);
    }

    public static Response post(Function<RequestSpecification, RequestSpecification> params, String url, HttpStatus status) {
        return httpMethod(params, url, HttpMethod.POST, status);
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
    protected ArrFindingAidVersion getFindingAidOpenVersion(ArrFindingAid findingAid) {
        Response response = get(spec -> spec.parameter(FA_ID_ATT, findingAid.getFindingAidId()),
                GET_OPEN_VERSION_BY_FA_ID_URL);

        return response.getBody().as(ArrFindingAidVersion.class);
    }

    /**
     * Načte level v otevřené verzi podle nodeId.
     *
     * @param nodeId id uzlu
     *
     * @return level
     */
    protected ArrLevelExt getLevelByNodeId(Integer nodeId) {
        Response response = get(spec -> spec.parameter(NODE_ID_ATT, nodeId), GET_LEVEL_URL);

        return response.getBody().as(ArrLevelExt.class);
    }

    /**
     * Načte typy výstupů podle pravidel tvorby.
     *
     * @param ruleSet pravidla tvorby
     *
     * @return typy výstupů
     */
    protected List<RulArrangementType> getArrangementTypes(RulRuleSet ruleSet) {
        Response response = get(spec -> spec.parameter(RULE_SET_ID_ATT, ruleSet.getRuleSetId()), GET_ARRANGEMENT_TYPES_URL);

        return Arrays.asList(response.getBody().as(RulArrangementType[].class));
    }

    /**
     * Vytvoření archivní pomůcky.
     *
     * @param ruleSet pravidla tvorby
     * @param arrangementType typ výstupu
     * @param httpStatus stav jakým má skončit volání
     *
     * @return archivní pomůcka
     */
    protected ArrFindingAid createFindingAid(RulRuleSet ruleSet, RulArrangementType arrangementType, HttpStatus httpStatus) {
        Response response = put(spec -> spec.parameter(FA_NAME_ATT, TEST_NAME)
                .parameter(ARRANGEMENT_TYPE_ID_ATT, arrangementType.getArrangementTypeId())
                .parameter(RULE_SET_ID_ATT, ruleSet.getRuleSetId()), CREATE_FA_URL, httpStatus);

        if (httpStatus == HttpStatus.OK) {
            return response.getBody().as(ArrFindingAid.class);
        }

        return null;
    }

    /**
     * Uloží balík hodnot.
     *
     * @param savePack hodnoty
     *
     * @return uložené i vymazané hodnoty
     */
    protected List<ArrDescItem> storeSavePack(ArrDescItemSavePack savePack) {
        Response response = post((spec) -> spec.body(savePack), SAVE_DESCRIPTION_ITEMS_URL);

        ArrDescItems descItems = response.getBody().as(ArrDescItems.class);
        return descItems.getDescItems();
    }

    /**
     * Vytvoří RESTově záznam rejstříku.
     *
     * @return  záznam
     */
    protected RegRecord restCreateRecord() {
        RegRecord regRecord = new RegRecord();
        regRecord.setRecord(TEST_NAME);
        regRecord.setCharacteristics("CHARACTERISTICS");
        regRecord.setLocal(false);

        RegRegisterType registerType = createRegisterType();
        regRecord.setRegisterType(registerType);

        Response response = put(spec -> spec.body(regRecord), CREATE_RECORD_URL);

        return response.getBody().as(RegRecord.class);
    }

    /**
     * Vytvoří přes REST osobu.
     *
     * @return osoba
     */
    protected ParParty restCreateParty() {
        final ParPartySubtype partySubtype = findPartySubtype();
        partySubtype.setPartyType(null);
        final RegRecord record = restCreateRecord();

        ParParty requestBody = new ParParty();
        requestBody.setPartySubtype(partySubtype);
        requestBody.setRecord(record);

        Response response = put(spec -> spec.body(requestBody), INSERT_ABSTRACT_PARTY);

        return response.getBody().as(ParParty.class);
    }


    /**
     * Najde podřízené úrovně.
     *
     * @param rootNode nadřazený uzel pro který hledáme potomky
     * @param version verze, může být null
     *
     * @return potomky předaného uzlu
     */
    protected List<ArrLevel> getSubLevels(ArrNode rootNode, ArrFindingAidVersion version) {
        Response response;
        if (version == null) {
            response = get(spec -> spec.parameter(NODE_ID_ATT, rootNode.getNodeId()), FIND_SUB_LEVELS_URL);
        } else {
            response = get(spec -> spec.parameter(NODE_ID_ATT, rootNode.getNodeId())
                    .parameter(VERSION_ID_ATT, version.getFindingAidVersionId()), FIND_SUB_LEVELS_URL);
        }

        return Arrays.asList(response.getBody().as(ArrLevel[].class));
    }

    /**
     * Vytvoří nový uzel pod předaným uzlem.
     *
     * @param levelWithExtraNode rodičovský uzel
     *
     * @return nový uzel
     */
    protected ArrLevelWithExtraNode createLevelChild(ArrLevelWithExtraNode levelWithExtraNode) {
        Response response = put(spec -> spec.body(levelWithExtraNode), ADD_LEVEL_CHILD_URL);
        ArrLevelWithExtraNode parent = response.getBody().as(ArrLevelWithExtraNode.class);

        return parent;
    }

    /**
     * Vytvoří nový uzel pod předaným uzlem.
     *
     * @param levelWithExtraNode rodičovský uzel
     */
    protected void createLevelChildWithError(ArrLevelWithExtraNode levelWithExtraNode) {
        put(spec -> spec.body(levelWithExtraNode), ADD_LEVEL_CHILD_URL, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Vytvoří nový uzel před předaným uzlem.
     *
     * @param levelWithExtraNode uzal před kterým se vytvoří nový uzel
     *
     * @return nový uzel
     */
    protected ArrLevelWithExtraNode createLevelBefore(ArrLevelWithExtraNode levelWithExtraNode) {
        Response response = put(spec -> spec.body(levelWithExtraNode), ADD_LEVEL_BEFORE_URL);
        ArrLevelWithExtraNode parent = response.getBody().as(ArrLevelWithExtraNode.class);

        return parent;
    }

    /**
     * Vytvoří nový uzel za předaným uzlem.
     *
     * @param levelWithExtraNode uzal za kterým se vytvoří nový uzel
     *
     * @return nový uzel
     */
    protected ArrLevelWithExtraNode createLevelAfter(ArrLevelWithExtraNode levelWithExtraNode) {
        Response response = put(spec -> spec.body(levelWithExtraNode), ADD_LEVEL_AFTER_URL);
        ArrLevelWithExtraNode parent = response.getBody().as(ArrLevelWithExtraNode.class);

        return parent;
    }

    /**
     * Přesune jeden uzel před druhý.
     *
     * @param movedLevel přesouvaný uzel
     * @param targetLevel uzel před který se má vložit přesouvaný uzel
     * @param version verze archivní pomůcky
     *
     * @return přesunutý uzel
     */
    protected ArrLevelWithExtraNode moveLevelBefore(ArrLevel movedLevel, ArrLevel targetLevel, ArrFindingAidVersion version) {
        ArrLevelWithExtraNode levelWithExtraNode = new ArrLevelWithExtraNode();
        levelWithExtraNode.setLevel(movedLevel);
        levelWithExtraNode.setLevelTarget(targetLevel);
        levelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

        Response response = put(spec -> spec.body(levelWithExtraNode), MOVE_LEVEL_BEFORE_URL);

        return response.getBody().as(ArrLevelWithExtraNode.class);
    }

    /**
     * Přesune jeden uzel před druhý. Očekává se chyba.
     *
     * @param movedLevel přesouvaný uzel
     * @param targetLevel uzel před který se má vložit přesouvaný uzel
     * @param version verze archivní pomůcky
     */
    protected void moveLevelBeforeWithError(ArrLevel movedLevel, ArrLevel targetLevel, ArrFindingAidVersion version) {
        ArrLevelWithExtraNode levelWithExtraNode = new ArrLevelWithExtraNode();
        levelWithExtraNode.setLevel(movedLevel);
        levelWithExtraNode.setLevelTarget(targetLevel);
        levelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

        put(spec -> spec.body(levelWithExtraNode), MOVE_LEVEL_BEFORE_URL, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Přesune jeden uzel pod druhý.
     *
     * @param movedLevel přesouvaný uzel
     * @param targetLevel uzel pod který se má vložit přesouvaný uzel
     * @param version verze archivní pomůcky
     *
     * @return přesunutý uzel
     */
    protected ArrLevelWithExtraNode moveLevelUnder(ArrLevel movedLevel, ArrLevel targetLevel, ArrFindingAidVersion version) {
        ArrLevelWithExtraNode levelWithExtraNode = new ArrLevelWithExtraNode();
        levelWithExtraNode.setLevel(movedLevel);
        levelWithExtraNode.setExtraNode(targetLevel.getNode());
        levelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

        Response response = put(spec -> spec.body(levelWithExtraNode), MOVE_LEVEL_UNDER_URL);

        return response.getBody().as(ArrLevelWithExtraNode.class);
    }

    /**
     * Přesune jeden uzel za druhý.
     *
     * @param movedLevel přesouvaný uzel
     * @param targetLevel uzel za který se má vložit přesouvaný uzel
     * @param version verze archivní pomůcky
     *
     * @return přesunutý uzel
     */
    protected ArrLevelWithExtraNode moveLevelAfter(ArrLevel movedLevel, ArrLevel targetLevel, ArrFindingAidVersion version) {
        ArrLevelWithExtraNode levelWithExtraNode = new ArrLevelWithExtraNode();
        levelWithExtraNode.setLevel(movedLevel);
        levelWithExtraNode.setLevelTarget(targetLevel);
        levelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

        Response response = put(spec -> spec.body(levelWithExtraNode), MOVE_LEVEL_AFTER_URL);

        return response.getBody().as(ArrLevelWithExtraNode.class);
    }

    /**
     * Přesune jeden uzel za druhý.
     *
     * @param version verze archivní pomůcky
     *
     * @return přesunutý uzel
     */
    protected ArrLevelWithExtraNode deleteLevel(ArrLevel levelToDelete, ArrFindingAidVersion version) {
        ArrLevelWithExtraNode levelWithExtraNode = new ArrLevelWithExtraNode();
        levelWithExtraNode.setLevel(levelToDelete);
        levelWithExtraNode.setExtraNode(levelToDelete.getNodeParent());
        levelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

        Response response = put(spec -> spec.body(levelWithExtraNode), DELETE_LEVEL_URL);

        return response.getBody().as(ArrLevelWithExtraNode.class);
    }
}