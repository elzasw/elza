package cz.tacr.elza.controller;

import static com.jayway.restassured.RestAssured.given;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.EncoderConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;

import cz.tacr.elza.AbstractTest;
import cz.tacr.elza.controller.vo.ArrFindingAidVO;
import cz.tacr.elza.controller.vo.ArrFindingAidVersionVO;
import cz.tacr.elza.controller.vo.RulArrangementTypeVO;
import cz.tacr.elza.controller.vo.RulDataTypeVO;
import cz.tacr.elza.controller.vo.RulRuleSetVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.ValidationResult;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.service.ArrMoveLevelService;


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

    // RULE
    protected static final String RULE_SETS = RULE_CONTROLLER_URL + "/getRuleSets";
    protected static final String DATA_TYPES = RULE_CONTROLLER_URL + "/dataTypes";
    protected static final String DESC_ITEM_TYPES = RULE_CONTROLLER_URL + "//*";
    protected static final String PACKAGES = RULE_CONTROLLER_URL + "/getPackages";

    // Validation
    protected static final String VALIDATE_UNIT_DATE = VALIDATION_CONTROLLER_URL + "/unitDate";

    @Value("${local.server.port}")
    private int port;

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
     * @param findingAidVersion verze archivní pomůcky
     * @param staticNode uzel vůči kterému přesouvám
     * @param staticNodeParent rodič uzlu vůči kterému přesouvám
     * @param transportNodes přesouvaný uzly
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
     * @param staticNode uzel který mažu
     * @param staticNodeParent rodič uzlu který mažu
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
     * Získání rule data types
     *
     * @return list rule data types
     */
    protected List<RulDataTypeVO> getDataTypes() {
        return Arrays.asList(get(DATA_TYPES).getBody().as(RulDataTypeVO[].class));
    }

    /**
     * Získání rule data types
     *
     * @return list rule data types
     */
    protected List<RulDescItemTypeExtVO> getDescItemTypes() {
        return Arrays.asList(get(DESC_ITEM_TYPES).getBody().as(RulDescItemTypeExtVO[].class));
    }

    /**
     * Získání rule data types
     *
     * @return list rule data types
     */
    protected List<RulPackage> getPackages() {
        return Arrays.asList(get(PACKAGES).getBody().as(RulPackage[].class));
    }


}