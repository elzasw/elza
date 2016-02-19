package cz.tacr.elza.controller;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.EncoderConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import cz.tacr.elza.AbstractTest;
import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemSpecExtVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.*;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.service.ArrMoveLevelService;
import org.apache.commons.lang.BooleanUtils;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import static com.jayway.restassured.RestAssured.given;


public abstract class AbstractControllerTest extends AbstractTest {

    private static final RestAssuredConfig UTF8_ENCODER_CONFIG = RestAssuredConfig.newConfig().encoderConfig(
            EncoderConfig.encoderConfig().defaultContentCharset("UTF-8"));

    private static final Logger logger = LoggerFactory.getLogger(AbstractControllerTest.class);
    protected static final String CONTENT_TYPE_HEADER = "content-type";
    protected static final String JSON_CONTENT_TYPE = "application/json";
    private static final Header JSON_CT_HEADER = new Header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE);

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
    protected static final String CREATE_FINDING_AID = ARRANGEMENT_CONTROLLER_URL + "/findingAids";
    protected static final String UPDATE_FINDING_AID = ARRANGEMENT_CONTROLLER_URL + "/updateFindingAid";
    protected static final String FINDING_AIDS = ARRANGEMENT_CONTROLLER_URL + "/getFindingAids";
    protected static final String APPROVE_VERSION = ARRANGEMENT_CONTROLLER_URL + "/approveVersion";
    protected static final String ADD_LEVEL = ARRANGEMENT_CONTROLLER_URL + "/levels";
    protected static final String DELETE_LEVEL = ARRANGEMENT_CONTROLLER_URL + "/levels";
    protected static final String SCENARIOS = ARRANGEMENT_CONTROLLER_URL + "/scenarios";
    protected static final String FA_TREE = ARRANGEMENT_CONTROLLER_URL + "/faTree";
    protected static final String MOVE_LEVEL_AFTER = ARRANGEMENT_CONTROLLER_URL + "/moveLevelAfter";
    protected static final String MOVE_LEVEL_BEFORE = ARRANGEMENT_CONTROLLER_URL + "/moveLevelBefore";
    protected static final String MOVE_LEVEL_UNDER = ARRANGEMENT_CONTROLLER_URL + "/moveLevelUnder";
    protected static final String CREATE_DESC_ITEM = ARRANGEMENT_CONTROLLER_URL
            + "/descItems/{findingAidVersionId}/{nodeId}/{nodeVersion}/{descItemTypeId}/create";
    protected static final String UPDATE_DESC_ITEM = ARRANGEMENT_CONTROLLER_URL
            + "/descItems/{findingAidVersionId}/{nodeVersion}/update/{createNewVersion}";
    protected static final String DELETE_DESC_ITEM = ARRANGEMENT_CONTROLLER_URL
            + "/descItems/{findingAidVersionId}/{nodeVersion}/delete";

    // REGISTRY
    protected static final String DEFAULT_SCOPES = REGISTRY_CONTROLLER_URL + "/defaultScopes";
    protected static final String CREATE_SCOPE = REGISTRY_CONTROLLER_URL + "/scopes";
    protected static final String UPDATE_SCOPE = REGISTRY_CONTROLLER_URL + "/scopes/";
    protected static final String DELETE_SCOPE = REGISTRY_CONTROLLER_URL + "/scopes/";
    protected static final String FA_SCOPES = REGISTRY_CONTROLLER_URL + "/faScopes";
    protected static final String ALL_SCOPES = REGISTRY_CONTROLLER_URL + "/scopes";
    protected static final String RECORD_TYPES = REGISTRY_CONTROLLER_URL + "/recordTypes";

    protected static final String FIND_RECORD = REGISTRY_CONTROLLER_URL + "/findRecord";
    protected static final String GET_RECORD = REGISTRY_CONTROLLER_URL + "/getRecord";
    protected static final String CREATE_RECORD = REGISTRY_CONTROLLER_URL + "/createRecord";
    protected static final String UPDATE_RECORD = REGISTRY_CONTROLLER_URL + "/updateRecord";
    protected static final String DELETE_RECORD = REGISTRY_CONTROLLER_URL + "/deleteRecord";

    protected static final String CREATE_VARIANT_RECORD = REGISTRY_CONTROLLER_URL + "/createVariantRecord";
    protected static final String UPDATE_VARIANT_RECORD = REGISTRY_CONTROLLER_URL + "/updateVariantRecord";
    protected static final String DELETE_VARIANT_RECORD = REGISTRY_CONTROLLER_URL + "/deleteVariantRecord";

    // RULE
    protected static final String RULE_SETS = RULE_CONTROLLER_URL + "/getRuleSets";
    protected static final String DATA_TYPES = RULE_CONTROLLER_URL + "/dataTypes";
    protected static final String DESC_ITEM_TYPES = RULE_CONTROLLER_URL + "/descItemTypes";
    protected static final String PACKAGES = RULE_CONTROLLER_URL + "/getPackages";

    // Validation
    protected static final String VALIDATE_UNIT_DATE = VALIDATION_CONTROLLER_URL + "/unitDate";

    @Value("${local.server.port}")
    private int port;

    private List<RulDataTypeVO> dataTypes = null;
    private List<RulDescItemTypeExtVO> descItemTypes = null;

    @Before
    public void setUp() {
        super.setUp();
        RestAssured.port = port;                        // nastavi default port pro REST-assured
        RestAssured.baseURI = RestAssured.DEFAULT_URI;  // nastavi default URI pro REST-assured. Nejcasteni localhost
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
     * Získání seznamu pravidel.
     *
     * @return seznam pravidel
     */
    protected List<RulRuleSetVO> getRuleSets() {
        Response response = get(RULE_SETS);
        return Arrays.asList(response.getBody().as(RulRuleSetVO[].class));
    }

    /**
     * Vytvoření archivní pomůcky.
     *
     * @param name              název AP
     * @param arrangementTypeId identifikátor výstupu
     * @param ruleSetId         identifikátor pravidel
     * @return ap
     */
    protected ArrFindingAidVO createFindingAid(final String name,
                                               final Integer arrangementTypeId,
                                               final Integer ruleSetId) {
        Response response = post(spec -> spec
                .queryParameter("name", name)
                .queryParameter("arrangementTypeId", arrangementTypeId)
                .queryParameter("ruleSetId", ruleSetId), CREATE_FINDING_AID);
        return response.getBody().as(ArrFindingAidVO.class);
    }

    /**
     * Vytvoření výchozí archivní pomůcky.
     *
     * @param name název AP
     * @return ap
     */
    protected ArrFindingAidVO createFindingAid(final String name) {
        List<RulRuleSetVO> ruleSets = getRuleSets();
        RulRuleSetVO ruleSet = ruleSets.get(0);
        RulArrangementTypeVO arrangementType = ruleSet.getArrangementTypes().get(0);
        return createFindingAid(name, arrangementType.getId(), ruleSet.getId());
    }

    /**
     * Úprava archivní pomůcky.
     *
     * @param findingAid ap k úpravě
     * @return ap
     */
    protected ArrFindingAidVO updateFindingAid(final ArrFindingAidVO findingAid) {
        Response response = post(spec -> spec.body(findingAid), UPDATE_FINDING_AID);
        return response.getBody().as(ArrFindingAidVO.class);
    }

    /**
     * Uzavření verze archivní pomůcky.
     *
     * @param findingAidVersion verze archivní pomůcky
     * @param arrangementType   typ výstupu
     * @return nová verze ap
     */
    protected ArrFindingAidVersionVO approveVersion(final ArrFindingAidVersionVO findingAidVersion,
                                                    final RulArrangementTypeVO arrangementType) {
        return approveVersion(findingAidVersion.getId(), arrangementType.getId(), arrangementType.getRuleSetId());
    }

    /**
     * Uzavření verze archivní pomůcky.
     *
     * @param versionId         identifikátor verze archivní pomůcky
     * @param arrangementTypeId identifikátor výstupu
     * @param ruleSetId         identifikátor pravidel
     * @return nová verze ap
     */
    protected ArrFindingAidVersionVO approveVersion(final Integer versionId,
                                                    final Integer arrangementTypeId,
                                                    final Integer ruleSetId) {
        Response response = put(spec -> spec
                .queryParameter("versionId", versionId)
                .queryParameter("arrangementTypeId", arrangementTypeId)
                .queryParameter("ruleSetId", ruleSetId), APPROVE_VERSION);
        return response.getBody().as(ArrFindingAidVersionVO.class);
    }

    /**
     * Vrátí archivní pomůcky s verzema.
     *
     * @return archivní pomůcky
     */
    protected List<ArrFindingAidVO> getFindingAids() {
        Response response = get(FINDING_AIDS);
        return Arrays.asList(response.getBody().as(ArrFindingAidVO[].class));
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
    protected TreeData getFaTree(final ArrangementController.FaTreeParam input) {
        Response response = post(spec -> spec.body(input), FA_TREE);
        return response.getBody().as(TreeData.class);
    }

    /**
     * Přidání nového uzlu.
     *
     * @param direction         směr přidání
     * @param findingAidVersion verze archivní pomůcky
     * @param staticNode        uzel vůči kterému přidávám
     * @param parentStaticNode  rodič uzlu vůči kterému přidávám
     * @return vytvořený uzel
     */
    protected ArrangementController.NodeWithParent addLevel(final ArrMoveLevelService.AddLevelDirection direction,
                                                            final ArrFindingAidVersionVO findingAidVersion,
                                                            final ArrNodeVO staticNode,
                                                            final ArrNodeVO parentStaticNode) {
        ArrangementController.AddLevelParam addLevelParam = new ArrangementController.AddLevelParam();
        addLevelParam.setVersionId(findingAidVersion.getId());
        addLevelParam.setDirection(direction);
        addLevelParam.setStaticNode(staticNode);
        addLevelParam.setStaticNodeParent(parentStaticNode);

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
     * @param findingAidVersion   verze archivní pomůcky
     * @param staticNode          uzel vůči kterému přesouvám
     * @param staticNodeParent    rodič uzlu vůči kterému přesouvám
     * @param transportNodes      přesouvaný uzly
     * @param transportNodeParent rodič přesouvaných uzlů
     */
    protected void moveLevelBefore(final ArrFindingAidVersionVO findingAidVersion,
                                   final ArrNodeVO staticNode,
                                   final ArrNodeVO staticNodeParent,
                                   final List<ArrNodeVO> transportNodes,
                                   final ArrNodeVO transportNodeParent) {
        ArrangementController.LevelMoveParam moveParam = createMoveParam(findingAidVersion, staticNode,
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
     * @param findingAidVersion   verze archivní pomůcky
     * @param staticNode          uzel vůči kterému přesouvám
     * @param staticNodeParent    rodič uzlu vůči kterému přesouvám
     * @param transportNodes      přesouvaný uzly
     * @param transportNodeParent rodič přesouvaných uzlů
     */
    protected void moveLevelAfter(final ArrFindingAidVersionVO findingAidVersion,
                                  final ArrNodeVO staticNode,
                                  final ArrNodeVO staticNodeParent,
                                  final List<ArrNodeVO> transportNodes,
                                  final ArrNodeVO transportNodeParent) {
        ArrangementController.LevelMoveParam moveParam = createMoveParam(findingAidVersion, staticNode,
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
     * @param findingAidVersion   verze archivní pomůcky
     * @param staticNode          uzel vůči kterému přesouvám
     * @param staticNodeParent    rodič uzlu vůči kterému přesouvám
     * @param transportNodes      přesouvaný uzly
     * @param transportNodeParent rodič přesouvaných uzlů
     */
    protected void moveLevelUnder(final ArrFindingAidVersionVO findingAidVersion,
                                  final ArrNodeVO staticNode,
                                  final ArrNodeVO staticNodeParent,
                                  final List<ArrNodeVO> transportNodes,
                                  final ArrNodeVO transportNodeParent) {
        ArrangementController.LevelMoveParam moveParam = createMoveParam(findingAidVersion, staticNode,
                staticNodeParent, transportNodes, transportNodeParent);
        moveLevelUnder(moveParam);
    }

    /**
     * Vytvoření parametrů pro přesun.
     *
     * @param findingAidVersion   verze archivní pomůcky
     * @param staticNode          uzel vůči kterému přesouvám
     * @param staticNodeParent    rodič uzlu vůči kterému přesouvám
     * @param transportNodes      přesouvaný uzly
     * @param transportNodeParent rodič přesouvaných uzlů
     * @return parametry přesunu
     */
    private ArrangementController.LevelMoveParam createMoveParam(final ArrFindingAidVersionVO findingAidVersion,
                                                                 final ArrNodeVO staticNode,
                                                                 final ArrNodeVO staticNodeParent,
                                                                 final List<ArrNodeVO> transportNodes,
                                                                 final ArrNodeVO transportNodeParent) {
        ArrangementController.LevelMoveParam moveParam = new ArrangementController.LevelMoveParam();
        moveParam.setVersionId(findingAidVersion.getId());
        moveParam.setStaticNode(staticNode);
        moveParam.setStaticNodeParent(staticNodeParent);
        moveParam.setTransportNodes(transportNodes);
        moveParam.setTransportNodeParent(transportNodeParent);
        return moveParam;
    }

    /**
     * Smazání uzlu.
     *
     * @param findingAidVersion verze archivní pomůcky
     * @param staticNode        uzel který mažu
     * @param staticNodeParent  rodič uzlu který mažu
     * @return smazaný uzel s rodičem
     */
    protected ArrangementController.NodeWithParent deleteLevel(final ArrFindingAidVersionVO findingAidVersion,
                                                               final ArrNodeVO staticNode,
                                                               final ArrNodeVO staticNodeParent) {
        ArrangementController.NodeParam nodeParam = new ArrangementController.NodeParam();
        nodeParam.setVersionId(findingAidVersion.getId());
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
     * @param findingAidVersion verze archivní pomůcky
     * @param node              uzel
     * @param descItemType      typ atributu
     * @return vytvořená hodnota atributu
     */
    protected ArrangementController.DescItemResult createDescItem(final ArrDescItemVO descItem,
                                                                  final ArrFindingAidVersionVO findingAidVersion,
                                                                  final ArrNodeVO node,
                                                                  final RulDescItemTypeVO descItemType) {
        return createDescItem(descItem, findingAidVersion.getId(), descItemType.getId(), node.getId(),
                node.getVersion());
    }

    /**
     * Vytvoření hodnoty atributu.
     *
     * @param descItem            hodnota atributu
     * @param findingAidVersionId identifikátor verze AP
     * @param descItemTypeId      identifikátor typu hodnoty atributu
     * @param nodeId              identfikátor uzlu
     * @param nodeVersion         verze uzlu
     * @return vytvořená hodnota atributu
     */
    protected ArrangementController.DescItemResult createDescItem(final ArrDescItemVO descItem,
                                                                  final Integer findingAidVersionId,
                                                                  final Integer descItemTypeId,
                                                                  final Integer nodeId,
                                                                  final Integer nodeVersion) {
        Response response = put(spec -> spec
                .body(descItem)
                .pathParameter("findingAidVersionId", findingAidVersionId)
                .pathParameter("descItemTypeId", descItemTypeId)
                .pathParameter("nodeId", nodeId)
                .pathParameter("nodeVersion", nodeVersion), CREATE_DESC_ITEM);
        return response.getBody().as(ArrangementController.DescItemResult.class);
    }

    /**
     * Upravení hodnoty atributu.
     *
     * @param descItem          hodnota atributu
     * @param findingAidVersion verze archivní pomůcky
     * @param node              uzel
     * @param createNewVersion  vytvořit novou verzi?
     * @return upravená hodnota atributu
     */
    protected ArrangementController.DescItemResult updateDescItem(final ArrDescItemVO descItem,
                                                                  final ArrFindingAidVersionVO findingAidVersion,
                                                                  final ArrNodeVO node,
                                                                  final Boolean createNewVersion) {
        return updateDescItem(descItem, findingAidVersion.getId(), node.getVersion(), createNewVersion);
    }

    /**
     * Upravení hodnoty atributu.
     *
     * @param descItem            hodnota atributu
     * @param findingAidVersionId identifikátor verze AP
     * @param nodeVersion         verze uzlu
     * @param createNewVersion    vytvořit novou verzi?
     * @return upravená hodnota atributu
     */
    protected ArrangementController.DescItemResult updateDescItem(final ArrDescItemVO descItem,
                                                                  final Integer findingAidVersionId,
                                                                  final Integer nodeVersion,
                                                                  final Boolean createNewVersion) {
        Response response = put(spec -> spec
                .body(descItem)
                .pathParameter("findingAidVersionId", findingAidVersionId)
                .pathParameter("nodeVersion", nodeVersion)
                .pathParameter("createNewVersion", createNewVersion), UPDATE_DESC_ITEM);
        return response.getBody().as(ArrangementController.DescItemResult.class);
    }

    /**
     * Smazání hodnoty atributu.
     *
     * @param descItem          hodnota atributu
     * @param findingAidVersion verze archivní pomůcky
     * @param node              uzel
     * @return smazaná hodnota atributu
     */
    protected ArrangementController.DescItemResult deleteDescItem(final ArrDescItemVO descItem,
                                                                  final ArrFindingAidVersionVO findingAidVersion,
                                                                  final ArrNodeVO node) {
        return deleteDescItem(descItem, findingAidVersion.getId(), node.getVersion());
    }

    /**
     * Smazání hodnoty atributu.
     *
     * @param descItem            hodnota atributu
     * @param findingAidVersionId identifikátor verze AP
     * @param nodeVersion         verze uzlu
     * @return smazaná hodnota atributu
     */
    protected ArrangementController.DescItemResult deleteDescItem(final ArrDescItemVO descItem,
                                                                  final Integer findingAidVersionId,
                                                                  final Integer nodeVersion) {
        Response response = post(spec -> spec
                .body(descItem)
                .pathParameter("findingAidVersionId", findingAidVersionId)
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
    protected RegScopeVO createScope(RegScopeVO scope) {
        return post(spec -> spec.body(scope), CREATE_SCOPE).getBody().as(RegScopeVO.class);
    }

    /**
     * Aktualizace třídy.
     *
     * @param scope objekt třídy
     * @return aktualizovaný objekt třídy
     */
    protected RegScopeVO updateScope(RegScopeVO scope) {
        return put(spec -> spec.body(scope), UPDATE_SCOPE + scope.getId()).getBody().as(RegScopeVO.class);
    }

    /**
     * Smazání třídy. Třída nesmí být napojena na rejstříkové heslo.
     *
     * @param id id třídy.
     */
    protected Response deleteScope(final int id) {
        return delete(spec -> spec, DELETE_SCOPE + id);
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

        return get(spec -> spec.queryParameters(params), FIND_RECORD).getBody().as(RegRecordWithCount.class).getRecordList();
    }


    protected RegVariantRecordVO createVariantRecord(RegVariantRecordVO recordVO) {
        return put(spec -> spec.body(recordVO), CREATE_VARIANT_RECORD).getBody().as(RegVariantRecordVO.class);
    }

    protected Response deleteVariantRecord(int id) {
        return delete(spec -> spec.queryParam("variantRecordId", id), DELETE_VARIANT_RECORD);
    }
}