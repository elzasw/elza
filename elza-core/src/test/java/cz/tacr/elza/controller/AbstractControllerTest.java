package cz.tacr.elza.controller;

import static com.jayway.restassured.RestAssured.given;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.EncoderConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;

import cz.tacr.elza.AbstractTest;
import cz.tacr.elza.api.vo.XmlImportType;
import cz.tacr.elza.controller.vo.ArrCalendarTypeVO;
import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.ArrNodeRegisterVO;
import cz.tacr.elza.controller.vo.ArrPacketVO;
import cz.tacr.elza.controller.vo.ParInstitutionVO;
import cz.tacr.elza.controller.vo.ParPartyNameFormTypeVO;
import cz.tacr.elza.controller.vo.ParPartyTypeVO;
import cz.tacr.elza.controller.vo.ParPartyVO;
import cz.tacr.elza.controller.vo.ParPartyWithCount;
import cz.tacr.elza.controller.vo.ParRelationVO;
import cz.tacr.elza.controller.vo.RegRecordVO;
import cz.tacr.elza.controller.vo.RegRecordWithCount;
import cz.tacr.elza.controller.vo.RegRegisterTypeVO;
import cz.tacr.elza.controller.vo.RegScopeVO;
import cz.tacr.elza.controller.vo.RegVariantRecordVO;
import cz.tacr.elza.controller.vo.RulDataTypeVO;
import cz.tacr.elza.controller.vo.RulDescItemSpecVO;
import cz.tacr.elza.controller.vo.RulDescItemTypeVO;
import cz.tacr.elza.controller.vo.RulPacketTypeVO;
import cz.tacr.elza.controller.vo.RulRuleSetVO;
import cz.tacr.elza.controller.vo.ScenarioOfNewLevelVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNodeClient;
import cz.tacr.elza.controller.vo.ValidationResult;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemSpecExtVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemCoordinatesVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemDecimalVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemEnumVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemFormattedTextVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemIntVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemPacketVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemPartyRefVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemRecordRefVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemStringVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemTextVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemUnitdateVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemUnitidVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemVO;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.service.ArrMoveLevelService;


public abstract class AbstractControllerTest extends AbstractTest {

    private static final RestAssuredConfig UTF8_ENCODER_CONFIG = RestAssuredConfig.newConfig().encoderConfig(
            EncoderConfig.encoderConfig().defaultContentCharset("UTF-8"));

    private static final Logger logger = LoggerFactory.getLogger(AbstractControllerTest.class);
    protected static final String CONTENT_TYPE_HEADER = "content-type";
    protected static final String JSON_CONTENT_TYPE = "application/json";
    private static final Header JSON_CT_HEADER = new Header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE);
    private static final Header MULTIPART_HEADER = new Header(CONTENT_TYPE_HEADER, MediaType.MULTIPART_FORM_DATA_VALUE);

    protected static final String ADMIN_CONTROLLER_URL = "/api/admin";
    protected static final String ARRANGEMENT_CONTROLLER_URL = "/api/arrangementManagerV2";
    protected static final String BULK_ACTION_CONTROLLER_URL = "/api/bulkActionManagerV2";
    protected static final String PARTY_CONTROLLER_URL = "/api/partyManagerV2";
    protected static final String REGISTRY_CONTROLLER_URL = "/api/registryManagerV2";
    protected static final String VALIDATION_CONTROLLER_URL = "/api/validate";
    protected static final String RULE_CONTROLLER_URL = "/api/ruleSetManagerV2";
    protected static final String XML_IMPORT_CONTROLLER_URL = "/api/xmlImportManagerV2";

    // ADMIN
    protected static final String REINDEX = ADMIN_CONTROLLER_URL + "/reindex";
    protected static final String REINDEX_STATUS = ADMIN_CONTROLLER_URL + "/reindexStatus";

    // ARRANGEMENT
    protected static final String CREATE_FUND = ARRANGEMENT_CONTROLLER_URL + "/funds";
    protected static final String UPDATE_FUND = ARRANGEMENT_CONTROLLER_URL + "/updateFund";
    protected static final String FUNDS = ARRANGEMENT_CONTROLLER_URL + "/getFunds";
    protected static final String APPROVE_VERSION = ARRANGEMENT_CONTROLLER_URL + "/approveVersion";
    protected static final String ADD_LEVEL = ARRANGEMENT_CONTROLLER_URL + "/levels";
    protected static final String DELETE_LEVEL = ARRANGEMENT_CONTROLLER_URL + "/levels";
    protected static final String SCENARIOS = ARRANGEMENT_CONTROLLER_URL + "/scenarios";
    protected static final String CALENDAR_TYPES = ARRANGEMENT_CONTROLLER_URL + "/calendarTypes";
    protected static final String FA_TREE = ARRANGEMENT_CONTROLLER_URL + "/fundTree";
    protected static final String MOVE_LEVEL_AFTER = ARRANGEMENT_CONTROLLER_URL + "/moveLevelAfter";
    protected static final String MOVE_LEVEL_BEFORE = ARRANGEMENT_CONTROLLER_URL + "/moveLevelBefore";
    protected static final String MOVE_LEVEL_UNDER = ARRANGEMENT_CONTROLLER_URL + "/moveLevelUnder";
    protected static final String CREATE_DESC_ITEM = ARRANGEMENT_CONTROLLER_URL
            + "/descItems/{fundVersionId}/{nodeId}/{nodeVersion}/{descItemTypeId}/create";
    protected static final String UPDATE_DESC_ITEM = ARRANGEMENT_CONTROLLER_URL
            + "/descItems/{fundVersionId}/{nodeVersion}/update/{createNewVersion}";
    protected static final String DELETE_DESC_ITEM = ARRANGEMENT_CONTROLLER_URL
            + "/descItems/{fundVersionId}/{nodeVersion}/delete";
    protected static final String DELETE_DESC_ITEM_BY_TYPE = ARRANGEMENT_CONTROLLER_URL
            + "/descItems/{fundVersionId}/{nodeId}/{nodeVersion}/{descItemTypeId}";
    protected static final String PACKET_TYPES = ARRANGEMENT_CONTROLLER_URL + "/packets/types";
    protected static final String PACKETS = ARRANGEMENT_CONTROLLER_URL + "/packets/{fundId}";
    protected static final String INSERT_PACKET = ARRANGEMENT_CONTROLLER_URL + "/packets/{fundId}";
    protected static final String DEACTIVATE_PACKET = ARRANGEMENT_CONTROLLER_URL + "/packets/{fundId}/{packetId}";
    protected static final String UPDATE_PACKET = ARRANGEMENT_CONTROLLER_URL + "/packets/{fundId}";
    protected static final String FULLTEXT = ARRANGEMENT_CONTROLLER_URL + "/fulltext";
    protected static final String FIND_REGISTER_LINKS = ARRANGEMENT_CONTROLLER_URL + "/registerLinks/{nodeId}/{versionId}";
    protected static final String FIND_REGISTER_LINKS_FORM = ARRANGEMENT_CONTROLLER_URL + "/registerLinks/{nodeId}/{versionId}/form";
    protected static final String CREATE_REGISTER_LINK = ARRANGEMENT_CONTROLLER_URL + "/registerLinks/{nodeId}/{versionId}/create";
    protected static final String UPDATE_REGISTER_LINK = ARRANGEMENT_CONTROLLER_URL + "/registerLinks/{nodeId}/{versionId}/update";
    protected static final String DELETE_REGISTER_LINK = ARRANGEMENT_CONTROLLER_URL + "/registerLinks/{nodeId}/{versionId}/delete";
    protected static final String VALIDATE_VERSION = ARRANGEMENT_CONTROLLER_URL + "/validateVersion/{versionId}/{showAll}";
    protected static final String VALIDATE_VERSION_COUNT = ARRANGEMENT_CONTROLLER_URL + "/validateVersionCount/{versionId}";
    protected static final String FA_TREE_NODES = ARRANGEMENT_CONTROLLER_URL + "/fundTree/nodes";
    protected static final String NODE_PARENTS = ARRANGEMENT_CONTROLLER_URL + "/nodeParents";
    protected static final String NODE_FORM_DATA = ARRANGEMENT_CONTROLLER_URL + "/nodes/{nodeId}/{versionId}/form";
    protected static final String NODE_FORMS_DATA = ARRANGEMENT_CONTROLLER_URL + "/nodes/{versionId}/forms";
    protected static final String NODE_FORMS_DATA_AROUND = ARRANGEMENT_CONTROLLER_URL + "/nodes/{versionId}/{nodeId}/{around}/forms";
    protected static final String NODES = ARRANGEMENT_CONTROLLER_URL + "/nodes";
    protected static final String COPY_SIBLING = ARRANGEMENT_CONTROLLER_URL + "/copyOlderSiblingAttribute";
    protected static final String VERSIONS = ARRANGEMENT_CONTROLLER_URL + "/getVersions";
    protected static final String REPLACE_DATA_VALUES = ARRANGEMENT_CONTROLLER_URL + "/replaceDataValues/{versionId}";
    protected static final String FILTER_UNIQUE_VALUES = ARRANGEMENT_CONTROLLER_URL + "/filterUniqueValues/{versionId}";



    // Party
    protected static final String CREATE_RELATIONS = PARTY_CONTROLLER_URL + "/relations";
    protected static final String UPDATE_RELATIONS = PARTY_CONTROLLER_URL + "/relations/{relationId}";
    protected static final String DELETE_RELATIONS = PARTY_CONTROLLER_URL + "/relations/{relationId}";
    protected static final String FIND_PARTY = PARTY_CONTROLLER_URL + "/findParty";
    protected static final String FIND_PARTY_FOR_PARTY = PARTY_CONTROLLER_URL + "/findPartyForParty";
    protected static final String GET_PARTY = PARTY_CONTROLLER_URL + "/getParty";
    protected static final String GET_PARTY_TYPES = PARTY_CONTROLLER_URL + "/getPartyTypes";
    protected static final String GET_PARTY_NAME_FORM_TYPES = PARTY_CONTROLLER_URL + "/getPartyNameFormTypes";
    protected static final String INSERT_PARTY = PARTY_CONTROLLER_URL + "/insertParty";
    protected static final String UPDATE_PARTY = PARTY_CONTROLLER_URL + "/updateParty/{partyId}";
    protected static final String DELETE_PARTY = PARTY_CONTROLLER_URL + "/deleteParty";

    // REGISTRY
    protected static final String DEFAULT_SCOPES = REGISTRY_CONTROLLER_URL + "/defaultScopes";
    protected static final String CREATE_SCOPE = REGISTRY_CONTROLLER_URL + "/scopes";
    protected static final String UPDATE_SCOPE = REGISTRY_CONTROLLER_URL + "/scopes/{scopeId}";
    protected static final String DELETE_SCOPE = REGISTRY_CONTROLLER_URL + "/scopes/";
    protected static final String FA_SCOPES = REGISTRY_CONTROLLER_URL + "/fundScopes";
    protected static final String ALL_SCOPES = REGISTRY_CONTROLLER_URL + "/scopes";
    protected static final String RECORD_TYPES = REGISTRY_CONTROLLER_URL + "/recordTypes";

    protected static final String FIND_RECORD = REGISTRY_CONTROLLER_URL + "/findRecord";
    protected static final String FIND_RECORD_FOR_RELATION = REGISTRY_CONTROLLER_URL + "/findRecordForRelation";
    protected static final String GET_RECORD = REGISTRY_CONTROLLER_URL + "/getRecord";
    protected static final String CREATE_RECORD = REGISTRY_CONTROLLER_URL + "/createRecord";
    protected static final String UPDATE_RECORD = REGISTRY_CONTROLLER_URL + "/updateRecord";
    protected static final String DELETE_RECORD = REGISTRY_CONTROLLER_URL + "/deleteRecord";

    protected static final String CREATE_VARIANT_RECORD = REGISTRY_CONTROLLER_URL + "/createVariantRecord";
    protected static final String UPDATE_VARIANT_RECORD = REGISTRY_CONTROLLER_URL + "/updateVariantRecord";
    protected static final String DELETE_VARIANT_RECORD = REGISTRY_CONTROLLER_URL + "/deleteVariantRecord";

    protected static final String RECORD_TYPES_FOR_PARTY_TYPE = REGISTRY_CONTROLLER_URL + "/recordTypesForPartyType";

    protected static final String INSTITUTIONS = PARTY_CONTROLLER_URL + "/institutions";

    // RULE
    protected static final String RULE_SETS = RULE_CONTROLLER_URL + "/getRuleSets";
    protected static final String DATA_TYPES = RULE_CONTROLLER_URL + "/dataTypes";
    protected static final String DESC_ITEM_TYPES = RULE_CONTROLLER_URL + "/descItemTypes";
    protected static final String PACKAGES = RULE_CONTROLLER_URL + "/getPackages";

    // Validation
    protected static final String VALIDATE_UNIT_DATE = VALIDATION_CONTROLLER_URL + "/unitDate";

    // XmlImport
    protected final static String XML_IMPORT = XML_IMPORT_CONTROLLER_URL + "/import";

    @Value("${local.server.port}")
    private int port;

    private List<RulDataTypeVO> dataTypes = null;
    private List<RulDescItemTypeExtVO> descItemTypes = null;

    // Import institucí
    private final static String XML_INSTITUTION = "institution-import.xml";

    // Výchozí scope
    private final static String IMPORT_SCOPE = "GLOBAL";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        RestAssured.port = port;                        // nastavi default port pro REST-assured
        RestAssured.baseURI = RestAssured.DEFAULT_URI;  // nastavi default URI pro REST-assured. Nejcasteni localhost

        importXmlFile(null, null, XmlImportType.PARTY, IMPORT_SCOPE, 1, XmlImportControllerTest.getFile(XML_INSTITUTION));
    }

    public static Response delete(Function<RequestSpecification, RequestSpecification> params, String url) {
        return httpMethod(params, url, HttpMethod.DELETE, HttpStatus.OK);
    }

    public static Response post(Function<RequestSpecification, RequestSpecification> params,
                                String url,
                                HttpStatus status) {
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

    public static Response put(Function<RequestSpecification, RequestSpecification> params,
                               String url,
                               HttpStatus status) {
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

    public static Response httpMethod(Function<RequestSpecification, RequestSpecification> params,
                                      String url,
                                      HttpMethod method,
                                      HttpStatus status) {
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
     * Multipart request
     *
     * @param params
     * @param url
     * @return
     */
    protected static Response multipart(Function<RequestSpecification, RequestSpecification> params,
                                        String url) {
        Assert.assertNotNull(params);
        Assert.assertNotNull(url);

        RequestSpecification requestSpecification = params.apply(given());

        requestSpecification.header(MULTIPART_HEADER).log().all().config(UTF8_ENCODER_CONFIG);

        Response response = requestSpecification.post(url);
        logger.info("Response status: " + response.statusLine() + ", response body:");
        response.prettyPrint();
        Assert.assertEquals(HttpStatus.OK.value(), response.statusCode());

        return response;
    }

    /**
     * Získání seznamu pravidel.
     *
     * @return seznam pravidel
     */
    protected List<RulRuleSetVO> getRuleSets() {
        Response response = get(RULE_SETS);
        return Arrays.asList(response.getBody().as(RulRuleSetVO[].class));
    }

    /**
     * Získání seznamu institucí.
     *
     * @return seznamu institucí
     */
    protected List<ParInstitutionVO> getInstitutions() {
        Response response = get(INSTITUTIONS);
        return Arrays.asList(response.getBody().as(ParInstitutionVO[].class));
    }

    /**
     * Vytvoření archivní pomůcky.
     *
     * @param name              název AP
     * @param ruleSetId         identifikátor pravidel
     * @param institutionId     identifikátor instituce
     * @param dateRange         vysčítaná informace o časovém rozsahu fondu
     * @return ap
     */
    protected ArrFundVO createFund(final String name,
                                   final Integer ruleSetId,
                                   final Integer institutionId,
                                   final String internalCode,
                                   final String dateRange) {
        Response response = post(spec -> spec
                .queryParameter("name", name)
                .queryParameter("ruleSetId", ruleSetId)
                .queryParameter("institutionId", institutionId)
                .queryParameter("internalCode", internalCode)
                .queryParameter("dateRange", dateRange), CREATE_FUND);
        return response.getBody().as(ArrFundVO.class);
    }

    /**
     * Vytvoření výchozí archivní pomůcky.
     *
     * @param name název AP
     * @return ap
     */
    protected ArrFundVO createFund(final String name, final String internalCode) {
        List<RulRuleSetVO> ruleSets = getRuleSets();
        RulRuleSetVO ruleSet = ruleSets.get(0);
        ParInstitutionVO institution = getInstitutions().get(0);
        return createFund(name, ruleSet.getId(), institution.getId(), internalCode, null);
    }

    /**
     * Úprava archivní pomůcky.
     *
     * @param fund ap k úpravě
     * @return ap
     */
    protected ArrFundVO fundAid(final ArrFundVO fund) {
        Response response = post(spec -> spec.body(fund), UPDATE_FUND);
        return response.getBody().as(ArrFundVO.class);
    }

    /**
     * Uzavření verze archivní pomůcky.
     *
     * @param fundVersion verze archivní pomůcky
     * @param ruleSet   typ výstupu
     * @return nová verze ap
     */
    protected ArrFundVersionVO approveVersion(final ArrFundVersionVO fundVersion,
                                              final RulRuleSetVO ruleSet,
                                              final String dateRange) {
        return approveVersion(fundVersion.getId(), ruleSet.getId(), dateRange);
    }

    /**
     * Uzavření verze archivní pomůcky.
     *
     * @param versionId         identifikátor verze archivní pomůcky
     * @param dateRange identifikátor výstupu
     * @param ruleSetId         identifikátor pravidel
     * @return nová verze ap
     */
    protected ArrFundVersionVO approveVersion(final Integer versionId,
                                              final Integer ruleSetId,
                                              final String dateRange) {
        Response response = put(spec -> spec
                .queryParameter("versionId", versionId)
                .queryParameter("dateRange", dateRange)
                .queryParameter("ruleSetId", ruleSetId), APPROVE_VERSION);
        return response.getBody().as(ArrFundVersionVO.class);
    }

    /**
     * Vrátí archivní pomůcky s verzema.
     *
     * @return archivní pomůcky
     */
    protected List<ArrFundVO> getFunds() {
        Response response = get(FUNDS);
        return Arrays.asList(response.getBody().as(ArrFundVO[].class));
    }

    /**
     * Přidání nového uzlu.
     *
     * @param addLevelParam parametry pro vytvoření nového uzlu
     * @return nový uzel
     */
    protected ArrangementController.NodeWithParent addLevel(final ArrangementController.AddLevelParam addLevelParam) {
        Response response = put(spec -> spec.body(addLevelParam), ADD_LEVEL);
        return response.getBody().as(ArrangementController.NodeWithParent.class);
    }

    /**
     * Provede načtení stromu uzlů. Uzly mohou být rozbaleny.
     *
     * @param input vstupní data pro načtení
     * @return data stromu
     */
    protected TreeData getFundTree(final ArrangementController.FaTreeParam input) {
        Response response = post(spec -> spec.body(input), FA_TREE);
        return response.getBody().as(TreeData.class);
    }

    /**
     * Přidání nového uzlu.
     *
     * @param direction         směr přidání
     * @param fundVersion verze archivní pomůcky
     * @param staticNode        uzel vůči kterému přidávám
     * @param parentStaticNode  rodič uzlu vůči kterému přidávám
     * @return vytvořený uzel
     */
    protected ArrangementController.NodeWithParent addLevel(final ArrMoveLevelService.AddLevelDirection direction,
                                                            final ArrFundVersionVO fundVersion,
                                                            final ArrNodeVO staticNode,
                                                            final ArrNodeVO parentStaticNode,
                                                            final String scenarioName) {
        ArrangementController.AddLevelParam addLevelParam = new ArrangementController.AddLevelParam();
        addLevelParam.setVersionId(fundVersion.getId());
        addLevelParam.setDirection(direction);
        addLevelParam.setStaticNode(staticNode);
        addLevelParam.setStaticNodeParent(parentStaticNode);
        addLevelParam.setScenarioName(scenarioName);

        ArrangementController.NodeWithParent newLevel = addLevel(addLevelParam);

        org.springframework.util.Assert.notNull(newLevel.getNode());
        org.springframework.util.Assert.notNull(newLevel.getParentNode());

        return newLevel;
    }

    /**
     * Přesun uzlu - před.
     *
     * @param moveParam parametry přesunu
     */
    protected void moveLevelBefore(final ArrangementController.LevelMoveParam moveParam) {
        put(spec -> spec.body(moveParam), MOVE_LEVEL_BEFORE);
    }

    /**
     * Přesun uzlu - před.
     *
     * @param fundVersion   verze archivní pomůcky
     * @param staticNode          uzel vůči kterému přesouvám
     * @param staticNodeParent    rodič uzlu vůči kterému přesouvám
     * @param transportNodes      přesouvaný uzly
     * @param transportNodeParent rodič přesouvaných uzlů
     */
    protected void moveLevelBefore(final ArrFundVersionVO fundVersion,
                                   final ArrNodeVO staticNode,
                                   final ArrNodeVO staticNodeParent,
                                   final List<ArrNodeVO> transportNodes,
                                   final ArrNodeVO transportNodeParent) {
        ArrangementController.LevelMoveParam moveParam = createMoveParam(fundVersion, staticNode,
                staticNodeParent, transportNodes, transportNodeParent);
        moveLevelBefore(moveParam);
    }

    /**
     * Přesun uzlu - za.
     *
     * @param moveParam parametry přesunu
     */
    protected void moveLevelAfter(final ArrangementController.LevelMoveParam moveParam) {
        put(spec -> spec.body(moveParam), MOVE_LEVEL_AFTER);
    }

    /**
     * Přesun uzlu - za.
     *
     * @param fundVersion   verze archivní pomůcky
     * @param staticNode          uzel vůči kterému přesouvám
     * @param staticNodeParent    rodič uzlu vůči kterému přesouvám
     * @param transportNodes      přesouvaný uzly
     * @param transportNodeParent rodič přesouvaných uzlů
     */
    protected void moveLevelAfter(final ArrFundVersionVO fundVersion,
                                  final ArrNodeVO staticNode,
                                  final ArrNodeVO staticNodeParent,
                                  final List<ArrNodeVO> transportNodes,
                                  final ArrNodeVO transportNodeParent) {
        ArrangementController.LevelMoveParam moveParam = createMoveParam(fundVersion, staticNode,
                staticNodeParent, transportNodes, transportNodeParent);
        moveLevelAfter(moveParam);
    }

    /**
     * Přesun uzlu - pod.
     *
     * @param moveParam parametry přesunu
     */
    protected void moveLevelUnder(final ArrangementController.LevelMoveParam moveParam) {
        put(spec -> spec.body(moveParam), MOVE_LEVEL_UNDER);
    }

    /**
     * Přesun uzlu - pod.
     *
     * @param fundVersion   verze archivní pomůcky
     * @param staticNode          uzel vůči kterému přesouvám
     * @param staticNodeParent    rodič uzlu vůči kterému přesouvám
     * @param transportNodes      přesouvaný uzly
     * @param transportNodeParent rodič přesouvaných uzlů
     */
    protected void moveLevelUnder(final ArrFundVersionVO fundVersion,
                                  final ArrNodeVO staticNode,
                                  final ArrNodeVO staticNodeParent,
                                  final List<ArrNodeVO> transportNodes,
                                  final ArrNodeVO transportNodeParent) {
        ArrangementController.LevelMoveParam moveParam = createMoveParam(fundVersion, staticNode,
                staticNodeParent, transportNodes, transportNodeParent);
        moveLevelUnder(moveParam);
    }

    /**
     * Vytvoření parametrů pro přesun.
     *
     * @param fundVersion   verze archivní pomůcky
     * @param staticNode          uzel vůči kterému přesouvám
     * @param staticNodeParent    rodič uzlu vůči kterému přesouvám
     * @param transportNodes      přesouvaný uzly
     * @param transportNodeParent rodič přesouvaných uzlů
     * @return parametry přesunu
     */
    private ArrangementController.LevelMoveParam createMoveParam(final ArrFundVersionVO fundVersion,
                                                                 final ArrNodeVO staticNode,
                                                                 final ArrNodeVO staticNodeParent,
                                                                 final List<ArrNodeVO> transportNodes,
                                                                 final ArrNodeVO transportNodeParent) {
        ArrangementController.LevelMoveParam moveParam = new ArrangementController.LevelMoveParam();
        moveParam.setVersionId(fundVersion.getId());
        moveParam.setStaticNode(staticNode);
        moveParam.setStaticNodeParent(staticNodeParent);
        moveParam.setTransportNodes(transportNodes);
        moveParam.setTransportNodeParent(transportNodeParent);
        return moveParam;
    }

    /**
     * Smazání uzlu.
     *
     * @param fundVersion verze archivní pomůcky
     * @param staticNode        uzel který mažu
     * @param staticNodeParent  rodič uzlu který mažu
     * @return smazaný uzel s rodičem
     */
    protected ArrangementController.NodeWithParent deleteLevel(final ArrFundVersionVO fundVersion,
                                                               final ArrNodeVO staticNode,
                                                               final ArrNodeVO staticNodeParent) {
        ArrangementController.NodeParam nodeParam = new ArrangementController.NodeParam();
        nodeParam.setVersionId(fundVersion.getId());
        nodeParam.setStaticNode(staticNode);
        nodeParam.setStaticNodeParent(staticNodeParent);
        return deleteLevel(nodeParam);
    }

    /**
     * Smazání uzlu.
     *
     * @param nodeParam parametry mazání
     * @return smazaný uzel s rodičem
     */
    protected ArrangementController.NodeWithParent deleteLevel(final ArrangementController.NodeParam nodeParam) {
        Response response = delete(spec -> spec.body(nodeParam), DELETE_LEVEL);
        return response.getBody().as(ArrangementController.NodeWithParent.class);
    }

    /**
     * Validace unitDate
     *
     * @param value String unit date
     * @return výsledek validace
     */
    protected ValidationResult validateUnitDate(final String value) {
        Response response = get(spec -> spec.queryParameter("value", value), VALIDATE_UNIT_DATE);
        return response.getBody().as(ValidationResult.class);
    }

    /**
     * Získání listu RulDataTypeVO
     *
     * @return list RulDataTypeVO
     */
    protected List<RulDataTypeVO> getDataTypes() {
        return Arrays.asList(get(DATA_TYPES).getBody().as(RulDataTypeVO[].class));
    }

    /**
     * Získání listu RulDescItemTypeExtVO
     *
     * @return list RulDescItemTypeExtVO
     */
    protected List<RulDescItemTypeExtVO> getDescItemTypes() {
        return Arrays.asList(get(DESC_ITEM_TYPES).getBody().as(RulDescItemTypeExtVO[].class));
    }

    /**
     * Získání rule data types
     *
     * @return list rule data types
     */
    protected List<RulDescItemTypeExtVO> getDescItemTypesCached() {
        if (descItemTypes == null) {
            descItemTypes = getDescItemTypes();
        }
        return descItemTypes;
    }

    /**
     * Získání rule data types
     *
     * @return list rule data types
     */
    protected List<RulDataTypeVO> getDataTypesCached() {
        if (dataTypes == null) {
            dataTypes = getDataTypes();
        }
        return dataTypes;
    }

    /**
     * Získání listu RulPackage
     *
     * @return list RulPackage
     */
    protected List<RulPackage> getPackages() {
        return Arrays.asList(get(PACKAGES).getBody().as(RulPackage[].class));
    }

    /**
     * Vytvoření hodnoty atributu.
     *
     * @param descItem          hodnota atributu
     * @param fundVersion verze archivní pomůcky
     * @param node              uzel
     * @param descItemType      typ atributu
     * @return vytvořená hodnota atributu
     */
    protected ArrangementController.DescItemResult createDescItem(final ArrDescItemVO descItem,
                                                                  final ArrFundVersionVO fundVersion,
                                                                  final ArrNodeVO node,
                                                                  final RulDescItemTypeVO descItemType) {
        return createDescItem(descItem, fundVersion.getId(), descItemType.getId(), node.getId(),
                node.getVersion());
    }

    /**
     * Vytvoření hodnoty atributu.
     *
     * @param descItem            hodnota atributu
     * @param fundVersionId identifikátor verze AP
     * @param descItemTypeId      identifikátor typu hodnoty atributu
     * @param nodeId              identfikátor uzlu
     * @param nodeVersion         verze uzlu
     * @return vytvořená hodnota atributu
     */
    protected ArrangementController.DescItemResult createDescItem(final ArrDescItemVO descItem,
                                                                  final Integer fundVersionId,
                                                                  final Integer descItemTypeId,
                                                                  final Integer nodeId,
                                                                  final Integer nodeVersion) {
        Response response = put(spec -> spec
                .body(descItem)
                .pathParameter("fundVersionId", fundVersionId)
                .pathParameter("descItemTypeId", descItemTypeId)
                .pathParameter("nodeId", nodeId)
                .pathParameter("nodeVersion", nodeVersion), CREATE_DESC_ITEM);
        return response.getBody().as(ArrangementController.DescItemResult.class);
    }

    /**
     * Upravení hodnoty atributu.
     *
     * @param descItem          hodnota atributu
     * @param fundVersion verze archivní pomůcky
     * @param node              uzel
     * @param createNewVersion  vytvořit novou verzi?
     * @return upravená hodnota atributu
     */
    protected ArrangementController.DescItemResult updateDescItem(final ArrDescItemVO descItem,
                                                                  final ArrFundVersionVO fundVersion,
                                                                  final ArrNodeVO node,
                                                                  final Boolean createNewVersion) {
        return updateDescItem(descItem, fundVersion.getId(), node.getVersion(), createNewVersion);
    }

    /**
     * Upravení hodnoty atributu.
     *
     * @param descItem            hodnota atributu
     * @param fundVersionId identifikátor verze AP
     * @param nodeVersion         verze uzlu
     * @param createNewVersion    vytvořit novou verzi?
     * @return upravená hodnota atributu
     */
    protected ArrangementController.DescItemResult updateDescItem(final ArrDescItemVO descItem,
                                                                  final Integer fundVersionId,
                                                                  final Integer nodeVersion,
                                                                  final Boolean createNewVersion) {
        Response response = put(spec -> spec
                .body(descItem)
                .pathParameter("fundVersionId", fundVersionId)
                .pathParameter("nodeVersion", nodeVersion)
                .pathParameter("createNewVersion", createNewVersion), UPDATE_DESC_ITEM);
        return response.getBody().as(ArrangementController.DescItemResult.class);
    }

    /**
     * Smazání hodnoty atributu.
     *
     * @param descItem          hodnota atributu
     * @param fundVersion verze archivní pomůcky
     * @param node              uzel
     * @return smazaná hodnota atributu
     */
    protected ArrangementController.DescItemResult deleteDescItem(final ArrDescItemVO descItem,
                                                                  final ArrFundVersionVO fundVersion,
                                                                  final ArrNodeVO node) {
        return deleteDescItem(descItem, fundVersion.getId(), node.getVersion());
    }

    /**
     * Smazání hodnoty atributu.
     *
     * @param descItem            hodnota atributu
     * @param fundVersionId identifikátor verze AP
     * @param nodeVersion         verze uzlu
     * @return smazaná hodnota atributu
     */
    protected ArrangementController.DescItemResult deleteDescItem(final ArrDescItemVO descItem,
                                                                  final Integer fundVersionId,
                                                                  final Integer nodeVersion) {
        Response response = post(spec -> spec
                .body(descItem)
                .pathParameter("fundVersionId", fundVersionId)
                .pathParameter("nodeVersion", nodeVersion), DELETE_DESC_ITEM);
        return response.getBody().as(ArrangementController.DescItemResult.class);
    }


    /**
     * Vytvoření objektu pro hodnotu atributu.
     *
     * @param typeCode         kód typu atributu
     * @param specCode         kód specifikace atributu
     * @param value            hodnota
     * @param position         pozice
     * @param descItemObjectId identifikátor hodnoty atributu
     * @return vytvořený object hodnoty atributu
     */
    protected ArrDescItemVO buildDescItem(final String typeCode,
                                          final String specCode,
                                          final Object value,
                                          final Integer position,
                                          final Integer descItemObjectId) {
        org.springframework.util.Assert.notNull(typeCode, "Musí být vyplněn kód typu atributu");

        RulDescItemTypeExtVO type = findDescItemTypeByCode(typeCode);
        org.springframework.util.Assert.notNull(type, "Typ atributu neexistuje -> CODE: " + typeCode);

        RulDescItemSpecVO spec = null;

        if (specCode != null) {
            spec = findDescItemSpecByCode(specCode, type);
            org.springframework.util.Assert.notNull(spec, "Specifikace atributu neexistuje -> CODE: " + specCode);
        }

        RulDataTypeVO dataType = findDataType(type.getDataTypeId());

        ArrDescItemVO descItem;

        switch (dataType.getCode()) {

            case "INT": {
                descItem = new ArrDescItemIntVO();
                ((ArrDescItemIntVO) descItem).setValue((Integer) value);
                break;
            }

            case "STRING": {
                descItem = new ArrDescItemStringVO();
                ((ArrDescItemStringVO) descItem).setValue((String) value);
                break;
            }

            case "TEXT": {
                descItem = new ArrDescItemTextVO();
                ((ArrDescItemTextVO) descItem).setValue((String) value);
                break;
            }

            case "UNITDATE": {
                descItem = new ArrDescItemUnitdateVO();
                ((ArrDescItemUnitdateVO) descItem).setValue((String) value);
                ((ArrDescItemUnitdateVO) descItem).setCalendarTypeId(getCalendarTypes().get(0).getId());
                break;
            }

            case "UNITID": {
                descItem = new ArrDescItemUnitidVO();
                ((ArrDescItemUnitidVO) descItem).setValue((String) value);
                break;
            }

            case "FORMATTED_TEXT": {
                descItem = new ArrDescItemFormattedTextVO();
                ((ArrDescItemFormattedTextVO) descItem).setValue((String) value);
                break;
            }

            case "COORDINATES": {
                descItem = new ArrDescItemCoordinatesVO();
                ((ArrDescItemCoordinatesVO) descItem).setValue((String) value);
                break;
            }

            case "PARTY_REF": {
                descItem = new ArrDescItemPartyRefVO();
                ((ArrDescItemPartyRefVO) descItem).setValue(((ParPartyVO) value).getPartyId());
                break;
            }

            case "RECORD_REF": {
                descItem = new ArrDescItemRecordRefVO();
                ((ArrDescItemRecordRefVO) descItem).setValue(((RegRecordVO) value).getRecordId());
                break;
            }

            case "DECIMAL": {
                descItem = new ArrDescItemDecimalVO();
                ((ArrDescItemDecimalVO) descItem).setValue((BigDecimal) value);
                break;
            }

            case "PACKET_REF": {
                descItem = new ArrDescItemPacketVO();
                ((ArrDescItemPacketVO) descItem).setValue(((ArrPacketVO) value).getId());
                break;
            }

            case "ENUM": {
                descItem = new ArrDescItemEnumVO();
                if (BooleanUtils.isNotTrue(type.getUseSpecification())) {
                    throw new IllegalStateException(
                            "Specifikace u typu musí být povinná pro ENUM -> CODE: " + type.getCode());
                }
                break;
            }

            default:
                throw new IllegalStateException("Neimplementovaný datový typ atributu -> CODE: " + dataType.getCode());

        }

        if (spec != null) {
            descItem.setDescItemSpecId(spec.getId());
        }

        descItem.setPosition(position);
        descItem.setDescItemObjectId(descItemObjectId);

        return descItem;
    }

    /**
     * Vyhledání datového typu atributu.
     *
     * @param dataTypeId    identifikátor datového typu atributu
     * @return datový typ atributu
     */
    protected RulDataTypeVO findDataType(final Integer dataTypeId) {
        List<RulDataTypeVO> dataTypes = getDataTypesCached();

        for (RulDataTypeVO dataType : dataTypes) {
            if (dataType.getId().equals(dataTypeId)) {
                return dataType;
            }
        }

        return null;
    }

    /**
     * Vyhledání specifikace atributu podle kódu.
     *
     * @param code  kód specifikace
     * @param type  typ atributu
     * @return specifikace atributu
     */
    protected RulDescItemSpecExtVO findDescItemSpecByCode(final String code, final RulDescItemTypeExtVO type) {
        for (RulDescItemSpecExtVO descItemSpec : type.getDescItemSpecs()) {
            if (descItemSpec.getCode().equals(code)) {
                return descItemSpec;
            }
        }
        return null;
    }

    /**
     * Seznam typů obalů.
     *
     * @return typy obalů
     */
    protected List<RulPacketTypeVO> getPacketTypes() {
        return Arrays.asList(get(PACKET_TYPES).getBody().as(RulPacketTypeVO[].class));
    }

    /**
     * Seznam obalů pro AP.
     *
     * @param fund AP
     * @return obaly pro AP
     */
    protected List<ArrPacketVO> getPackets(final ArrFundVO fund) {
        return getPackets(fund.getId());
    }

    /**
     * Seznam obalů pro AP.
     *
     * @param fundId identifikátor AP
     * @return obaly pro AP
     */
    protected List<ArrPacketVO> getPackets(final Integer fundId) {
        return Arrays.asList(get(spec ->
                spec.pathParameter("fundId", fundId), PACKETS).getBody().as(ArrPacketVO[].class));
    }

    /**
     * Vložení nového obalu pro AP.
     *
     * @param fund    ap
     * @param packetVO      obal
     * @return obal
     */
    protected ArrPacketVO insertPacket(final ArrFundVO fund, final ArrPacketVO packetVO) {
        return insertPacket(fund.getId(), packetVO);
    }

    /**
     * Vložení nového obalu pro AP.
     *
     * @param fundId  identifikátor AP
     * @param packetVO      obal
     * @return obal
     */
    protected ArrPacketVO insertPacket(final Integer fundId, final ArrPacketVO packetVO) {
        return post(spec -> spec
                .pathParameter("fundId", fundId)
                .body(packetVO), INSERT_PACKET).getBody().as(ArrPacketVO.class);
    }

    /**
     * Smazání obalu.
     *
     * @param fund    AP
     * @param packet        obal pro smazání
     * @return obal
     */
    protected ArrPacketVO deactivatePacket(final ArrFundVO fund, final ArrPacketVO packet) {
        return deactivatePacket(fund.getId(), packet.getId());
    }

    /**
     * Smazání obalu.
     *
     * @param fundId  identifikátor AP
     * @param packetId      identfikátor obalu pro smazání
     * @return obal
     */
    protected ArrPacketVO deactivatePacket(final Integer fundId, final Integer packetId) {
        return delete(spec -> spec
                .pathParameter("fundId", fundId)
                .pathParameter("packetId", packetId), DEACTIVATE_PACKET).getBody().as(ArrPacketVO.class);
    }

    /**
     * Upravení obalu.
     *
     * @param fund   AP
     * @param packet     obal
     * @return obal
     */
    protected ArrPacketVO updatePacket(final ArrFundVO fund,
                                       final ArrPacketVO packet) {
        return updatePacket(fund.getId(), packet);
    }

    /**
     * Upravení obalu.
     *
     * @param fundId identifikátor AP
     * @param packetVO     obal
     * @return obal
     */
    protected ArrPacketVO updatePacket(final Integer fundId,
                                       final ArrPacketVO packetVO) {
        return put(spec -> spec
                .pathParameter("fundId", fundId)
                .body(packetVO), UPDATE_PACKET).getBody().as(ArrPacketVO.class);
    }

    /**
     * Provede načtení stromu uzlů. Uzly mohou být rozbaleny.
     *
     * @param fundVersion     verze AP
     * @param node        uzel
     * @param searchValue hledaný text
     * @param depth       hloubka vyhledávání
     * @return seznam výsledků
     */
    protected List<ArrangementController.TreeNodeFulltext> fulltext(final ArrFundVersionVO fundVersion,
                                                                    final ArrNodeVO node,
                                                                    final String searchValue,
                                                                    final ArrangementController.Depth depth) {
        ArrangementController.FaFulltextParam input = new ArrangementController.FaFulltextParam();
        input.setVersionId(fundVersion.getId());
        input.setNodeId(node.getId());
        input.setSearchValue(searchValue);
        input.setDepth(depth);
        return fulltext(input);
    }

    /**
     * Provede načtení stromu uzlů. Uzly mohou být rozbaleny.
     *
     * @param input vstupní data pro načtení
     * @return data stromu
     */
    protected List<ArrangementController.TreeNodeFulltext> fulltext(final ArrangementController.FaFulltextParam input) {
        return Arrays.asList(post(spec -> spec
                .body(input), FULLTEXT).getBody().as(ArrangementController.TreeNodeFulltext[].class));
    }

    /**
     * Nalezení otevřené verze AP.
     *
     * @param fund archivní pomůcka
     * @return otevřená verze AP
     */
    protected ArrFundVersionVO getOpenVersion(final ArrFundVO fund) {
        org.springframework.util.Assert.notNull(fund);

        List<ArrFundVO> funds = getFunds();

        for (ArrFundVO fundFound : funds) {
            if (fundFound.getId().equals(fund.getId())) {
                for (ArrFundVersionVO fundVersion : fundFound.getVersions()) {
                    if (fundVersion.getLockDate() == null) {
                        return fundVersion;
                    }
                }
            }
        }

        return null;
    }


    /**
     * Převod TreeNodeClient na ArrNodeVO.
     *
     * @param treeNodeClients seznam uzlů stromu
     * @return převedený seznam uzlů stromu
     */
    protected List<ArrNodeVO> convertTreeNodes(final Collection<TreeNodeClient> treeNodeClients) {
        List<ArrNodeVO> nodes = new ArrayList<>(treeNodeClients.size());
        for (TreeNodeClient treeNodeClient : treeNodeClients) {
            nodes.add(convertTreeNode(treeNodeClient));
        }
        return nodes;
    }

    /**
     * Převod TreeNodeClient na ArrNodeVO.
     *
     * @param treeNodeClient uzel stromu
     * @return převedený uzel stromu
     */
    protected ArrNodeVO convertTreeNode(final TreeNodeClient treeNodeClient) {
        ArrNodeVO rootNode = new ArrNodeVO();
        BeanUtils.copyProperties(treeNodeClient, rootNode);
        return rootNode;
    }

    /**
     * Načte číselník typů kalendářů.
     *
     * @return typy kalendářů
     */
    protected List<ArrCalendarTypeVO> getCalendarTypes() {
        return Arrays.asList(get(CALENDAR_TYPES).getBody().as(ArrCalendarTypeVO[].class));
    }

    /**
     * Vyhledání vazeb AP - rejstříky.
     *
     * @param versionId id verze stromu
     * @param nodeId    identfikátor JP
     * @return vazby
     */
    protected List<ArrNodeRegisterVO> findRegisterLinks(final Integer versionId,
                                                        final Integer nodeId) {
        return Arrays.asList(get(spec -> spec
                .pathParameter("versionId", versionId)
                .pathParameter("nodeId", nodeId), FIND_REGISTER_LINKS).getBody().as(ArrNodeRegisterVO[].class));
    }

    /**
     * Vyhledání vazeb AP - rejstříky pro formulář.
     *
     * @param versionId id verze stromu
     * @param nodeId    identfikátor JP
     * @return vazby pro formulář
     */
    protected ArrangementController.NodeRegisterDataVO findRegisterLinksForm(final Integer versionId,
                                                                             final Integer nodeId) {
        return get(spec -> spec
                        .pathParameter("versionId", versionId)
                        .pathParameter("nodeId", nodeId),
                FIND_REGISTER_LINKS_FORM).getBody().as(ArrangementController.NodeRegisterDataVO.class);
    }

    /**
     * Vytvoření vazby AP - rejstříky
     *
     * @param versionId      id verze stromu
     * @param nodeId         identfikátor JP
     * @param nodeRegisterVO vazba
     * @return vazba
     */
    protected ArrNodeRegisterVO createRegisterLinks(final Integer versionId,
                                                    final Integer nodeId,
                                                    final ArrNodeRegisterVO nodeRegisterVO) {
        return put(spec -> spec
                .pathParameter("versionId", versionId)
                .pathParameter("nodeId", nodeId)
                .body(nodeRegisterVO), CREATE_REGISTER_LINK).getBody().as(ArrNodeRegisterVO.class);
    }

    /**
     * Upravení vazby AP - rejstříky.
     *
     * @param versionId      id verze stromu
     * @param nodeId         identfikátor JP
     * @param nodeRegisterVO vazba
     * @return vazba
     */
    protected ArrNodeRegisterVO updateRegisterLinks(final Integer versionId,
                                                    final Integer nodeId,
                                                    final ArrNodeRegisterVO nodeRegisterVO) {
        return post(spec -> spec
                .pathParameter("versionId", versionId)
                .pathParameter("nodeId", nodeId)
                .body(nodeRegisterVO), UPDATE_REGISTER_LINK).getBody().as(ArrNodeRegisterVO.class);
    }

    /**
     * Smazání vazby AP - rejstříky.
     *
     * @param versionId      id verze stromu
     * @param nodeId         identfikátor JP
     * @param nodeRegisterVO vazba
     * @return vazba
     */
    protected ArrNodeRegisterVO deleteRegisterLinks(final Integer versionId,
                                                    final Integer nodeId,
                                                    final ArrNodeRegisterVO nodeRegisterVO) {
        return post(spec -> spec
                .pathParameter("versionId", versionId)
                .pathParameter("nodeId", nodeId)
                .body(nodeRegisterVO), DELETE_REGISTER_LINK).getBody().as(ArrNodeRegisterVO.class);
    }

    /**
     * Nalezne hierarchický typ.
     *
     * @param list seznam typů
     * @return nalezený typ
     */
    protected RegRegisterTypeVO getHierarchicalRegRegisterType(List<RegRegisterTypeVO> list, List<RegRegisterTypeVO> exclude) {
        if (exclude == null) {
            exclude = new ArrayList<>();
        }
        for (RegRegisterTypeVO type : list) {
            if (type.getHierarchical() && type.getAddRecord() && !exclude.contains(type)) {
                return type;
            }
        }

        for (RegRegisterTypeVO type : list) {
            if (type.getChildren() != null) {
                RegRegisterTypeVO res = getHierarchicalRegRegisterType(type.getChildren(), exclude);
                if (res != null) {
                    return res;
                }
            }
        }
        return null;
    }



    /**
     * Validuje verzi archivní pomůcky a vrátí list chyb.
     * Pokud je počet chyb 0 pak předpokládáme že stav AP = OK
     *
     * @param versionId         verze, která se má validovat
     * @return Objekt s listem (prvních 20) chyb
     */
    public List<ArrangementController.VersionValidationItem> validateVersion(final Integer versionId) {
        return Arrays.asList(get(spec -> spec
                        .pathParameter("versionId", versionId)
                        .pathParameter("showAll", true),
                VALIDATE_VERSION).getBody().as(ArrangementController.VersionValidationItem[].class));
    }

    /**
     * Validuje verzi archivní pomůcky a vrátí počet chyb
     * Pokud je počet chyb 0 pak předpokládáme že stav AP = OK
     *
     * @param versionId         verze, která se má validovat
     */
    protected void validateVersionCount(final Integer versionId) {
        get(spec -> spec.pathParameter("versionId", versionId), VALIDATE_VERSION_COUNT);
    }

    /**
     * Provede načtení požadovaných uzlů ze stromu.
     *
     * @param input vstupní data pro načtení
     * @return data stromu
     */
    protected List<TreeNodeClient> getFundTreeNodes(final ArrangementController.FaTreeNodesParam input) {
        return Arrays.asList(post(spec -> spec
                .body(input), FA_TREE_NODES).getBody().as(TreeNodeClient[].class));
    }

    /**
     * Načte seznam rodičů daného uzlu. Seřazeno od prvního rodiče po kořen stromu.
     *
     * @param nodeId    nodeid uzlu
     * @param versionId id verze stromu
     * @return seznam rodičů
     */
    protected List<TreeNodeClient> getNodeParents(final Integer nodeId,
                                               final Integer versionId) {
        return Arrays.asList(get(spec -> spec
                .queryParameter("nodeId", nodeId)
                .queryParameter("versionId", versionId), NODE_PARENTS).getBody().as(TreeNodeClient[].class));
    }

    /**
     * Získání dat pro formulář.
     *
     * @param nodeId    identfikátor JP
     * @param versionId id verze stromu
     * @return formulář
     */
    protected ArrangementController.NodeFormDataNewVO getNodeFormData(final Integer nodeId,
                                                                      final Integer versionId) {
        return get(spec -> spec
                .pathParameter("nodeId", nodeId)
                .pathParameter("versionId", versionId),
                NODE_FORM_DATA).getBody().as(ArrangementController.NodeFormDataNewVO.class);
    }

    /**
     * Získání dat pro formuláře.
     * @param nodeIds   identfikátory JP
     * @param versionId id verze stromu
     * @return formuláře
     */
    protected ArrangementController.NodeFormsDataVO getNodeFormsData(final Integer versionId, final Integer... nodeIds) {
        return get(spec -> spec
                .queryParameter("nodeIds", nodeIds)
                .pathParameter("versionId", versionId),
                NODE_FORMS_DATA).getBody().as(ArrangementController.NodeFormsDataVO.class);
    }

    /**
     * Získání dat formuláře pro JP a jeho okolí.
     *
     * @param nodeId    identfikátory JP
     * @param versionId id verze stromu
     * @param around    velikost okolí - počet před a za uvedeným uzlem
     * @return formuláře
     */
    protected ArrangementController.NodeFormsDataVO getNodeWithAroundFormsData(final Integer versionId,
                                                      final Integer nodeId,
                                                      final Integer around) {
        return get(spec -> spec
                        .pathParameter("nodeId", nodeId)
                        .pathParameter("around", around)
                        .pathParameter("versionId", versionId),
                NODE_FORMS_DATA_AROUND).getBody().as(ArrangementController.NodeFormsDataVO.class);
    }

    /**
     * Načte seznam uzlů podle jejich id.
     *
     * @param idsParam seznam id
     * @return seznam vo uzlů s danými id
     */
    protected List<TreeNodeClient> getNodes(final ArrangementController.IdsParam idsParam) {
        return Arrays.asList(post(spec -> spec.body(idsParam), NODES).getBody().as(TreeNodeClient[].class));
    }

    /**
     * Načte FA pro dané verze.
     *
     * @param idsParam id verzí
     * @return seznam FA, každá obsahuje pouze jednu verzi, jinak je vrácená víckrát
     */
    protected List<ArrFundVO> getFundsByVersionIds(final ArrangementController.IdsParam idsParam) {
        return Arrays.asList(post(spec -> spec.body(idsParam), VERSIONS).getBody().as(ArrFundVO[].class));
    }

    /**
     * Smazání hodnot atributu podle typu.
     *
     * @param fundVersionId   identfikátor verze AP
     * @param nodeId                identfikátor JP
     * @param nodeVersion           verze JP
     * @param descItemTypeId        identfikátor typu hodnoty atributu
     */
    protected ArrangementController.DescItemResult deleteDescItemsByType(final Integer fundVersionId,
                                                final Integer nodeId,
                                                final Integer nodeVersion,
                                                final Integer descItemTypeId) {
        return delete(spec -> spec
                        .pathParameter("fundVersionId", fundVersionId)
                        .pathParameter("nodeId", nodeId)
                        .pathParameter("nodeVersion", nodeVersion)
                        .pathParameter("descItemTypeId", descItemTypeId),
                        DELETE_DESC_ITEM_BY_TYPE).getBody().as(ArrangementController.DescItemResult.class);
    }

    /**
     * Vyhledá scénáře pro možné archivní pomůcky
     *
     * @param param vstupní parametry
     * @return List scénářů
     */
    protected List<ScenarioOfNewLevelVO> getDescriptionItemTypesForNewLevel(final Boolean withGroups,
                                                                         final ArrangementController.DescriptionItemParam param) {
        return Arrays.asList(post(spec -> spec
                .queryParameter("withGroups", withGroups)
                .body(param), SCENARIOS).getBody().as(ScenarioOfNewLevelVO[].class));
    }

    /**
     * Provede zkopírování atributu daného typu ze staršího bratra uzlu.
     *
     * @param versionId      id verze stromu
     * @param descItemTypeId typ atributu, který chceme zkopírovat
     * @param nodeVO         uzel, na který nastavíme hodnoty ze staršího bratra
     * @return vytvořené hodnoty
     */
    protected ArrangementController.CopySiblingResult copyOlderSiblingAttribute(
            final Integer versionId,
            final Integer descItemTypeId,
            final ArrNodeVO nodeVO) {
        return put(spec -> spec
                .queryParameter("versionId", versionId)
                .queryParameter("descItemTypeId", descItemTypeId)
                .body(nodeVO), COPY_SIBLING).getBody().as(ArrangementController.CopySiblingResult.class);
    }

    /**
     * Vyhledání typu atributu podle kódu
     * @param code kód typu
     * @return typ atributu
     */
    protected RulDescItemTypeExtVO findDescItemTypeByCode(final String code) {
        for (RulDescItemTypeExtVO descItemType : getDescItemTypesCached()) {
            if (descItemType.getCode().equals(code)) {
                return descItemType;
            }
        }
        return null;
    }

    /**
     * Vrací výchozí třídy rejstříků z databáze.
     */
    protected List<RegScopeVO> getDefaultScopes() {
        return Arrays.asList(get(DEFAULT_SCOPES).getBody().as(RegScopeVO[].class));
    }

    /**
     * Vložení nové třídy.
     *
     * @param scope objekt třídy
     * @return nový objekt třídy
     */
    protected RegScopeVO createScope(final RegScopeVO scope) {
        return post(spec -> spec.body(scope), CREATE_SCOPE).getBody().as(RegScopeVO.class);
    }

    /**
     * Aktualizace třídy.
     *
     * @param scope objekt třídy
     * @return aktualizovaný objekt třídy
     */
    protected RegScopeVO updateScope(final RegScopeVO scope) {
        return put(spec -> spec.body(scope).pathParam("scopeId", scope.getId()), UPDATE_SCOPE).getBody().as(RegScopeVO.class);
    }

    /**
     * Smazání třídy. Třída nesmí být napojena na rejstříkové heslo.
     *
     * @param id id třídy.
     */
    protected Response deleteScope(final int id) {
        return delete(spec -> spec.queryParam("scopeId", id), DELETE_SCOPE);
    }

    /**
     * Pokud je nastavená verze, vrací třídy napojené na verzi, jinak vrací třídy nastavené v konfiguraci elzy (YAML).
     *
     * @param versionId id verze nebo null
     * @return seznam tříd
     */
    protected Response getScopeIdsByVersion(@Nullable final Integer versionId) {
        return versionId == null ? get(FA_SCOPES) : get(spec -> spec.queryParameter("versionId", versionId), FA_SCOPES);
    }

    /**
     * Vrací všechny třídy rejstříků z databáze.
     */
    protected List<RegScopeVO> getAllScopes() {
        return Arrays.asList(get(ALL_SCOPES).getBody().as(RegScopeVO[].class));
    }

    /**
     * Vrátí seznam typů rejstříku (typů hesel).
     *
     * @return seznam typů rejstříku (typů hesel)
     */
    protected List<RegRegisterTypeVO> getRecordTypes() {
        return Arrays.asList(get(RECORD_TYPES).getBody().as(RegRegisterTypeVO[].class));
    }


    /**
     * Vrátí jedno heslo (s variantními hesly) dle id.
     *
     * @param recordId id požadovaného hesla
     */
    protected RegRecordVO getRecord(final int recordId) {
        return get(spec -> spec.queryParameter("recordId", recordId), GET_RECORD).getBody().as(RegRecordVO.class);
    }

    /**
     * Vytvoření rejstříkového hesla.
     *
     * @param record VO rejstříkové heslo
     */
    protected RegRecordVO createRecord(final RegRecordVO record) {
        return put(spec -> spec.body(record), CREATE_RECORD).getBody().as(RegRecordVO.class);
    }

    /**
     * Aktualizace rejstříkového hesla.
     *
     * @param record VO rejstříkové heslo
     */
    protected RegRecordVO updateRecord(final RegRecordVO record) {
        return put(spec -> spec.body(record), UPDATE_RECORD).getBody().as(RegRecordVO.class);
    }

    /**
     * Smazání rejstříkového hesla.
     *
     * @param recordId id rejstříkového hesla
     */
    protected Response deleteRecord(final Integer recordId) {
        return delete(spec -> spec.queryParam("recordId", recordId), DELETE_RECORD);
    }

    /**
     * Vyhledávání v RegRecord
     *
     * @param search
     * @param from
     * @param count
     * @param registerTypeId
     * @param parentRecordId
     * @param versionId
     * @return List nalezených záznamů
     */
    protected List<RegRecordVO> findRecord(final String search,
                                           final Integer from, final Integer count,
                                           final Integer registerTypeId,
                                           final Integer parentRecordId,
                                           final Integer versionId) {
        HashMap<String, Object> params = new HashMap<>();

        if (search != null) {
            params.put("search", search);
        }
        if (versionId != null) {
            params.put("versionId", versionId);
        }
        if (parentRecordId != null) {
            params.put("parentRecordId", parentRecordId);
        }
        if (registerTypeId != null) {
            params.put("registerTypeId", registerTypeId);
        }
        params.put("from", from != null ? from : 0);
        params.put("count", count != null ? count : 20);

        return get(spec -> spec.queryParameters(params), FIND_RECORD).getBody().as(
                RegRecordWithCount.class).getRecordList();
    }


    /**
     * Vytvoření variantního hesla
     *
     * @param recordVO VO objektu k vytvoření
     * @return VO
     */
    protected RegVariantRecordVO createVariantRecord(final RegVariantRecordVO recordVO) {
        return put(spec -> spec.body(recordVO), CREATE_VARIANT_RECORD).getBody().as(RegVariantRecordVO.class);
    }

    /**
     * Úprava variantního hesla
     *
     * @param recordVO VO objektu k vytvoření
     * @return VO
     */
    protected RegVariantRecordVO updateVariantRecord(final RegVariantRecordVO recordVO) {
        return put(spec -> spec.body(recordVO), UPDATE_VARIANT_RECORD).getBody().as(RegVariantRecordVO.class);
    }

    /**
     * Smazání variantního hesla
     *
     * @param id variantního hesla
     * @return response
     */
    protected Response deleteVariantRecord(final int id) {
        return delete(spec -> spec.queryParam("variantRecordId", id), DELETE_VARIANT_RECORD);
    }

    /**
     * Vytvoření party
     *
     * @param partyVO Party VO
     * @return VO vytvořené party
     */
    protected ParPartyVO insertParty(final ParPartyVO partyVO) {
        return post(spec -> spec.body(partyVO), INSERT_PARTY).getBody().as(ParPartyVO.class);
    }

    protected ParPartyVO updateParty(final ParPartyVO partyVO) {
        return put(spec -> spec.body(partyVO).pathParam("partyId", partyVO.getPartyId()), UPDATE_PARTY).getBody().as(ParPartyVO.class);
    }

    /**
     * Získání osoby
     *
     * @param partyId id osoby
     * @return získaná osoba
     */
    protected ParPartyVO getParty(final int partyId) {
        return get(spec -> spec.queryParam("partyId", partyId), GET_PARTY).getBody().as(ParPartyVO.class);
    }

    /**
     * Odstranení osoby
     *
     * @param partyId id osoby
     * @return response
     */
    protected Response deleteParty(final int partyId) {
        return delete(spec -> spec.queryParam("partyId", partyId), DELETE_PARTY);
    }

    /**
     * Typy osob
     *
     * @return Typy osob
     */
    protected List<ParPartyTypeVO> getPartyTypes() {
        return Arrays.asList(get(GET_PARTY_TYPES).getBody().as(ParPartyTypeVO[].class));
    }

    /**
     * Typy názvů osob
     *
     * @return Typy názvů osob
     */
    protected List<ParPartyNameFormTypeVO> getPartyNameFormTypes() {
        return Arrays.asList(get(GET_PARTY_NAME_FORM_TYPES).getBody().as(ParPartyNameFormTypeVO[].class));
    }

    /**
     * Typy rejstříků pro daný typ osob
     *
     * @param partyTypeId
     * @return typy rejstříků
     */
    protected List<RegRegisterTypeVO> recordTypesForPartyType(final int partyTypeId) {
        return Arrays
                .asList(get(spec -> spec.queryParam("partyTypeId", partyTypeId), RECORD_TYPES_FOR_PARTY_TYPE).getBody()
                        .as(RegRegisterTypeVO[].class));
    }

    /**
     * Scopy
     *
     * @return list scope
     */
    protected List<RegScopeVO> faScopes() {
        return Arrays.asList(get(FA_SCOPES).getBody().as(RegScopeVO[].class));
    }


    /**
     * Vyhledávání v Party
     *
     * @param search
     * @param from
     * @param count
     * @param partyTypeId
     * @param versionId
     * @return List nalezených záznamů
     */
    protected List<ParPartyVO> findParty(final String search,
                                         final Integer from, final Integer count,
                                         final Integer partyTypeId,
                                         final Integer versionId) {
        HashMap<String, Object> params = new HashMap<>();

        if (search != null) {
            params.put("search", search);
        }
        if (versionId != null) {
            params.put("versionId", versionId);
        }
        if (partyTypeId != null) {
            params.put("partyTypeId", partyTypeId);
        }
        params.put("from", from != null ? from : 0);
        params.put("count", count != null ? count : 20);

        return get(spec -> spec.queryParameters(params), FIND_PARTY).getBody().as(ParPartyWithCount.class).getRecordList();
    }

    /**
     * Vytvoření relace
     *
     * @param relationVO relace k vytvoření
     * @return vytvořená relace
     */
    protected ParRelationVO insertRelation(ParRelationVO relationVO) {
        return post(spec -> spec.body(relationVO), CREATE_RELATIONS).getBody().as(ParRelationVO.class);
    }

    /**
     * Upravení relace
     *
     * @param relationVO relace k vytvoření
     * @return vytvořená relace
     */
    protected ParRelationVO updateRelation(ParRelationVO relationVO) {
        return put(spec -> spec.body(relationVO).pathParam("relationId", relationVO.getRelationId()), UPDATE_RELATIONS).getBody().as(ParRelationVO.class);
    }

    /**
     * Smazání relace
     *
     * @param relationId id relace ke smazání
     * @return response
     */
    protected Response deleteRelation(final int relationId) {
        return delete(spec -> spec.pathParam("relationId", relationId), DELETE_RELATIONS);
    }

    /**
     * Vyhledání osob podle osoby
     *
     * @param partyId     id osoby
     * @param search      název / jméno osoby
     * @param from        od
     * @param count       počet
     * @param partyTypeId typ osoby
     * @param versionId   verze
     * @return List osob
     */
    protected List<ParPartyVO> findPartyForParty(final Integer partyId,
                                                 final String search,
                                                 final Integer from, final Integer count,
                                                 final Integer partyTypeId,
                                                 final Integer versionId) {
        HashMap<String, Object> params = new HashMap<>();

        if (search != null) {
            params.put("search", search);
        }
        if (versionId != null) {
            params.put("versionId", versionId);
        }
        if (partyTypeId != null) {
            params.put("partyTypeId", partyTypeId);
        }
        params.put("partyId", partyId);
        params.put("from", from != null ? from : 0);
        params.put("count", count != null ? count : 20);

        return get(spec -> spec.queryParameters(params), FIND_PARTY_FOR_PARTY).getBody().as(ParPartyWithCount.class).getRecordList();
    }

    /**
     * Vyhledání rejstříkových hesel dle relace
     *
     * @param search     hledaný řetězec
     * @param from       odkud se mají vracet výsledka
     * @param count      počet vracených výsledků
     * @param roleTypeId id typu vztahu
     * @param partyId    id osoby, ze které je načtena hledaná třída rejstříku
     * @return list rejstříkových hesel
     */
    protected List<RegRecordVO> findRecordForRelation(final String search,
                                                      final Integer from, final Integer count,
                                                      final Integer roleTypeId,
                                                      final Integer partyId) {
        HashMap<String, Object> params = new HashMap<>();

        if (search != null) {
            params.put("search", search);
        }
        params.put("partyId", partyId);
        params.put("roleTypeId", roleTypeId);
        params.put("from", from != null ? from : 0);
        params.put("count", count != null ? count : 20);
        return get(spec -> spec.queryParams(params), FIND_RECORD_FOR_RELATION).getBody().as(RegRecordWithCount.class).getRecordList();
    }

    protected Response importXmlFile(final String transformationName,
                                     final Boolean stopOnError,
                                     final XmlImportType type,
                                     final String scopeName,
                                     final Integer scopeId,
                                     final File xmlFile) {
        HashMap<String, Object> params = new HashMap<>();

        if (transformationName != null) {
            params.put("transformationName", transformationName);
        }
        if (stopOnError != null) {
            params.put("stopOnError", stopOnError);
        }
        if (type != null) {
            params.put("importDataFormat", type);
        }
        if (scopeName != null) {
            params.put("scopeName", scopeName);
        }
        if (scopeId != null) {
            params.put("scopeId", scopeId);
        }
        return multipart(spec -> spec.multiPart("xmlFile", xmlFile).params(params), XML_IMPORT);
    }


    /**
     * Nahrazení textu v hodnotách textových atributů.
     * @param versionId id verze stromu
     * @param descItemTypeId typ atributu
     * @param searchText hledaný text v atributu
     * @param replaceText text, který nahradí hledaný text v celém textu
     * @param nodes seznam uzlů, ve kterých hledáme
     */
    protected void replaceDataValues(final Integer versionId,
                                     final Integer descItemTypeId,
                                     final String searchText,
                                     final String replaceText,
                                     final ArrangementController.ReplaceDataBody replaceDataBody) {

        put(spec -> spec
                .pathParameter("versionId", versionId)
                .queryParameter("descItemTypeId", descItemTypeId)
                .queryParameter("searchText", searchText)
                .queryParameter("replaceText", replaceText)
                .body(replaceDataBody), REPLACE_DATA_VALUES);

    }

    /**
     * Získání unikátních hodnot atributů podle typu.
     *
     * @param versionId      verze stromu
     * @param descItemTypeId typ atributu
     * @param fulltext       fultextové hledání
     * @param specIds        id specifikací
     * @return seznam unikátních hodnot
     */
    protected List<String> filterUniqueValues(final Integer versionId,
                                              final Integer descItemTypeId,
                                              final String fulltext,
                                              final Set<Integer> specIds) {
        Response result;
        if (CollectionUtils.isEmpty(specIds)) {
            result = put(spec -> spec
                    .pathParameter("versionId", versionId)
                    .queryParameter("descItemTypeId", descItemTypeId)
                    .queryParameter("fulltext", fulltext)
                    .queryParameter("max", 200), FILTER_UNIQUE_VALUES);
        } else {
            result = put(spec -> spec
                    .pathParameter("versionId", versionId)
                    .queryParameter("descItemTypeId", descItemTypeId)
                    .queryParameter("fulltext", fulltext)
                    .queryParameter("max", 200)
                    .body(specIds), FILTER_UNIQUE_VALUES);
        }

        return result.as(List.class);
    }
}