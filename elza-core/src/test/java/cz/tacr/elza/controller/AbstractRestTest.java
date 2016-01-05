package cz.tacr.elza.controller;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.EncoderConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import cz.tacr.elza.ElzaCoreTest;
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
import cz.tacr.elza.domain.ArrNodeConformityErrors;
import cz.tacr.elza.domain.ArrNodeConformityInfo;
import cz.tacr.elza.domain.ArrNodeConformityMissing;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.ParComplementType;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameFormType;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParRelationRoleType;
import cz.tacr.elza.domain.ParRelationType;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemConstraint;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulFaView;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.vo.ArrCalendarTypes;
import cz.tacr.elza.domain.vo.ArrDescItemSavePack;
import cz.tacr.elza.domain.vo.ArrLevelWithExtraNode;
import cz.tacr.elza.domain.vo.ArrNodeHistoryPack;
import cz.tacr.elza.domain.vo.RelatedNodeDirectionWithDescItems;
import cz.tacr.elza.domain.vo.RelatedNodeDirectionWithLevelPack;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.ComplementTypeRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DataStringRepository;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.repository.DescItemConstraintRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DescItemSpecRegisterRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;
import cz.tacr.elza.repository.ExternalSourceRepository;
import cz.tacr.elza.repository.FaBulkActionRepository;
import cz.tacr.elza.repository.FaViewRepository;
import cz.tacr.elza.repository.FindingAidRepository;
import cz.tacr.elza.repository.FindingAidVersionConformityInfoRepository;
import cz.tacr.elza.repository.FindingAidVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeConformityErrorsRepository;
import cz.tacr.elza.repository.NodeConformityInfoRepository;
import cz.tacr.elza.repository.NodeConformityMissingRepository;
import cz.tacr.elza.repository.NodeRegisterRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.PackageRepository;
import cz.tacr.elza.repository.PacketRepository;
import cz.tacr.elza.repository.PacketTypeRepository;
import cz.tacr.elza.repository.PartyNameComplementRepository;
import cz.tacr.elza.repository.PartyNameFormTypeRepository;
import cz.tacr.elza.repository.PartyNameRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.PartyTypeComplementTypeRepository;
import cz.tacr.elza.repository.PartyTypeRelationRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.RelationRoleTypeRepository;
import cz.tacr.elza.repository.RelationTypeRepository;
import cz.tacr.elza.repository.RelationTypeRoleTypeRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.VariantRecordRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.RegistryService;
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
import java.io.File;
import java.net.URL;
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
@SpringApplicationConfiguration(classes = ElzaCoreTest.class)
@IntegrationTest("server.port:0") // zvoli volny port, lze spustit i s aktivni Elzou
@WebAppConfiguration
public abstract class AbstractRestTest {

    private static final RestAssuredConfig UTF8_ENCODER_CONFIG = RestAssuredConfig.newConfig().encoderConfig(
            EncoderConfig.encoderConfig().defaultContentCharset("UTF-8"));

    private static final Logger logger = LoggerFactory.getLogger(ArrangementManagerTest.class);

    protected static final String ARRANGEMENT_MANAGER_URL = "/api/arrangementManager";
    protected static final String RULE_MANAGER_URL = "/api/ruleSetManager";
    protected static final String REGISTRY_MANAGER_URL = "/api/registryManager";
    protected static final String REGISTRY_MANAGER_URL_V2 = "/api/registryManagerV2";
    protected static final String PARTY_MANAGER_URL = "/api/partyManager";
    protected static final String PARTY_MANAGER_URL_V2 = "/api/partyManagerV2";
    protected static final String BULK_ACTION_MANAGER_URL = "/api/bulkActionManager";

    protected static final String TEST_CODE = "ZP";
    protected static final String TEST_NAME = "Test name";
    protected static final String TEST_UPDATE_NAME = "Update name";

    protected static final String DATA_TYP_STRING = "STR";
    protected static final String DATA_TYP_RECORD = "REC";

    protected static final String CONTENT_TYPE_HEADER = "content-type";
    protected static final String JSON_CONTENT_TYPE = "application/json";
    private static final Header JSON_CT_HEADER = new Header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE);

    // BULK ACTION MANAGER CONSTATNS

    protected static final String GET_BULK_ACTION_TYPES = BULK_ACTION_MANAGER_URL + "/bulkactiontypes";
    protected static final String CREATE_BULK_ACTION = BULK_ACTION_MANAGER_URL + "/bulkaction";
    protected static final String DELETE_BULK_ACTION = BULK_ACTION_MANAGER_URL + "/bulkaction";
    protected static final String UPDATE_BULK_ACTION = BULK_ACTION_MANAGER_URL + "/bulkaction";
    protected static final String RELOAD_BULK_ACTIONS = BULK_ACTION_MANAGER_URL + "/reload";
    protected static final String GET_ALL_BULK_ACTIONS = BULK_ACTION_MANAGER_URL + "/bulkactions/{versionId}";
    protected static final String GET_MANDATORY_BULK_ACTIONS = BULK_ACTION_MANAGER_URL + "/bulkactions/{versionId}/mandatory";
    protected static final String GET_BULK_ACTION = BULK_ACTION_MANAGER_URL + "/bulkaction/{bulkActionCode}";
    protected static final String GET_BULK_ACTION_STATES = BULK_ACTION_MANAGER_URL + "/bulkaction/{versionId}/states";
    protected static final String RUN_BULK_ACTION = BULK_ACTION_MANAGER_URL + "/run/{versionId}";
    protected static final String VALIDATE_BULK_ACTION = BULK_ACTION_MANAGER_URL + "/validate/{versionId}";

    // END - BULK ACTION MANAGER CONSTATNS

    // REGISTRY MANAGER CONSTANTS
    protected static final String GET_REGISTER_TYPES_URL = REGISTRY_MANAGER_URL + "/getRegisterTypes";
    protected static final String CREATE_RECORD_URL = REGISTRY_MANAGER_URL + "/createRecord";
    protected static final String CREATE_VARIANT_RECORD_URL = REGISTRY_MANAGER_URL + "/createVariantRecord";
    protected static final String FIND_RECORD_URL = REGISTRY_MANAGER_URL + "/findRecord";
    protected static final String FIND_RECORD_URL_V2 = REGISTRY_MANAGER_URL_V2 + "/findRecord";
    protected static final String UPDATE_RECORD_URL = REGISTRY_MANAGER_URL + "/updateRecord";
    protected static final String DELETE_RECORD_URL = REGISTRY_MANAGER_URL + "/deleteRecord";
    protected static final String UPDATE_VARIANT_RECORD_URL = REGISTRY_MANAGER_URL + "/updateVariantRecord";
    protected static final String DELETE_VARIANT_RECORD_URL = REGISTRY_MANAGER_URL + "/deleteVariantRecord";
    protected static final String GET_RECORD_URL = REGISTRY_MANAGER_URL + "/getRecord";
    protected static final String GET_RECORD_URL_V2 = REGISTRY_MANAGER_URL_V2 + "/getRecord";


    protected static final String RECORD_ID_ATT = "recordId";
    protected static final String VARIANT_RECORD_ID_ATT = "variantRecordId";
    protected static final String SEARCH_ATT = "search";
    protected static final String FROM_ATT = "from";
    protected static final String COUNT_ATT = "count";
    protected static final String REGISTER_TYPE_ID_ATT = "registerTypeIds";

    // END - REGISTRY MANAGER CONSTANTS

    // PARTY MANAGER CONSTANTS
    protected static final String GET_PARTY_TYPES = PARTY_MANAGER_URL + "/getPartyTypes";
    protected static final String GET_PARTY_TYPES_V2 = PARTY_MANAGER_URL_V2 + "/getPartyTypes";
    protected static final String INSERT_ABSTRACT_PARTY = PARTY_MANAGER_URL + "/insertParty";
    protected static final String FIND_ABSTRACT_PARTY = PARTY_MANAGER_URL + "/findParty";
    protected static final String FIND_ABSTRACT_PARTY_V2 = PARTY_MANAGER_URL_V2 + "/findParty";
    protected static final String UPDATE_ABSTRACT_PARTY = PARTY_MANAGER_URL + "/updateParty";
    protected static final String DELETE_ABSTRACT_PARTY = PARTY_MANAGER_URL + "/deleteParty";
    protected static final String GET_ABSTRACT_PARTY = PARTY_MANAGER_URL + "/getParty";
    protected static final String GET_ABSTRACT_PARTY_V2 = PARTY_MANAGER_URL_V2 + "/getParty";

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
    protected static final String GET_CALENDAR_TYPES = ARRANGEMENT_MANAGER_URL + "/getCalendarTypes";
    protected static final String FIND_SUB_LEVELS_EXT_URL = ARRANGEMENT_MANAGER_URL + "/findSubLevelsExt";
    protected static final String FIND_SUB_LEVELS_URL = ARRANGEMENT_MANAGER_URL + "/findSubLevels";
    protected static final String GET_HISTORY_FOR_NODE = ARRANGEMENT_MANAGER_URL + "/getHistoryForNode/{findingAidId}/{nodeId}";
    protected static final String GET_PACKET_TYPES = ARRANGEMENT_MANAGER_URL + "/getPacketTypes";

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
    protected static final String DELETE_DESCRIPTION_ITEM_URL = ARRANGEMENT_MANAGER_URL + "/deleteDescriptionItem/{versionId}";
    protected static final String SAVE_DESCRIPTION_ITEMS_URL = ARRANGEMENT_MANAGER_URL + "/saveDescriptionItems";
    protected static final String MODIFY_NODE_REGISTER_LINKS_URL = ARRANGEMENT_MANAGER_URL + "/modifyArrNodeRegisterLinks/{versionId}";
    protected static final String FIND_NODE_REGISTER_LINKS_URL = ARRANGEMENT_MANAGER_URL + "/findNodeRegisterLinks";
    protected static final String INSERT_ABSTRACT_PACKET = ARRANGEMENT_MANAGER_URL + "/insertPacket";

    protected static final String FA_NAME_ATT = "name";
    protected static final String FA_ID_ATT = "findingAidId";
    protected static final String ARRANGEMENT_TYPE_ID_ATT = "arrangementTypeId";
    protected static final String RULE_SET_ID_ATT = "ruleSetId";
    protected static final String NODE_ID_ATT = "nodeId";
    protected static final String PARENT_NODE_ID_ATT = "parentNodeId";
    protected static final String FOLLOWER_NODE_ID_ATT = "followerNodeId";
    protected static final String PREDECESSOR_NODE_ID_ATT = "predecessorNodeId";
    protected static final String FINDING_AID_ID_ATT = "findingAidId";
    protected static final String VERSION_ID_ATT = "versionId";
    protected static final String BULK_ACTION_CODE = "bulkActionCode";
    protected static final String CREATE_NEW_VERSION_ATT = "createNewVersion";

    protected static final Integer DATA_TYPE_INTEGER = 1;
    protected static final Integer DATA_TYPE_STRING = 2;
    protected static final Integer DATA_TYPE_TEXT = 3;
    protected static final Integer DATA_TYPE_UNITDATE = 4;
    protected static final Integer DATA_TYPE_UNITID = 5;
    protected static final Integer DATA_TYPE_FORMATTED_TEXT = 6;
    protected static final Integer DATA_TYPE_COORDINATES = 7;
    protected static final Integer DATA_TYPE_PARTY_REF = 8;
    protected static final Integer DATA_TYPE_RECORD_REF = 9;
    protected static final Integer DATA_TYPE_DECIMAL = 10;
    // END ARRANGEMENT MANAGER CONSTANTS

    // RULE MANAGER CONSTANTS
    protected static final String GET_DESC_ITEM_SPEC = RULE_MANAGER_URL + "/getDescItemSpecById";
    protected static final String GET_RS_URL = RULE_MANAGER_URL + "/getRuleSets";
    protected static final String GET_DIT_URL = RULE_MANAGER_URL + "/getDescriptionItemTypes";
    protected static final String GET_DIT_FOR_NODE_ID_URL = RULE_MANAGER_URL + "/getDescriptionItemTypesForNode/{faVersionId}/{nodeId}";
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
    protected static final String DT_DECIMAL = "DECIMAL";
    protected static final String DT_PACKET_REF = "PACKET_REF";
    protected static final String DT_ENUM = "ENUM";
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
    protected DescItemTypeRepository descItemTypeRepository;
    @Autowired
    protected DescItemSpecRepository descItemSpecRepository;
    @Autowired
    private DescItemSpecRegisterRepository descItemSpecRegisterRepository;
    @Autowired
    private DescItemConstraintRepository descItemConstraintRepository;
    @Autowired
    protected DataTypeRepository dataTypeRepository;
    @Autowired
    private FaViewRepository faViewRepository;
    @Autowired
    private DataStringRepository arrDataStringRepository;
    @Autowired
    private DataRepository arrDataRepository;
    @Autowired
    protected RegisterTypeRepository registerTypeRepository;
    @Autowired
    private ExternalSourceRepository externalSourceRepository;
    @Autowired
    protected PartyRepository partyRepository;
    @Autowired
    private VariantRecordRepository variantRecordRepository;
    @Autowired
    protected RegRecordRepository recordRepository;
    @Autowired
    private PartyTypeRepository partyTypeRepository;
    @Autowired
    private PacketTypeRepository packetTypeRepository;
    @Autowired
    private DataRecordRefRepository dataRecordRefRepository;
    @Autowired
    protected NodeRepository nodeRepository;
    @Autowired
    private PartyNameRepository partyNameRepository;
    @Autowired
    private PartyNameComplementRepository partyNameComplementRepository;
    @Autowired
    private NodeRegisterRepository nodeRegisterRepository;
    @Autowired
    private PacketRepository packetRepository;
    @Autowired
    private FaBulkActionRepository faBulkActionRepository;
    @Autowired
    protected NodeConformityInfoRepository nodeConformityInfoRepository;
    @Autowired
    protected NodeConformityErrorsRepository nodeConformityErrorsRepository;
    @Autowired
    protected NodeConformityMissingRepository nodeConformityMissingRepository;
    @Autowired
    protected FindingAidVersionConformityInfoRepository findingAidVersionConformityInfoRepository;

    @Autowired
    protected PackageRepository packageRepository;

    @Autowired
    protected RuleManager ruleManager;

    @Autowired
    private ArrangementService arrangementService;

    protected RulPackage rulPackage;

    //servisní třídy
    @Autowired
    protected RegistryService registryService;


    @Autowired
    protected PartyTypeRelationRepository partyTypeRelationRepository;

    @Autowired
    protected RelationTypeRepository relationTypeRepository;

    @Autowired
    protected RelationRoleTypeRepository relationRoleTypeRepository;

    @Autowired
    protected RelationTypeRoleTypeRepository relationTypeRoleTypeRepository;

    @Autowired
    protected PartyTypeComplementTypeRepository partyTypeComplementTypeRepository;

    @Autowired
    protected ComplementTypeRepository complementTypeRepository;

    @Autowired
    protected PartyNameFormTypeRepository partyNameFormTypeRepository;


    @Before
    public void setUp() {

        // nastavi default port pro REST-assured
        RestAssured.port = port;

        // nastavi default URI pro REST-assured. Nejcasteni localhost
        RestAssured.baseURI = RestAssured.DEFAULT_URI;


        if (packageRepository.count() == 0) {
            URL url = Thread.currentThread().getContextClassLoader().getResource("package-test.zip");
            File file = new File(url.getPath());


            ruleManager.importPackage(file);
        }


        if (rulPackage == null) {
            rulPackage = packageRepository.findAll().get(0);
        }

        // potřebné delete, jen data, ne číselníky
        arrDataRepository.deleteAll();
//        partyNameRepository.unsetAllParty();

        faBulkActionRepository.deleteAll();

        packetRepository.deleteAll();

        partyNameComplementRepository.deleteAll();
        partyRepository.unsetAllPreferredName();
        partyNameRepository.deleteAll();
        partyRepository.deleteAll();
        variantRecordRepository.deleteAll();
        nodeRegisterRepository.deleteAll();
        recordRepository.deleteAll();

        nodeConformityErrorsRepository.deleteAll();
        nodeConformityMissingRepository.deleteAll();
        nodeConformityInfoRepository.deleteAll();
        descItemConstraintRepository.deleteAll();
        faViewRepository.deleteAll();
        findingAidVersionConformityInfoRepository.deleteAll();
        findingAidVersionRepository.deleteAll();
        //arrangementTypeRepository.deleteAll();
        //ruleSetRepository.deleteAll();
        findingAidRepository.deleteAll();
        levelRepository.deleteAll();
        descItemRepository.deleteAll();
        descItemSpecRegisterRepository.deleteAll();

        //descItemSpecRepository.deleteAll();
        //descItemTypeRepository.deleteAll();
        nodeRepository.deleteAll();
        changeRepository.deleteAll();
        registerTypeRepository.deleteAll();
    }

    protected RulArrangementType createArrangementType(RulRuleSet ruleSet) {
        return arrangementTypeRepository.findAll().get(0);
    }

    protected RulRuleSet createRuleSet() {
        return ruleSetRepository.findAll().get(0);
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
        version.setLastChange(createChange);

        return findingAidVersionRepository.save(version);
    }

    protected ArrLevel createLevel(final Integer position, final ArrLevel parent, final ArrChange change) {
        ArrLevel level = new ArrLevel();
        level.setPosition(position);
        if (parent != null) {
            level.setNodeParent(parent.getNode());
        }
        level.setCreateChange(change);
        level.setNode(arrangementService.createNode());
        return levelRepository.save(level);
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
        RulDescItemConstraint itemConstraint = createDescItemConstrain(descItemType, rulDescItemSpec, "CODE20");
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
            RegRecord record = createRecord("KOD1");
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
        RulDescItemType itemType = descItemTypeRepository.findOneByCode("DI" + index);
        if (itemType == null) {
            itemType = new RulDescItemType();
        }
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
        itemType.setFaOnly(false);
        itemType.setPackage(rulPackage);
        return descItemTypeRepository.save(itemType);
    }

    protected RulDescItemConstraint createDescItemConstrain(final RulDescItemType itemType, RulDescItemSpec rulDescItemSpec, String code) {
        RulDescItemConstraint itemConstrain = new RulDescItemConstraint();
        itemConstrain.setDescItemSpec(rulDescItemSpec);
        itemConstrain.setDescItemType(itemType);
        itemConstrain.setCode(code);
        itemConstrain.setPackage(rulPackage);
        descItemConstraintRepository.save(itemConstrain);
        return itemConstrain;
    }

    private RulDescItemSpec createDescItemSpec(final RulDescItemType itemType, final int index) {
        RulDescItemSpec rulDescItemSpec = descItemSpecRepository.findOneByCode("IS" + index);
        if (rulDescItemSpec == null) {
            rulDescItemSpec = new RulDescItemSpec();
        }
        rulDescItemSpec.setCode("IS" + index);
        rulDescItemSpec.setDescItemType(itemType);
        rulDescItemSpec.setName("Item Spec " + index);
        rulDescItemSpec.setShortcut("ISpec " + index);
        rulDescItemSpec.setDescription("popis");
        rulDescItemSpec.setViewOrder(index);
        rulDescItemSpec.setPackage(rulPackage);
        return descItemSpecRepository.save(rulDescItemSpec);
    }

    protected RulDescItemType createDescItemType(RulDataType rulDataType, String code, String name, String shortcut,
            String description, Boolean isValueUnique, Boolean canBeOrdered, Boolean useSpecification, Integer viewOrder) {
        RulDescItemType dataTypeItem = descItemTypeRepository.findOneByCode(code);
        if (dataTypeItem == null) {
            dataTypeItem = new RulDescItemType();
        }
        dataTypeItem.setDataType(rulDataType);
        dataTypeItem.setCode(code);
        dataTypeItem.setName(name);
        dataTypeItem.setShortcut(shortcut);
        dataTypeItem.setDescription(description);
        dataTypeItem.setIsValueUnique(isValueUnique);
        dataTypeItem.setCanBeOrdered(canBeOrdered);
        dataTypeItem.setUseSpecification(useSpecification);
        dataTypeItem.setViewOrder(viewOrder);
        dataTypeItem.setFaOnly(false);
        dataTypeItem.setPackage(rulPackage);
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
        RulDescItemSpec dataSpecItem = descItemSpecRepository.findOneByCode(code);
        if (dataSpecItem == null) {
            dataSpecItem = new RulDescItemSpec();
        }
        dataSpecItem.setDescItemType(rulDescItemType);
        dataSpecItem.setCode(code);
        dataSpecItem.setName(name);
        dataSpecItem.setShortcut(shortcut);
        dataSpecItem.setDescription(description);
        dataSpecItem.setViewOrder(viewOrder);
        dataSpecItem.setPackage(rulPackage);
        return descItemSpecRepository.save(dataSpecItem);
    }

    protected RulDescItemConstraint createDescItemConstrain(RulDescItemType rulDescItemType, RulDescItemSpec rulDescItemSpec,
            ArrFindingAidVersion faVersion, Boolean repeatable, String regexp, Integer textLengthLimit, String code) {
        RulDescItemConstraint itemConstraint = new RulDescItemConstraint();
        itemConstraint.setDescItemType(rulDescItemType);
        itemConstraint.setDescItemSpec(rulDescItemSpec);
        itemConstraint.setVersion(faVersion);
        itemConstraint.setRepeatable(repeatable);
        itemConstraint.setRegexp(regexp);
        itemConstraint.setTextLenghtLimit(textLengthLimit);
        itemConstraint.setPackage(rulPackage);
        itemConstraint.setCode(code);
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

    protected ParPartyType findPartyType() {
        return partyTypeRepository.findOne(2);
    }

    protected RulPacketType findPacketType() {
        Response response = get(GET_PACKET_TYPES);
        List<RulPacketType> list = Arrays.asList(response.getBody().as(RulPacketType[].class));
        return list.get(0);
    }

    protected ParParty createParParty(final Integer codeIndex) {
        final ParPartyType partyType = findPartyType();
        final RegRecord record = createRecord(codeIndex);

        ParPartyNameFormType partyNameFormType = new ParPartyNameFormType();
        partyNameFormType.setCode("PPNFT" + codeIndex);
        partyNameFormTypeRepository.save(partyNameFormType);

        final ParPartyName partyName = new ParPartyName();
        partyName.setNameFormType(partyNameFormType);
        partyName.setMainPart("MAIN_PART");

        return createParParty(partyType, record, partyName);
    }

    protected ParPartyType createPartyType(final String code) {
        ParPartyType parPartyType = new ParPartyType();
        parPartyType.setCode(code);
        parPartyType.setName(code);
        parPartyType.setDescription(code);
        return partyTypeRepository.save(parPartyType);
    }

    protected ParRelationType createRelationType(final String code) {
        ParRelationType relationType = new ParRelationType();
        relationType.setCode(code);
        relationType.setName(code);
        return relationTypeRepository.save(relationType);
    }

    protected ParRelationRoleType createRelationRoleType(final String code){
        ParRelationRoleType relationRoleType = new ParRelationRoleType();
        relationRoleType.setCode(code);
        relationRoleType.setName(code);
        return relationRoleTypeRepository.save(relationRoleType);
    }

    protected ParComplementType createComplementType(final String code){
        ParComplementType complementType = new ParComplementType();
        complementType.setCode(code);
        complementType.setName(code);
        complementType.setViewOrder(0);
        return complementTypeRepository.save(complementType);
    }


    protected ParParty createParParty(final ParPartyType partySubtype, final RegRecord record, final ParPartyName partyName) {
        ParParty party = new ParParty();
        party.setPartyType(partySubtype);
        party.setRecord(record);
        party = partyRepository.save(party);

        if (partyName != null) {
            partyName.setParty(party);
            partyNameRepository.save(partyName);

            party.setPreferredName(partyName);
            partyRepository.save(party);
        }

        return party;
    }

    /**
     * Vytvoření jednoho typu rejstříku.
     * @return  vytvořený objekt, zapsaný do db
     */
    protected RegRegisterType createRegisterType(final String uniqueCode) {
        RegRegisterType regRegisterType = new RegRegisterType();
        regRegisterType.setCode(uniqueCode);
        regRegisterType.setName(TEST_NAME);
        return registerTypeRepository.save(regRegisterType);
    }

    /**
     * Vytvoření jednoho záznamu rejstříku defaultního typu.
     * @return  vytvořený objekt, zapsaný do db
     */
    protected RegRecord createRecord(final String uniqueCode) {
        RegRecord regRecord = new RegRecord();
        regRecord.setRecord(TEST_NAME);
        regRecord.setCharacteristics("CHARACTERISTICS");
        regRecord.setLocal(false);
        regRecord.setRegisterType(createRegisterType(uniqueCode));

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
    protected ParParty createParty(String obsah, final String uniqueCode) {
        final RegRecord record = createRecord(uniqueCode);
        final ParPartyType partySubtype = findPartyType();
        createVariantRecord(obsah, record);

//        ParPartyName preferredName = new ParPartyName();
//        partyNameRepository.save(preferredName);

        ParParty party = new ParParty();
        party.setRecord(record);
        party.setPartyType(partySubtype);
//        party.setPreferredName(preferredName);
        party = partyRepository.save(party);
//        preferredName.setParty(party);
//        partyNameRepository.save(preferredName);
        return party;
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
     * @param findingAidVersionId id verze uzlu
     * @return level
     */
    protected ArrLevelExt getLevelByNodeId(Integer nodeId, final Integer findingAidVersionId) {
        Response response = get(spec -> spec.parameter(NODE_ID_ATT, nodeId)
                .parameter(VERSION_ID_ATT, findingAidVersionId), GET_LEVEL_URL);

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
        Response response = get(spec -> spec.parameter(RULE_SET_ID_ATT, ruleSet.getRuleSetId()),
                GET_ARRANGEMENT_TYPES_URL);

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

        RelatedNodeDirectionWithDescItems as = response.getBody().as(RelatedNodeDirectionWithDescItems.class);
        return as.getArrDescItems().getDescItems();
    }

    /**
     * Vytvoří RESTově záznam rejstříku.
     *
     * @return  záznam
     */
    protected RegRecord restCreateRecord(final String uniqueCode) {
        RegRecord regRecord = new RegRecord();
        regRecord.setRecord(TEST_NAME);
        regRecord.setCharacteristics("CHARACTERISTICS");
        regRecord.setLocal(false);

        RegRegisterType registerType = createRegisterType(uniqueCode);
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
        final ParPartyType partySubtype = findPartyType();

        final RegRecord record = restCreateRecord("KOD1");

        final ParPartyName partyName = new ParPartyName();

        ParParty requestBody = new ParParty();
        requestBody.setPartyType(partySubtype);
        requestBody.setRecord(record);
        requestBody.setPreferredName(partyName);

        Response response = put(spec -> spec.body(requestBody), INSERT_ABSTRACT_PARTY);

        return response.getBody().as(ParParty.class);
    }

    /**
     * Vytvoří přes REST obal.
     *
     * @return obal
     */
    protected ArrPacket restCreatePacket(ArrFindingAid findingAid) {
        final RulPacketType partySubtype = findPacketType();

        ArrPacket requestBody = new ArrPacket();
        requestBody.setPacketType(partySubtype);
        requestBody.setInvalidPacket(false);
        requestBody.setFindingAid(findingAid);
        requestBody.setStorageNumber("123456789");

        Response response = put(spec -> spec.body(requestBody), INSERT_ABSTRACT_PACKET);

        return response.getBody().as(ArrPacket.class);
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
     * Vrátí změny v uzlech a atributech podle uzlu.
     *
     * @param nodeId        identifikátor uzlu
     * @param findingAidId  identifikátor archivní pomůcky
     * @return  objekt se změnami
     */
    protected ArrNodeHistoryPack getHistoryForNode(Integer nodeId, Integer findingAidId) {
        Response response = get(
                (spec) -> spec.pathParameter(NODE_ID_ATT, nodeId).pathParameter(FINDING_AID_ID_ATT, findingAidId),
                GET_HISTORY_FOR_NODE);
        return response.getBody().as(ArrNodeHistoryPack.class);
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
        RelatedNodeDirectionWithLevelPack related = response.getBody().as(RelatedNodeDirectionWithLevelPack.class);
        ArrLevelWithExtraNode parent = related.getArrLevelPack();

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
        RelatedNodeDirectionWithLevelPack related = response.getBody().as(RelatedNodeDirectionWithLevelPack.class);
        ArrLevelWithExtraNode parent = related.getArrLevelPack();
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
        RelatedNodeDirectionWithLevelPack related = response.getBody().as(RelatedNodeDirectionWithLevelPack.class);
        ArrLevelWithExtraNode parent = related.getArrLevelPack();
        return parent;
    }

    /**
     * Vytvoří objekt {@link ArrNodeConformityInfo}.
     *
     * @param node    uzel
     * @param version verze uzlu
     * @return info
     */
    protected ArrNodeConformityInfo createNodeConformityInfo(final ArrNode node, final ArrFindingAidVersion version){
        ArrNodeConformityInfo info = new ArrNodeConformityInfo();
        info.setNode(node);
        info.setFaVersion(version);
        info.setState(cz.tacr.elza.api.ArrNodeConformityInfo.State.OK);
        return nodeConformityInfoRepository.save(info);
    }

    /**
     * Vytvoří objekt {@link ArrNodeConformityMissing}.
     *
     * @param info    stav
     * @param type typ
     * @return chybějící data
     */
    protected ArrNodeConformityMissing createNodeConformityMissing(final ArrNodeConformityInfo info,
                                                                   final RulDescItemType type) {
        ArrNodeConformityMissing result = new ArrNodeConformityMissing();
        result.setNodeConformityInfo(info);
        result.setDescItemType(type);
        return nodeConformityMissingRepository.save(result);
    }

    /**
     * Vytvoří objekt {@link ArrNodeConformityErrors}.
     *
     * @param info     stav
     * @param descItem atribut
     * @return chyba
     */
    protected ArrNodeConformityErrors createNodeConformityError(final ArrNodeConformityInfo info,
                                                                final ArrDescItem descItem) {
        ArrNodeConformityErrors result = new ArrNodeConformityErrors();
        result.setNodeConformityInfo(info);
        result.setDescItem(descItem);
        return result;
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

        RelatedNodeDirectionWithLevelPack related = response.getBody().as(RelatedNodeDirectionWithLevelPack.class);
        return related.getArrLevelPack();
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
        RelatedNodeDirectionWithLevelPack related = response.getBody().as(RelatedNodeDirectionWithLevelPack.class);
        return related.getArrLevelPack();
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

        RelatedNodeDirectionWithLevelPack related = response.getBody().as(RelatedNodeDirectionWithLevelPack.class);
        return related.getArrLevelPack();
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

        RelatedNodeDirectionWithLevelPack related = response.getBody().as(RelatedNodeDirectionWithLevelPack.class);
        return related.getArrLevelPack();
    }

    /**
     * Načte dostupné typy kalendářů
     * @return  dostupné typy kalendářů
     */
    protected ArrCalendarTypes getCalendarTypes() {
        Response response = get(GET_CALENDAR_TYPES);
        return response.getBody().as(ArrCalendarTypes.class);
    }
}