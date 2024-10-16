package cz.tacr.elza.controller;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.HEAD;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.task.TaskSchedulerBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSession.Receiptable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import cz.tacr.elza.AbstractTest;
import cz.tacr.elza.controller.ArrangementController.FaFilteredFulltextParam;
import cz.tacr.elza.controller.ArrangementWebsocketControllerTest.ReceiptStatus;
import cz.tacr.elza.controller.vo.AddLevelParam;
import cz.tacr.elza.controller.vo.ApAccessPointCreateVO;
import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.ApEidTypeVO;
import cz.tacr.elza.controller.vo.ApPartFormVO;
import cz.tacr.elza.controller.vo.ApScopeVO;
import cz.tacr.elza.controller.vo.ApScopeWithConnectedVO;
import cz.tacr.elza.controller.vo.ApTypeVO;
import cz.tacr.elza.controller.vo.ArrDaoLinkVO;
import cz.tacr.elza.controller.vo.ArrDaoVO;
import cz.tacr.elza.controller.vo.ArrFundFulltextResult;
import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.ArrOutputVO;
import cz.tacr.elza.controller.vo.ArrRefTemplateEditVO;
import cz.tacr.elza.controller.vo.ArrRefTemplateMapTypeVO;
import cz.tacr.elza.controller.vo.ArrRefTemplateVO;
import cz.tacr.elza.controller.vo.ArrStructureDataVO;
import cz.tacr.elza.controller.vo.CopyNodesParams;
import cz.tacr.elza.controller.vo.CopyNodesValidate;
import cz.tacr.elza.controller.vo.CopyNodesValidateResult;
import cz.tacr.elza.controller.vo.CreateUserVO;
import cz.tacr.elza.controller.vo.CreatedPartVO;
import cz.tacr.elza.controller.vo.FilterNode;
import cz.tacr.elza.controller.vo.FilterNodePosition;
import cz.tacr.elza.controller.vo.FilteredResultVO;
import cz.tacr.elza.controller.vo.FindFundsResult;
import cz.tacr.elza.controller.vo.FulltextFundRequest;
import cz.tacr.elza.controller.vo.FundDetail;
import cz.tacr.elza.controller.vo.FundListCountResult;
import cz.tacr.elza.controller.vo.LanguageVO;
import cz.tacr.elza.controller.vo.NodeItemWithParent;
import cz.tacr.elza.controller.vo.OutputSettingsVO;
import cz.tacr.elza.controller.vo.PackageVO;
import cz.tacr.elza.controller.vo.ParInstitutionVO;
import cz.tacr.elza.controller.vo.RulDataTypeVO;
import cz.tacr.elza.controller.vo.RulDescItemSpecVO;
import cz.tacr.elza.controller.vo.RulDescItemTypeVO;
import cz.tacr.elza.controller.vo.RulExportFilterVO;
import cz.tacr.elza.controller.vo.RulOutputFilterVO;
import cz.tacr.elza.controller.vo.RulOutputTypeVO;
import cz.tacr.elza.controller.vo.RulPartTypeVO;
import cz.tacr.elza.controller.vo.RulPolicyTypeVO;
import cz.tacr.elza.controller.vo.RulRuleSetVO;
import cz.tacr.elza.controller.vo.RulStructureTypeVO;
import cz.tacr.elza.controller.vo.RulTemplateVO;
import cz.tacr.elza.controller.vo.ScenarioOfNewLevelVO;
import cz.tacr.elza.controller.vo.StructureExtensionFundVO;
import cz.tacr.elza.controller.vo.SysExternalSystemVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.controller.vo.UniqueValue;
import cz.tacr.elza.controller.vo.UserInfoVO;
import cz.tacr.elza.controller.vo.UsrGroupVO;
import cz.tacr.elza.controller.vo.UsrPermissionVO;
import cz.tacr.elza.controller.vo.UsrUserVO;
import cz.tacr.elza.controller.vo.ValidationResult;
import cz.tacr.elza.controller.vo.WfCommentVO;
import cz.tacr.elza.controller.vo.WfIssueListVO;
import cz.tacr.elza.controller.vo.WfIssueStateVO;
import cz.tacr.elza.controller.vo.WfIssueTypeVO;
import cz.tacr.elza.controller.vo.WfIssueVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemAccessPointRefVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemCoordinatesVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemDateVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemDecimalVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemEnumVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemFormattedTextVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemIntVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemJsonTableVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemStringVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemTextVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemUnitdateVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemUnitidVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemVO;
import cz.tacr.elza.controller.vo.ap.item.ApUpdateItemVO;
import cz.tacr.elza.controller.vo.filter.Filters;
import cz.tacr.elza.controller.vo.nodes.ArrNodeExtendVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.NodeData;
import cz.tacr.elza.controller.vo.nodes.NodeDataParam;
import cz.tacr.elza.controller.vo.nodes.RulDescItemSpecExtVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemBitVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemCoordinatesVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemDateVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemDecimalVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemEnumVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemFormattedTextVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemIntVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemJsonTableVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemRecordRefVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemStringVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemStructureVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemTextVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemUnitdateVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemUnitidVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemUriRefVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.controller.vo.nodes.descitems.UpdateOp;
import cz.tacr.elza.controller.vo.usage.RecordUsageVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.SearchType;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.UsrAuthentication;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ExceptionResponse;
import cz.tacr.elza.exception.ExceptionUtils;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.service.vo.ChangesResult;
import cz.tacr.elza.test.ApiClient;
import cz.tacr.elza.test.controller.AccesspointsApi;
import cz.tacr.elza.test.controller.AdminApi;
import cz.tacr.elza.test.controller.DaosApi;
import cz.tacr.elza.test.controller.DescitemsApi;
import cz.tacr.elza.test.controller.FundsApi;
import cz.tacr.elza.test.controller.IoApi;
import cz.tacr.elza.test.controller.SearchApi;
import cz.tacr.elza.test.controller.vo.ApStateUpdate;
import cz.tacr.elza.test.controller.vo.CreateFund;
import cz.tacr.elza.test.controller.vo.DataBit;
import cz.tacr.elza.test.controller.vo.DataCoordinates;
import cz.tacr.elza.test.controller.vo.DataDate;
import cz.tacr.elza.test.controller.vo.DataDecimal;
import cz.tacr.elza.test.controller.vo.DataFileRef;
import cz.tacr.elza.test.controller.vo.DataFormattedText;
import cz.tacr.elza.test.controller.vo.DataInteger;
import cz.tacr.elza.test.controller.vo.DataJsonTable;
import cz.tacr.elza.test.controller.vo.DataNull;
import cz.tacr.elza.test.controller.vo.DataRecordRef;
import cz.tacr.elza.test.controller.vo.DataString;
import cz.tacr.elza.test.controller.vo.DataStructureRef;
import cz.tacr.elza.test.controller.vo.ItemData;
import cz.tacr.elza.test.controller.vo.NodeItem;
import cz.tacr.elza.test.controller.vo.DataText;
import cz.tacr.elza.test.controller.vo.DataUnitdate;
import cz.tacr.elza.test.controller.vo.DataUnitid;
import cz.tacr.elza.test.controller.vo.DataUriRef;
import cz.tacr.elza.test.controller.vo.Fund;
import cz.tacr.elza.websocket.WebSocketStompClientElza;
import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.Header;
import io.restassured.internal.support.Prettifier;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;
import io.restassured.response.ResponseOptions;
import io.restassured.specification.RequestSpecification;
import jakarta.annotation.Nullable;

public abstract class AbstractControllerTest extends AbstractTest {

	protected boolean loadInstitutions = true;

	private static final RestAssuredConfig UTF8_ENCODER_CONFIG = RestAssuredConfig.newConfig()
			.encoderConfig(EncoderConfig.encoderConfig().defaultContentCharset("UTF-8"));

	protected static final Logger logger = LoggerFactory.getLogger(AbstractControllerTest.class);
	protected static final String CONTENT_TYPE_HEADER = "content-type";
	protected static final String JSON_CONTENT_TYPE = "application/json";
	protected static final String WWW_FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";
	private static final Header JSON_CT_HEADER = new Header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE);
	private static final Header WWW_FORM_CT_HEADER = new Header(CONTENT_TYPE_HEADER, WWW_FORM_CONTENT_TYPE);
	private static final Header MULTIPART_HEADER = new Header(CONTENT_TYPE_HEADER, MediaType.MULTIPART_FORM_DATA_VALUE);

	protected static final String REST_CONTROLLER_URL = "/api/v1";

	protected static final String ADMIN_CONTROLLER_URL = "/api/admin";
	protected static final String ARRANGEMENT_CONTROLLER_URL = "/api/arrangement";
	protected static final String STRUCTURE_CONTROLLER_URL = "/api/structure";
	protected static final String BULK_ACTION_CONTROLLER_URL = "/api/action";
	protected static final String PARTY_CONTROLLER_URL = "/api/party";
	protected static final String AP_CONTROLLER_URL = "/api/registry";
	protected static final String KML_CONTROLLER_URL = "/api/kml";
	protected static final String VALIDATION_CONTROLLER_URL = "/api/validate";
	protected static final String RULE_CONTROLLER_URL = "/api/rule";
	protected static final String DE_IMPORT_CONTROLLER_URL = "/api/import";
	// protected static final String DE_EXPORT_CONTROLLER_URL = "/api/export";
	protected static final String USER_CONTROLLER_URL = "/api/user";
	protected static final String GROUP_CONTROLLER_URL = "/api/group";
	protected static final String ISSUE_CONTROLLER_URL = "/api/issue";
	protected static final String FUND_CONTROLLER_URL = "/api/v1";

	// ADMIN
	protected static final String REINDEX = ADMIN_CONTROLLER_URL + "/reindex";
	protected static final String REINDEX_STATUS = ADMIN_CONTROLLER_URL + "/reindexStatus";
	protected static final String CACHE_RESET = ADMIN_CONTROLLER_URL + "/cache/reset";
	protected static final String EXTERNAL_SYSTEMS = ADMIN_CONTROLLER_URL + "/externalSystems";
	protected static final String FIND_EXTERNAL_SYSTEMS = EXTERNAL_SYSTEMS;
	protected static final String CREATE_EXTERNAL_SYSTEM = EXTERNAL_SYSTEMS;
	protected static final String UPDATE_EXTERNAL_SYSTEM = EXTERNAL_SYSTEMS + "/{externalSystemId}";
	protected static final String DELETE_EXTERNAL_SYSTEM = EXTERNAL_SYSTEMS + "/{externalSystemId}";

	// STRUCTURE
	protected static final String CREATE_STRUCTURE_DATA = STRUCTURE_CONTROLLER_URL + "/data/{fundVersionId}";
	protected static final String CONFIRM_STRUCTURE_DATA = STRUCTURE_CONTROLLER_URL
			+ "/data/{fundVersionId}/{structureDataId}/confirm";
	protected static final String SET_ASSIGNABLE_STRUCTURE_DATA_LIST = STRUCTURE_CONTROLLER_URL
			+ "/data/{fundVersionId}/assignable/{assignable}";
	@Deprecated
	protected static final String DELETE_STRUCTURE_DATA = STRUCTURE_CONTROLLER_URL
			+ "/data/{fundVersionId}/{structureDataId}";
	protected static final String FIND_STRUCTURE_DATA = STRUCTURE_CONTROLLER_URL
			+ "/data/{fundVersionId}/{structureTypeCode}/search";
	protected static final String GET_STRUCTURE_DATA = STRUCTURE_CONTROLLER_URL
			+ "/data/{fundVersionId}/{structureDataId}";
	protected static final String FIND_STRUCTURE_TYPES = STRUCTURE_CONTROLLER_URL + "/type";
	protected static final String FIND_PART_TYPES = STRUCTURE_CONTROLLER_URL + "/part-type";
	protected static final String FIND_FUND_STRUCTURE_EXTENSION = STRUCTURE_CONTROLLER_URL
			+ "/extension/{fundVersionId}/{structureTypeCode}";
	protected static final String SET_FUND_STRUCTURE_EXTENSION = STRUCTURE_CONTROLLER_URL
			+ "/extension/{fundVersionId}/{structureTypeCode}";
	protected static final String CREATE_STRUCTURE_ITEM = STRUCTURE_CONTROLLER_URL
			+ "/item/{fundVersionId}/{structureDataId}/{itemTypeId}/create";
	protected static final String UPDATE_STRUCTURE_ITEM = STRUCTURE_CONTROLLER_URL
			+ "/item/{fundVersionId}/update/{createNewVersion}";
	protected static final String DELETE_STRUCTURE_ITEM = STRUCTURE_CONTROLLER_URL + "/item/{fundVersionId}/delete";
	protected static final String DELETE_STRUCTURE_ITEMS_BY_TYPE = STRUCTURE_CONTROLLER_URL
			+ "/item/{fundVersionId}/{structureDataId}/{itemTypeId}";
	protected static final String GET_FORM_STRUCTURE_ITEMS = STRUCTURE_CONTROLLER_URL
			+ "/item/form/{fundVersionId}/{structureDataId}";
	protected static final String DUPLICATE_STRUCTURE_DATA_BATCH = STRUCTURE_CONTROLLER_URL
			+ "/data/{fundVersionId}/{structureDataId}/batch";
	protected static final String UPDATE_STRUCTURE_DATA_BATCH = STRUCTURE_CONTROLLER_URL
			+ "/data/{fundVersionId}/{structureTypeCode}/batchUpdate";

	// ARRANGEMENT
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
	protected static final String DESC_ITEM_CSV_IMPORT = ARRANGEMENT_CONTROLLER_URL + "/descItems/{fundVersionId}/csv/import";
	protected static final String DESC_ITEM_CSV_EXPORT = ARRANGEMENT_CONTROLLER_URL + "/descItems/{fundVersionId}/csv/export";
	@Deprecated
	protected static final String CREATE_DESC_ITEM = ARRANGEMENT_CONTROLLER_URL + "/descItems/{fundVersionId}/{nodeId}/{nodeVersion}/{descItemTypeId}/create";
	protected static final String CREATE_DESC_ITEM_NEW = ARRANGEMENT_CONTROLLER_URL + "/descItems/{fundVersionId}/{nodeId}/{nodeVersion}/create";
	@Deprecated
	protected static final String UPDATE_DESC_ITEM = ARRANGEMENT_CONTROLLER_URL + "/descItems/{fundVersionId}/{nodeId}/{nodeVersion}/update/{createNewVersion}";
	protected static final String UPDATE_DESC_ITEM_NEW = ARRANGEMENT_CONTROLLER_URL + "/descItems/{fundVersionId}/{nodeId}/{nodeVersion}/update/{createNewVersion}/new";
	protected static final String DELETE_DESC_ITEM = ARRANGEMENT_CONTROLLER_URL + "/descItems/{fundVersionId}/{nodeId}/{nodeVersion}/delete";
	protected static final String DELETE_DESC_ITEM_BY_TYPE = ARRANGEMENT_CONTROLLER_URL + "/descItems/{fundVersionId}/{nodeId}/{nodeVersion}/{descItemTypeId}";
	protected static final String DELETE_OUTPUT_ITEM_BY_TYPE = ARRANGEMENT_CONTROLLER_URL + "/outputItems/{fundVersionId}/{outputId}/{outputVersion}/{itemTypeId}";
	protected static final String CREATE_OUTPUT_ITEM = ARRANGEMENT_CONTROLLER_URL + "/outputItems/{fundVersionId}/{outputId}/{outputVersion}/{itemTypeId}/create";
	protected static final String UPDATE_OUTPUT_ITEM = ARRANGEMENT_CONTROLLER_URL + "/outputItems/{fundVersionId}/{outputVersion}/update/{createNewVersion}";
	protected static final String DELETE_OUTPUT_ITEM = ARRANGEMENT_CONTROLLER_URL + "/outputItems/{fundVersionId}/{outputVersion}/delete";
	protected static final String FULLTEXT = ARRANGEMENT_CONTROLLER_URL + "/fulltext";
	protected static final String FUND_FULLTEXT = ARRANGEMENT_CONTROLLER_URL + "/fundFulltext";
	protected static final String FUND_FULLTEXT_LIST = ARRANGEMENT_CONTROLLER_URL + "/fundFulltext/{fundId}";
	protected static final String VALIDATE_VERSION = ARRANGEMENT_CONTROLLER_URL + "/validateVersion/{versionId}/{showAll}";
	protected static final String VALIDATE_VERSION_COUNT = ARRANGEMENT_CONTROLLER_URL + "/validateVersionCount/{versionId}";
	protected static final String FA_TREE_NODES = ARRANGEMENT_CONTROLLER_URL + "/fundTree/nodes";
	protected static final String NODE_DATA = ARRANGEMENT_CONTROLLER_URL + "/nodeData";
	protected static final String NODE_FORM_DATA = ARRANGEMENT_CONTROLLER_URL + "/nodes/{nodeId}/{versionId}/form";
	protected static final String OUTPUT_FORM_DATA = ARRANGEMENT_CONTROLLER_URL + "/output/{outputId}/{versionId}/form";
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
	protected static final String ADD_NODES_NAMED_OUTPUT = OUTPUTS + "/{fundVersionId}/{outputId}/add";
	protected static final String REMOVE_NODES_NAMED_OUTPUT = OUTPUTS + "/{fundVersionId}/{outputId}/remove";
	protected static final String DELETE_NAMED_OUTPUT = OUTPUTS + "/{fundVersionId}/{outputId}";
	protected static final String UPDATE_NAMED_OUTPUT = OUTPUTS + "/{fundVersionId}/{outputId}/update";
	protected static final String UPDATE_OUTPUT_SETTINGS = OUTPUTS + "/{outputId}/settings";
	protected static final String FILTER_NODES = ARRANGEMENT_CONTROLLER_URL + "/filterNodes/{versionId}";
	protected static final String FILTERED_NODES = ARRANGEMENT_CONTROLLER_URL + "/getFilterNodes/{versionId}";
	protected static final String FILTERED_FULLTEXT_NODES = ARRANGEMENT_CONTROLLER_URL
			+ "/getFilteredFulltext/{versionId}";
	protected static final String FUND_POLICY = ARRANGEMENT_CONTROLLER_URL + "/fund/policy/{fundVersionId}";
	protected static final String VALIDATION = ARRANGEMENT_CONTROLLER_URL
			+ "/validation/{fundVersionId}/{fromIndex}/{toIndex}";
	protected static final String VALIDATION_ERROR = ARRANGEMENT_CONTROLLER_URL
			+ "/validation/{fundVersionId}/find/{nodeId}/{direction}";
	protected static final String FIND_CHANGE = ARRANGEMENT_CONTROLLER_URL + "/changes/{fundVersionId}";
	protected static final String FIND_CHANGE_BY_DATE = ARRANGEMENT_CONTROLLER_URL + "/changes/{fundVersionId}/date";
	protected static final String REVERT_CHANGES = ARRANGEMENT_CONTROLLER_URL + "/changes/{fundVersionId}/revert";
	protected static final String SET_NOT_IDENTIFIED_DESCITEM = ARRANGEMENT_CONTROLLER_URL
			+ "/descItems/{fundVersionId}/{nodeId}/{nodeVersion}/notUndefined/set";
	protected static final String UNSET_NOT_IDENTIFIED_DESCITEM = ARRANGEMENT_CONTROLLER_URL
			+ "/descItems/{fundVersionId}/{nodeId}/{nodeVersion}/notUndefined/unset";
	protected static final String SET_NOT_IDENTIFIED_OUTPUTITEM = ARRANGEMENT_CONTROLLER_URL
			+ "/outputItems/{fundVersionId}/{outputId}/{outputVersion}/notUndefined/set";
	protected static final String UNSET_NOT_IDENTIFIED_OUTPUTITEM = ARRANGEMENT_CONTROLLER_URL
			+ "/outputItems/{fundVersionId}/{outputId}/{outputVersion}/notUndefined/unset";
	protected static final String COPY_LEVELS_VALIDATE = ARRANGEMENT_CONTROLLER_URL + "/levels/copy/validate";
	protected static final String COPY_LEVELS = ARRANGEMENT_CONTROLLER_URL + "/levels/copy";
	protected static final String NODE_INFO = ARRANGEMENT_CONTROLLER_URL + "/nodeInfo/{fundVersionId}/{nodeId}";

	// DAOs
	protected static final String ARRANGEMENT_DAOS_LINK_CREATE = ARRANGEMENT_CONTROLLER_URL
			+ "/daos/{fundVersionId}/{daoId}/{nodeId}/create";
	protected static final String ARRANGEMENT_DAOS_LINK_DELETE = ARRANGEMENT_CONTROLLER_URL
			+ "/daolinks/{fundVersionId}/{daoLinkId}";
	protected static final String FIND_DAOS = ARRANGEMENT_CONTROLLER_URL + "/daos/{fundVersionId}";

	// PARTY
	protected static final String INSTITUTIONS = PARTY_CONTROLLER_URL + "/institutions";

	// REGISTRY
	protected static final String GET_SCOPE_WITH_CONNECTED = AP_CONTROLLER_URL + "/scopes/{scopeId}/withConnected";
	protected static final String CREATE_SCOPE = AP_CONTROLLER_URL + "/scopes";
	protected static final String UPDATE_SCOPE = AP_CONTROLLER_URL + "/scopes/{scopeId}";
	protected static final String DELETE_SCOPE = AP_CONTROLLER_URL + "/scopes/{scopeId}";
	protected static final String CONNECT_SCOPE = AP_CONTROLLER_URL + "/scopes/{scopeId}/connect";
	protected static final String DISCONNECT_SCOPE = AP_CONTROLLER_URL + "/scopes/{scopeId}/disconnect";
	protected static final String FA_SCOPES = AP_CONTROLLER_URL + "/fundScopes";
	protected static final String ALL_SCOPES = AP_CONTROLLER_URL + "/scopes";
	protected static final String RECORD_TYPES = AP_CONTROLLER_URL + "/recordTypes";

	protected static final String FIND_RECORD = AP_CONTROLLER_URL + "/search";
	protected static final String GET_RECORD = AP_CONTROLLER_URL + "/{recordId}";
	protected static final String CREATE_ACCESS_POINT = AP_CONTROLLER_URL + "/";
	protected static final String UPDATE_RECORD = AP_CONTROLLER_URL + "/{recordId}";
	protected static final String USAGES_RECORD = AP_CONTROLLER_URL + "/{recordId}/usage";
	protected static final String MERGE_AP = AP_CONTROLLER_URL + "/revision/{entityId}/merge";
	protected static final String REPLACE_RECORD = AP_CONTROLLER_URL + "/{recordId}/replace";
	protected static final String CHANGE_STATE = REST_CONTROLLER_URL + "/accesspoint/{recordId}/state";

	protected static final String GET_LANGUAGES = AP_CONTROLLER_URL + "/languages";
	protected static final String GET_EXTERNAL_ID_TYPES = AP_CONTROLLER_URL + "/eidTypes";

	protected static final String CREATE_VARIANT_RECORD = AP_CONTROLLER_URL + "/variantRecord/";
	protected static final String UPDATE_VARIANT_RECORD = AP_CONTROLLER_URL + "/variantRecord/{variantRecordId}";
	protected static final String DELETE_VARIANT_RECORD = AP_CONTROLLER_URL + "/variantRecord/{variantRecordId}";

	protected static final String RECORD_TYPES_FOR_PARTY_TYPE = AP_CONTROLLER_URL + "/recordTypesForPartyType";

	// PART
	protected static final String CREATE_PART = AP_CONTROLLER_URL + "/{accessPointId}/part";
	protected static final String UPDATE_PART = AP_CONTROLLER_URL + "/{accessPointId}/part/{partId}";
	protected static final String DELETE_PART = REST_CONTROLLER_URL + "/accesspoint/{accessPointId}/part/{partId}";
	protected static final String SET_PREFER_NAME = REST_CONTROLLER_URL
			+ "/accesspoint/{accessPointId}/part/{partId}/prefer-name";

	// RULE
	protected static final String RULE_SETS = RULE_CONTROLLER_URL + "/getRuleSets";
	protected static final String DATA_TYPES = RULE_CONTROLLER_URL + "/dataTypes";
	protected static final String DESC_ITEM_TYPES = RULE_CONTROLLER_URL + "/descItemTypes";
	protected static final String TEMPLATES = RULE_CONTROLLER_URL + "/templates";
	protected static final String PACKAGES = RULE_CONTROLLER_URL + "/getPackages";
	protected static final String OUTPUT_FILTERS = RULE_CONTROLLER_URL + "/outputFilters";
	protected static final String EXPORT_FILTERS = RULE_CONTROLLER_URL + "/exportFilters";
	protected static final String ITEM_TYPE_CODES = RULE_CONTROLLER_URL + "/itemTypeCodes/{ruleSetId}";
	protected static final String POLICY = RULE_CONTROLLER_URL + "/policy";
	protected static final String POLICY_TYPES = POLICY + "/types/{fundVersionId}";
	protected static final String POLICY_ALL_TYPES = POLICY + "/types";
	protected static final String POLICY_SET = POLICY + "/{nodeId}/{fundVersionId}";
	protected static final String POLICY_GET = POLICY + "/{nodeId}/{fundVersionId}";

	// Validation
	protected static final String VALIDATE_UNIT_DATE = VALIDATION_CONTROLLER_URL + "/unitDate";

	// Import/Export
	protected final static String DE_IMPORT = DE_IMPORT_CONTROLLER_URL + "/import";
	// protected final static String DE_EXPORT = DE_EXPORT_CONTROLLER_URL +
	// "/create";

	// Import coodrinates
	protected static final String IMPORT_COORDINATES = AP_CONTROLLER_URL + "/import/coordinates";

	// Uživatelé a skupiny
	protected final static String USER_DETAIL = USER_CONTROLLER_URL + "/detail";
	protected final static String CHANGE_PASSWORD = USER_CONTROLLER_URL + "/{userId}/password";
	protected final static String CHANGE_PASSWORD_USER = USER_CONTROLLER_URL + "/password";
	protected final static String FIND_USER = USER_CONTROLLER_URL;
	protected final static String GET_USER = USER_CONTROLLER_URL + "/{userId}";
	protected final static String ACTIVE_USER = USER_CONTROLLER_URL + "/{userId}/active/{active}";
	protected final static String CREATE_USER = USER_CONTROLLER_URL;
	protected final static String FIND_GROUP = GROUP_CONTROLLER_URL;
	protected final static String GET_GROUP = GROUP_CONTROLLER_URL + "/{groupId}";
	protected final static String CREATE_GROUP = GROUP_CONTROLLER_URL;
	protected final static String DELETE_GROUP = GROUP_CONTROLLER_URL + "/{groupId}";
	protected final static String CHANGE_GROUP = GROUP_CONTROLLER_URL + "/{groupId}";
	protected final static String JOIN_GROUP = USER_CONTROLLER_URL + "/group/join";
	protected final static String LEAVE_GROUP = USER_CONTROLLER_URL + "/group/{groupId}/leave/{userId}";

	protected final static String ADD_USER_PERMISSION = USER_CONTROLLER_URL + "/{userId}/permission/add";
	protected final static String DELETE_USER_PERMISSION = USER_CONTROLLER_URL + "/{userId}/permission/delete";
	protected final static String DELETE_USER_FUND_PERMISSION = USER_CONTROLLER_URL
			+ "/{userId}/permission/delete/fund/{fundId}";
	protected final static String DELETE_USER_FUND_ALL_PERMISSION = USER_CONTROLLER_URL
			+ "/{userId}/permission/delete/fund/all";

	protected final static String ADD_GROUP_PERMISSION = GROUP_CONTROLLER_URL + "/{groupId}/permission/add";
	protected final static String DELETE_GROUP_PERMISSION = GROUP_CONTROLLER_URL + "/{groupId}/permission/delete";
	protected final static String DELETE_GROUP_FUND_PERMISSION = GROUP_CONTROLLER_URL
			+ "/{groupId}/permission/delete/fund/{fundId}";
	protected final static String DELETE_GROUP_FUND_ALL_PERMISSION = GROUP_CONTROLLER_URL
			+ "/{groupId}/permission/delete/fund/all";

	protected static final String ALL_ISSUE_TYPES = ISSUE_CONTROLLER_URL + "/issue_types";
	protected static final String ALL_ISSUE_STATES = ISSUE_CONTROLLER_URL + "/issue_states";
	protected static final String CREATE_ISSUE_LIST = ISSUE_CONTROLLER_URL + "/issue_lists";
	protected static final String CREATE_ISSUE = ISSUE_CONTROLLER_URL + "/issues";
	protected static final String CREATE_COMMENT = ISSUE_CONTROLLER_URL + "/comments";
	protected static final String GET_ISSUE_LIST = ISSUE_CONTROLLER_URL + "/issue_lists/{issueListId}";
	protected static final String GET_ISSUE = ISSUE_CONTROLLER_URL + "/issues/{issueId}";
	protected static final String GET_COMMENT = ISSUE_CONTROLLER_URL + "/comments/{commentId}";
	protected static final String FIND_ISSUE_LISTS = ISSUE_CONTROLLER_URL + "/funds/{fundId}/issue_lists";
	protected static final String FIND_ISSUES = ISSUE_CONTROLLER_URL + "/issue_lists/{issueListId}/issues";
	protected static final String FIND_COMMENTS = ISSUE_CONTROLLER_URL + "/issues/{issueId}/comments";
	protected static final String UPDATE_ISSUE_LIST = ISSUE_CONTROLLER_URL + "/issue_lists/{issueListId}";
	protected static final String UPDATE_ISSUE = ISSUE_CONTROLLER_URL + "/issues/{issueId}";
	protected static final String SET_ISSUE_TYPE = ISSUE_CONTROLLER_URL + "/issues/{issueId}/type";
	protected static final String UPDATE_COMMENT = ISSUE_CONTROLLER_URL + "/comments/{commentId}";
	protected static final String EXPORT_ISSUE_LIST = ISSUE_CONTROLLER_URL + "/issue_lists/{issueListId}/export";

	// FUND
	protected static final String FUND_V1 = FUND_CONTROLLER_URL + "/fund/{id}";
	protected static final String FUNDS_V1 = FUND_CONTROLLER_URL + "/fund";
	protected static final String FUND_DELETE_STRUCTURE_DATA = FUND_CONTROLLER_URL + "/fund/{id}/structuredObject";

	// ACCESSPOINTS
	protected static final String DELETE_ACCESSPOINTS_ID = FUND_CONTROLLER_URL + "/accesspoints/{id}";

	// ŠABLONY PRO JP
	protected static final String CREATE_NODE_TEMPLATE = NODES + "/{fundId}/template/create";
	protected static final String UPDATE_NODE_TEMPLATE = NODES + "/template/{templateId}";
	protected static final String DELETE_NODE_TEMPLATE = NODES + "/template/{templateId}";
	protected static final String GET_NODE_TEMPLATES = NODES + "/{fundId}/template";
	protected static final String CREATE_NODE_TEMPLATE_MAP_TYPE = NODES + "/template/{templateId}/map-type";
	protected static final String UPDATE_NODE_TEMPLATE_MAP_TYPE = NODES + "/template/{templateId}/map-type/{mapTypeId}";
	protected static final String DELETE_NODE_TEMPLATE_MAP_TYPE = NODES + "/template/{templateId}/map-type/{mapTypeId}";

	// ArrangementWebsocketControllerTest
	protected static final String INHIBIT_DESC_ITEM = "/app/arrangement/descItems/inhibit";
	protected static final String ALLOW_DESC_ITEM = "/app/arrangement/descItems/allow";

	protected final static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.000ZZZZZ");

	public static final String SCOPE_GLOBAL = "GLOBAL";
	public static final String SCOPE_COPY = "COPY";

	@Value("${local.server.port}")
	protected int port;

	private List<RulDataTypeVO> dataTypes = null;
	private List<RulDescItemTypeExtVO> descItemTypes = null;

	private ApiClient elzaApiClient;

	protected FundsApi fundsApi;

	protected AccesspointsApi accesspointsApi;

	protected SearchApi searchApi;

	protected DaosApi daosApi;

	protected IoApi ioApi;

	protected AdminApi adminApi;

	protected DescitemsApi descitemsApi;

	protected static Map<String, String> cookies = null;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		RestAssured.port = port; // nastavi default port pro REST-assured
		RestAssured.baseURI = RestAssured.DEFAULT_URI; // nastavi default URI pro REST-assured. Nejcasteni localhost

		// Inicializace openapi clienta
		elzaApiClient = new ApiClient();
		elzaApiClient.setBasePath(RestAssured.DEFAULT_URI + ":" + port + REST_CONTROLLER_URL);
//        elzaApiClient.setConnectTimeout(5 * 60 * 1000);
//        elzaApiClient.setReadTimeout(5 * 60 * 1000);
//        elzaApiClient.setWriteTimeout(5 * 60 * 1000);

		adminApi = new cz.tacr.elza.test.controller.AdminApi(elzaApiClient);
		fundsApi = new cz.tacr.elza.test.controller.FundsApi(elzaApiClient);
		daosApi = new cz.tacr.elza.test.controller.DaosApi(elzaApiClient);
		accesspointsApi = new cz.tacr.elza.test.controller.AccesspointsApi(elzaApiClient);
		searchApi = new cz.tacr.elza.test.controller.SearchApi(elzaApiClient);
		ioApi = new cz.tacr.elza.test.controller.IoApi(elzaApiClient);
		descitemsApi = new cz.tacr.elza.test.controller.DescitemsApi(elzaApiClient);

		loginAsAdmin();

		// Nastaveni autentizace
		cookies.forEach((name, value) -> {
			elzaApiClient.addDefaultCookie(name, value);
		});

		if (loadInstitutions) {
			importXmlFile(null, 1, getResourceFile(XML_INSTITUTION));
		}
	}

	/**
	 * Provede prihlášení jako uživatel 'admin'
	 */
	protected void loginAsAdmin() {
		login("admin", "admin");
	}

	/**
	 * Provede prihlášení jako daný uživatel
	 */
	protected void login(String username, String password) {
		RequestSpecification requestSpecification = given();
		requestSpecification.formParam("username", username);
		requestSpecification.formParam("password", password);
		requestSpecification.header(WWW_FORM_CT_HEADER).config(UTF8_ENCODER_CONFIG);

		Response response = requestSpecification.post("/login");
		if (response.getStatusCode() != HttpStatus.OK.value()) {
			// log request
			requestSpecification.log().all();
		}
		cookies = response.getCookies();
	}

	private Map<String, Integer> counterMap = new HashMap<>();

	protected void counter(final String text) {
		Integer count = counterMap.get(text);
		if (count == null) {
			count = 1;
			counterMap.put(text, count);
		} else {
			counterMap.put(text, ++count);
		}
		logger.info(text + ": #" + count);
	}

	public static Response get(final String url) {
		return httpMethod((spec) -> spec, url, HttpMethod.GET, HttpStatus.OK);
	}

	public static Response get(final Function<RequestSpecification, RequestSpecification> params, final String url) {
		return httpMethod(params, url, HttpMethod.GET, HttpStatus.OK);
	}

	public static Response post(final Function<RequestSpecification, RequestSpecification> params, final String url) {
		return httpMethod(params, url, POST, HttpStatus.OK);
	}

	public static Response post(final Function<RequestSpecification, RequestSpecification> params, final String url,
			final HttpStatus status) {
		return httpMethod(params, url, HttpMethod.POST, status);
	}

	public static Response put(final Function<RequestSpecification, RequestSpecification> params, final String url) {
		return httpMethod(params, url, PUT, HttpStatus.OK);
	}

	public static Response put(final Function<RequestSpecification, RequestSpecification> params, final String url,
			final HttpStatus status) {
		return httpMethod(params, url, PUT, status);
	}

	public static Response delete(final Function<RequestSpecification, RequestSpecification> params, final String url) {
		return httpMethod(params, url, HttpMethod.DELETE, HttpStatus.OK);
	}

	public static Response httpMethod(final Function<RequestSpecification, RequestSpecification> params,
			final String url, final HttpMethod method, final HttpStatus expectedStatus) {
		return httpMethod(params, url, method, expectedStatus, JSON_CT_HEADER);
	}

	public static Response httpMethod(final Function<RequestSpecification, RequestSpecification> params,
			final String url, final HttpMethod method, final HttpStatus expectedStatus, final Header header) {
		Assert.assertNotNull(params);
		Assert.assertNotNull(url);
		Assert.assertNotNull(method);

		RequestSpecification requestSpecification = params.apply(given());

		// add header
		requestSpecification.header(header);
		requestSpecification.config(UTF8_ENCODER_CONFIG);
		requestSpecification.cookies(cookies);

		Response response;
		if (GET.equals(method)) {
			response = requestSpecification.get(url);
		} else if (PUT.equals(method)) {
			response = requestSpecification.put(url);
		} else if (DELETE.equals(method)) {
			response = requestSpecification.delete(url);
		} else if (HEAD.equals(method)) {
			response = requestSpecification.head(url);
		} else if (OPTIONS.equals(method)) {
			response = requestSpecification.options(url);
		} else if (PATCH.equals(method)) {
			response = requestSpecification.patch(url);
		} else if (POST.equals(method)) {
			response = requestSpecification.post(url);
		} else {
			throw new IllegalStateException("Nedefinovaný stav " + method + ".");
		}

		// if the expected status is defined, check it
		if (expectedStatus != null) {
			if (expectedStatus.value() != response.statusCode()) {
				// Log request if status code failed
				requestSpecification.log().all();

				String msg = formatResponse(response);
				logger.info(msg);

				StringBuilder msgBuilder = new StringBuilder();
				msgBuilder.append("Received unexpected status code: ").append(response.statusCode())
						.append(", expected: ").append(expectedStatus.value()).append(", detail: ").append(msg);
				Assert.fail(msgBuilder.toString());
			}
		}

		return response;
	}

	private static String formatResponse(final Response response) {
		String contentType = response.contentType();
		StringBuilder msg = new StringBuilder();
		msg.append("Response status: ").append(response.statusLine());
		if (contentType != null) {
			msg.append(", content-type: ").append(contentType);

			if (contentType.startsWith(JSON_CONTENT_TYPE) || contentType.startsWith("text/")) {
				ResponseBody<?> responseBody = response;
				ResponseOptions<?> responseOptions = response;
				String body = new Prettifier().getPrettifiedBodyIfPossible(responseOptions, responseBody);
				msg.append("\n").append("Response body:").append(body);

			}
		}
		return msg.toString();
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

		requestSpecification.header(MULTIPART_HEADER).cookies(cookies);
		requestSpecification.config(UTF8_ENCODER_CONFIG);

		Response response = requestSpecification.post(url);

		if (HttpStatus.OK.value() != response.statusCode()) {
			// log only if error
			requestSpecification.log().all();

			String msg = formatResponse(response);
			logger.error(msg);
			Assert.fail("Received error, code: " + response.statusCode() + ", detail: " + msg);
		}

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
	 * @param versionId verze AP
	 * @return seznam pravidel
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
	 * Vytvoření výchozí archivní pomůcky.
	 *
	 * @param name název AP
	 * @return ap
	 * @throws ApiException
	 */
	protected Fund createFund(final String name, final String internalCode) {
		List<RulRuleSetVO> ruleSets = getRuleSets();
		RulRuleSetVO ruleSet = ruleSets.get(1);
		ParInstitutionVO institution = getInstitutions().get(0);

		CreateFund createFund = new CreateFund();
		createFund.setName(name);
		createFund.setRuleSetCode(ruleSet.getCode());
		createFund.setInstitutionIdentifier(institution.getCode());
		createFund.setInternalCode(internalCode);

		List<String> scopes = new ArrayList<>();
		scopes.add(SCOPE_GLOBAL);
		createFund.setScopes(scopes);

		return fundsApi.fundCreateFund(createFund);
	}

	protected FundDetail getFundV1(final Integer id) {
		Response response = get(spec -> spec.pathParam("id", id), FUND_V1);
		return response.getBody().as(FundDetail.class);
	}

	protected FindFundsResult findFunds(final String fulltext, final String institutionIdentifier, final Integer max,
			final Integer from) {
		Response response = get(
				spec -> spec.queryParam("fulltext", fulltext).queryParam("institutionIdentifier", institutionIdentifier)
						.queryParam("max", max).queryParam("from", from),
				FUNDS_V1);
		return response.getBody().as(FindFundsResult.class);
	}

	/**
	 * Uzavření verze archivní pomůcky.
	 *
	 * @param fundVersion verze archivní pomůcky
	 * @return nová verze ap
	 */
	protected ArrFundVersionVO approveVersion(final ArrFundVersionVO fundVersion) {
		return approveVersion(fundVersion.getId());
	}

	/**
	 * Uzavření verze archivní pomůcky.
	 *
	 * @param versionId identifikátor verze archivní pomůcky
	 * @return nová verze ap
	 */
	protected ArrFundVersionVO approveVersion(final Integer versionId) {
		Response response = put(spec -> spec.queryParam("versionId", versionId), APPROVE_VERSION);
		return response.getBody().as(ArrFundVersionVO.class);
	}

	/**
	 * Vrátí archivní pomůcky s verzema.
	 *
	 * @return archivní pomůcky
	 */
	protected List<ArrFundVO> getFunds() {
		Response response = get(spec -> spec.queryParam("max", 200), FUNDS);
		FundListCountResult as = response.getBody().as(FundListCountResult.class);
		return as.getList();
	}

	/**
	 * Vrátí archivní pomůcku.
	 *
	 * @return archivní pomůcka
	 */
	protected ArrFundVO getFund(final Integer fundId) {
		Response response = get(spec -> spec.pathParam("fundId", fundId), FUND);
		return response.getBody().as(ArrFundVO.class);
	}

	/**
	 * Vrátí node + fund
	 *
	 * @param fundVersionId
	 * @param nodeId
	 * @return
	 */
	protected ArrNodeExtendVO getNodeInfo(final Integer fundVersionId, final Integer nodeId) {
		Response response = get(spec -> spec.pathParam("fundVersionId", fundVersionId).pathParam("nodeId", nodeId),
				NODE_INFO);
		return response.getBody().as(ArrNodeExtendVO.class);
	}

	/**
	 * Přidání nového uzlu.
	 *
	 * @param addLevelParam parametry pro vytvoření nového uzlu
	 * @return nový uzel
	 */
	protected ArrangementController.NodeWithParent addLevel(final AddLevelParam addLevelParam) {
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
	 * @param direction        směr přidání
	 * @param fundVersion      verze archivní pomůcky
	 * @param staticNode       uzel vůči kterému přidávám
	 * @param parentStaticNode rodič uzlu vůči kterému přidávám
	 * @return vytvořený uzel
	 */
	protected ArrangementController.NodeWithParent addLevel(final FundLevelService.AddLevelDirection direction,
			final ArrFundVersionVO fundVersion, final ArrNodeVO staticNode, final ArrNodeVO parentStaticNode,
			final String scenarioName) {
		AddLevelParam addLevelParam = new AddLevelParam();
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
	 * @param fundVersion         verze archivní pomůcky
	 * @param staticNode          uzel vůči kterému přesouvám
	 * @param staticNodeParent    rodič uzlu vůči kterému přesouvám
	 * @param transportNodes      přesouvaný uzly
	 * @param transportNodeParent rodič přesouvaných uzlů
	 */
	protected void moveLevelBefore(final ArrFundVersionVO fundVersion, final ArrNodeVO staticNode,
			final ArrNodeVO staticNodeParent, final List<ArrNodeVO> transportNodes,
			final ArrNodeVO transportNodeParent) {
		ArrangementController.LevelMoveParam moveParam = createMoveParam(fundVersion, staticNode, staticNodeParent,
				transportNodes, transportNodeParent);
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
	 * @param fundVersion         verze archivní pomůcky
	 * @param staticNode          uzel vůči kterému přesouvám
	 * @param staticNodeParent    rodič uzlu vůči kterému přesouvám
	 * @param transportNodes      přesouvaný uzly
	 * @param transportNodeParent rodič přesouvaných uzlů
	 */
	protected void moveLevelAfter(final ArrFundVersionVO fundVersion, final ArrNodeVO staticNode,
			final ArrNodeVO staticNodeParent, final List<ArrNodeVO> transportNodes,
			final ArrNodeVO transportNodeParent) {
		ArrangementController.LevelMoveParam moveParam = createMoveParam(fundVersion, staticNode, staticNodeParent,
				transportNodes, transportNodeParent);
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
	 * @param fundVersion         verze archivní pomůcky
	 * @param staticNode          uzel vůči kterému přesouvám
	 * @param staticNodeParent    rodič uzlu vůči kterému přesouvám
	 * @param transportNodes      přesouvaný uzly
	 * @param transportNodeParent rodič přesouvaných uzlů
	 */
	protected void moveLevelUnder(final ArrFundVersionVO fundVersion, final ArrNodeVO staticNode,
			final ArrNodeVO staticNodeParent, final List<ArrNodeVO> transportNodes,
			final ArrNodeVO transportNodeParent) {
		ArrangementController.LevelMoveParam moveParam = createMoveParam(fundVersion, staticNode, staticNodeParent,
				transportNodes, transportNodeParent);
		moveLevelUnder(moveParam);
	}

	/**
	 * Vytvoření parametrů pro přesun.
	 *
	 * @param fundVersion         verze archivní pomůcky
	 * @param staticNode          uzel vůči kterému přesouvám
	 * @param staticNodeParent    rodič uzlu vůči kterému přesouvám
	 * @param transportNodes      přesouvaný uzly
	 * @param transportNodeParent rodič přesouvaných uzlů
	 * @return parametry přesunu
	 */
	private ArrangementController.LevelMoveParam createMoveParam(final ArrFundVersionVO fundVersion,
			final ArrNodeVO staticNode, final ArrNodeVO staticNodeParent, final List<ArrNodeVO> transportNodes,
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
	 *
	 * @param fundId id souboru
	 */
	protected void deleteFund(final Integer fundId) {
		delete(spec -> spec.pathParam("fundId", fundId), DELETE_FUND);
	}

	/**
	 * Smazání uzlu.
	 *
	 * @param fundVersion      verze archivní pomůcky
	 * @param staticNode       uzel který mažu
	 * @param staticNodeParent rodič uzlu který mažu
	 * @return smazaný uzel s rodičem
	 */
	protected ArrangementController.NodeWithParent deleteLevel(final ArrFundVersionVO fundVersion,
			final ArrNodeVO staticNode, final ArrNodeVO staticNodeParent) {
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
		Response response = get(spec -> spec.queryParam("value", value), VALIDATE_UNIT_DATE);
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
	protected List<PackageVO> getPackages() {
		return Arrays.asList(get(PACKAGES).getBody().as(PackageVO[].class));
	}

	/**
	 * Získání listu RulOutputFilter
	 *
	 * @return list RulOutputFilter
	 */
	protected List<RulOutputFilterVO> getOutputFilters() {
		return Arrays.asList(get(OUTPUT_FILTERS).getBody().as(RulOutputFilterVO[].class));
	}

	/**
	 * Získání listu RulExportFilter
	 *
	 * @return list RulExportFilter
	 */
	protected List<RulExportFilterVO> getExportFilters() {
		return Arrays.asList(get(EXPORT_FILTERS).getBody().as(RulExportFilterVO[].class));
	}

	/**
	 * Získání listu kódů možných item typů
	 *
	 * @return list kódů item typů
	 */
	protected List<String> getItemTypeCodesByRuleSet(RulRuleSetVO rulRuleSetVO) {
		Response response = get(spec -> spec.pathParam("ruleSetId", rulRuleSetVO.getId()), ITEM_TYPE_CODES);
		return Arrays.asList(response.getBody().as(String[].class));
	}

	/**
	 * Vytvoření hodnoty atributu.
	 *
	 * @param descItem     hodnota atributu
	 * @param fundVersion  verze archivní pomůcky
	 * @param node         uzel
	 * @param descItemType typ atributu
	 * @return vytvořená hodnota atributu
	 */
	@Deprecated
	protected ArrangementController.DescItemResult createDescItem(final ArrItemVO descItem,
			final ArrFundVersionVO fundVersion, final ArrNodeVO node, final RulDescItemTypeVO descItemType) {
		return createDescItem(descItem, fundVersion.getId(), descItemType.getId(), node.getId(), node.getVersion());
	}

	/**
	 * Vytvoření hodnoty atributu.
	 *
	 * @param descItem       hodnota atributu
	 * @param fundVersionId  identifikátor verze AP
	 * @param descItemTypeId identifikátor typu hodnoty atributu
	 * @param nodeId         identfikátor uzlu
	 * @param nodeVersion    verze uzlu
	 * @return vytvořená hodnota atributu
	 */
	@Deprecated
	protected ArrangementController.DescItemResult createDescItem(final ArrItemVO descItem, final Integer fundVersionId,
			final Integer descItemTypeId, final Integer nodeId, final Integer nodeVersion) {
		Response response = put(spec -> spec.body(descItem).pathParam("fundVersionId", fundVersionId)
				.pathParam("descItemTypeId", descItemTypeId).pathParam("nodeId", nodeId)
				.pathParam("nodeVersion", nodeVersion), CREATE_DESC_ITEM, null);

		if (response.getStatusCode() == 500) {
			ExceptionResponse exResponse = response.getBody().as(ExceptionResponse.class);
			throw new BusinessException(exResponse.getMessage(),
					ExceptionUtils.getErrorCodeEnum(exResponse.getType(), exResponse.getCode()));
		}
		return response.getBody().as(ArrangementController.DescItemResult.class);
	}

	protected ArrangementController.OutputItemResult createOutputItem(final ArrItemVO outputItemVO,
			final Integer fundVersionId, final Integer itemTypeId, final Integer outputId,
			final Integer outputVersion) {
		assertNotNull(outputItemVO);
		assertNotNull(fundVersionId);
		assertNotNull(itemTypeId);
		assertNotNull(outputId);
		assertNotNull(outputVersion);

		Response response = put(spec -> spec.body(outputItemVO).pathParam("fundVersionId", fundVersionId)
				.pathParam("itemTypeId", itemTypeId).pathParam("outputId", outputId)
				.pathParam("outputVersion", outputVersion), CREATE_OUTPUT_ITEM);
		return response.getBody().as(ArrangementController.OutputItemResult.class);
	}

	protected ArrangementController.OutputItemResult updateOutputItem(final ArrItemVO outputItemVO,
			final Integer fundVersionId, final Integer outputVersion, final Boolean createNewVersion) {
		Response response = put(
				spec -> spec.body(outputItemVO).pathParam("fundVersionId", fundVersionId)
						.pathParam("createNewVersion", createNewVersion).pathParam("outputVersion", outputVersion),
				UPDATE_OUTPUT_ITEM);
		return response.getBody().as(ArrangementController.OutputItemResult.class);
	}

	public ArrangementController.OutputItemResult deleteOutputItem(final Integer outputItemId,
			final Integer fundVersionId, final Integer outputVersion) {
		Response response = post(spec -> spec.body(outputItemId).pathParam("fundVersionId", fundVersionId)
				.pathParam("outputVersion", outputVersion), DELETE_OUTPUT_ITEM);
		return response.getBody().as(ArrangementController.OutputItemResult.class);
	}

	protected InputStream descItemCsvExport(final Integer fundVersionId, final Integer descItemObjectId) {

		Response response = get(
				spec -> spec.pathParam("fundVersionId", fundVersionId).param("descItemObjectId", descItemObjectId),
				DESC_ITEM_CSV_EXPORT);
		return response.getBody().asInputStream();
	}

	protected ArrangementController.DescItemResult descItemCsvImport(final Integer fundVersionId,
			final Integer nodeVersion, final Integer nodeId, final Integer descItemTypeId, final File importFile) {

		HashMap<String, Object> params = new HashMap<>();
		params.put("nodeVersion", nodeVersion);
		params.put("nodeId", nodeId);
		params.put("descItemTypeId", descItemTypeId);

		Response response = multipart(
				spec -> spec.pathParam("fundVersionId", fundVersionId).multiPart("file", importFile).params(params),
				DESC_ITEM_CSV_IMPORT);
		return response.getBody().as(ArrangementController.DescItemResult.class);
	}

	/**
	 * Upravení hodnoty atributu.
	 *
	 * @param descItem         hodnota atributu
	 * @param fundVersion      verze archivní pomůcky
	 * @param node             uzel
	 * @param createNewVersion vytvořit novou verzi?
	 * @return upravená hodnota atributu
	 */
	@Deprecated
	protected ArrangementController.DescItemResult updateDescItem(final ArrItemVO descItem,
			final ArrFundVersionVO fundVersion, final ArrNodeVO node, final Boolean createNewVersion) {
		return updateDescItem(descItem, fundVersion.getId(), node.getId(), node.getVersion(), createNewVersion);
	}

	/**
	 * Upravení hodnoty atributu.
	 *
	 * @param descItem         hodnota atributu
	 * @param fundVersionId    identifikátor verze AP
	 * @param nodeId           identifikátor uzlu
	 * @param nodeVersion      verze uzlu
	 * @param createNewVersion vytvořit novou verzi?
	 * @return upravená hodnota atributu
	 */
	@Deprecated
	protected ArrangementController.DescItemResult updateDescItem(final ArrItemVO descItem, final Integer fundVersionId,
			final Integer nodeId, final Integer nodeVersion, final Boolean createNewVersion) {
		Response response = put(spec -> spec.body(descItem).pathParam("fundVersionId", fundVersionId)
				.pathParam("nodeVersion", nodeVersion).pathParam("nodeId", nodeId)
				.pathParam("createNewVersion", createNewVersion), UPDATE_DESC_ITEM);
		return response.getBody().as(ArrangementController.DescItemResult.class);
	}

	/**
	 * Smazání hodnoty atributu.
	 *
	 * @param descItem    hodnota atributu
	 * @param fundVersion verze archivní pomůcky
	 * @param node        uzel
	 * @return smazaná hodnota atributu
	 */
	protected ArrangementController.DescItemResult deleteDescItem(final ArrItemVO descItem,
			final ArrFundVersionVO fundVersion, final ArrNodeVO node) {
		return deleteDescItem(descItem, fundVersion.getId(), node.getId(), node.getVersion());
	}

	/**
	 * Smazání hodnoty atributu.
	 *
	 * @param descItem      hodnota atributu
	 * @param fundVersionId identifikátor verze AP
	 * @param nodeId        identifikátor uzlu
	 * @param nodeVersion   verze uzlu
	 * @return smazaná hodnota atributu
	 */
	protected ArrangementController.DescItemResult deleteDescItem(final ArrItemVO descItem, final Integer fundVersionId,
			final Integer nodeId, final Integer nodeVersion) {
		Response response = post(spec -> spec.body(descItem).pathParam("fundVersionId", fundVersionId)
				.pathParam("nodeId", nodeId).pathParam("nodeVersion", nodeVersion), DELETE_DESC_ITEM);
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
	 * @param undefined        nezjištěný (bez hodnoty)
	 * @return vytvořený object hodnoty atributu
	 */
	@Deprecated
	protected ArrItemVO buildDescItem(final String typeCode, final String specCode, final Object value,
			final Integer position, final Integer descItemObjectId, final Boolean undefined) {
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

		case "RECORD_REF": {
			descItem = new ArrItemRecordRefVO();
			((ArrItemRecordRefVO) descItem).setValue(((ApAccessPointVO) value).getId());
			break;
		}

		case "DECIMAL": {
			descItem = new ArrItemDecimalVO();
			((ArrItemDecimalVO) descItem).setValue((BigDecimal) value);
			break;
		}

		case "STRUCTURED": {
			descItem = new ArrItemStructureVO();
			((ArrItemStructureVO) descItem).setValue(((ArrStructureDataVO) value).getId());
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

		case "URI_REF": {
			descItem = new ArrItemUriRefVO();
			((ArrItemUriRefVO) descItem).setValue(((String) value));
			break;
		}

		case "BIT": {
			descItem = new ArrItemBitVO();
			((ArrItemBitVO) descItem).setValue(((boolean) value));
			break;
		}

		case "DATE": {
			descItem = new ArrItemDateVO();
			((ArrItemDateVO) descItem).setValue((LocalDate) value);
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
		descItem.setUndefined(undefined);

		return descItem;
	}

	/**
	 * Vytvoření objektu pro hodnotu atributu (nová).
	 *
	 * @param type       typ atributu
	 * @param node       atributový uzel
	 * @param value      hodnota
	 * @param itemSpecId id spec atributu
	 * @param undefined  nezjištěný (bez hodnoty)
	 * @return vytvořený object hodnoty atributu
	 */
	protected NodeItem buildDescItem(final RulDescItemTypeExtVO type,
			                         final ArrNodeVO node,
									 final Object value, 
									 final Integer itemSpecId, 
									 final Boolean undefined) {
		Validate.notNull(type, "Musí být vyplněn typ atributu");
		Validate.notNull(type.getDataTypeId(), "Musí být vyplněn kód typu atributu");
		Validate.notNull(node, "Musí být vyplněn atributový uzel");

		ItemData data = getItemDataByDataTypeId(type.getDataTypeId(), value);
		data.setDataTypeId(type.getDataTypeId());

		NodeItem nodeItem = new NodeItem();
		nodeItem.setItemTypeId(type.getId());
		nodeItem.setItemSpecId(itemSpecId);
		nodeItem.setNodeId(node.getId());
		nodeItem.setNodeVersion(node.getVersion());
		nodeItem.setUndefined(undefined);
		nodeItem.setData(data);

		return nodeItem;
	}

	/**
	 * Získání odpovídající třídy s daty
	 * 
	 * @param dataTypeId datový typ id 
	 * @param value		 hodnota
	 * @return
	 */
	public ItemData getItemDataByDataTypeId(final Integer dataTypeId, final Object value) {
    	Validate.notNull(dataTypeId, "Id datového typu musí být vyplněna");
    	Validate.notNull(value, "Hodnota musí být vyplněna");

    	ItemData data = new ItemData();
        DataType dataType = DataType.fromId(dataTypeId);
        switch (dataType) {
	        case INT:
	            data = new DataInteger();
	            ((DataInteger) data).setIntegerValue((Integer) value);
	            break;
	        case STRING:
	            data = new DataString();
	            ((DataString) data).setStringValue((String) value);
	            break;
	        case TEXT:
	        	data = new DataText();
	            ((DataText) data).setTextValue((String) value);
	            break;
	        case UNITDATE:
	        	data = new DataUnitdate();
	        	((DataUnitdate) data).setValue((String) value);
	        	break;
	        case UNITID:
	        	data = new DataUnitid();
	        	((DataUnitid) data).setUnitId((String) value);
	        	break;
	        case FORMATTED_TEXT:
	        	data = new DataFormattedText();
	        	((DataFormattedText) data).setValue((String) value);
	        	break;
	        case COORDINATES:
	        	data = new DataCoordinates();
	        	((DataCoordinates) data).setValue((String) value);
	        	break;
	        case RECORD_REF:
	        	data = new DataRecordRef();
	        	((DataRecordRef) data).setValue(((ApAccessPointVO) value).getId());
	        	break;
	        case DECIMAL:
	        	data = new DataDecimal();
	        	((DataDecimal) data).setValue(((BigDecimal) value));
	        	break;
	        case STRUCTURED:
	        	data = new DataStructureRef();
	        	((DataStructureRef) data).setStructuredObjectId(((ArrStructureDataVO) value).getId());
	        	break;
	        case ENUM:
	        	data = new DataNull();
	        	break;
	        case FILE_REF:
	        	data = new DataFileRef();
	        	((DataFileRef) data).setFileId((Integer) value);
	        	break;
	        case JSON_TABLE:
	        	data = new DataJsonTable();
	        	((DataJsonTable) data).setValue(ElzaTable.toJsonString((ElzaTable) value));
	        	break;
	        case DATE:
				data = new DataDate();
				((DataDate) data).setValue((LocalDate) value);
	        	break;
	        case URI_REF:
				data = new DataUriRef();
				((DataUriRef) data).setValue(((String) value));
	        	break;
	        case BIT:
	        	data = new DataBit();
	        	((DataBit) data).setBitValue(((boolean) value));
	        	break;
	        default:
	            throw new NotImplementedException("Neimplementovaný datový typ atributu -> dataTypeId: " + dataTypeId);
        }
    	return data;
    }

	protected ApUpdateItemVO buildApItem(final UpdateOp updateOp, final String typeCode, final String specCode,
			final Object value, final Integer position, final Integer objectId) {
		return buildApItem(updateOp, buildApItem(typeCode, specCode, value, position, objectId));
	}

	protected ApUpdateItemVO buildApItem(final UpdateOp updateOp, final ApItemVO item) {
		ApUpdateItemVO updateItem = new ApUpdateItemVO();
		updateItem.setUpdateOp(updateOp);
		updateItem.setItem(item);
		return updateItem;
	}

	/**
	 * Vytvoření objektu pro hodnotu atributu.
	 *
	 * @param typeCode kód typu atributu
	 * @param specCode kód specifikace atributu
	 * @param value    hodnota
	 * @param position pozice
	 * @param objectId identifikátor hodnoty atributu
	 * @return vytvořený object hodnoty atributu
	 */
	protected ApItemVO buildApItem(final String typeCode, final String specCode, final Object value,
			final Integer position, final Integer objectId) {
		Assert.assertNotNull("Musí být vyplněn kód typu atributu", typeCode);

		RulDescItemTypeExtVO type = findDescItemTypeByCode(typeCode);
		Assert.assertNotNull("Typ atributu neexistuje -> CODE: " + typeCode, type);

		RulDescItemSpecVO spec = null;

		if (specCode != null) {
			spec = findDescItemSpecByCode(specCode, type);
			Assert.assertNotNull("Specifikace atributu neexistuje -> CODE: " + specCode, spec);
		}

		DataType dataType = DataType.fromId(type.getDataTypeId());
		ApItemVO item;

		switch (dataType) {

		case INT: {
			item = new ApItemIntVO();
			((ApItemIntVO) item).setValue((Integer) value);
			break;
		}

		case STRING: {
			item = new ApItemStringVO();
			((ApItemStringVO) item).setValue((String) value);
			break;
		}

		case TEXT: {
			item = new ApItemTextVO();
			((ApItemTextVO) item).setValue((String) value);
			break;
		}

		case UNITDATE: {
			item = new ApItemUnitdateVO();
			((ApItemUnitdateVO) item).setValue((String) value);
			break;
		}

		case UNITID: {
			item = new ApItemUnitidVO();
			((ApItemUnitidVO) item).setValue((String) value);
			break;
		}

		case FORMATTED_TEXT: {
			item = new ApItemFormattedTextVO();
			((ApItemFormattedTextVO) item).setValue((String) value);
			break;
		}

		case COORDINATES: {
			item = new ApItemCoordinatesVO();
			((ApItemCoordinatesVO) item).setValue((String) value);
			break;
		}

		case RECORD_REF: {
			item = new ApItemAccessPointRefVO();
			((ApItemAccessPointRefVO) item).setValue(((ApAccessPointVO) value).getId());
			break;
		}

		case DECIMAL: {
			item = new ApItemDecimalVO();
			((ApItemDecimalVO) item).setValue((BigDecimal) value);
			break;
		}

		case ENUM: {
			item = new ApItemEnumVO();
			if (BooleanUtils.isNotTrue(type.getUseSpecification())) {
				throw new IllegalStateException(
						"Specifikace u typu musí být povinná pro ENUM -> CODE: " + type.getCode());
			}
			break;
		}

		case JSON_TABLE: {
			item = new ApItemJsonTableVO();
			((ApItemJsonTableVO) item).setValue(((ElzaTable) value));
			break;
		}

		case DATE: {
			item = new ApItemDateVO();
			((ApItemDateVO) item).setValue((LocalDate) value);
			break;
		}

		default:
			throw new IllegalStateException("Neimplementovaný datový typ atributu -> CODE: " + dataType.getCode());

		}

		if (spec != null) {
			item.setSpecId(spec.getId());
		}

		item.setPosition(position);
		item.setObjectId(objectId);
		item.setTypeId(type.getId());

		return item;
	}

	/**
	 * Vyhledání datového typu atributu.
	 *
	 * @param dataTypeId identifikátor datového typu atributu
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
	 * @param code kód specifikace
	 * @param type typ atributu
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
	 * Vyhledání specifikace atributu podle id.
	 *
	 * @param id   specifikace
	 * @param type typ atributu
	 * @return specifikace atributu
	 */
	protected RulDescItemSpecExtVO findDescItemSpecByCode(final Integer id, final RulDescItemTypeExtVO type) {
		for (RulDescItemSpecExtVO descItemSpec : type.getDescItemSpecs()) {
			if (descItemSpec.getId().equals(id)) {
				return descItemSpec;
			}
		}
		return null;
	}

	/**
	 * Provede načtení stromu uzlů. Uzly mohou být rozbaleny.
	 *
	 * @param fundVersion verze AP
	 * @param node        uzel
	 * @param searchValue hledaný text
	 * @param depth       hloubka vyhledávání
	 * @return seznam výsledků
	 */
	protected List<ArrangementController.TreeNodeFulltext> fulltext(final ArrFundVersionVO fundVersion,
			final ArrNodeVO node, final String searchValue, final ArrangementController.Depth depth) {
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
		return Arrays.asList(
				post(spec -> spec.body(input), FULLTEXT).getBody().as(ArrangementController.TreeNodeFulltext[].class));
	}

	/**
	 * Fulltexove vyhledavani pres vice uzlu.
	 */
	protected List<ArrFundFulltextResult> fundFulltext(final FulltextFundRequest input) {
		return Arrays.asList(post(spec -> spec.body(input), FUND_FULLTEXT).getBody().as(ArrFundFulltextResult[].class));
	}

	/**
	 * Seznam uzlu vyhledaneho AS po fulltextovem vyhledani serazeny podle relevance
	 * pri vyhledani.
	 */
	protected List<TreeNodeVO> fundFulltextNodeList(final Integer fundId) {
		return Arrays.asList(
				get(spec -> spec.pathParam("fundId", fundId), FUND_FULLTEXT_LIST).getBody().as(TreeNodeVO[].class));
	}

	/**
	 * Nalezení otevřené verze AP.
	 *
	 * @param fund archivní pomůcka
	 * @return otevřená verze AP
	 */
	protected ArrFundVersionVO getOpenVersion(final Fund fund) {
		Assert.assertNotNull(fund);

		return getOpenVersion(fund.getId());
	}

	/**
	 * Nalezení otevřené verze AP.
	 *
	 * @param fundId archivní pomůcka
	 * @return otevřená verze AP
	 */
	protected ArrFundVersionVO getOpenVersion(final Integer fundId) {
		Assert.assertNotNull(fundId);

		List<ArrFundVO> funds = getFunds();

		for (ArrFundVO fundFound : funds) {
			if (fundFound.getId().equals(fundId)) {
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
	protected List<ArrNodeVO> convertTreeNodes(final Collection<TreeNodeVO> treeNodeClients) {
		List<ArrNodeVO> nodes = new ArrayList<>(treeNodeClients.size());
		for (TreeNodeVO treeNodeClient : treeNodeClients) {
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
	protected ArrNodeVO convertTreeNode(final TreeNodeVO treeNodeClient) {
		ArrNodeVO node = new ArrNodeVO();
		node.setId(treeNodeClient.getId());
		node.setVersion(treeNodeClient.getVersion());
		return node;
	}

	/**
	 * Validuje verzi archivní pomůcky a vrátí list chyb. Pokud je počet chyb 0 pak
	 * předpokládáme že stav AP = OK
	 *
	 * @param versionId verze, která se má validovat
	 * @return Objekt s listem (prvních 20) chyb
	 */
	public List<ArrangementController.VersionValidationItem> validateVersion(final Integer versionId) {
		return Arrays
				.asList(get(spec -> spec.pathParam("versionId", versionId).pathParam("showAll", true), VALIDATE_VERSION)
						.getBody().as(ArrangementController.VersionValidationItem[].class));
	}

	/**
	 * Validuje verzi archivní pomůcky a vrátí počet chyb Pokud je počet chyb 0 pak
	 * předpokládáme že stav AP = OK
	 *
	 * @param versionId verze, která se má validovat
	 */
	protected void validateVersionCount(final Integer versionId) {
		get(spec -> spec.pathParam("versionId", versionId), VALIDATE_VERSION_COUNT);
	}

	/**
	 * Provede načtení požadovaných uzlů ze stromu.
	 *
	 * @param input vstupní data pro načtení
	 * @return data stromu
	 */
	protected List<TreeNodeVO> getFundTreeNodes(final ArrangementController.FaTreeNodesParam input) {
		return Arrays.asList(post(spec -> spec.body(input), FA_TREE_NODES).getBody().as(TreeNodeVO[].class));
	}

	/**
	 * Získání dat pro JP.
	 *
	 * @param param parametry dat, které chceme získat (formálář, sourozence,
	 *              potomky, předky, ...)
	 * @return požadovaná data
	 */
	public NodeData getNodeData(final NodeDataParam param) {
		return post(spec -> spec.body(param), NODE_DATA).getBody().as(NodeData.class);
	}

	/**
	 * Získání dat pro formulář.
	 *
	 * @param nodeId    identfikátor JP
	 * @param versionId id verze stromu
	 * @return formulář
	 */
	protected ArrangementController.DescFormDataNewVO getNodeFormData(final Integer nodeId, final Integer versionId) {
		return get(spec -> spec.pathParam("nodeId", nodeId).pathParam("versionId", versionId), NODE_FORM_DATA).getBody()
				.as(ArrangementController.DescFormDataNewVO.class);
	}

	/**
	 * Získání dat pro formulář.
	 *
	 * @param outputId  identfikátor outputu
	 * @param versionId id verze stromu
	 * @return formulář
	 */
	protected ArrangementController.OutputFormDataNewVO getOutputFormData(final Integer outputId,
			final Integer versionId) {
		return get(spec -> spec.pathParam("outputId", outputId).pathParam("versionId", versionId), OUTPUT_FORM_DATA)
				.getBody().as(ArrangementController.OutputFormDataNewVO.class);
	}

	/**
	 * Získání dat pro formuláře.
	 *
	 * @param nodeIds   identfikátory JP
	 * @param versionId id verze stromu
	 * @return formuláře
	 */
	protected ArrangementController.NodeFormsDataVO getNodeFormsData(final Integer versionId,
			final Integer... nodeIds) {
		return get(spec -> spec.queryParam("nodeIds", nodeIds).pathParam("versionId", versionId), NODE_FORMS_DATA)
				.getBody().as(ArrangementController.NodeFormsDataVO.class);
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
			final Integer nodeId, final Integer around) {
		return get(
				spec -> spec.pathParam("nodeId", nodeId).pathParam("around", around).pathParam("versionId", versionId),
				NODE_FORMS_DATA_AROUND).getBody().as(ArrangementController.NodeFormsDataVO.class);
	}

	/**
	 * Načte seznam uzlů podle jejich id.
	 *
	 * @param idsParam seznam id
	 * @return seznam vo uzlů s danými id
	 */
	protected List<TreeNodeVO> getNodes(final ArrangementController.IdsParam idsParam) {
		return Arrays.asList(post(spec -> spec.body(idsParam), NODES).getBody().as(TreeNodeVO[].class));
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
	 * @param fundVersionId  identfikátor verze AP
	 * @param nodeId         identfikátor JP
	 * @param nodeVersion    verze JP
	 * @param descItemTypeId identfikátor typu hodnoty atributu
	 */
	protected ArrangementController.DescItemResult deleteDescItemsByType(final Integer fundVersionId,
			final Integer nodeId, final Integer nodeVersion, final Integer descItemTypeId) {
		return delete(
				spec -> spec.pathParam("fundVersionId", fundVersionId).pathParam("nodeId", nodeId)
						.pathParam("nodeVersion", nodeVersion).pathParam("descItemTypeId", descItemTypeId),
				DELETE_DESC_ITEM_BY_TYPE).getBody().as(ArrangementController.DescItemResult.class);
	}

	/**
	 * Smazání hodnot atributu podle typu.
	 *
	 * @param fundVersionId identfikátor verze AP
	 * @param outputId      identfikátor výstupu
	 * @param outputVersion verze výstupu
	 * @param itemTypeId    identfikátor typu hodnoty atributu
	 */
	protected ArrangementController.OutputItemResult deleteOutputItemsByType(final Integer fundVersionId,
			final Integer outputId, final Integer outputVersion, final Integer itemTypeId) {
		return delete(
				spec -> spec.pathParam("fundVersionId", fundVersionId).pathParam("outputId", outputId)
						.pathParam("outputVersion", outputVersion).pathParam("itemTypeId", itemTypeId),
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
		return Arrays.asList(post(spec -> spec.queryParam("withGroups", withGroups).body(param), SCENARIOS).getBody()
				.as(ScenarioOfNewLevelVO[].class));
	}

	/**
	 * Provede zkopírování atributu daného typu ze staršího bratra uzlu.
	 *
	 * @param versionId      id verze stromu
	 * @param descItemTypeId typ atributu, který chceme zkopírovat
	 * @param nodeVO         uzel, na který nastavíme hodnoty ze staršího bratra
	 * @return vytvořené hodnoty
	 */
	protected ArrangementController.CopySiblingResult copyOlderSiblingAttribute(final Integer versionId,
			final Integer descItemTypeId, final ArrNodeVO nodeVO) {
		return put(spec -> spec.queryParam("versionId", versionId).queryParam("descItemTypeId", descItemTypeId)
				.body(nodeVO), COPY_SIBLING).getBody().as(ArrangementController.CopySiblingResult.class);
	}

	/**
	 * Vyhledání typu atributu podle kódu
	 *
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
	 * Vyhledání typu atributu podle id
	 *
	 * @param id typu
	 * @return typ atributu
	 */
	protected RulDescItemTypeExtVO findDescItemTypeById(final Integer id) {
		for (RulDescItemTypeExtVO descItemType : getDescItemTypesCached()) {
			if (descItemType.getDataTypeId().equals(id)) {
				return descItemType;
			}
		}
		return null;
	}

	/**
	 * Načtení třídy včetně navázaných tříd.
	 *
	 * @return objekt třídy
	 */
	protected ApScopeWithConnectedVO getScopeWithConnected(final int id) {
		return get(spec -> spec.pathParam("scopeId", id), GET_SCOPE_WITH_CONNECTED).getBody()
				.as(ApScopeWithConnectedVO.class);
	}

	/**
	 * Vložení nové třídy.
	 *
	 * @return nový objekt třídy
	 */
	protected ApScopeVO createScope() {
		return post(spec -> spec, CREATE_SCOPE).getBody().as(ApScopeVO.class);
	}

	/**
	 * Aktualizace třídy.
	 *
	 * @param scope objekt třídy
	 * @return aktualizovaný objekt třídy
	 */
	protected ApScopeVO updateScope(final ApScopeVO scope) {
		return put(spec -> spec.body(scope).pathParam("scopeId", scope.getId()), UPDATE_SCOPE).getBody()
				.as(ApScopeVO.class);
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
	 * Provázání tříd.
	 *
	 * @param id  ID třídy ke které se bude navazovat
	 * @param id2 ID navazované třídy
	 */
	protected void connectScope(final int id, final int id2) {
		post(spec -> spec.body(id2).pathParam("scopeId", id), CONNECT_SCOPE);
	}

	/**
	 * Zrušení provázání tříd.
	 *
	 * @param id  ID třídy na které se bude rušit provázání
	 * @param id2 ID navázané třídy
	 */
	protected void disconnectScope(final int id, final int id2) {
		post(spec -> spec.body(id2).pathParam("scopeId", id), DISCONNECT_SCOPE);
	}

	/**
	 * Pokud je nastavená verze, vrací třídy napojené na verzi, jinak vrací třídy
	 * nastavené v konfiguraci elzy (YAML).
	 *
	 * @param versionId id verze nebo null
	 * @return seznam tříd
	 */
	protected Response getScopeIdsByVersion(@Nullable final Integer versionId) {
		return versionId == null ? get(FA_SCOPES) : get(spec -> spec.queryParam("versionId", versionId), FA_SCOPES);
	}

	/**
	 * Vrací všechny třídy rejstříků z databáze.
	 */
	protected List<ApScopeVO> getAllScopes() {
		return Arrays.asList(get(ALL_SCOPES).getBody().as(ApScopeVO[].class));
	}

	/**
	 * Vrátí seznam typů rejstříku (typů hesel).
	 *
	 * @return seznam typů rejstříku (typů hesel)
	 */
	protected List<ApTypeVO> getRecordTypes() {
		return Arrays.asList(get(RECORD_TYPES).getBody().as(ApTypeVO[].class));
	}

	/**
	 * Vrátí jedno heslo (s variantními hesly) dle id.
	 *
	 * @param accessPointId id požadovaného hesla
	 */
	protected ApAccessPointVO getAccessPoint(final int accessPointId) {
		return get(spec -> spec.pathParam("recordId", accessPointId), GET_RECORD).getBody().as(ApAccessPointVO.class);
	}

	/**
	 * Vrátí jedno heslo (s variantními hesly) dle id.
	 *
	 * @param accessPointId id požadovaného hesla
	 */
	protected ApAccessPointVO getAccessPoint(final String accessPointId) {
		return get(spec -> spec.pathParam("recordId", accessPointId), GET_RECORD).getBody().as(ApAccessPointVO.class);
	}

	/**
	 * Vytvoření rejstříkového hesla.
	 *
	 * @param accessPoint VO rejstříkové heslo
	 */
	protected ApAccessPointVO createAccessPoint(final ApAccessPointCreateVO accessPoint) {
		return post(spec -> spec.body(accessPoint), CREATE_ACCESS_POINT).getBody().as(ApAccessPointVO.class);
	}

	/**
	 * Aktualizace rejstříkového hesla.
	 *
	 * @param record VO rejstříkové heslo
	 */
	protected ApAccessPointVO updateRecord(final ApAccessPointVO record) {
		return put(spec -> spec.pathParam("recordId", record.getId()).body(record), UPDATE_RECORD).getBody()
				.as(ApAccessPointVO.class);
	}

	/**
	 * Nahrazení rejstříkového hesla.
	 *
	 * @param recordId id rejstříkového hesla
	 */
	protected RecordUsageVO usagesRecord(final Integer recordId) {
		return get(spec -> spec.pathParam("recordId", recordId), USAGES_RECORD).getBody().as(RecordUsageVO.class);
	}

	protected void mergeRevision(final Integer entityId, @Nullable final ApState.StateApproval state) {
		post(spec -> spec.pathParam("entityId", entityId).param("state", state), MERGE_AP);
	}

	/**
	 * Nahrazení rejstříkového hesla.
	 *
	 * @param replacedId    id rejstříkového hesla které nahrazujeme
	 * @param replacementId id rejstříkového hesla kterým nahrazujeme
	 */
	protected Response replaceRecord(final Integer replacedId, final Integer replacementId) {
		return post(spec -> spec.pathParam("recordId", replacedId).body(replacementId), REPLACE_RECORD);
	}

	protected Response changeState(final Integer apId, ApStateUpdate stateUpdate) {
		return put(spec -> spec.pathParam("recordId", apId).body(stateUpdate), CHANGE_STATE);
	}

	/**
	 * Vyhledávání v ApRecord
	 *
	 * @param search
	 * @param from
	 * @param count
	 * @param apTypeId
	 * @param versionId
	 * @return List nalezených záznamů
	 */
	protected List<ApAccessPointVO> findRecord(final String search, final Integer from, final Integer count,
			final Integer apTypeId, final Integer versionId) {
		HashMap<String, Object> params = new HashMap<>();

		if (search != null) {
			params.put("search", search);
		}
		if (versionId != null) {
			params.put("versionId", versionId);
		}
		if (apTypeId != null) {
			params.put("apTypeId", apTypeId);
		}
		params.put("from", from != null ? from : 0);
		params.put("count", count != null ? count : 20);
		params.put("excludeInvalid", true);

		return post(spec -> spec.queryParams(params), FIND_RECORD).getBody().as(FilteredResultVO.class).getRows();
	}

	/**
	 * Vyhledávání v ApRecord
	 *
	 * @param search
	 * @param from
	 * @param count
	 * @param apTypeId
	 * @param versionId
	 * @return List nalezených záznamů
	 */
	protected List<ApAccessPointVO> findRecord(final String search, final Integer from, final Integer count,
			final Integer apTypeId, final Integer versionId, final SearchType searchType) {
		HashMap<String, Object> params = new HashMap<>();

		if (search != null) {
			params.put("search", search);
		}
		if (versionId != null) {
			params.put("versionId", versionId);
		}
		if (apTypeId != null) {
			params.put("apTypeId", apTypeId);
		}
		if (searchType != null) {
			params.put("searchType", searchType);
		}
		params.put("from", from != null ? from : 0);
		params.put("count", count != null ? count : 20);
		params.put("excludeInvalid", true);

		return post(spec -> spec.queryParams(params), FIND_RECORD).getBody().as(FilteredResultVO.class).getRows();
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
	 * Scopy
	 *
	 * @return list scope
	 */
	protected List<ApScopeVO> faScopes() {
		return Arrays.asList(get(FA_SCOPES).body().as(ApScopeVO[].class));
	}

	protected Response importXmlFile(final String transformationName, final Integer scopeId, final File xmlFile) {
		HashMap<String, Object> params = new HashMap<>();

		if (transformationName != null) {
			params.put("transformationName", transformationName);
		}
		if (scopeId != null) {
			params.put("scopeId", scopeId);
		}
		return multipart(spec -> spec.multiPart("xmlFile", xmlFile).params(params), DE_IMPORT);
	}

	/**
	 * Nahrazení textu v hodnotách textových atributů.
	 *
	 * @param versionId       id verze stromu
	 * @param descItemTypeId  typ atributu
	 * @param searchText      hledaný text v atributu
	 * @param replaceText     text, který nahradí hledaný text v celém textu
	 * @param replaceDataBody seznam uzlů, ve kterých hledáme
	 */
	protected void replaceDataValues(final Integer versionId, final Integer descItemTypeId, final String searchText,
			final String replaceText, final ArrangementController.ReplaceDataBody replaceDataBody) {

		put(spec -> spec.pathParam("versionId", versionId).queryParam("descItemTypeId", descItemTypeId)
				.queryParam("searchText", searchText).queryParam("replaceText", replaceText).body(replaceDataBody),
				REPLACE_DATA_VALUES);

	}

	/**
	 * Nahrazení textu v hodnotách textových atributů.
	 *
	 * @param versionId       id verze stromu
	 * @param descItemTypeId  typ atributu
	 * @param text            hledaný text v atributu
	 * @param replaceDataBody seznam uzlů, ve kterých hledáme
	 */
	protected void placeDataValues(final Integer versionId, final Integer descItemTypeId, final String text,
			final ArrangementController.ReplaceDataBody replaceDataBody) {

		put(spec -> spec.pathParam("versionId", versionId).queryParam("descItemTypeId", descItemTypeId)
				.queryParam("text", text).body(replaceDataBody), PLACE_DATA_VALUES);
	}

	/**
	 * Smazání hodnot atributů daného typu pro vybrané uzly.
	 *
	 * @param versionId       id verze stromu
	 * @param descItemTypeId  id typu atributu
	 * @param replaceDataBody seznam uzlů a specifikaci
	 */
	protected void deleteDescItems(final Integer versionId, final Integer descItemTypeId,
			final ArrangementController.ReplaceDataBody replaceDataBody) {
		put(spec -> spec.pathParam("versionId", versionId).queryParam("descItemTypeId", descItemTypeId)
				.body(replaceDataBody), DELETE_DATA_VALUES);
	}

	/**
	 * Provede filtraci uzlů podle filtru a uloží filtrované id do session.
	 *
	 * @param versionId id verze
	 * @param filters   filtry
	 * @return počet všech záznamů splňujících filtry
	 */
	protected void filterNodes(final Integer versionId, final Filters filters) {
		put(spec -> spec.pathParam("versionId", versionId).body(filters), FILTER_NODES);
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
	protected List<FilterNode> getFilteredNodes(final Integer versionId, final Integer page, final Integer pageSize,
			final Set<Integer> descItemTypeIds) {
		return Arrays.asList(put(spec -> spec.pathParam("versionId", versionId).queryParam("page", page)
				.queryParam("pageSize", pageSize).body(descItemTypeIds), FILTERED_NODES).as(FilterNode[].class));
	}

	/**
	 * Ve filtrovaném seznamu najde uzly podle fulltextu. Vrací seřazený seznam uzlů
	 * podle jejich indexu v seznamu všech filtrovaných uzlů.
	 *
	 * @param versionId   id verze stromu
	 * @param fulltext    fulltext
	 * @param luceneQuery v hodnotě fulltext je lucene query (např:
	 *                    +specification:*čís* -fulltextValue:ddd), false - normální
	 *                    fulltext
	 * @return seznam uzlů a jejich indexu v seznamu filtrovaných uzlů, seřazené
	 *         podle indexu
	 */
	protected List<FilterNodePosition> getFilteredFulltextNodes(final Integer versionId, final String fulltext,
			final Boolean luceneQuery) {
		FaFilteredFulltextParam param = new FaFilteredFulltextParam();
		param.setFulltext(fulltext);
		param.setLuceneQuery(BooleanUtils.isTrue(luceneQuery));

		return Arrays.asList(post(spec -> spec.pathParam("versionId", versionId).body(param), FILTERED_FULLTEXT_NODES)
				.as(FilterNodePosition[].class));
	}

	protected ArrangementController.ValidationItems getValidation(final Integer fundVersionId, final Integer fromIndex,
			final Integer toIndex) {
		return get(spec -> spec.pathParam("fundVersionId", fundVersionId).pathParam("fromIndex", fromIndex)
				.pathParam("toIndex", toIndex), VALIDATION).as(ArrangementController.ValidationItems.class);
	}

	protected ArrangementController.ValidationItems findValidationError(final Integer fundVersionId,
			final Integer nodeId, final Integer direction) {
		return get(spec -> spec.pathParam("fundVersionId", fundVersionId).pathParam("nodeId", nodeId)
				.pathParam("direction", direction), VALIDATION_ERROR).as(ArrangementController.ValidationItems.class);
	}

	protected List<NodeItemWithParent> getAllNodesVisiblePolicy(final Integer fundVersionId) {
		return Arrays.asList(get(spec -> spec.pathParam("fundVersionId", fundVersionId), FUND_POLICY)
				.as(NodeItemWithParent[].class));
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
	protected UsrUserVO changePassword(final Integer userId, final UserController.ChangePassword params) {
		return put(spec -> spec.body(params).pathParam("userId", userId), CHANGE_PASSWORD).as(UsrUserVO.class);
	}

	/**
	 * Změna hesla uživatele.
	 *
	 * @param params parametry změny hesla
	 * @return uživatel
	 */
	protected UsrUserVO changePassword(final UserController.ChangePassword params) {
		return put(spec -> spec.body(params), CHANGE_PASSWORD_USER).as(UsrUserVO.class);
	}

	/**
	 * Změna hesla uživatele.
	 *
	 * @return uživatel
	 */
	protected UsrUserVO changePassword(final String oldPassword, final String newPassword) {
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
	protected UsrUserVO changePassword(final UsrUserVO user, final String newPassword) {
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
	protected UsrUserVO createUser(final CreateUserVO params) {
		return post(spec -> spec.body(params), CREATE_USER).as(UsrUserVO.class);
	}

	/**
	 * @param user   uživatel
	 * @param active je aktivní?
	 * @return uživatel
	 */
	protected UsrUserVO changeActive(final UsrUserVO user, final Boolean active) {
		return put(spec -> spec.pathParam("active", active).pathParam("userId", user.getId()), ACTIVE_USER)
				.as(UsrUserVO.class);
	}

	/**
	 * Vytvořené nového uživatele.
	 *
	 * @return vytvořený uživatel
	 */
	protected UsrUserVO createUser(final String username, final Map<UsrAuthentication.AuthType, String> valueMap,
			final Integer apId) {
		CreateUserVO params = new CreateUserVO();
		params.setUsername(username);
		params.setValuesMap(valueMap);
		params.setAccessPointId(apId);
		return createUser(params);
	}

	/**
	 * Vytvořené skupiny.
	 *
	 * @param params parametry pro vytvoření skupiny
	 * @return vytvořená skupina
	 */
	protected UsrGroupVO createGroup(final GroupController.CreateGroup params) {
		return post(spec -> spec.body(params), CREATE_GROUP).as(UsrGroupVO.class);
	}

	/**
	 * Smazání skupiny.
	 *
	 * @param groupId identifikátor skupiny
	 */
	protected void deleteGroup(final Integer groupId) {
		delete(spec -> spec.pathParam("groupId", groupId), DELETE_GROUP);
	}

	/**
	 * Načte seznam skupin.
	 *
	 * @param search hledaný řetězec
	 * @param from   počáteční záznam
	 * @param count  počet vrácených záznamů
	 * @return seznam s celkovým počtem
	 */
	protected FilteredResultVO<UsrGroupVO> findGroup(final String search, final Integer from, final Integer count) {
		return get(spec -> spec.queryParam("search", search).queryParam("from", from).queryParam("count", count),
				FIND_GROUP).as(FilteredResultVO.class);
	}

	/**
	 * Změna skupiny.
	 *
	 * @param groupId identifikátor skupiny
	 * @param params  parametry změny skupiny
	 */
	protected UsrGroupVO changeGroup(final Integer groupId, final GroupController.ChangeGroup params) {
		return put(spec -> spec.body(params).pathParam("groupId", groupId), CHANGE_GROUP).as(UsrGroupVO.class);
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
	protected FilteredResultVO<UsrUserVO> findUser(@Nullable final String search, final Boolean active,
			final Boolean disabled, final Integer from, final Integer count, final Integer excludedGroupId) {
		return get(spec -> spec.queryParam("active", active).queryParam("search", search).queryParam("from", from)
				.queryParam("count", count).queryParam("disabled", disabled)
				.queryParam("excludedGroupId", excludedGroupId), FIND_USER).as(FilteredResultVO.class);
	}

	/**
	 * Načtení uživatele s daty pro zobrazení na detailu s možností editace.
	 *
	 * @param userId id
	 * @return VO
	 */
	protected UsrUserVO getUser(final Integer userId) {
		return get(spec -> spec.pathParam("userId", userId), GET_USER).as(UsrUserVO.class);
	}

	/**
	 * Načtení skupiny s daty pro zobrazení na detailu s možností editace.
	 *
	 * @param groupId id
	 * @return VO
	 */
	protected UsrGroupVO getGroup(final Integer groupId) {
		return get(spec -> spec.pathParam("groupId", groupId), GET_GROUP).as(UsrGroupVO.class);
	}

	/**
	 * Přidání uživatelů do skupin.
	 *
	 * @param groupIds identifikátor skupin, do které přidáváme uživatele
	 * @param userIds  identifikátor přidávaných uživatelů
	 */
	protected void joinGroup(final Set<Integer> groupIds, final Set<Integer> userIds) {
		UserController.IdsParam param = new UserController.IdsParam(groupIds, userIds);
		post(spec -> spec.body(param), JOIN_GROUP);
	}

	/**
	 * Přidání uživatele do skupiny.
	 *
	 * @param groupId identifikátor skupiny, ze které odebírám uživatel
	 * @param userId  identifikátor odebíraného uživatele
	 */
	protected void leaveGroup(final Integer groupId, final Integer userId) {
		post(spec -> spec.pathParam("groupId", groupId).pathParam("userId", userId), LEAVE_GROUP);
	}

	/**
	 * Přidání oprávnění uživatele.
	 *
	 * @param userId      identifikátor uživatele
	 * @param permissions seznam oprávnění
	 */
	protected void addUserPermission(final Integer userId, final List<UsrPermissionVO> permissions) {
		post(spec -> spec.pathParam("userId", userId).body(permissions), ADD_USER_PERMISSION);
	}

	/**
	 * Odebrání oprávnění uživatele.
	 *
	 * @param userId     identifikátor uživatele
	 * @param permission seznam oprávnění
	 */
	protected void deleteUserPermission(final Integer userId, final UsrPermissionVO permission) {
		post(spec -> spec.pathParam("userId", userId).body(permission), DELETE_USER_PERMISSION);
	}

	/**
	 * Odebrání oprávnění uživatele typu AS all.
	 *
	 * @param userId identifikátor uživatele
	 */
	protected void deleteUserFundAllPermission(final Integer userId) {
		post(spec -> spec.pathParam("userId", userId), DELETE_USER_FUND_ALL_PERMISSION);
	}

	/**
	 * Odebrání oprávnění uživatele na daný AS.
	 *
	 * @param userId identifikátor uživatele
	 * @param fundId identifikátor AS
	 */
	protected void deleteUserFundPermission(final Integer userId, final Integer fundId) {
		post(spec -> spec.pathParam("userId", userId).pathParam("fundId", fundId), DELETE_USER_FUND_PERMISSION);
	}

	/**
	 * Přidání oprávnění skupiny.
	 *
	 * @param groupId     identifikátor skupiny
	 * @param permissions seznam oprávnění
	 */
	protected void addGroupPermission(final Integer groupId, final List<UsrPermissionVO> permissions) {
		post(spec -> spec.pathParam("groupId", groupId).body(permissions), ADD_GROUP_PERMISSION);
	}

	/**
	 * Odebrání oprávnění skupiny.
	 *
	 * @param groupId    identifikátor skupiny
	 * @param permission seznam oprávnění
	 */
	protected void deleteGroupPermission(final Integer groupId, final UsrPermissionVO permission) {
		post(spec -> spec.pathParam("groupId", groupId).body(permission), DELETE_GROUP_PERMISSION);
	}

	/**
	 * Odebrání oprávnění skupiny typu AS all.
	 *
	 * @param groupId identifikátor skupiny
	 */
	protected void deleteGroupFundAllPermission(final Integer groupId) {
		post(spec -> spec.pathParam("groupId", groupId), DELETE_GROUP_FUND_ALL_PERMISSION);
	}

	/**
	 * Odebrání oprávnění skupiny na daný AS.
	 *
	 * @param groupId identifikátor skupiny
	 * @param fundId  identifikátor AS
	 */
	protected void deleteGroupFundPermission(final Integer groupId, final Integer fundId) {
		post(spec -> spec.pathParam("groupId", groupId).pathParam("fundId", fundId), DELETE_GROUP_FUND_PERMISSION);
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
	protected List<UniqueValue> filterUniqueValues(final Integer versionId, final Integer descItemTypeId,
			final String fulltext, final Set<Integer> specIds) {
		Response result;
		if (CollectionUtils.isEmpty(specIds)) {
			result = put(spec -> spec.pathParam("versionId", versionId).queryParam("descItemTypeId", descItemTypeId)
					.queryParam("fulltext", fulltext).queryParam("max", 200), FILTER_UNIQUE_VALUES);
		} else {
			result = put(
					spec -> spec.pathParam("versionId", versionId).queryParam("descItemTypeId", descItemTypeId)
							.queryParam("fulltext", fulltext).queryParam("max", 200).body(specIds),
					FILTER_UNIQUE_VALUES);
		}

		return Arrays.asList(result.as(UniqueValue[].class));
	}

	/**
	 * Načtení seznamu outputů - objekt outputu s vazbou na objekt named output.
	 *
	 * @param fundVersionId identfikátor verze AS
	 * @return seznam outputů
	 */
	protected List<ArrOutputVO> getOutputs(final Integer fundVersionId) {
		return Arrays.asList(get(spec -> spec.pathParam("fundVersionId", fundVersionId), GET_OUTPUTS).getBody()
				.as(ArrOutputVO[].class));
	}

	/**
	 * Načtení detailu outputu objekt output s vazbou na named output a seznamem
	 * připojených node.
	 *
	 * @param fundVersionId identfikátor verze AS
	 * @param outputId      identifikátor výstupu
	 * @return output
	 */
	protected ArrOutputVO getOutput(final Integer fundVersionId, final Integer outputId) {
		return get(spec -> spec.pathParam("fundVersionId", fundVersionId).pathParam("outputId", outputId), GET_OUTPUT)
				.getBody().as(ArrOutputVO.class);
	}

	/**
	 * Vytvoření nového pojmenovaného výstupu.
	 *
	 * @param fundVersionId identfikátor verze AS
	 * @param param         vstupní parametry pro vytvoření outputu
	 * @return vytvořený výstup
	 */
	protected ArrOutputVO createNamedOutput(final Integer fundVersionId,
			final ArrangementController.OutputNameParam param) {
		return put(spec -> spec.pathParam("fundVersionId", fundVersionId).body(param), CREATE_NAMED_OUTPUT).getBody()
				.as(ArrOutputVO.class);
	}

	/**
	 * Vytvoření nového pojmenovaného výstupu.
	 *
	 * @param fundVersion verze AS
	 * @param name        název výstupu
	 * @param code        kód výstupu
	 * @return vytvořený výstup
	 */
	protected ArrOutputVO createNamedOutput(final ArrFundVersionVO fundVersion, final String name, final String code,
			final Integer outputTypeId) {
		ArrangementController.OutputNameParam param = new ArrangementController.OutputNameParam();
		param.setInternalCode(code);
		param.setName(name);
		param.setOutputTypeId(outputTypeId);
		return createNamedOutput(fundVersion.getId(), param);
	}

	/**
	 * Přidání uzlů k výstupu.
	 *
	 * @param fundVersionId identfikátor verze AS
	 * @param outputId      identifikátor výstupu
	 * @param nodeIds       seznam přidáváných identifikátorů uzlů
	 */
	protected void addNodesNamedOutput(final Integer fundVersionId, final Integer outputId,
			final List<Integer> nodeIds) {
		post(spec -> spec.pathParam("fundVersionId", fundVersionId).pathParam("outputId", outputId).body(nodeIds),
				ADD_NODES_NAMED_OUTPUT);
	}

	/**
	 * Odebrání uzlů u výstupu.
	 *
	 * @param fundVersionId identfikátor verze AS
	 * @param outputId      identifikátor výstupu
	 * @param nodeIds       seznam odebíraných identifikátorů uzlů
	 */
	protected void removeNodesNamedOutput(final Integer fundVersionId, final Integer outputId,
			final List<Integer> nodeIds) {
		post(spec -> spec.pathParam("fundVersionId", fundVersionId).pathParam("outputId", outputId).body(nodeIds),
				REMOVE_NODES_NAMED_OUTPUT);
	}

	/**
	 * Smazání pojmenovaného výstupu.
	 *
	 * @param fundVersionId identfikátor verze AS
	 * @param outputId      identifikátor výstupu
	 */
	protected void deleteNamedOutput(final Integer fundVersionId, final Integer outputId) {
		delete(spec -> spec.pathParam("fundVersionId", fundVersionId).pathParam("outputId", outputId),
				DELETE_NAMED_OUTPUT);
	}

	/**
	 * Upravení výstupu.
	 *
	 * @param fundVersion verze AS
	 * @param output      výstup
	 * @param name        název výstupu
	 * @param code        kód výstupu
	 */
	protected void updateNamedOutput(final ArrFundVersionVO fundVersion, final ArrOutputVO output, final String name,
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
	protected void updateNamedOutput(final Integer fundVersionId, final Integer outputId,
			final ArrangementController.OutputNameParam param) {
		post(spec -> spec.pathParam("fundVersionId", fundVersionId).pathParam("outputId", outputId).body(param),
				UPDATE_NAMED_OUTPUT);
	}

	/**
	 * Vytvoření propojení na DAO
	 *
	 * @param fundVersionId identfikátor verze AS
	 * @param daoId         identfikátor dao
	 * @param nodeId        uzel k nemuz je DAO pripojeno
	 */
	protected ArrDaoLinkVO createDaoLink(final Integer fundVersionId, final Integer daoId, final Integer nodeId) {
		Response resp = put(spec -> spec.pathParam("fundVersionId", fundVersionId).pathParam("daoId", daoId)
				.pathParam("nodeId", nodeId), ARRANGEMENT_DAOS_LINK_CREATE);
		return resp.as(ArrDaoLinkVO.class);
	}

	/**
	 * Vymazani propojení na DAO
	 *
	 * @param fundVersionId identfikátor verze AS
	 * @param daoLinkId     identfikátor propojeni Dao
	 */
	protected void deleteDaoLink(final Integer fundVersionId, final Integer daoLinkId) {
		delete(spec -> spec.pathParam("fundVersionId", fundVersionId).pathParam("daoLinkId", daoLinkId),
				ARRANGEMENT_DAOS_LINK_DELETE);
	}

	/**
	 * Vyhledání DAOs
	 */
	protected List<ArrDaoVO> findDaos(final Integer fundVersionId) {
		Response resp = get(spec -> spec.pathParam("fundVersionId", fundVersionId), FIND_DAOS);
		return Arrays.asList(resp.as(ArrDaoVO[].class));
	}

	/**
	 * Vyhledání DAOs
	 */
	protected List<ArrDaoVO> findDaos(final Integer fundVersionId, final Integer nodeId) {
		Response resp = get(spec -> spec.pathParam("fundVersionId", fundVersionId).param("nodeId", nodeId), FIND_DAOS);
		return Arrays.asList(resp.as(ArrDaoVO[].class));
	}

	/**
	 * Vrací typy oprávnění podle verze fondu.
	 *
	 * @param fundVersionId identifikátor verze AS
	 * @return seznam typů oprávnění
	 */
	protected List<RulPolicyTypeVO> getPolicyTypes(final Integer fundVersionId) {
		return Arrays.asList(
				get(spec -> spec.pathParam("fundVersionId", fundVersionId), POLICY_TYPES).as(RulPolicyTypeVO[].class));
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
	protected void setVisiblePolicy(final Integer nodeId, final Integer fundVersionId,
			final RuleController.VisiblePolicyParams visiblePolicyParams) {
		put(spec -> spec.pathParam("nodeId", nodeId).pathParam("fundVersionId", fundVersionId)
				.body(visiblePolicyParams), POLICY_SET);
	}

	/**
	 * Získání nastavení oprávnění pro uzly.
	 *
	 * @param nodeId        identifikátor node ke kterému hledám oprávnění
	 * @param fundVersionId identifikátor verze AS
	 * @return mapa uzlů map typů a jejich zobrazení
	 */
	protected RuleController.VisiblePolicyTypes getVisiblePolicy(final Integer nodeId, final Integer fundVersionId) {
		return get(spec -> spec.pathParam("nodeId", nodeId).pathParam("fundVersionId", fundVersionId), POLICY_GET)
				.as(RuleController.VisiblePolicyTypes.class);
	}

	/**
	 * Načtení souboru na základě cesty, např. coordinates/all.kml odkazuje do test
	 * resources.
	 *
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
	 * @param changeId      identifikátor změny, vůči které chceme počítat offset
	 *                      (pokud není vyplněn, bere se vždy poslední)
	 * @param nodeId        identifikátor JP u které vyhledáváme změny (pokud není
	 *                      vyplněn, vyhledává se přes celý AS)
	 * @return výsledek hledání
	 */
	protected ChangesResult findChanges(final Integer fundVersionId, final Integer maxSize, final Integer offset,
			final Integer changeId, final Integer nodeId) {
		return get(
				spec -> spec.pathParam("fundVersionId", fundVersionId).queryParam("maxSize", maxSize)
						.queryParam("offset", offset).queryParam("changeId", changeId).queryParam("nodeId", nodeId),
				FIND_CHANGE).as(ChangesResult.class);
	}

	/**
	 * Vyhledání provedení změn nad AS, případně nad konkrétní JP z AS.
	 *
	 * @param fundVersionId identfikátor verze AS
	 * @param maxSize       maximální počet záznamů
	 * @param fromDate      datum vůči kterému vyhledávám v seznamu (př. formátu
	 *                      query parametru: 2016-11-07T10:32:04)
	 * @param changeId      identifikátor změny, vůči které chceme počítat offset
	 *                      (pokud není vyplněn, bere se vždy poslední)
	 * @param nodeId        identifikátor JP u které vyhledáváme změny (pokud není
	 *                      vyplně, vyhledává se přes celý AS)
	 * @return výsledek hledání
	 */
	protected ChangesResult findChangesByDate(final Integer fundVersionId, final Integer maxSize,
			final OffsetDateTime fromDate, final Integer changeId, final Integer nodeId) {
		String strDate = fromDate.format(FORMATTER);
		return get(
				spec -> spec.pathParam("fundVersionId", fundVersionId).queryParam("maxSize", maxSize)
						.queryParam("fromDate", strDate).queryParam("changeId", changeId).queryParam("nodeId", nodeId),
				FIND_CHANGE_BY_DATE).as(ChangesResult.class);
	}

	/**
	 * Provede revertování AS / JP k požadovanému stavu.
	 *
	 * @param fundVersionId identfikátor verze AS
	 * @param fromChangeId  identifikátor změny, vůči které provádíme revertování
	 *                      (od)
	 * @param toChangeId    identifikátor změny, ke které provádíme revertování (do)
	 * @param nodeId        identifikátor JP u které provádíme změny (pokud není
	 *                      vyplněn, revertuje se přes celý AS)
	 */
	public void revertChanges(final Integer fundVersionId, final Integer fromChangeId, final Integer toChangeId,
			final Integer nodeId) {
		get(spec -> spec.pathParam("fundVersionId", fundVersionId).queryParam("fromChangeId", fromChangeId)
				.queryParam("toChangeId", toChangeId).queryParam("nodeId", nodeId), REVERT_CHANGES);
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
			final Integer nodeId, final Integer nodeVersion, final Integer descItemTypeId, final Integer descItemSpecId,
			final Integer descItemObjectId) {
		return put(
				spec -> spec.pathParam("fundVersionId", fundVersionId).pathParam("nodeId", nodeId)
						.pathParam("nodeVersion", nodeVersion).queryParam("descItemTypeId", descItemTypeId)
						.queryParam("descItemSpecId", descItemSpecId).queryParam("descItemObjectId", descItemObjectId),
				SET_NOT_IDENTIFIED_DESCITEM).as(ArrangementController.DescItemResult.class);
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
			final Integer nodeId, final Integer nodeVersion, final Integer descItemTypeId, final Integer descItemSpecId,
			final Integer descItemObjectId) {
		return put(
				spec -> spec.pathParam("fundVersionId", fundVersionId).pathParam("nodeId", nodeId)
						.pathParam("nodeVersion", nodeVersion).queryParam("descItemTypeId", descItemTypeId)
						.queryParam("descItemSpecId", descItemSpecId).queryParam("descItemObjectId", descItemObjectId),
				UNSET_NOT_IDENTIFIED_DESCITEM).as(ArrangementController.DescItemResult.class);
	}

	/**
	 * Nastavení atributu na "Nezjištěno".
	 *
	 * @param fundVersionId      id archivního souboru
	 * @param fundVersionId      id archivního souboru
	 * @param outputId           identifikátor výstupu
	 * @param outputVersion      verze výstupu
	 * @param outputItemTypeId   dentfikátor typu hodnoty atributu
	 * @param outputItemSpecId   identfikátor specifikace hodnoty atributu
	 * @param outputItemObjectId identifikátor existující hodnoty atributu
	 * @return upravená hodnota atributu nastavená na nezjištěno
	 */
	protected ArrangementController.OutputItemResult setNotIdentifiedOutputItem(final Integer fundVersionId,
			final Integer outputId, final Integer outputVersion, final Integer outputItemTypeId,
			final Integer outputItemSpecId, final Integer outputItemObjectId) {
		return put(spec -> spec.pathParam("fundVersionId", fundVersionId).pathParam("outputId", outputId)
				.pathParam("outputVersion", outputVersion).queryParam("outputItemTypeId", outputItemTypeId)
				.queryParam("outputItemSpecId", outputItemSpecId).queryParam("outputItemObjectId", outputItemObjectId),
				SET_NOT_IDENTIFIED_OUTPUTITEM).as(ArrangementController.OutputItemResult.class);
	}

	/**
	 * Zrušení nastavení atributu na "Nezjištěno".
	 *
	 * @param fundVersionId      id archivního souboru
	 * @param outputId           identifikátor výstupu
	 * @param outputVersion      verze výstupu
	 * @param outputItemTypeId   dentfikátor typu hodnoty atributu
	 * @param outputItemSpecId   identfikátor specifikace hodnoty atributu
	 * @param outputItemObjectId identifikátor existující hodnoty atributu
	 * @return odstraněný atribut
	 */
	protected ArrangementController.OutputItemResult unsetNotIdentifiedOutputItem(final Integer fundVersionId,
			final Integer outputId, final Integer outputVersion, final Integer outputItemTypeId,
			final Integer outputItemSpecId, final Integer outputItemObjectId) {
		return put(spec -> spec.pathParam("fundVersionId", fundVersionId).pathParam("outputId", outputId)
				.pathParam("outputVersion", outputVersion).queryParam("outputItemTypeId", outputItemTypeId)
				.queryParam("outputItemSpecId", outputItemSpecId).queryParam("outputItemObjectId", outputItemObjectId),
				UNSET_NOT_IDENTIFIED_OUTPUTITEM).as(ArrangementController.OutputItemResult.class);
	}

	protected CopyNodesValidateResult copyLevelsValidate(final CopyNodesValidate copyNodesValidate) {
		return post(spec -> spec.body(copyNodesValidate), COPY_LEVELS_VALIDATE).as(CopyNodesValidateResult.class);
	}

	protected void copyLevels(final CopyNodesParams copyNodesParams) {
		post(spec -> spec.body(copyNodesParams), COPY_LEVELS);
	}

	protected void setOutputSettings(Integer outputId, OutputSettingsVO settings) {
		put(spec -> spec.pathParam("outputId", outputId).body(settings), UPDATE_OUTPUT_SETTINGS);
	}

	/**
	 * Vytvoření hodnoty strukturovaného datového typu.
	 *
	 * @param structureTypeCode kód strukturovaného datového typu
	 * @param fundVersionId     identifikátor verze AS
	 * @return vytvořená dočasná entita
	 */
	protected ArrStructureDataVO createStructureData(final String structureTypeCode, final Integer fundVersionId) {
		return post(spec -> spec.body(structureTypeCode).pathParam("fundVersionId", fundVersionId),
				CREATE_STRUCTURE_DATA).as(ArrStructureDataVO.class);
	}

	/**
	 * Potvrzení hodnoty strukturovaného datového typu. Provede nastavení hodnoty.
	 *
	 * @param fundVersionId   identifikátor verze AS
	 * @param structureDataId identifikátor hodnoty strukturovaného datového typu
	 * @return potvrzená entita
	 */
	protected ArrStructureDataVO confirmStructureData(final Integer fundVersionId, final Integer structureDataId) {
		return post(
				spec -> spec.pathParam("structureDataId", structureDataId).pathParam("fundVersionId", fundVersionId),
				CONFIRM_STRUCTURE_DATA).as(ArrStructureDataVO.class);
	}

	/**
	 * Smazání hodnoty strukturovaného datového typu.
	 *
	 * @param fundVersionId    identifikátor verze AS
	 * @param structureDataIds seznam identifikátorů hodnot strukturovaného datového
	 *                         typu
	 * @return seznam identifikátorů smazaných entit
	 */
	protected List<Integer> deleteStructureData(final Integer fundVersionId, final List<Integer> structureDataIds) {
		return delete(spec -> spec.pathParams("id", fundVersionId).body(structureDataIds), FUND_DELETE_STRUCTURE_DATA)
				.as(ArrayList.class);
	}

	/**
	 * Vyhledání hodnot strukturovaného datového typu.
	 *
	 * @param structureTypeCode kód typu strukturovaného datového
	 * @param fundVersionId     identifikátor verze AS
	 * @param search            text pro filtrování (nepovinné)
	 * @param assignable        přiřaditelnost
	 * @param from              od položky
	 * @param count             maximální počet položek
	 * @return nalezené položky
	 */
	protected FilteredResultVO<ArrStructureDataVO> findStructureData(final String structureTypeCode,
			final Integer fundVersionId, final String search, final Boolean assignable, final Integer from,
			final Integer count) {
		return get(spec -> spec.pathParam("structureTypeCode", structureTypeCode)
				.pathParam("fundVersionId", fundVersionId).queryParam("search", search)
				.queryParam("assignable", assignable).queryParam("from", from).queryParam("count", count),
				FIND_STRUCTURE_DATA).as(FilteredResultVO.class);
	}

	/**
	 * Vyhledá možné typy strukt. datových typů, které lze v AS používat.
	 *
	 * @return nalezené entity
	 */
	protected List<RulStructureTypeVO> findStructureTypes() {
		return Arrays.asList(get(spec -> spec, FIND_STRUCTURE_TYPES).as(RulStructureTypeVO[].class));
	}

	/**
	 * Vyhledá možné typy partů.
	 *
	 * @return nalezené entity
	 */
	protected List<RulPartTypeVO> findPartTypes() {
		return Arrays.asList(get(spec -> spec, FIND_PART_TYPES).as(RulPartTypeVO[].class));
	}

	protected Map<String, RulPartTypeVO> findPartTypesMap() {
		return findPartTypes().stream().collect(Collectors.toMap(RulPartTypeVO::getCode, Function.identity()));
	}

	/**
	 * Vyhledá dostupná a aktivovaná rozšíření k AS.
	 *
	 * @param fundVersionId identifikátor verze AS
	 * @return nalezené entity
	 */
	protected List<StructureExtensionFundVO> findFundStructureExtension(final Integer fundVersionId,
			final String structureTypeCode) {
		return Arrays.asList(get(spec -> spec.pathParam("fundVersionId", fundVersionId).pathParam("structureTypeCode",
				structureTypeCode), FIND_FUND_STRUCTURE_EXTENSION).as(StructureExtensionFundVO[].class));
	}

	/**
	 * Nastaví konkrétní rozšíření na AS.
	 *
	 * @param fundVersionId           identifikátor verze AS
	 * @param structureTypeCode       kód strukturovaného datového typu
	 * @param structureExtensionCodes seznam kódů rozšíření, které mají být
	 *                                aktivovány na AS
	 */
	protected void setFundStructureExtensions(final Integer fundVersionId, final String structureTypeCode,
			final List<String> structureExtensionCodes) {
		put(spec -> spec.pathParam("fundVersionId", fundVersionId).pathParam("structureTypeCode", structureTypeCode)
				.body(structureExtensionCodes), SET_FUND_STRUCTURE_EXTENSION);
	}

	/**
	 * Vytvoření položky k hodnotě strukt. datového typu.
	 *
	 * @param itemVO          položka
	 * @param fundVersionId   identifikátor verze AS
	 * @param itemTypeId      identifikátor typu atributu
	 * @param structureDataId identifikátor hodnoty strukturovaného datového typu
	 * @return vytvořená entita
	 */
	protected StructureController.StructureItemResult createStructureItem(final ArrItemVO itemVO,
			final Integer fundVersionId, final Integer itemTypeId, final Integer structureDataId) {
		return post(spec -> spec.pathParam("fundVersionId", fundVersionId).pathParam("itemTypeId", itemTypeId)
				.pathParam("structureDataId", structureDataId).body(itemVO), CREATE_STRUCTURE_ITEM)
				.as(StructureController.StructureItemResult.class);
	}

	/**
	 * Upravení položky k hodnotě strukt. datového typu.
	 *
	 * @param itemVO           položka
	 * @param fundVersionId    identifikátor verze AS
	 * @param createNewVersion provést verzovanou změnu
	 * @return upravená entita
	 */
	protected StructureController.StructureItemResult updateStructureItem(final ArrItemVO itemVO,
			final Integer fundVersionId, final Boolean createNewVersion) {
		return put(spec -> spec.pathParam("fundVersionId", fundVersionId)
				.pathParam("createNewVersion", createNewVersion).body(itemVO), UPDATE_STRUCTURE_ITEM)
				.as(StructureController.StructureItemResult.class);
	}

	/**
	 * Odstranení položky k hodnotě strukt. datového typu.
	 *
	 * @param itemVO        položka
	 * @param fundVersionId identifikátor verze AS
	 * @return smazaná entita
	 */
	protected StructureController.StructureItemResult deleteStructureItem(final ArrItemVO itemVO,
			final Integer fundVersionId) {
		return post(spec -> spec.pathParam("fundVersionId", fundVersionId).body(itemVO), DELETE_STRUCTURE_ITEM)
				.as(StructureController.StructureItemResult.class);
	}

	/**
	 * Odstranení položek k hodnotě strukt. datového typu podle typu atributu.
	 *
	 * @param fundVersionId   identifikátor verze AS
	 * @param structureDataId identifikátor hodnoty strukturovaného datového typu
	 * @param itemTypeId      identifikátor typu atributu
	 */
	protected StructureController.StructureItemResult deleteStructureItemsByType(final Integer fundVersionId,
			final Integer structureDataId, final Integer itemTypeId) {
		return delete(spec -> spec.pathParam("fundVersionId", fundVersionId)
				.pathParam("structureDataId", structureDataId).pathParam("itemTypeId", itemTypeId),
				DELETE_STRUCTURE_ITEMS_BY_TYPE).as(StructureController.StructureItemResult.class);
	}

	/**
	 * Získání dat pro formulář strukt. datového typu.
	 *
	 * @param fundVersionId   identifikátor verze AS
	 * @param structureDataId identifikátor hodnoty strukturovaného datového typu
	 * @return data formuláře
	 */
	protected StructureController.StructureDataFormDataVO getFormStructureItems(final Integer fundVersionId,
			final Integer structureDataId) {
		return get(spec -> spec.pathParam("fundVersionId", fundVersionId).pathParam("structureDataId", structureDataId),
				GET_FORM_STRUCTURE_ITEMS).as(StructureController.StructureDataFormDataVO.class);
	}

	/**
	 * Založení duplikátů strukturovaného datového typu a autoinkrementační.
	 * Předloha musí být ve stavu {@link ArrStructuredObject.State#TEMP}.
	 *
	 * @param structureDataId identifikátor předlohy hodnoty strukturovaného
	 *                        datového typu
	 * @param fundVersionId   identifikátor verze AS
	 * @param count           počet položek, které se budou budou vytvářet
	 * @param itemTypeIds     identifikátory číselných typů atributu, které se budou
	 *                        incrementovat
	 */
	protected void duplicateStructureDataBatch(final Integer fundVersionId, final Integer structureDataId,
			final Integer count, final List<Integer> itemTypeIds) {
		post(spec -> spec.pathParam("fundVersionId", fundVersionId).pathParam("structureDataId", structureDataId)
				.body(new StructureController.StructureDataBatch(count, itemTypeIds)), DUPLICATE_STRUCTURE_DATA_BATCH);
	}

	/**
	 * Získání hodnoty strukturovaného datového typu.
	 *
	 * @param fundVersionId   identifikátor verze AS
	 * @param structureDataId identifikátor hodnoty strukturovaného datového typu
	 * @return nalezená entita
	 */
	protected ArrStructureDataVO getStructureData(final Integer fundVersionId, final Integer structureDataId) {
		return get(spec -> spec.pathParam("fundVersionId", fundVersionId).pathParam("structureDataId", structureDataId),
				GET_STRUCTURE_DATA).as(ArrStructureDataVO.class);
	}

	/**
	 * Nastavení přiřaditelnosti.
	 *
	 * @param fundVersionId    identifikátor verze AS
	 * @param assignable       přiřaditelný
	 * @param structureDataIds identifikátory hodnoty strukturovaného datového typu
	 */
	protected void setAssignableStructureData(final Integer fundVersionId, final boolean assignable,
			List<Integer> structureDataIds) {
		post(spec -> spec.pathParam("fundVersionId", fundVersionId).pathParam("assignable", assignable)
				.body(structureDataIds), SET_ASSIGNABLE_STRUCTURE_DATA_LIST);
	}

	/**
	 * Hromadná úprava položek/hodnot strukt. typu.
	 *
	 * @param fundVersionId            identifikátor verze AS
	 * @param structureTypeCode        kód strukturovaného datového typu
	 * @param structureDataBatchUpdate data pro hromadnou úpravu hodnot
	 */
	protected void updateStructureDataBatch(final Integer fundVersionId, final String structureTypeCode,
			final StructureController.StructureDataBatchUpdate structureDataBatchUpdate) {
		post(spec -> spec.pathParam("fundVersionId", fundVersionId).pathParam("structureTypeCode", structureTypeCode)
				.body(structureDataBatchUpdate), UPDATE_STRUCTURE_DATA_BATCH);
	}

	/**
	 * Založení nové části přístupového bodu.
	 *
	 * @param accessPointId identifikátor přístupového bodu (PK)
	 * @param apPartFormVO  data pro vytvoření části
	 */
	protected CreatedPartVO createPart(final Integer accessPointId, final ApPartFormVO apPartFormVO) {
		return post(spec -> spec.pathParam("accessPointId", accessPointId).body(apPartFormVO), CREATE_PART)
				.as(CreatedPartVO.class);
	}

	/**
	 * Úprava části přístupového bodu.
	 *
	 * @param accessPointId identifikátor přístupového bodu (PK)
	 * @param partId        identifikátor upravované části
	 * @param apPartFormVO  data pro úpravu části
	 */
	protected void updatePart(final Integer accessPointId, final Integer partId, final ApPartFormVO apPartFormVO) {
		post(spec -> spec.pathParam("accessPointId", accessPointId).pathParam("partId", partId).body(apPartFormVO),
				UPDATE_PART);
	}

	/**
	 * Smazání části přístupového bodu.
	 *
	 * @param accessPointId identifikátor přístupového bodu (PK)
	 * @param partId        identifikátor mazané části
	 */
	public void deletePart(final Integer accessPointId, final Integer partId) {
		delete(spec -> spec.pathParam("accessPointId", accessPointId).pathParam("partId", partId), DELETE_PART);
	}

	/**
	 * Nastavení preferovaného jména přístupového bodu. Možné pouze pro části typu
	 * Označení.
	 *
	 * @param accessPointId identifikátor přístupového bodu (PK)
	 * @param partId        identifikátor části, kterou nastavujeme jako
	 *                      preferovanou
	 */
	public void setPreferName(final Integer accessPointId, final Integer partId) {
		put(spec -> spec.pathParam("accessPointId", accessPointId).pathParam("partId", partId), SET_PREFER_NAME);
	}

	/**
	 * Vrací všechny jazyky.
	 */
	public Map<String, LanguageVO> getAllLanguages() {
		return Arrays.stream(get(GET_LANGUAGES).getBody().as(LanguageVO[].class))
				.collect(Collectors.toMap(LanguageVO::getCode, Function.identity()));
	}

	/**
	 * Vrací typy externích identifikátorů.
	 */
	public Map<String, ApEidTypeVO> getAllExternalIdTypes() {
		return Arrays.stream(get(GET_EXTERNAL_ID_TYPES).getBody().as(ApEidTypeVO[].class))
				.collect(Collectors.toMap(ApEidTypeVO::getCode, Function.identity()));
	}

	// --- Issues ---

	/**
	 * Získání druhů připomínek.
	 *
	 * @returns seznam druhů připomínek
	 */
	protected List<WfIssueTypeVO> findAllIssueTypes() {
		return Arrays.asList(get(ALL_ISSUE_TYPES).getBody().as(WfIssueTypeVO[].class));
	}

	/**
	 * Získání stavů připomínek.
	 *
	 * @returns seznam stavů připomínek
	 */
	protected List<WfIssueStateVO> findAllIssueStates() {
		return Arrays.asList(get(ALL_ISSUE_STATES).getBody().as(WfIssueStateVO[].class));
	}

	/**
	 * Získání detailu protokolu.
	 *
	 * @param issueListId identifikátor protokolu
	 * @return protokol
	 */
	protected WfIssueListVO getIssueList(Integer issueListId) {
		return get(spec -> spec.pathParam("issueListId", issueListId), GET_ISSUE_LIST).as(WfIssueListVO.class);
	}

	/**
	 * Získání detailu připomínky.
	 *
	 * @param issueId identifikátor připomínky
	 * @return připomínka
	 */
	protected WfIssueVO getIssue(Integer issueId) {
		return get(spec -> spec.pathParam("issueId", issueId), GET_ISSUE).as(WfIssueVO.class);
	}

	/**
	 * Získání detailu komentáře.
	 *
	 * @param commentId identifikátor komentáře
	 * @returns komentář
	 */
	protected WfCommentVO getIssueComment(Integer commentId) {
		return get(spec -> spec.pathParam("commentId", commentId), GET_COMMENT).as(WfCommentVO.class);
	}

	/**
	 * Založí nový protokol k danému AS
	 *
	 * @param issueListVO data pro založení protokolu
	 * @return detail založeného protokolu
	 */
	protected WfIssueListVO addIssueList(WfIssueListVO issueListVO) {
		return post(spec -> spec.body(issueListVO), CREATE_ISSUE_LIST).getBody().as(WfIssueListVO.class);
	}

	/**
	 * Založení nové připomínky k danému protokolu
	 *
	 * @param issueVO data pro založení připomínky
	 * @return detail založené připomínky
	 */
	protected WfIssueVO addIssue(WfIssueVO issueVO) {
		return post(spec -> spec.body(issueVO), CREATE_ISSUE).getBody().as(WfIssueVO.class);
	}

	/**
	 * Založení nového komentáře k dané připomínce
	 *
	 * @param commentVO data pro založení protokolu
	 * @return detail založeného komentáře
	 */
	protected WfCommentVO addIssueComment(WfCommentVO commentVO) {
		return post(spec -> spec.body(commentVO), CREATE_COMMENT).getBody().as(WfCommentVO.class);
	}

	/**
	 * Vyhledá protokoly k danému archivní souboru - řazeno nejprve otevřené a pak
	 * uzavřené
	 *
	 * @param fundId identifikátor AS
	 * @param open   filtr pro stav (otevřený/uzavřený)
	 * @return seznam protokolů
	 */
	public List<WfIssueListVO> findIssueListByFund(Integer fundId, Boolean open) {
		return Arrays.asList(get(spec -> {
			spec.pathParam("fundId", fundId);
			if (open != null) {
				spec.queryParam("open", open);
			}
			return spec;
		}, FIND_ISSUE_LISTS).as(WfIssueListVO[].class));
	}

	/**
	 * Vyhledá připomínky k danému protokolu - řazeno vzestupně podle čísla
	 * připomínky
	 *
	 * @param issueListId  identifikátor protokolu
	 * @param issueStateId identifikátor stavu připomínky, dle kterého filtrujeme
	 * @param issueTypeId  identifikátor druhu připomínky, dle kterého filtrujeme
	 */
	protected List<WfIssueVO> findIssueByIssueList(Integer issueListId, Integer issueStateId, Integer issueTypeId) {
		return Arrays.asList(get(spec -> {
			spec.pathParam("issueListId", issueListId);
			if (issueStateId != null) {
				spec.queryParam("issueStateId", issueStateId);
			}
			if (issueTypeId != null) {
				spec.queryParam("issueTypeId", issueTypeId);
			}
			return spec;
		}, FIND_ISSUES).as(WfIssueVO[].class));
	}

	/**
	 * Vyhledá komentáře k dané připomínce - řazeno vzestupně podle času
	 *
	 * @param issueId identifikátor připomínky
	 * @return seznam komentářů
	 */
	protected List<WfCommentVO> findIssueCommentByIssue(Integer issueId) {
		return Arrays.asList(get(spec -> spec.pathParam("issueId", issueId), FIND_COMMENTS).as(WfCommentVO[].class));
	}

	/**
	 * Úprava vlastností existujícího protokolu
	 *
	 * @param issueListId identifikátor protokolu
	 * @param issueListVO data pro uložení protokolu
	 * @return detail uloženého protokolu
	 */
	protected WfIssueListVO updateIssueList(Integer issueListId, WfIssueListVO issueListVO) {
		return put(spec -> spec.pathParam("issueListId", issueListId).body(issueListVO), UPDATE_ISSUE_LIST).getBody()
				.as(WfIssueListVO.class);
	}

	/**
	 * Úprava připomínky
	 *
	 * @param issueId identifikátor připomínky
	 * @param issueVO data pro uložení připomínky
	 * @return detail založené připomínky
	 */
	protected WfIssueVO updateIssue(Integer issueId, WfIssueVO issueVO) {
		return put(spec -> spec.pathParam("issueId", issueId).body(issueVO), UPDATE_ISSUE).getBody()
				.as(WfIssueVO.class);
	}

	/**
	 * Změna druhu připomínky
	 *
	 * @param issueTypeId identifikátor stavu připomínky
	 */
	protected void setIssueType(Integer issueId, Integer issueTypeId) {
		post(spec -> spec.pathParam("issueId", issueId).queryParam("issueTypeId", issueTypeId), SET_ISSUE_TYPE);
	}

	/**
	 * Úprava komentáře
	 *
	 * @param commentId identifikátor komentáře
	 * @param commentVO data pro založení protokolu
	 * @return detail založeného komentáře
	 */
	protected WfCommentVO updateIssueComment(Integer commentId, WfCommentVO commentVO) {
		return put(spec -> spec.pathParam("commentId", commentId).body(commentVO), UPDATE_COMMENT).getBody()
				.as(WfCommentVO.class);
	}

	/**
	 * Založení šablony pro JP
	 *
	 * @param fundId identifikátor AS
	 * @return šablona pro JP
	 */
	protected ArrRefTemplateVO createRefTemplate(Integer fundId) {
		return put(spec -> spec.pathParam("fundId", fundId), CREATE_NODE_TEMPLATE).getBody().as(ArrRefTemplateVO.class);
	}

	/**
	 * Úprava šablony pro JP
	 *
	 * @param templateId        identifikátor šablony
	 * @param refTemplateEditVO formulář editace
	 * @return šablona JP
	 */
	protected ArrRefTemplateVO updateRefTemplate(Integer templateId, ArrRefTemplateEditVO refTemplateEditVO) {
		return post(spec -> spec.pathParam("templateId", templateId).body(refTemplateEditVO), UPDATE_NODE_TEMPLATE)
				.getBody().as(ArrRefTemplateVO.class);
	}

	/**
	 * Smazání šablony pro JP
	 *
	 * @param templateId identifikátor šablony
	 */
	protected void deleteRefTemplate(Integer templateId) {
		delete(spec -> spec.pathParam("templateId", templateId), DELETE_NODE_TEMPLATE);
	}

	/**
	 * Získání seznamu šablon pro AS
	 *
	 * @param fundId identifikátor AS
	 * @return seznam šablon
	 */
	protected List<ArrRefTemplateVO> getRefTemplate(Integer fundId) {
		return Arrays.asList(get(spec -> spec.pathParam("fundId", fundId), GET_NODE_TEMPLATES).getBody()
				.as(ArrRefTemplateVO[].class));
	}

	/**
	 * Založení nového mapování pro šablonu
	 *
	 * @param templateId               identifikátor šablony
	 * @param refTemplateMapTypeFormVO formulář mapování
	 */
	protected ArrRefTemplateMapTypeVO createRefTemplateMapType(Integer templateId,
			ArrRefTemplateMapTypeVO refTemplateMapTypeFormVO) {
		return post(spec -> spec.pathParam("templateId", templateId).body(refTemplateMapTypeFormVO),
				CREATE_NODE_TEMPLATE_MAP_TYPE).getBody().as(ArrRefTemplateMapTypeVO.class);
	}

	/**
	 * Editace mapování šablony
	 *
	 * @param templateId               identifikátor šablony
	 * @param mapTypeId                identifikátor mapování
	 * @param refTemplateMapTypeFormVO formulář mapování
	 */
	protected ArrRefTemplateMapTypeVO updateRefTemplateMapType(Integer templateId, Integer mapTypeId,
			ArrRefTemplateMapTypeVO refTemplateMapTypeFormVO) {
		return post(spec -> spec.pathParam("templateId", templateId).pathParam("mapTypeId", mapTypeId)
				.body(refTemplateMapTypeFormVO), UPDATE_NODE_TEMPLATE_MAP_TYPE).getBody()
				.as(ArrRefTemplateMapTypeVO.class);
	}

	/**
	 * Smazání mapování šablony
	 *
	 * @param templateId identifikátor šablony
	 * @param mapTypeId  identifikátor mapování
	 */
	protected void deleteRefTemplateMapType(Integer templateId, Integer mapTypeId) {
		delete(spec -> spec.pathParam("templateId", templateId).pathParam("mapTypeId", mapTypeId),
				DELETE_NODE_TEMPLATE_MAP_TYPE);
	}

	/**
	 * Připojení Websocket
	 * 
	 * @param sessionHandler
	 * @param recepiptStore
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	protected StompSession connectWebSocketStompClient(StompSessionHandler sessionHandler,
			final Map<String, Message<byte[]>> recepiptStore) throws InterruptedException, ExecutionException {
		WebSocketClient client = new StandardWebSocketClient();
		WebSocketStompClientElza stompClient = new WebSocketStompClientElza(client, (rcpId, msg) -> {
			recepiptStore.put(rcpId, msg);
		});

		ThreadPoolTaskScheduler taskScheduler = new TaskSchedulerBuilder().poolSize(1).build();
		taskScheduler.initialize();
		stompClient.setTaskScheduler(taskScheduler);

		stompClient.setMessageConverter(new MappingJackson2MessageConverter());

		// Dostáváme údaje k autorizaci
		WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
		StringBuilder cookie = new StringBuilder();
		cookies.forEach((name, value) -> {
			if (cookie.length() > 0) {
				cookie.append(';');
			}
			cookie.append(String.format("%s=%s", name, value));
		});
		if (cookie.length() > 0) {
			handshakeHeaders.set(HttpHeaders.COOKIE, cookie.toString());
		}

		return stompClient.connect("ws://localhost:" + port + "/stomp", handshakeHeaders, sessionHandler).get();
	}

	/**
	 * Čekáme na odpověď WebSocket StompClient
	 * 
	 * @param receipt
	 * @param sessionHandler
	 * @return
	 * @throws InterruptedException
	 */
	protected ReceiptStatus waitingForReceipt(Receiptable receipt, MyStompSessionHandler sessionHandler)
			throws InterruptedException {
		AtomicReference<ReceiptStatus> receiptStatus = new AtomicReference<ReceiptStatus>();
		receipt.addReceiptTask(() -> {
			logger.debug("Receipt received");
			receiptStatus.set(ReceiptStatus.RCP_RECEIVED);
		});
		receipt.addReceiptLostTask(() -> {
			logger.debug("Receipt lost");
			receiptStatus.set(ReceiptStatus.RCP_LOST);
		});
		while (receiptStatus.get() == null && !sessionHandler.hasError()) {
			logger.info("Waiting on receipt...");
			Thread.sleep(100);
		}
		if (sessionHandler.hasError()) {
			logger.debug("Receipt error over WebSocket");
			receiptStatus.set(ReceiptStatus.RCP_ERROR);
		}
		return receiptStatus.get();
	}

	protected class MyStompSessionHandler implements StompSessionHandler {

		private Logger logger = LoggerFactory.getLogger(this.getClass());

		int errorCount = 0;

		@Override
		public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
			logger.info("New session established : " + session.getSessionId());
		}

		@Override
		public Type getPayloadType(StompHeaders headers) {
			return String.class;
		}

		@Override
		public void handleFrame(StompHeaders headers, Object payload) {
			logger.info("Received: {}", payload);
		}

		@Override
		public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload,
				Throwable exception) {
			logger.error("Got an exception: ", exception);
			errorCount++;
		}

		@Override
		public void handleTransportError(StompSession session, Throwable exception) {
			logger.error("Got an transport error: ", exception);
			errorCount++;
		}

		public boolean hasError() {
			return errorCount > 0;
		}
	}
}
