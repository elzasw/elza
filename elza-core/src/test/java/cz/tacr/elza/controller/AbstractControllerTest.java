package cz.tacr.elza.controller;

import static com.jayway.restassured.RestAssured.given;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;

import cz.tacr.elza.controller.vo.CopyNodesParams;
import cz.tacr.elza.controller.vo.CopyNodesValidate;
import cz.tacr.elza.controller.vo.CopyNodesValidateResult;
import cz.tacr.elza.service.vo.ChangesResult;
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
import com.jayway.restassured.internal.support.Prettifier;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ResponseBody;
import com.jayway.restassured.response.ResponseOptions;
import com.jayway.restassured.specification.RequestSpecification;

import cz.tacr.elza.AbstractTest;
import cz.tacr.elza.controller.ArrangementController.FaFilteredFulltextParam;
import cz.tacr.elza.controller.vo.ArrCalendarTypeVO;
import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.ArrNodeRegisterVO;
import cz.tacr.elza.controller.vo.ArrOutputDefinitionVO;
import cz.tacr.elza.controller.vo.ArrOutputExtVO;
import cz.tacr.elza.controller.vo.ArrOutputVO;
import cz.tacr.elza.controller.vo.ArrPacketVO;
import cz.tacr.elza.controller.vo.FilterNode;
import cz.tacr.elza.controller.vo.FilterNodePosition;
import cz.tacr.elza.controller.vo.FilteredResultVO;
import cz.tacr.elza.controller.vo.FundListCountResult;
import cz.tacr.elza.controller.vo.NodeItemWithParent;
import cz.tacr.elza.controller.vo.ParInstitutionVO;
import cz.tacr.elza.controller.vo.ParPartyNameFormTypeVO;
import cz.tacr.elza.controller.vo.ParPartyTypeVO;
import cz.tacr.elza.controller.vo.ParPartyVO;
import cz.tacr.elza.controller.vo.ParRelationVO;
import cz.tacr.elza.controller.vo.RegCoordinatesVO;
import cz.tacr.elza.controller.vo.RegRecordVO;
import cz.tacr.elza.controller.vo.RegRegisterTypeVO;
import cz.tacr.elza.controller.vo.RegScopeVO;
import cz.tacr.elza.controller.vo.RegVariantRecordVO;
import cz.tacr.elza.controller.vo.RulDataTypeVO;
import cz.tacr.elza.controller.vo.RulDescItemSpecVO;
import cz.tacr.elza.controller.vo.RulDescItemTypeVO;
import cz.tacr.elza.controller.vo.RulOutputTypeVO;
import cz.tacr.elza.controller.vo.RulPacketTypeVO;
import cz.tacr.elza.controller.vo.RulPolicyTypeVO;
import cz.tacr.elza.controller.vo.RulRuleSetVO;
import cz.tacr.elza.controller.vo.RulTemplateVO;
import cz.tacr.elza.controller.vo.ScenarioOfNewLevelVO;
import cz.tacr.elza.controller.vo.SysExternalSystemVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNodeClient;
import cz.tacr.elza.controller.vo.UserInfoVO;
import cz.tacr.elza.controller.vo.UsrGroupVO;
import cz.tacr.elza.controller.vo.UsrUserVO;
import cz.tacr.elza.controller.vo.ValidationResult;
import cz.tacr.elza.controller.vo.filter.Filters;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemSpecExtVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemCoordinatesVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemDecimalVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemEnumVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemFormattedTextVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemIntVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemJsonTableVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemPacketVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemPartyRefVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemRecordRefVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemStringVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemTextVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemUnitdateVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemUnitidVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.service.ArrMoveLevelService;


public abstract class AbstractControllerTest extends AbstractTest {

    protected static boolean loadInstitutions = true;

    private static final RestAssuredConfig UTF8_ENCODER_CONFIG = RestAssuredConfig.newConfig().encoderConfig(
            EncoderConfig.encoderConfig().defaultContentCharset("UTF-8"));

    private static final Logger logger = LoggerFactory.getLogger(AbstractControllerTest.class);
    protected static final String CONTENT_TYPE_HEADER = "content-type";
    protected static final String JSON_CONTENT_TYPE = "application/json";
    protected static final String WWW_FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final Header JSON_CT_HEADER = new Header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE);
    private static final Header WWW_FORM_CT_HEADER = new Header(CONTENT_TYPE_HEADER, WWW_FORM_CONTENT_TYPE);
    private static final Header MULTIPART_HEADER = new Header(CONTENT_TYPE_HEADER, MediaType.MULTIPART_FORM_DATA_VALUE);

    protected static final String ADMIN_CONTROLLER_URL = "/api/admin";
    protected static final String ARRANGEMENT_CONTROLLER_URL = "/api/arrangement";
    protected static final String BULK_ACTION_CONTROLLER_URL = "/api/action";
    protected static final String PARTY_CONTROLLER_URL = "/api/party";
    protected static final String REGISTRY_CONTROLLER_URL = "/api/registry";
    protected static final String KML_CONTROLLER_URL = "/api/kml";
    protected static final String VALIDATION_CONTROLLER_URL = "/api/validate";
    protected static final String RULE_CONTROLLER_URL = "/api/rule";
    protected static final String XML_IMPORT_CONTROLLER_URL = "/api/import";
    protected static final String USER_CONTROLLER_URL = "/api/user";

    // ADMIN
    protected static final String REINDEX = ADMIN_CONTROLLER_URL + "/reindex";
    protected static final String REINDEX_STATUS = ADMIN_CONTROLLER_URL + "/reindexStatus";
    protected static final String CACHE_RESET = ADMIN_CONTROLLER_URL + "/cache/reset";
    protected static final String EXTERNAL_SYSTEMS = ADMIN_CONTROLLER_URL + "/externalSystems";
    protected static final String FIND_EXTERNAL_SYSTEMS = EXTERNAL_SYSTEMS;
    protected static final String CREATE_EXTERNAL_SYSTEM = EXTERNAL_SYSTEMS;
    protected static final String UPDATE_EXTERNAL_SYSTEM = EXTERNAL_SYSTEMS + "/{externalSystemId}";
    protected static final String DELETE_EXTERNAL_SYSTEM = EXTERNAL_SYSTEMS + "/{externalSystemId}";

    // ARRANGEMENT
    protected static final String CREATE_FUND = ARRANGEMENT_CONTROLLER_URL + "/funds";
    protected static final String UPDATE_FUND = ARRANGEMENT_CONTROLLER_URL + "/updateFund";
    protected static final String FUND = ARRANGEMENT_CONTROLLER_URL + "/getFund/{fundId}";
    protected static final String FUNDS = ARRANGEMENT_CONTROLLER_URL + "/getFunds";
    protected static final String APPROVE_VERSION = ARRANGEMENT_CONTROLLER_URL + "/approveVersion";
    protected static final String ADD_LEVEL = ARRANGEMENT_CONTROLLER_URL + "/levels";
    protected static final String DELETE_LEVEL = ARRANGEMENT_CONTROLLER_URL + "/levels";
    protected static final String DELETE_FUND = ARRANGEMENT_CONTROLLER_URL + "/deleteFund/{fundId}";
    protected static final String SCENARIOS = ARRANGEMENT_CONTROLLER_URL + "/scenarios";
    protected static final String CALENDAR_TYPES = ARRANGEMENT_CONTROLLER_URL + "/calendarTypes";
    protected static final String FA_TREE = ARRANGEMENT_CONTROLLER_URL + "/fundTree";
    protected static final String MOVE_LEVEL_AFTER = ARRANGEMENT_CONTROLLER_URL + "/moveLevelAfter";
    protected static final String MOVE_LEVEL_BEFORE = ARRANGEMENT_CONTROLLER_URL + "/moveLevelBefore";
    protected static final String MOVE_LEVEL_UNDER = ARRANGEMENT_CONTROLLER_URL + "/moveLevelUnder";
    protected static final String CREATE_DESC_ITEM = ARRANGEMENT_CONTROLLER_URL
            + "/descItems/{fundVersionId}/{nodeId}/{nodeVersion}/{descItemTypeId}/create";
    protected static final String DESC_ITEM_CSV_IMPORT = ARRANGEMENT_CONTROLLER_URL
            + "/descItems/{fundVersionId}/csv/import";
    protected static final String DESC_ITEM_CSV_EXPORT = ARRANGEMENT_CONTROLLER_URL
            + "/descItems/{fundVersionId}/csv/export";
    protected static final String UPDATE_DESC_ITEM = ARRANGEMENT_CONTROLLER_URL
            + "/descItems/{fundVersionId}/{nodeVersion}/update/{createNewVersion}";
    protected static final String DELETE_DESC_ITEM = ARRANGEMENT_CONTROLLER_URL
            + "/descItems/{fundVersionId}/{nodeVersion}/delete";
    protected static final String DELETE_DESC_ITEM_BY_TYPE = ARRANGEMENT_CONTROLLER_URL
            + "/descItems/{fundVersionId}/{nodeId}/{nodeVersion}/{descItemTypeId}";
    protected static final String DELETE_OUTPUT_ITEM_BY_TYPE = ARRANGEMENT_CONTROLLER_URL
            + "/outputItems/{fundVersionId}/{outputDefinitionId}/{outputDefinitionVersion}/{itemTypeId}";
    protected static final String CREATE_OUTPUT_ITEM = ARRANGEMENT_CONTROLLER_URL
            + "/outputItems/{fundVersionId}/{outputDefinitionId}/{outputDefinitionVersion}/{itemTypeId}/create";
    protected static final String UPDATE_OUTPUT_ITEM = ARRANGEMENT_CONTROLLER_URL
            + "/outputItems/{fundVersionId}/{outputDefinitionVersion}/update/{createNewVersion}";
    protected static final String DELETE_OUTPUT_ITEM = ARRANGEMENT_CONTROLLER_URL
            + "/outputItems/{fundVersionId}/{outputDefinitionVersion}/delete";
    protected static final String PACKET_TYPES = ARRANGEMENT_CONTROLLER_URL + "/packets/types";
    protected static final String FIND_FORM_PACKETS = ARRANGEMENT_CONTROLLER_URL + "/packets/{fundId}/find/form";
    protected static final String FIND_PACKETS = ARRANGEMENT_CONTROLLER_URL + "/packets/{fundId}/find";
    protected static final String INSERT_PACKET = ARRANGEMENT_CONTROLLER_URL + "/packets/{fundId}";
    protected static final String DELETE_PACKETS = ARRANGEMENT_CONTROLLER_URL + "/packets/{fundId}";
    protected static final String UPDATE_PACKET = ARRANGEMENT_CONTROLLER_URL + "/packets/{fundId}";
    protected static final String SET_STATE_PACKETS = ARRANGEMENT_CONTROLLER_URL + "/packets/{fundId}";
    protected static final String GENERATE_PACKETS = ARRANGEMENT_CONTROLLER_URL + "/packets/{fundId}/generate";
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
    protected static final String OUTPUT_FORM_DATA = ARRANGEMENT_CONTROLLER_URL + "/output/{outputDefinitionId}/{versionId}/form";
    protected static final String NODE_FORMS_DATA = ARRANGEMENT_CONTROLLER_URL + "/nodes/{versionId}/forms";
    protected static final String NODE_FORMS_DATA_AROUND = ARRANGEMENT_CONTROLLER_URL + "/nodes/{versionId}/{nodeId}/{around}/forms";
    protected static final String NODES = ARRANGEMENT_CONTROLLER_URL + "/nodes";
    protected static final String COPY_SIBLING = ARRANGEMENT_CONTROLLER_URL + "/copyOlderSiblingAttribute";
    protected static final String VERSIONS = ARRANGEMENT_CONTROLLER_URL + "/getVersions";
    protected static final String REPLACE_DATA_VALUES = ARRANGEMENT_CONTROLLER_URL + "/replaceDataValues/{versionId}";
    protected static final String PLACE_DATA_VALUES = ARRANGEMENT_CONTROLLER_URL + "/placeDataValues/{versionId}";
    protected static final String DELETE_DATA_VALUES = ARRANGEMENT_CONTROLLER_URL + "/deleteDataValues/{versionId}";
    protected static final String FILTER_UNIQUE_VALUES = ARRANGEMENT_CONTROLLER_URL + "/filterUniqueValues/{versionId}";
    protected static final String OUTPUTS = ARRANGEMENT_CONTROLLER_URL + "/output";
    protected static final String OUTPUT_TYPES = OUTPUTS + "/types/{versionId}";
    protected static final String GET_OUTPUTS = OUTPUTS + "/{fundVersionId}";
    protected static final String GET_OUTPUT = OUTPUTS + "/{fundVersionId}/{outputId}";
    protected static final String CREATE_NAMED_OUTPUT = OUTPUTS + "/{fundVersionId}";
    protected static final String OUTPUT_LOCK = OUTPUTS + "/{fundVersionId}/{outputId}/lock";
    protected static final String ADD_NODES_NAMED_OUTPUT = OUTPUTS + "/{fundVersionId}/{outputId}/add";
    protected static final String REMOVE_NODES_NAMED_OUTPUT = OUTPUTS + "/{fundVersionId}/{outputId}/remove";
    protected static final String DELETE_NAMED_OUTPUT = OUTPUTS + "/{fundVersionId}/{outputId}";
    protected static final String UPDATE_NAMED_OUTPUT = OUTPUTS + "/{fundVersionId}/{outputId}/update";
    protected static final String FILTER_NODES = ARRANGEMENT_CONTROLLER_URL + "/filterNodes/{versionId}";
    protected static final String FILTERED_NODES = ARRANGEMENT_CONTROLLER_URL + "/getFilterNodes/{versionId}";
    protected static final String FILTERED_FULLTEXT_NODES = ARRANGEMENT_CONTROLLER_URL + "/getFilteredFulltext/{versionId}";
    protected static final String FUND_POLICY = ARRANGEMENT_CONTROLLER_URL + "/fund/policy/{fundVersionId}";
    protected static final String VALIDATION = ARRANGEMENT_CONTROLLER_URL + "/validation/{fundVersionId}/{fromIndex}/{toIndex}";
    protected static final String VALIDATION_ERROR = ARRANGEMENT_CONTROLLER_URL + "/validation/{fundVersionId}/find/{nodeId}/{direction}";
    protected static final String FIND_CHANGE = ARRANGEMENT_CONTROLLER_URL + "/changes/{fundVersionId}";
    protected static final String FIND_CHANGE_BY_DATE = ARRANGEMENT_CONTROLLER_URL + "/changes/{fundVersionId}/date";
    protected static final String REVERT_CHANGES = ARRANGEMENT_CONTROLLER_URL + "/changes/{fundVersionId}/revert";
    protected static final String SET_NOT_IDENTIFIED_DESCITEM = ARRANGEMENT_CONTROLLER_URL + "/descItems/{fundVersionId}/{nodeId}/{nodeVersion}/notUndefined/set";
    protected static final String UNSET_NOT_IDENTIFIED_DESCITEM = ARRANGEMENT_CONTROLLER_URL + "/descItems/{fundVersionId}/{nodeId}/{nodeVersion}/notUndefined/unset";
    protected static final String SET_NOT_IDENTIFIED_OUTPUTITEM = ARRANGEMENT_CONTROLLER_URL + "/outputItems/{fundVersionId}/{outputDefinitionId}/{outputDefinitionVersion}/notUndefined/set";
    protected static final String UNSET_NOT_IDENTIFIED_OUTPUTITEM = ARRANGEMENT_CONTROLLER_URL + "/outputItems/{fundVersionId}/{outputDefinitionId}/{outputDefinitionVersion}/notUndefined/unset";
    protected static final String COPY_LEVELS_VALIDATE = ARRANGEMENT_CONTROLLER_URL + "/levels/copy/validate";
    protected static final String COPY_LEVELS = ARRANGEMENT_CONTROLLER_URL + "/levels/copy";

    // Party
    protected static final String CREATE_RELATIONS = PARTY_CONTROLLER_URL + "/relation";
    protected static final String UPDATE_RELATIONS = PARTY_CONTROLLER_URL + "/relation/{relationId}";
    protected static final String DELETE_RELATIONS = PARTY_CONTROLLER_URL + "/relation/{relationId}";
    protected static final String FIND_PARTY = PARTY_CONTROLLER_URL + "/";
    protected static final String FIND_PARTY_FOR_PARTY = PARTY_CONTROLLER_URL + "/findPartyForParty";
    protected static final String GET_PARTY = PARTY_CONTROLLER_URL + "/{partyId}";
    protected static final String GET_PARTY_TYPES = PARTY_CONTROLLER_URL + "/partyTypes";
    protected static final String GET_PARTY_NAME_FORM_TYPES = PARTY_CONTROLLER_URL + "/partyNameFormTypes";
    protected static final String INSERT_PARTY = PARTY_CONTROLLER_URL + "/";
    protected static final String UPDATE_PARTY = PARTY_CONTROLLER_URL + "/{partyId}";
    protected static final String DELETE_PARTY = PARTY_CONTROLLER_URL + "/{partyId}";

    protected static final String INSTITUTIONS = PARTY_CONTROLLER_URL + "/institutions";

    // REGISTRY
    protected static final String DEFAULT_SCOPES = REGISTRY_CONTROLLER_URL + "/defaultScopes";
    protected static final String CREATE_SCOPE = REGISTRY_CONTROLLER_URL + "/scopes";
    protected static final String UPDATE_SCOPE = REGISTRY_CONTROLLER_URL + "/scopes/{scopeId}";
    protected static final String DELETE_SCOPE = REGISTRY_CONTROLLER_URL + "/scopes/{scopeId}";
    protected static final String FA_SCOPES = REGISTRY_CONTROLLER_URL + "/fundScopes";
    protected static final String ALL_SCOPES = REGISTRY_CONTROLLER_URL + "/scopes";
    protected static final String RECORD_TYPES = REGISTRY_CONTROLLER_URL + "/recordTypes";

    protected static final String FIND_RECORD = REGISTRY_CONTROLLER_URL + "/";
    protected static final String FIND_RECORD_FOR_RELATION = REGISTRY_CONTROLLER_URL + "/findRecordForRelation";
    protected static final String GET_RECORD = REGISTRY_CONTROLLER_URL + "/{recordId}";
    protected static final String CREATE_RECORD = REGISTRY_CONTROLLER_URL + "/";
    protected static final String UPDATE_RECORD = REGISTRY_CONTROLLER_URL + "/{recordId}";
    protected static final String DELETE_RECORD = REGISTRY_CONTROLLER_URL + "/{recordId}";


    protected static final String CREATE_REG_COORDINATES = REGISTRY_CONTROLLER_URL + "/regCoordinates/";
    protected static final String UPDATE_REG_COORDINATES = REGISTRY_CONTROLLER_URL + "/regCoordinates/{coordinatesId}";
    protected static final String DELETE_REG_COORDINATES = REGISTRY_CONTROLLER_URL + "/regCoordinates/{coordinatesId}";

    protected static final String CREATE_VARIANT_RECORD = REGISTRY_CONTROLLER_URL + "/variantRecord/";
    protected static final String UPDATE_VARIANT_RECORD = REGISTRY_CONTROLLER_URL + "/variantRecord/{variantRecordId}";
    protected static final String DELETE_VARIANT_RECORD = REGISTRY_CONTROLLER_URL + "/variantRecord/{variantRecordId}";

    protected static final String RECORD_TYPES_FOR_PARTY_TYPE = REGISTRY_CONTROLLER_URL + "/recordTypesForPartyType";

    // RULE
    protected static final String RULE_SETS = RULE_CONTROLLER_URL + "/getRuleSets";
    protected static final String DATA_TYPES = RULE_CONTROLLER_URL + "/dataTypes";
    protected static final String DESC_ITEM_TYPES = RULE_CONTROLLER_URL + "/descItemTypes";
    protected static final String TEMPLATES = RULE_CONTROLLER_URL + "/templates";
    protected static final String PACKAGES = RULE_CONTROLLER_URL + "/getPackages";
    protected static final String POLICY = RULE_CONTROLLER_URL + "/policy";
    protected static final String POLICY_TYPES = POLICY + "/types/{fundVersionId}";
    protected static final String POLICY_ALL_TYPES = POLICY + "/types";
    protected static final String POLICY_SET = POLICY + "/{nodeId}/{fundVersionId}";
    protected static final String POLICY_GET = POLICY + "/{nodeId}/{fundVersionId}/{includeParents}";

    // Validation
    protected static final String VALIDATE_UNIT_DATE = VALIDATION_CONTROLLER_URL + "/unitDate";

    // XmlImport
    protected final static String XML_IMPORT = XML_IMPORT_CONTROLLER_URL + "/import";

    // Uživatelé a skupiny
    protected final static String USER_DETAIL = USER_CONTROLLER_URL + "/detail";
    protected final static String CHANGE_PASSWORD = USER_CONTROLLER_URL + "/{userId}/password";
    protected final static String CHANGE_PASSWORD_USER = USER_CONTROLLER_URL + "/password";
    protected final static String FIND_USER = USER_CONTROLLER_URL;
    protected final static String GET_USER = USER_CONTROLLER_URL + "/{userId}";
    protected final static String ACTIVE_USER = USER_CONTROLLER_URL + "/{userId}/active/{active}";
    protected final static String CREATE_USER = USER_CONTROLLER_URL;
    protected final static String FIND_GROUP = USER_CONTROLLER_URL + "/group";
    protected final static String GET_GROUP = USER_CONTROLLER_URL + "/group/{groupId}";
    protected final static String CREATE_GROUP = USER_CONTROLLER_URL + "/group";
    protected final static String DELETE_GROUP = USER_CONTROLLER_URL + "/group/{groupId}";
    protected final static String CHANGE_GROUP = USER_CONTROLLER_URL + "/group/{groupId}";
    protected final static String CHANGE_GROUP_PERMISSION = USER_CONTROLLER_URL + "/group/{groupId}/permission";
    protected final static String JOIN_GROUP = USER_CONTROLLER_URL + "/group/join";
    protected final static String LEAVE_GROUP = USER_CONTROLLER_URL + "/group/{groupId}/leave/{userId}";
    protected final static String CHANGE_USER_PERMISSION = USER_CONTROLLER_URL + "/{userId}/permission";

    protected final static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.00");

    @Value("${local.server.port}")
    private int port;

    private List<RulDataTypeVO> dataTypes = null;
    private List<RulDescItemTypeExtVO> descItemTypes = null;

    // Import institucí
    private final static String XML_INSTITUTION = "institution-import.xml";

    // Výchozí scope
    private final static String IMPORT_SCOPE = "GLOBAL";

    private static Map<String, String> cookies = null;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        RestAssured.port = port;                        // nastavi default port pro REST-assured
        RestAssured.baseURI = RestAssured.DEFAULT_URI;  // nastavi default URI pro REST-assured. Nejcasteni localhost
        login();
        if (loadInstitutions) {
            importXmlFile(null, 1, XmlImportControllerTest.getResourceFile(XML_INSTITUTION));
        }
    }

    protected void login() {
        RequestSpecification requestSpecification = given();
        requestSpecification.formParam("username", "admin");
        requestSpecification.formParam("password", "admin");
        requestSpecification.header(WWW_FORM_CT_HEADER).log().all().config(UTF8_ENCODER_CONFIG);
        Response response = requestSpecification.post("/login");
        cookies = response.getCookies();
    }

    public static Response delete(final Function<RequestSpecification, RequestSpecification> params, final String url) {
        return httpMethod(params, url, HttpMethod.DELETE, HttpStatus.OK);
    }

    public static Response post(final Function<RequestSpecification, RequestSpecification> params,
                                final String url,
                                final HttpStatus status) {
        return httpMethod(params, url, HttpMethod.POST, status);
    }

    public static Response post(final Function<RequestSpecification, RequestSpecification> params, final String url) {
        return httpMethod(params, url, HttpMethod.POST, HttpStatus.OK);
    }

    public static Response put(final Function<RequestSpecification, RequestSpecification> params, final String url) {
        return httpMethod(params, url, HttpMethod.PUT, HttpStatus.OK);
    }

    public static Response putError(final Function<RequestSpecification, RequestSpecification> params, final String url) {
        return httpMethod(params, url, HttpMethod.PUT, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static Response put(final Function<RequestSpecification, RequestSpecification> params,
                               final String url,
                               final HttpStatus status) {
        return httpMethod(params, url, HttpMethod.PUT, status);
    }

    public static Response get(final Function<RequestSpecification, RequestSpecification> params, final String url) {
        return httpMethod(params, url, HttpMethod.GET, HttpStatus.OK);
    }

    public static Response getError(final Function<RequestSpecification, RequestSpecification> params, final String url) {
        return httpMethod(params, url, HttpMethod.GET, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static Response get(final String url) {
        return httpMethod((spec) -> spec, url, HttpMethod.GET, HttpStatus.OK);
    }

    public static Response httpMethod(final Function<RequestSpecification, RequestSpecification> params,
                                      final String url,
                                      final HttpMethod method,
                                      final HttpStatus status) {
        return httpMethod(params, url, method, status, JSON_CT_HEADER);
    }

    public static Response httpMethod(final Function<RequestSpecification, RequestSpecification> params,
                                      final String url,
                                      final HttpMethod method,
                                      final HttpStatus status,
                                      final Header header) {
        Assert.assertNotNull(params);
        Assert.assertNotNull(url);
        Assert.assertNotNull(method);

        RequestSpecification requestSpecification = params.apply(given());

        requestSpecification.header(header).log().all().config(UTF8_ENCODER_CONFIG).cookies(cookies);

        Response response;
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
        
        logResponse(response);
        Assert.assertEquals(status.value(), response.statusCode());

        return response;
    }

    private static void logResponse(Response response) {
        String contentType = response.contentType();
        logger.info("Response status: " + response.statusLine() + ", content-type: " + contentType);
        // print body in some cases
        if(contentType!=null) {
        	if(contentType.startsWith(JSON_CONTENT_TYPE)||contentType.startsWith("text/")) {
        		
        		ResponseBody<?> responseBody = (ResponseBody<?>)response;
        		ResponseOptions<?> responseOptions = (ResponseOptions<?>)response;
        		String body = new Prettifier().getPrettifiedBodyIfPossible(responseOptions, responseBody);
        		logger.info("Response body:" + body);
        	}
        }		
	}

	/**
     * Multipart request
     *
     * @param params
     * @param url
     * @return
     */
    protected static Response multipart(final Function<RequestSpecification, RequestSpecification> params,
                                        final String url) {
        Assert.assertNotNull(params);
        Assert.assertNotNull(url);

        RequestSpecification requestSpecification = params.apply(given());

        requestSpecification.header(MULTIPART_HEADER).log().all().config(UTF8_ENCODER_CONFIG).cookies(cookies);

        Response response = requestSpecification.post(url);
        logResponse(response);
        
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
     * Získání seznamu pravidel.
     *
     * @return seznam pravidel
     * @param versionId verze AP
     */
    protected List<RulOutputTypeVO> getOutputTypes(final Integer versionId) {
        Response response = get(spec -> spec.pathParam("versionId", versionId), OUTPUT_TYPES);
        return Arrays.asList(response.getBody().as(RulOutputTypeVO[].class));
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
     * @param ruleSetId id pravidel otevřené verze
     * @return ap
     */
    protected ArrFundVO fundAid(final ArrFundVO fund, final Integer ruleSetId) {
        Response response = post(spec ->
                spec.queryParameter("ruleSetId", ruleSetId)
                        .body(fund), UPDATE_FUND);
        return response.getBody().as(ArrFundVO.class);
    }

    /**
     * Uzavření verze archivní pomůcky.
     *
     * @param fundVersion verze archivní pomůcky
     * @param dateRange
     * @return nová verze ap
     */
    protected ArrFundVersionVO approveVersion(final ArrFundVersionVO fundVersion,
                                              final String dateRange) {
        return approveVersion(fundVersion.getId(), dateRange);
    }

    /**
     * Uzavření verze archivní pomůcky.
     *
     * @param versionId         identifikátor verze archivní pomůcky
     * @param dateRange identifikátor výstupu
     * @return nová verze ap
     */
    protected ArrFundVersionVO approveVersion(final Integer versionId,
                                              final String dateRange) {
        Response response = put(spec -> spec
                .queryParameter("versionId", versionId)
                .queryParameter("dateRange", dateRange), APPROVE_VERSION);
        return response.getBody().as(ArrFundVersionVO.class);
    }

    /**
     * Vrátí archivní pomůcky s verzema.
     *
     * @return archivní pomůcky
     */
    protected List<ArrFundVO> getFunds() {
        Response response = get(spec -> spec.queryParameter("max", 200), FUNDS);
        FundListCountResult as = response.getBody().as(FundListCountResult.class);
        return as.getList();
    }

    /**
     * Vrátí archivní pomůcku.
     *
     * @return archivní pomůcka
     */
    protected ArrFundVO getFund(final Integer fundId) {
        Response response = get(spec -> spec.pathParameters("fundId", fundId), FUND);
        return response.getBody().as(ArrFundVO.class);
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

        Assert.assertNotNull(newLevel.getNode());
        Assert.assertNotNull(newLevel.getParentNode());

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
     * Smazání archivního souboru.
     * @param fundId id souboru
     */
    protected void deleteFund(final Integer fundId){
        delete(spec -> spec.pathParameters("fundId", fundId), DELETE_FUND);
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
     * Získání listu RulTemplateVO
     *
     * @return list RulTemplateVO
     */
    protected List<RulTemplateVO> getTemplates() {
        return Arrays.asList(get(TEMPLATES).getBody().as(RulTemplateVO[].class));
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
    protected ArrangementController.DescItemResult createDescItem(final ArrItemVO descItem,
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
    protected ArrangementController.DescItemResult createDescItem(final ArrItemVO descItem,
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

    protected ArrangementController.OutputItemResult createOutputItem(final ArrItemVO outputItemVO,
                                                                   final Integer fundVersionId,
                                                                   final Integer itemTypeId,
                                                                   final Integer outputDefinitionId,
                                                                   final Integer outputDefinitionVersion) {
        Response response = put(spec -> spec
                .body(outputItemVO)
                .pathParameter("fundVersionId", fundVersionId)
                .pathParameter("itemTypeId", itemTypeId)
                .pathParameter("outputDefinitionId", outputDefinitionId)
                .pathParameter("outputDefinitionVersion", outputDefinitionVersion), CREATE_OUTPUT_ITEM);
        return response.getBody().as(ArrangementController.OutputItemResult.class);
    }

    protected ArrangementController.OutputItemResult updateOutputItem(final ArrItemVO outputItemVO,
                                                                      final Integer fundVersionId,
                                                                      final Integer outputDefinitionVersion,
                                                                      final Boolean createNewVersion) {
        Response response = put(spec -> spec
                .body(outputItemVO)
                .pathParameter("fundVersionId", fundVersionId)
                .pathParameter("createNewVersion", createNewVersion)
                .pathParameter("outputDefinitionVersion", outputDefinitionVersion), UPDATE_OUTPUT_ITEM);
        return response.getBody().as(ArrangementController.OutputItemResult.class);
    }

    public ArrangementController.OutputItemResult deleteOutputItem(final ArrItemVO outputItemVO,
                                                                   final Integer fundVersionId,
                                                                   final Integer outputDefinitionVersion) {
        Response response = post(spec -> spec
                .body(outputItemVO)
                .pathParameter("fundVersionId", fundVersionId)
                .pathParameter("outputDefinitionVersion", outputDefinitionVersion), DELETE_OUTPUT_ITEM);
        return response.getBody().as(ArrangementController.OutputItemResult.class);
    }

    protected InputStream descItemCsvExport(
            final Integer fundVersionId,
            final Integer descItemObjectId) {

        Response response = get(spec ->
                spec
                .pathParameter("fundVersionId", fundVersionId)
                .param("descItemObjectId", descItemObjectId)
                ,DESC_ITEM_CSV_EXPORT);

        return response.getBody().asInputStream();
    }

    protected ArrangementController.DescItemResult descItemCsvImport(
            final Integer fundVersionId,
            final Integer nodeVersion,
            final Integer nodeId,
            final Integer descItemTypeId,
            final File importFile) {

        HashMap<String, Object> params = new HashMap<>();
        params.put("nodeVersion", nodeVersion);
        params.put("nodeId", nodeId);
        params.put("descItemTypeId", descItemTypeId);

        Response response = multipart(spec ->
                spec
                        .pathParameter("fundVersionId", fundVersionId)
                        .multiPart("file", importFile)
                        .params(params)
                , DESC_ITEM_CSV_IMPORT
        );
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
    protected ArrangementController.DescItemResult updateDescItem(final ArrItemVO descItem,
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
    protected ArrangementController.DescItemResult updateDescItem(final ArrItemVO descItem,
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
    protected ArrangementController.DescItemResult deleteDescItem(final ArrItemVO descItem,
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
    protected ArrangementController.DescItemResult deleteDescItem(final ArrItemVO descItem,
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
    protected ArrItemVO buildDescItem(final String typeCode,
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

        ArrItemVO descItem;

        switch (dataType.getCode()) {

            case "INT": {
                descItem = new ArrItemIntVO();
                ((ArrItemIntVO) descItem).setValue((Integer) value);
                break;
            }

            case "STRING": {
                descItem = new ArrItemStringVO();
                ((ArrItemStringVO) descItem).setValue((String) value);
                break;
            }

            case "TEXT": {
                descItem = new ArrItemTextVO();
                ((ArrItemTextVO) descItem).setValue((String) value);
                break;
            }

            case "UNITDATE": {
                descItem = new ArrItemUnitdateVO();
                ((ArrItemUnitdateVO) descItem).setValue((String) value);
                ((ArrItemUnitdateVO) descItem).setCalendarTypeId(getCalendarTypes().get(0).getId());
                break;
            }

            case "UNITID": {
                descItem = new ArrItemUnitidVO();
                ((ArrItemUnitidVO) descItem).setValue((String) value);
                break;
            }

            case "FORMATTED_TEXT": {
                descItem = new ArrItemFormattedTextVO();
                ((ArrItemFormattedTextVO) descItem).setValue((String) value);
                break;
            }

            case "COORDINATES": {
                descItem = new ArrItemCoordinatesVO();
                ((ArrItemCoordinatesVO) descItem).setValue((String) value);
                break;
            }

            case "PARTY_REF": {
                descItem = new ArrItemPartyRefVO();
                ((ArrItemPartyRefVO) descItem).setValue(((ParPartyVO) value).getId());
                break;
            }

            case "RECORD_REF": {
                descItem = new ArrItemRecordRefVO();
                ((ArrItemRecordRefVO) descItem).setValue(((RegRecordVO) value).getId());
                break;
            }

            case "DECIMAL": {
                descItem = new ArrItemDecimalVO();
                ((ArrItemDecimalVO) descItem).setValue((BigDecimal) value);
                break;
            }

            case "PACKET_REF": {
                descItem = new ArrItemPacketVO();
                ((ArrItemPacketVO) descItem).setValue(((ArrPacketVO) value).getId());
                break;
            }

            case "ENUM": {
                descItem = new ArrItemEnumVO();
                if (BooleanUtils.isNotTrue(type.getUseSpecification())) {
                    throw new IllegalStateException(
                            "Specifikace u typu musí být povinná pro ENUM -> CODE: " + type.getCode());
                }
                break;
            }

            case "JSON_TABLE": {
                descItem = new ArrItemJsonTableVO();
                ((ArrItemJsonTableVO) descItem).setValue(((ElzaTable) value));
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
    protected List<ArrPacketVO> findPackets(final ArrFundVO fund, final String prefix, final ArrPacket.State state) {
        ArrangementController.PacketFindParam input = new ArrangementController.PacketFindParam();
        input.setState(state);
        input.setPrefix(prefix);
        return findPackets(fund.getId(), input);
    }

    /**
     * Seznam obalů pro AP.
     *
     * @param fundId identifikátor AP
     * @return obaly pro AP
     */
    protected List<ArrPacketVO> findPackets(final Integer fundId, final ArrangementController.PacketFindParam input) {
        return Arrays.asList(post(spec ->
                spec.body(input).pathParameter("fundId", fundId), FIND_PACKETS).getBody().as(ArrPacketVO[].class));
    }

    /**
     * Seznam obalů pro JP.
     *
     * @param fund AP
     * @return obaly pro JP
     */
    protected List<ArrPacketVO> findFormPackets(final ArrFundVO fund, final String text, final Integer limit) {
        ArrangementController.PacketFindFormParam input = new ArrangementController.PacketFindFormParam();
        input.setLimit(limit);
        input.setText(text);
        return findFormPackets(fund.getId(), input);
    }

    /**
     * Seznam obalů pro JP.
     *
     * @param fundId identifikátor AP
     * @return obaly pro JP
     */
    protected List<ArrPacketVO> findFormPackets(final Integer fundId, final ArrangementController.PacketFindFormParam input) {
        return Arrays.asList(post(spec ->
                spec.body(input).pathParameter("fundId", fundId), FIND_FORM_PACKETS).getBody().as(ArrPacketVO[].class));
    }

    /**
     * Změna stavu obalů.
     *
     * @param fundId identifikátor fondu
     * @param input parametry změny
     * @return obaly pro JP
     */
    protected void setStatePackets(final Integer fundId, final ArrangementController.PacketSetStateParam input) {
        post(spec -> spec.body(input).pathParameter("fundId", fundId), SET_STATE_PACKETS);
    }

    /**
     * Změna stavu obalů.
     *
     * @param fund identifikátor AP
     * @param packets obaly
     * @param state nastavovaný stav
     * @return obaly pro JP
     */
    protected void setStatePackets(final ArrFundVO fund, final List<ArrPacketVO> packets, final ArrPacket.State state) {
        ArrangementController.PacketSetStateParam param = new ArrangementController.PacketSetStateParam();
        param.setState(state);
        param.setPacketIds(packets.stream().map(t -> t.getId()).toArray(size -> new Integer[size]));
        setStatePackets(fund.getId(), param);
    }

    /**
     * Generování obalů.
     *
     * @param fund  archivní fond
     * @param prefix    prefix označení obalu
     * @param packetTypeId  identifikátor typu obalu
     * @param fromNumber čísluj od
     * @param lenNumber  počet cifer
     * @param count      počet obalů
     * @param packets    obaly k přečíslování
     */
    protected void generatePackets(final ArrFundVO fund, final String prefix, final Integer packetTypeId,
                                   final Integer fromNumber, final Integer lenNumber, final Integer count,
                                   final List<ArrPacketVO> packets) {
        ArrangementController.PacketGenerateParam input = new ArrangementController.PacketGenerateParam();
        input.setPrefix(prefix);
        input.setPacketTypeId(packetTypeId);
        input.setFromNumber(fromNumber);
        input.setLenNumber(lenNumber);
        input.setCount(count);
        input.setPacketTypeId(packetTypeId);
        if (packets != null) {
            input.setPacketIds(packets.stream().map(t -> t.getId()).toArray(size -> new Integer[size]));
        }
        generatePackets(fund.getId(), input);
    }

    /**
     * Generování obalů.
     *
     * @param fundId idedntifikátor fondu
     * @param input vstupní parametry
     */
    protected void generatePackets(final Integer fundId, final ArrangementController.PacketGenerateParam input) {
        put(spec -> spec.body(input).pathParameter("fundId", fundId), GENERATE_PACKETS);
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
        return put(spec -> spec
                .pathParameter("fundId", fundId)
                .body(packetVO), INSERT_PACKET).getBody().as(ArrPacketVO.class);
    }

    /**
     * Smazání obalů.
     *
     * @param fund    AP
     * @param packets seznam obalů pro smazání
     */
    protected void deletePackets(final ArrFundVO fund, final List<ArrPacketVO> packets) {
        ArrangementController.PacketDeleteParam param = new ArrangementController.PacketDeleteParam();
        param.setPacketIds(packets.stream().map(t -> t.getId()).toArray(size -> new Integer[size]));
        deletePackets(fund.getId(), param);
    }

    /**
     * Smazání obalů.
     *
     * @param fundId  identifikátor AP
     * @param param   parametry smazání
     * @return obal
     */
    protected void deletePackets(final Integer fundId, final ArrangementController.PacketDeleteParam param) {
        delete(spec -> spec.pathParameter("fundId", fundId).body(param), DELETE_PACKETS);
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
        Assert.assertNotNull(fund);

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
    protected RegRegisterTypeVO getHierarchicalRegRegisterType(final List<RegRegisterTypeVO> list, List<RegRegisterTypeVO> exclude, final boolean hasPartyType) {
        if (exclude == null) {
            exclude = Collections.emptyList();
        }
        for (RegRegisterTypeVO type : list) {
            if (type.getHierarchical() && type.getAddRecord() && !exclude.contains(type) && ((!hasPartyType && type.getPartyTypeId() == null) || (hasPartyType && type.getPartyTypeId() != null))) {
                return type;
            }
        }

        for (RegRegisterTypeVO type : list) {
            if (type.getChildren() != null) {
                RegRegisterTypeVO res = getHierarchicalRegRegisterType(type.getChildren(), exclude, hasPartyType);
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
    protected ArrangementController.DescFormDataNewVO getNodeFormData(final Integer nodeId,
                                                                      final Integer versionId) {
        return get(spec -> spec
                .pathParameter("nodeId", nodeId)
                .pathParameter("versionId", versionId),
                NODE_FORM_DATA).getBody().as(ArrangementController.DescFormDataNewVO.class);
    }

    /**
     * Získání dat pro formulář.
     *
     * @param outputDefinitionId identfikátor outputu
     * @param versionId          id verze stromu
     * @return formulář
     */
    protected ArrangementController.OutputFormDataNewVO getOutputFormData(final Integer outputDefinitionId,
                                                                          final Integer versionId) {
        return get(spec -> spec
                        .pathParameter("outputDefinitionId", outputDefinitionId)
                        .pathParameter("versionId", versionId),
                OUTPUT_FORM_DATA).getBody().as(ArrangementController.OutputFormDataNewVO.class);
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
     * Smazání hodnot atributu podle typu.
     *
     * @param fundVersionId   identfikátor verze AP
     * @param outputDefinitionId                identfikátor výstupu
     * @param outputDefinitionVersion           verze výstupu
     * @param itemTypeId        identfikátor typu hodnoty atributu
     */
    protected ArrangementController.OutputItemResult deleteOutputItemsByType(final Integer fundVersionId,
                                                                         final Integer outputDefinitionId,
                                                                         final Integer outputDefinitionVersion,
                                                                         final Integer itemTypeId) {
        return delete(spec -> spec
                        .pathParameter("fundVersionId", fundVersionId)
                        .pathParameter("outputDefinitionId", outputDefinitionId)
                        .pathParameter("outputDefinitionVersion", outputDefinitionVersion)
                        .pathParameter("itemTypeId", itemTypeId),
                DELETE_OUTPUT_ITEM_BY_TYPE).getBody().as(ArrangementController.OutputItemResult.class);
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
        return delete(spec -> spec.pathParam("scopeId", id), DELETE_SCOPE);
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
        return get(spec -> spec.pathParam("recordId", recordId), GET_RECORD).getBody().as(RegRecordVO.class);
    }

    /**
     * Vytvoření rejstříkového hesla.
     *
     * @param record VO rejstříkové heslo
     */
    protected RegRecordVO createRecord(final RegRecordVO record) {
        return post(spec -> spec.body(record), CREATE_RECORD).getBody().as(RegRecordVO.class);
    }

    /**
     * Aktualizace rejstříkového hesla.
     *
     * @param record VO rejstříkové heslo
     */
    protected RegRecordVO updateRecord(final RegRecordVO record) {
        return put(spec -> spec.pathParam("recordId", record.getId()).body(record), UPDATE_RECORD).getBody().as(RegRecordVO.class);
    }

    /**
     * Smazání rejstříkového hesla.
     *
     * @param recordId id rejstříkového hesla
     */
    protected Response deleteRecord(final Integer recordId) {
        return delete(spec -> spec.pathParam("recordId", recordId), DELETE_RECORD);
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

        return get(spec -> spec.queryParameters(params), FIND_RECORD).getBody().as(FilteredResultVO.class).getRows();
    }


    /**
     * Vytvoření variantního hesla
     *
     * @param recordVO VO objektu k vytvoření
     * @return VO
     */
    protected RegVariantRecordVO createVariantRecord(final RegVariantRecordVO recordVO) {
        return post(spec -> spec.body(recordVO), CREATE_VARIANT_RECORD).getBody().as(RegVariantRecordVO.class);
    }

    /**
     * Úprava variantního hesla
     *
     * @param recordVO VO objektu k vytvoření
     * @return VO
     */
    protected RegVariantRecordVO updateVariantRecord(final RegVariantRecordVO recordVO) {
        return put(spec -> spec.pathParam("variantRecordId", recordVO.getId()).body(recordVO), UPDATE_VARIANT_RECORD).getBody().as(RegVariantRecordVO.class);
    }

    /**
     * Smazání variantního hesla
     *
     * @param id variantního hesla
     * @return response
     */
    protected Response deleteVariantRecord(final int id) {
        return delete(spec -> spec.pathParam("variantRecordId", id), DELETE_VARIANT_RECORD);
    }


    /**
     * Vytvoření reg souřadnic
     *
     * @param coordinates VO objektu k vytvoření
     * @return VO
     */
    protected RegCoordinatesVO createRegCoordinates(final RegCoordinatesVO coordinates) {
        return post(spec -> spec.body(coordinates), CREATE_REG_COORDINATES).getBody().as(RegCoordinatesVO.class);
    }

    /**
     * Úprava reg souřadnic
     *
     * @param coordinates VO objektu k vytvoření
     * @return VO
     */
    protected RegCoordinatesVO updateRegCoordinates(final RegCoordinatesVO coordinates) {
        return put(spec -> spec.pathParam("coordinatesId", coordinates.getId()).body(coordinates), UPDATE_REG_COORDINATES).getBody().as(RegCoordinatesVO.class);
    }

    /**
     * Smazání reg souřadnic
     *
     * @param id variantního hesla
     * @return response
     */
    protected Response deleteRegCoordinates(final int id) {
        return delete(spec -> spec.pathParam("coordinatesId", id), DELETE_REG_COORDINATES);
    }

    /**
     * Vytvoření party
     *
     * @param partyVO Party VO
     * @return VO vytvořené party
     */
    protected ParPartyVO createParty(final ParPartyVO partyVO) {
        return post(spec -> spec.body(partyVO), INSERT_PARTY).getBody().as(ParPartyVO.class);
    }

    protected ParPartyVO updateParty(final ParPartyVO partyVO) {
        return put(spec -> spec.body(partyVO).pathParam("partyId", partyVO.getId()), UPDATE_PARTY).getBody().as(ParPartyVO.class);
    }

    /**
     * Získání osoby
     *
     * @param partyId id osoby
     * @return získaná osoba
     */
    protected ParPartyVO getParty(final int partyId) {
        return get(spec -> spec.pathParam("partyId", partyId), GET_PARTY).getBody().as(ParPartyVO.class);
    }

    /**
     * Odstranení osoby
     *
     * @param partyId id osoby
     * @return response
     */
    protected Response deleteParty(final int partyId) {
        return delete(spec -> spec.pathParam("partyId", partyId), DELETE_PARTY);
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

        return get(spec -> spec.queryParameters(params), FIND_PARTY).getBody().as(FilteredResultVO.class).getRows();
    }

    /**
     * Vytvoření relace
     *
     * @param relationVO relace k vytvoření
     * @return vytvořená relace
     */
    protected ParRelationVO insertRelation(final ParRelationVO relationVO) {
        return post(spec -> spec.body(relationVO), CREATE_RELATIONS).getBody().as(ParRelationVO.class);
    }

    /**
     * Upravení relace
     *
     * @param relationVO relace k vytvoření
     * @return vytvořená relace
     */
    protected ParRelationVO updateRelation(final ParRelationVO relationVO) {
        return put(spec -> spec.body(relationVO).pathParam("relationId", relationVO.getId()), UPDATE_RELATIONS).getBody().as(ParRelationVO.class);
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

        return get(spec -> spec.queryParameters(params), FIND_PARTY_FOR_PARTY).getBody().as(FilteredResultVO.class).getRows();
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
        return get(spec -> spec.queryParams(params), FIND_RECORD_FOR_RELATION).getBody().as(FilteredResultVO.class).getRows();
    }

    protected Response importXmlFile(final String transformationName,
                                     final Integer scopeId,
                                     final File xmlFile) {
        HashMap<String, Object> params = new HashMap<>();

        if (transformationName != null) {
            params.put("transformationName", transformationName);
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
     * @param replaceDataBody seznam uzlů, ve kterých hledáme
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
     * Nahrazení textu v hodnotách textových atributů.
     * @param versionId id verze stromu
     * @param descItemTypeId typ atributu
     * @param text hledaný text v atributu
     * @param replaceDataBody seznam uzlů, ve kterých hledáme
     */
    protected void placeDataValues(final Integer versionId,
                                     final Integer descItemTypeId,
                                     final String text,
                                     final ArrangementController.ReplaceDataBody replaceDataBody) {

        put(spec -> spec
                .pathParameter("versionId", versionId)
                .queryParameter("descItemTypeId", descItemTypeId)
                .queryParameter("text", text)
                .body(replaceDataBody), PLACE_DATA_VALUES);
    }

    /**
     * Smazání hodnot atributů daného typu pro vybrané uzly.
     *
     * @param versionId       id verze stromu
     * @param descItemTypeId  id typu atributu
     * @param replaceDataBody seznam uzlů a specifikaci
     */
    protected void deleteDescItems(final Integer versionId,
                                   final Integer descItemTypeId,
                                   final ArrangementController.ReplaceDataBody replaceDataBody) {
        put(spec -> spec
                .pathParameter("versionId", versionId)
                .queryParameter("descItemTypeId", descItemTypeId)
                .body(replaceDataBody), DELETE_DATA_VALUES);
    }

    /**
     * Provede filtraci uzlů podle filtru a uloží filtrované id do session.
     *
     * @param versionId id verze
     * @param filters filtry
     *
     * @return počet všech záznamů splňujících filtry
     */
    protected void filterNodes(final Integer versionId,
                                  final Filters filters) {
        put(spec -> spec
                .pathParameter("versionId", versionId)
                .body(filters), FILTER_NODES);
    }

    /**
     * Do filtrovaného seznamu načte hodnoty atributů a vrátí podstránku záznamů.
     *
     * @param versionId       id verze
     * @param page            číslo stránky, od 0
     * @param pageSize        velikost stránky
     * @param descItemTypeIds id typů atributů, které chceme načíst
     * @return mapa hodnot atributů nodeId -> descItemId -> value
     */
    protected List<FilterNode> getFilteredNodes(final Integer versionId,
                                                final Integer page,
                                                final Integer pageSize,
                                                final Set<Integer> descItemTypeIds)
             {
        return Arrays.asList(put(spec -> spec
                .pathParameter("versionId", versionId)
                .queryParameter("page", page)
                .queryParameter("pageSize", pageSize)
                .body(descItemTypeIds), FILTERED_NODES).as(FilterNode[].class));
    }

    /**
     * Ve filtrovaném seznamu najde uzly podle fulltextu. Vrací seřazený seznam uzlů podle jejich indexu v seznamu
     * všech filtrovaných uzlů.
     *
     * @param versionId id verze stromu
     * @param fulltext  fulltext
     * @param luceneQuery v hodnotě fulltext je lucene query (např: +specification:*čís* -fulltextValue:ddd), false - normální fulltext
     * @return seznam uzlů a jejich indexu v seznamu filtrovaných uzlů, seřazené podle indexu
     */
    protected List<FilterNodePosition> getFilteredFulltextNodes(final Integer versionId,
                                                                final String fulltext,
                                                                final Boolean luceneQuery) {
        FaFilteredFulltextParam param = new FaFilteredFulltextParam();
        param.setFulltext(fulltext);
        param.setLuceneQuery(BooleanUtils.isTrue(luceneQuery));

        return Arrays.asList(post(spec -> spec
                .pathParameter("versionId", versionId)
                .body(param), FILTERED_FULLTEXT_NODES).as(FilterNodePosition[].class));
    }

    protected ArrangementController.ValidationItems getValidation(final Integer fundVersionId,
                                                                  final Integer fromIndex,
                                                                  final Integer toIndex) {
        return get(spec -> spec
                        .pathParameter("fundVersionId", fundVersionId)
                        .pathParameter("fromIndex", fromIndex)
                        .pathParameter("toIndex", toIndex)
                , VALIDATION).as(ArrangementController.ValidationItems.class);
    }

    protected ArrangementController.ValidationItems findValidationError(final Integer fundVersionId,
                                                                        final Integer nodeId,
                                                                        final Integer direction) {
        return get(spec -> spec
                .pathParameter("fundVersionId", fundVersionId)
                .pathParameter("nodeId", nodeId)
                .pathParameter("direction", direction)
                , VALIDATION_ERROR).as(ArrangementController.ValidationItems.class);
    }

    protected List<NodeItemWithParent> getAllNodesVisiblePolicy(final Integer fundVersionId) {
        return Arrays.asList(get(spec -> spec
                .pathParameter("fundVersionId", fundVersionId), FUND_POLICY).as(NodeItemWithParent[].class));
    }

    /**
     * Získání informací o přihlášeném uživateli.
     *
     * @return detail přihlášeného uživatele
     */
    protected UserInfoVO getUserDetail() {
        return get(spec -> spec, USER_DETAIL).as(UserInfoVO.class);
    }

    /**
     * Změna hesla uživatele.
     *
     * @param userId identifikátor uživatele
     * @param params parametry změny hesla
     * @return uživatel
     */
    protected UsrUserVO changePassword(final Integer userId,
                                       final UserController.ChangePassword params) {
        return put(spec -> spec.body(params)
                .pathParameter("userId", userId), CHANGE_PASSWORD).as(UserInfoVO.class);
    }

    /**
     * Změna hesla uživatele.
     *
     * @param params parametry změny hesla
     * @return uživatel
     */
    protected UsrUserVO changePassword(final UserController.ChangePassword params) {
        return put(spec -> spec.body(params), CHANGE_PASSWORD_USER).as(UserInfoVO.class);
    }


    /**
     * Změna hesla uživatele.
     *
     * @return uživatel
     */
    protected UsrUserVO changePassword(final String oldPassword,
                                       final String newPassword) {
        UserController.ChangePassword params = new UserController.ChangePassword();
        params.setNewPassword(newPassword);
        params.setOldPassword(oldPassword);
        return changePassword(params);
    }

    /**
     * Změna hesla uživatele.
     *
     * @return uživatel
     */
    protected UsrUserVO changePassword(final UsrUserVO user,
                                       final String newPassword) {
        UserController.ChangePassword params = new UserController.ChangePassword();
        params.setNewPassword(newPassword);
        return changePassword(user.getId(), params);
    }

    /**
     * Vytvořené nového uživatele.
     *
     * @param params parametry pro vytvoření uživatele
     * @return vytvořený uživatel
     */
    protected UsrUserVO createUser(final UserController.CreateUser params) {
        return post(spec -> spec.body(params), CREATE_USER).as(UsrUserVO.class);
    }

    /**
     *
     *
     * @param user   uživatel
     * @param active je aktivní?
     * @return uživatel
     */
    protected UsrUserVO changeActive(final UsrUserVO user,
                                     final Boolean active) {
        return put(spec -> spec.pathParameter("active", active)
                .pathParameter("userId", user.getId()), ACTIVE_USER).as(UsrUserVO.class);
    }

    /**
     * Vytvořené nového uživatele.
     *
     * @return vytvořený uživatel
     */
    protected UsrUserVO createUser(final String username,
                                   final String password,
                                   final Integer partyId) {
        UserController.CreateUser params = new UserController.CreateUser();
        params.setUsername(username);
        params.setPassword(password);
        params.setPartyId(partyId);
        return createUser(params);
    }

    /**
     * Vytvořené skupiny.
     *
     * @param params parametry pro vytvoření skupiny
     * @return vytvořená skupina
     */
    protected UsrGroupVO createGroup(final UserController.CreateGroup params) {
        return post(spec -> spec.body(params), CREATE_GROUP).as(UsrGroupVO.class);
    }

    /**
     * Smazání skupiny.
     *
     * @param groupId identifikátor skupiny
     */
    protected void deleteGroup(final Integer groupId) {
        delete(spec -> spec.pathParameter("groupId", groupId), DELETE_GROUP);
    }

    /**
     * Načte seznam skupin.
     *
     * @param search hledaný řetězec
     * @param from   počáteční záznam
     * @param count  počet vrácených záznamů
     * @return seznam s celkovým počtem
     */
    protected FilteredResultVO<UsrGroupVO> findGroup(final String search,
                                         final Integer from,
                                         final Integer count) {
        return get(spec -> spec.queryParam("search", search)
                .queryParam("from", from)
                .queryParam("count", count), FIND_GROUP).as(FilteredResultVO.class);
    }

    /**
     * Změna skupiny.
     *
     * @param groupId identifikátor skupiny
     * @param params  parametry změny skupiny
     */
    protected UsrGroupVO changeGroup(final Integer groupId,
                                  final UserController.ChangeGroup params) {
        return put(spec -> spec.body(params).pathParameter("groupId", groupId), CHANGE_GROUP).as(UsrGroupVO.class);
    }

    /**
    * Načte seznam uživatelů.
    *
    * @param search   hledaný řetězec
    * @param from     počáteční záznam
    * @param count    počet vrácených záznamů
    * @param active   mají se vracet aktivní osoby?
    * @param disabled mají se vracet zakázané osoby?
    * @return seznam s celkovým počtem
    */
    protected FilteredResultVO<UsrUserVO> findUser(@Nullable final String search,
                                                    final Boolean active,
                                                    final Boolean disabled,
                                                    final Integer from,
                                                    final Integer count,
                                                    final Integer excludedGroupId) {
        return get(spec -> spec.queryParam("active", active)
                .queryParam("search", search)
                .queryParam("from", from)
                .queryParam("count", count)
                .queryParam("disabled", disabled)
                .queryParam("excludedGroupId", excludedGroupId), FIND_USER).as(FilteredResultVO.class);
    }

    /**
     * Načtení uživatele s daty pro zobrazení na detailu s možností editace.
     *
     * @param userId id
     * @return VO
     */
    protected UsrUserVO getUser(final Integer userId) {
        return get(spec -> spec.pathParameter("userId", userId), GET_USER).as(UsrUserVO.class);
    }

    /**
     * Načtení skupiny s daty pro zobrazení na detailu s možností editace.
     *
     * @param groupId id
     * @return VO
     */
    protected UsrGroupVO getGroup(final Integer groupId) {
        return get(spec -> spec.pathParameter("groupId", groupId), GET_GROUP).as(UsrGroupVO.class);
    }

    /**
     * Přidání uživatelů do skupin.
     *
     * @param groupIds identifikátor skupin, do které přidáváme uživatele
     * @param userIds  identifikátor přidávaných uživatelů
     */
    protected void joinGroup(final Set<Integer> groupIds,
                          final Set<Integer> userIds) {
        UserController.IdsParam param = new UserController.IdsParam(groupIds, userIds);
        post(spec -> spec.body(param), JOIN_GROUP);
    }

    /**
     * Přidání uživatele do skupiny.
     *
     * @param groupId identifikátor skupiny, ze které odebírám uživatel
     * @param userId  identifikátor odebíraného uživatele
     */
    protected void leaveGroup(final Integer groupId,
                           final Integer userId) {
        post(spec -> spec.pathParameter("groupId", groupId).pathParameter("userId", userId), LEAVE_GROUP);
    }

    /**
     * Nastavení oprávnění uživatele.
     *
     * @param userId      identifikátor uživatele
     * @param permissions seznam oprávnění
     */
    protected void changeUserPermission(final Integer userId,
                                     final UserController.Permissions permissions) {
        post(spec -> spec.pathParameter("userId", userId).body(permissions), CHANGE_USER_PERMISSION);
    }

    /**
     * Nastavení oprávnění skupiny.
     *
     * @param groupId     identifikátor skupiny
     * @param permissions seznam oprávnění
     */
    protected void changeGroupPermission(final Integer groupId,
                                      final UserController.Permissions permissions) {
        post(spec -> spec.pathParameter("groupId", groupId).body(permissions), CHANGE_GROUP_PERMISSION);
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

    /**
     * Načtení seznamu outputů - objekt outputu s vazbou na objekt named output.
     *
     * @param fundVersionId identfikátor verze AS
     * @return  seznam outputů
     */
    protected List<ArrOutputExtVO> getOutputs(final Integer fundVersionId) {
        return Arrays.asList(get(spec -> spec
                .pathParam("fundVersionId", fundVersionId), GET_OUTPUTS)
                .getBody().as(ArrOutputExtVO[].class));
    }

    /**
     * Načtení detailu outputu objekt output s vazbou na named output a seznamem připojených node.
     *
     * @param fundVersionId identfikátor verze AS
     * @param outputId      identifikátor výstupu
     * @return output
     */
    protected ArrOutputExtVO getOutput(final Integer fundVersionId, final Integer outputId) {
        return get(spec -> spec
                .pathParam("fundVersionId", fundVersionId)
                .pathParam("outputId", outputId), GET_OUTPUT)
                .getBody().as(ArrOutputExtVO.class);
    }

    /**
     * Vytvoření nového pojmenovaného výstupu.
     *
     * @param fundVersionId identfikátor verze AS
     * @param param         vstupní parametry pro vytvoření outputu
     * @return vytvořený výstup
     */
    protected ArrOutputDefinitionVO createNamedOutput(final Integer fundVersionId,
                                                      final ArrangementController.OutputNameParam param) {
        return put(spec -> spec
                .pathParam("fundVersionId", fundVersionId)
                .body(param), CREATE_NAMED_OUTPUT)
                .getBody().as(ArrOutputDefinitionVO.class);
    }

    /**
     * Vytvoření nového pojmenovaného výstupu.
     *
     * @param fundVersion verze AS
     * @param name        název výstupu
     * @param code        kód výstupu
     * @param temporary   dočasný výstup?
     * @return vytvořený výstup
     */
    protected ArrOutputDefinitionVO createNamedOutput(final ArrFundVersionVO fundVersion,
                                                      final String name,
                                                      final String code,
                                                      final Boolean temporary,
                                                      final Integer outputTypeId) {
        ArrangementController.OutputNameParam param = new ArrangementController.OutputNameParam();
        param.setInternalCode(code);
        param.setName(name);
        param.setTemporary(temporary);
        param.setOutputTypeId(outputTypeId);
        return createNamedOutput(fundVersion.getId(), param);
    }

    /**
     * Zamknutí verze výstupu.
     *
     * @param fundVersionId identfikátor verze AS
     * @param outputId      identifikátor výstupu
     */
    protected void outputLock(final Integer fundVersionId,
                              final Integer outputId) {
        post(spec -> spec
                .pathParam("fundVersionId", fundVersionId)
                .pathParam("outputId", outputId), OUTPUT_LOCK);
    }

    /**
     * Přidání uzlů k výstupu.
     *
     * @param fundVersionId identfikátor verze AS
     * @param outputId      identifikátor výstupu
     * @param nodeIds       seznam přidáváných identifikátorů uzlů
     */
    protected void addNodesNamedOutput(final Integer fundVersionId,
                                       final Integer outputId,
                                       final List<Integer> nodeIds) {
        post(spec -> spec
                .pathParam("fundVersionId", fundVersionId)
                .pathParam("outputId", outputId)
                .body(nodeIds), ADD_NODES_NAMED_OUTPUT);
    }

    /**
     * Odebrání uzlů u výstupu.
     *
     * @param fundVersionId identfikátor verze AS
     * @param outputId      identifikátor výstupu
     * @param nodeIds       seznam odebíraných identifikátorů uzlů
     */
    protected void removeNodesNamedOutput(final Integer fundVersionId,
                                          final Integer outputId,
                                          final List<Integer> nodeIds) {
        post(spec -> spec
                .pathParam("fundVersionId", fundVersionId)
                .pathParam("outputId", outputId)
                .body(nodeIds), REMOVE_NODES_NAMED_OUTPUT);
    }

    /**
     * Smazání pojmenovaného výstupu.
     *
     * @param fundVersionId identfikátor verze AS
     * @param outputId      identifikátor výstupu
     */
    protected void deleteNamedOutput(final Integer fundVersionId,
                                     final Integer outputId) {
        delete(spec -> spec
                .pathParam("fundVersionId", fundVersionId)
                .pathParam("outputId", outputId), DELETE_NAMED_OUTPUT);
    }

    /**
     * Upravení výstupu.
     *
     * @param fundVersion verze AS
     * @param output      výstup
     * @param name        název výstupu
     * @param code        kód výstupu
     */
    protected void updateNamedOutput(final ArrFundVersionVO fundVersion,
                                     final ArrOutputVO output,
                                     final String name,
                                     final String code) {
        ArrangementController.OutputNameParam param = new ArrangementController.OutputNameParam();
        param.setInternalCode(code);
        param.setName(name);
        updateNamedOutput(fundVersion.getId(), output.getId(), param);
    }

    /**
     * Upravení výstupu.
     *
     * @param fundVersionId identfikátor verze AS
     * @param outputId      identfikátor výstupu
     * @param param         vstupní parametry pro úpravu outputu
     */
    protected void updateNamedOutput(final Integer fundVersionId,
                                     final Integer outputId,
                                     final ArrangementController.OutputNameParam param) {
        post(spec -> spec
                .pathParam("fundVersionId", fundVersionId)
                .pathParam("outputId", outputId)
                .body(param), UPDATE_NAMED_OUTPUT);
    }

    /**
     * Vrací typy oprávnění podle verze fondu.
     *
     * @param fundVersionId identifikátor verze AS
     * @return seznam typů oprávnění
     */
    protected List<RulPolicyTypeVO> getPolicyTypes(final Integer fundVersionId) {
        return Arrays.asList(get(spec -> spec.pathParameter("fundVersionId", fundVersionId), POLICY_TYPES).as(RulPolicyTypeVO[].class));
    }

    /**
     * Vrací typy oprávnění.
     *
     * @return seznam typů oprávnění
     */
    protected List<RulPolicyTypeVO> getAllPolicyTypes() {
        return Arrays.asList(get(spec -> spec, POLICY_ALL_TYPES).as(RulPolicyTypeVO[].class));
    }

    /**
     * Nastaví/smazaní viditelnost typu oprávnění.
     *
     * @param nodeId              identifikátor node ke kterému se hodnota vztahuje.
     * @param fundVersionId       identifikátor verze AS
     * @param visiblePolicyParams parametry nastavení
     */
    protected void setVisiblePolicy(final Integer nodeId,
                                    final Integer fundVersionId,
                                    final RuleController.VisiblePolicyParams visiblePolicyParams) {
        put(spec -> spec.pathParameter("nodeId", nodeId)
                .pathParameter("fundVersionId", fundVersionId)
                .body(visiblePolicyParams), POLICY_SET);
    }

    /**
     * Získání nastavení oprávnění pro uzly.
     *
     * @param nodeId         identifikátor node ke kterému hledám oprávnění
     * @param fundVersionId  identifikátor verze AS
     * @param includeParents zohlednit zděděné oprávnění od rodičů?
     * @return mapa uzlů map typů a jejich zobrazení
     */
    protected RuleController.VisiblePolicyTypes getVisiblePolicy(final Integer nodeId,
                                                                 final Integer fundVersionId,
                                                                 final Boolean includeParents) {
        return get(spec -> spec.pathParameter("nodeId", nodeId)
                .pathParameter("fundVersionId", fundVersionId)
                .pathParameter("includeParents", includeParents), POLICY_GET).as(RuleController.VisiblePolicyTypes.class);
    }

    /**
     * Načtení souboru na základě cesty, např. coordinates/all.kml odkazuje do test resources.
     * @param resourcePath cesta
     * @return soubor
     */
    public static File getFile(final String resourcePath) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
        Assert.assertNotNull(url);
        return new File(url.getPath());
    }

    /**
     * Vyhledá všechny externí systémy.
     *
     * @return seznam externích systémů
     */
    protected List<SysExternalSystemVO> getExternalSystems() {
        return Arrays.asList(get(FIND_EXTERNAL_SYSTEMS).as(SysExternalSystemVO[].class));
    }

    /**
     * Vytvoří externí systém.
     *
     * @param externalSystemVO vytvářený externí systém
     * @return vytvořený externí systém
     */
    protected SysExternalSystemVO createExternalSystem(final SysExternalSystemVO externalSystemVO) {
        return post(spec -> spec.body(externalSystemVO), CREATE_EXTERNAL_SYSTEM).as(SysExternalSystemVO.class);
    }

    /**
     * Upravení externího systému.
     *
     * @param externalSystemVO upravovaný externí systém
     * @return upravený externí systém
     */
    protected SysExternalSystemVO updateExternalSystem(final SysExternalSystemVO externalSystemVO) {
        return put(spec -> spec.body(externalSystemVO).pathParam("externalSystemId", externalSystemVO.getId()),
                UPDATE_EXTERNAL_SYSTEM).as(SysExternalSystemVO.class);
    }

    /**
     * Smazání externího systému.
     *
     * @param externalSystemVO mazaný externí systém
     */
    protected void deleteExternalSystem(final SysExternalSystemVO externalSystemVO) {
        delete(spec -> spec.pathParam("externalSystemId", externalSystemVO.getId()), DELETE_EXTERNAL_SYSTEM);
    }

    /**
     * Vyhledání provedení změn nad AS, případně nad konkrétní JP z AS.
     *
     * @param fundVersionId identfikátor verze AS
     * @param maxSize       maximální počet záznamů
     * @param offset        počet přeskočených záznamů
     * @param changeId      identifikátor změny, vůči které chceme počítat offset (pokud není vyplněn, bere se vždy poslední)
     * @param nodeId        identifikátor JP u které vyhledáváme změny (pokud není vyplněn, vyhledává se přes celý AS)
     * @return výsledek hledání
     */
    protected ChangesResult findChanges(final Integer fundVersionId, final Integer maxSize,
                                        final Integer offset,
                                        final Integer changeId,
                                        final Integer nodeId) {
        return get(spec -> spec.pathParam("fundVersionId", fundVersionId)
                        .queryParameter("maxSize", maxSize)
                        .queryParameter("offset", offset)
                        .queryParameter("changeId", changeId)
                        .queryParameter("nodeId", nodeId), FIND_CHANGE).as(ChangesResult.class);
    }

    /**
     * Vyhledání provedení změn nad AS, případně nad konkrétní JP z AS.
     *
     * @param fundVersionId identfikátor verze AS
     * @param maxSize       maximální počet záznamů
     * @param fromDate      datum vůči kterému vyhledávám v seznamu (př. formátu query parametru: 2016-11-07T10:32:04)
     * @param changeId      identifikátor změny, vůči které chceme počítat offset (pokud není vyplněn, bere se vždy poslední)
     * @param nodeId        identifikátor JP u které vyhledáváme změny (pokud není vyplně, vyhledává se přes celý AS)
     * @return výsledek hledání
     */
    protected ChangesResult findChangesByDate(final Integer fundVersionId,
                                              final Integer maxSize,
                                              final LocalDateTime fromDate,
                                              final Integer changeId,
                                              final Integer nodeId) {
        return get(spec -> spec.pathParam("fundVersionId", fundVersionId)
                        .queryParameter("maxSize", maxSize)
                        .queryParameter("fromDate", fromDate.format(FORMATTER))
                        .queryParameter("changeId", changeId)
                        .queryParameter("nodeId", nodeId), FIND_CHANGE_BY_DATE).as(ChangesResult.class);
    }

    /**
     * Provede revertování AS / JP k požadovanému stavu.
     *
     * @param fundVersionId identfikátor verze AS
     * @param fromChangeId  identifikátor změny, vůči které provádíme revertování (od)
     * @param toChangeId    identifikátor změny, ke které provádíme revertování (do)
     * @param nodeId        identifikátor JP u které provádíme změny (pokud není vyplněn, revertuje se přes celý AS)
     */
    public void revertChanges(final Integer fundVersionId,
                              final Integer fromChangeId,
                              final Integer toChangeId,
                              final Integer nodeId) {
        get(spec -> spec.pathParam("fundVersionId", fundVersionId)
                        .queryParameter("fromChangeId", fromChangeId)
                        .queryParameter("toChangeId", toChangeId)
                        .queryParameter("nodeId", nodeId), REVERT_CHANGES);
    }

    /**
     * Nastavení atributu na "Nezjištěno".
     *
     * @param fundVersionId    id archivního souboru
     * @param nodeId           id JP
     * @param nodeVersion      verze JP
     * @param descItemTypeId   identfikátor typu hodnoty atributu
     * @param descItemSpecId   identfikátor specifikace hodnoty atributu
     * @param descItemObjectId identifikátor existující hodnoty atributu
     * @return upravená hodnota atributu nastavená na nezjištěno
     */
    protected ArrangementController.DescItemResult setNotIdentifiedDescItem(final Integer fundVersionId,
                                                                            final Integer nodeId,
                                                                            final Integer nodeVersion,
                                                                            final Integer descItemTypeId,
                                                                            final Integer descItemSpecId,
                                                                            final Integer descItemObjectId) {
        return put(spec -> spec.pathParameter("fundVersionId", fundVersionId)
                        .pathParameter("nodeId", nodeId)
                        .pathParameter("nodeVersion", nodeVersion)
                        .queryParameter("descItemTypeId", descItemTypeId)
                        .queryParameter("descItemSpecId", descItemSpecId)
                        .queryParameter("descItemObjectId", descItemObjectId)
                , SET_NOT_IDENTIFIED_DESCITEM).as(ArrangementController.DescItemResult.class);
    }

    /**
     * Zrušení nastavení atributu na "Nezjištěno".
     *
     * @param fundVersionId    id archivního souboru
     * @param nodeId           id JP
     * @param nodeVersion      verze JP
     * @param descItemTypeId   identfikátor typu hodnoty atributu
     * @param descItemSpecId   identfikátor specifikace hodnoty atributu
     * @param descItemObjectId identifikátor existující hodnoty atributu
     * @return odstraněný atribut
     */
    protected ArrangementController.DescItemResult unsetNotIdentifiedDescItem(final Integer fundVersionId,
                                                                               final Integer nodeId,
                                                                               final Integer nodeVersion,
                                                                               final Integer descItemTypeId,
                                                                               final Integer descItemSpecId,
                                                                               final Integer descItemObjectId) {
        return put(spec -> spec.pathParameter("fundVersionId", fundVersionId)
                        .pathParameter("nodeId", nodeId)
                        .pathParameter("nodeVersion", nodeVersion)
                        .queryParameter("descItemTypeId", descItemTypeId)
                        .queryParameter("descItemSpecId", descItemSpecId)
                        .queryParameter("descItemObjectId", descItemObjectId)
                , UNSET_NOT_IDENTIFIED_DESCITEM).as(ArrangementController.DescItemResult.class);
    }

    /**
     * Nastavení atributu na "Nezjištěno".
     *
     * @param fundVersionId    id archivního souboru
     * @param fundVersionId           id archivního souboru
     * @param outputDefinitionId      identifikátor výstupu
     * @param outputDefinitionVersion verze výstupu
     * @param outputItemTypeId        dentfikátor typu hodnoty atributu
     * @param outputItemSpecId        identfikátor specifikace hodnoty atributu
     * @param outputItemObjectId      identifikátor existující hodnoty atributu
     * @return upravená hodnota atributu nastavená na nezjištěno
     */
    protected ArrangementController.OutputItemResult setNotIdentifiedOutputItem(final Integer fundVersionId,
                                                                            final Integer outputDefinitionId,
                                                                            final Integer outputDefinitionVersion,
                                                                            final Integer outputItemTypeId,
                                                                            final Integer outputItemSpecId,
                                                                            final Integer outputItemObjectId) {
        return put(spec -> spec.pathParameter("fundVersionId", fundVersionId)
                        .pathParameter("outputDefinitionId", outputDefinitionId)
                        .pathParameter("outputDefinitionVersion", outputDefinitionVersion)
                        .queryParameter("outputItemTypeId", outputItemTypeId)
                        .queryParameter("outputItemSpecId", outputItemSpecId)
                        .queryParameter("outputItemObjectId", outputItemObjectId)
                , SET_NOT_IDENTIFIED_OUTPUTITEM).as(ArrangementController.OutputItemResult.class);
    }

    /**
     * Zrušení nastavení atributu na "Nezjištěno".
     *
     * @param fundVersionId           id archivního souboru
     * @param outputDefinitionId      identifikátor výstupu
     * @param outputDefinitionVersion verze výstupu
     * @param outputItemTypeId        dentfikátor typu hodnoty atributu
     * @param outputItemSpecId        identfikátor specifikace hodnoty atributu
     * @param outputItemObjectId      identifikátor existující hodnoty atributu
     * @return odstraněný atribut
     */
    protected ArrangementController.OutputItemResult unsetNotIdentifiedOutputItem(final Integer fundVersionId,
                                                                                  final Integer outputDefinitionId,
                                                                                  final Integer outputDefinitionVersion,
                                                                                  final Integer outputItemTypeId,
                                                                                  final Integer outputItemSpecId,
                                                                                  final Integer outputItemObjectId) {
        return put(spec -> spec.pathParameter("fundVersionId", fundVersionId)
                        .pathParameter("outputDefinitionId", outputDefinitionId)
                        .pathParameter("outputDefinitionVersion", outputDefinitionVersion)
                        .queryParameter("outputItemTypeId", outputItemTypeId)
                        .queryParameter("outputItemSpecId", outputItemSpecId)
                        .queryParameter("outputItemObjectId", outputItemObjectId)
                , UNSET_NOT_IDENTIFIED_OUTPUTITEM).as(ArrangementController.OutputItemResult.class);
    }

    protected CopyNodesValidateResult copyLevelsValidate(final CopyNodesValidate copyNodesValidate) {
        return post(spec -> spec.body(copyNodesValidate), COPY_LEVELS_VALIDATE).as(CopyNodesValidateResult.class);
    }

    protected void copyLevels(final CopyNodesParams copyNodesParams) {
        post(spec -> spec.body(copyNodesParams), COPY_LEVELS);
    }
}
