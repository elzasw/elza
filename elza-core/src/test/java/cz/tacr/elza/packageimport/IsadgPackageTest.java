package cz.tacr.elza.packageimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.ElzaCoreMain;
import cz.tacr.elza.bulkaction.BulkActionConfigManager;
import cz.tacr.elza.bulkaction.generator.PersistentSortConfig;
import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.vo.ApPartFormVO;
import cz.tacr.elza.controller.vo.BulkActionVO;
import cz.tacr.elza.controller.vo.Institution;
import cz.tacr.elza.controller.vo.ap.item.ApItemStringVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemEnumVO;
import cz.tacr.elza.core.data.PackageTexts;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.data.ValidationMessage;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemSpecExt;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.vo.DataValidationResult;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.drools.DirectionLevel;
import cz.tacr.elza.drools.RulesExecutor;
import cz.tacr.elza.other.HelperTestService;
import cz.tacr.elza.repository.InstitutionTypeRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.ItemTypeSpecAssignRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.service.FundLevelService.AddLevelDirection;
import cz.tacr.elza.service.InstitutionService;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.StartupService;
import cz.tacr.elza.service.UserService;

/**
 * The ISAD(G) archival description package ISADG: its rule set next to its dependency ISAAR_CPF, a
 * fonds built by the scenarios of the levels and described by the rules, the validation of the
 * hierarchy and the containers, and the same installation with CZ_BASE and ZP2015.
 *
 * <p>Packages stay installed across test classes, so the class removes all of them first and at the
 * end; later test classes import the packages they need again.
 */
@ContextConfiguration(classes = ElzaCoreMain.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IsadgPackageTest {

    private static final String CODE = "ISADG";
    private static final String DIR = "rules-en-isadg";
    /** ISO 639-2 without the range reserved for local use, as ISAAR_CPF declares them */
    private static final int LANGUAGES = 486;
    private static final List<String> ESSENTIAL = List.of("ISADG_REFERENCE_CODE", "ISADG_TITLE", "ISADG_DATE",
                                                          "ISADG_EXTENT", "ISADG_CREATOR");

    @Autowired
    private HelperTestService helperTestService;
    @Autowired
    private StartupService startupService;
    @Autowired
    private StaticDataService staticDataService;
    @Autowired
    private UserService userService;
    @Autowired
    private AccessPointService apService;
    @Autowired
    private ScopeRepository scopeRepository;
    @Autowired
    private InstitutionService institutionService;
    @Autowired
    private InstitutionTypeRepository institutionTypeRepository;
    @Autowired
    private RuleSetRepository ruleSetRepository;
    @Autowired
    private ArrangementService arrangementService;
    @Autowired
    private FundLevelService fundLevelService;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private LevelRepository levelRepository;
    @Autowired
    private ItemTypeRepository itemTypeRepository;
    @Autowired
    private ItemSpecRepository itemSpecRepository;
    @Autowired
    private ItemTypeSpecAssignRepository itemTypeSpecAssignRepository;
    @Autowired
    private DescriptionItemService descriptionItemService;
    @Autowired
    private LevelTreeCacheService levelTreeCacheService;
    @Autowired
    private RuleService ruleService;
    @Autowired
    private RulesExecutor rulesExecutor;
    @Autowired
    private PackageTexts packageTexts;
    @Autowired
    private BulkActionConfigManager bulkActionConfigManager;
    @Autowired
    private ClientFactoryDO factoryDO;
    @Autowired
    @Qualifier("transactionManager")
    private PlatformTransactionManager txManager;

    private ParInstitution institution;
    private ArrFundVersion version;
    private Integer rootId;
    private Integer seriesId;
    private Integer fileId;
    private Integer itemId;

    @BeforeAll
    void loadPackages() {
        startupService.startNow();
        authorizeAsAdmin();
        // validation workers of a preceding test class may still write; they would collide with the delete
        helperTestService.waitForWorkers();
        helperTestService.deleteAllPackages();
        helperTestService.loadPackage("ISAAR_CPF", "package-isaar-cpf");
        helperTestService.loadPackage(CODE, DIR);
        institution = createInstitution();
    }

    @AfterAll
    void unloadPackages() {
        try {
            helperTestService.waitForWorkers();
            helperTestService.deleteAllPackages();
        } finally {
            startupService.stop();
        }
    }

    /** The rule set, its own vocabulary and the languages shared with ISAAR_CPF. */
    @Test
    @Order(1)
    void packageDeclaresTheRuleSetAndItsVocabulary() {
        tx(() -> {
            StaticDataProvider sdp = staticDataService.getData();
            assertNotNull(sdp.getRuleSetByCode(CODE));
            assertEquals(CODE, itemTypeRepository.findOneByCode("ISADG_TITLE").getRulPackage().getCode());
            assertNotNull(sdp.getStructuredTypeByCode("ISADG_CONTAINER"));
            // the languages are ISAAR_CPF's, assigned to the two language items of ISADG as well
            assertEquals("ISAAR_CPF", itemSpecRepository.findOneByCode("LNG_eng").getPackage().getCode());
            assertEquals(LANGUAGES, itemTypeSpecAssignRepository
                    .findByItemTypeSorted(itemTypeRepository.findOneByCode("ISADG_LANGUAGE")).size());
            List<String> codes = ruleService.getItemTypeCodesByRuleSet(sdp.getRuleSetByCode(CODE).getEntity());
            assertTrue(codes.contains("ISADG_TITLE") && codes.contains("ISADG_CONTAINER"), codes.toString());
            assertFalse(codes.contains("NM_MAIN"), "items of entities are not items of the fonds");
            // the persistent sort is recognised by its kind, so the rule set names its file freely
            assertEquals(PersistentSortConfig.TYPE, bulkActionConfigManager.getType("ISADG_PERSISTENT_SORT"));
            assertEquals(PersistentSortConfig.TYPE,
                         BulkActionVO.newInstance(bulkActionConfigManager.get("ISADG_PERSISTENT_SORT")).getType());
            assertEquals("FundValidation", bulkActionConfigManager.getType("ISADG_FUND_VALIDATION"));
        });
    }

    /** The root is a fonds; each level offers the scenarios of the levels allowed below it. */
    @Test
    @Order(2)
    void fondsIsBuiltByTheScenariosOfTheLevels() {
        tx(() -> {
            RulRuleSet ruleSet = ruleSetRepository.findByCode(CODE);
            version = arrangementService.createFundWithScenario("Smith family papers", ruleSet, "SMITH", institution,
                    null, null, null, null, null, null, null, null);
            rootId = version.getRootNode().getNodeId();
            assertEquals("ISADG_LEVEL_FONDS", level(rootId), "the only root scenario makes the root a fonds");
            assertEquals(List.of("Subfonds", "Series", "File", "Item"), scenarios(rootId));
            seriesId = addChild(rootId, "Series");
            assertEquals("ISADG_LEVEL_SERIES", level(seriesId));
            assertEquals(List.of("Series", "Subseries", "File", "Item"), scenarios(seriesId));
            fileId = addChild(seriesId, "File");
            assertEquals(List.of("File", "Item"), scenarios(fileId));
            itemId = addChild(fileId, "Item");
            assertEquals(List.of("Item"), scenarios(itemId));
            levelTreeCacheService.invalidateFundVersion(version);
        });
    }

    /** What a level offers follows ISAD(G): the essential elements at the fonds, the rest by level. */
    @Test
    @Order(3)
    void availableElementsFollowTheLevel() {
        tx(() -> {
            Map<String, RulItemType.Type> root = available(rootId);
            for (String essential : ESSENTIAL) {
                assertEquals(RulItemType.Type.REQUIRED, root.get(essential), essential);
            }
            assertEquals(RulItemType.Type.IMPOSSIBLE, root.get("ISADG_CONTAINER"), "no container at the fonds");
            assertEquals(List.of("ISADG_LEVEL_FONDS"), specs(rootId, "ISADG_LEVEL"));

            Map<String, RulItemType.Type> file = available(fileId);
            assertEquals(RulItemType.Type.REQUIRED, file.get("ISADG_TITLE"));
            assertEquals(RulItemType.Type.RECOMMENDED, file.get("ISADG_REFERENCE_CODE"));
            assertEquals(RulItemType.Type.RECOMMENDED, file.get("ISADG_DATE"));
            assertEquals(RulItemType.Type.POSSIBLE, file.get("ISADG_CONTAINER"));
            assertEquals(RulItemType.Type.POSSIBLE, file.get("ISADG_SCOPE_CONTENT"));
            assertEquals(RulItemType.Type.POSSIBLE, file.get("ISADG_ACCESS_POINT"));
            assertEquals(RulItemType.Type.IMPOSSIBLE, file.get("ISADG_SCALE"), "the scale only for maps and drawings");
            assertEquals(RulItemType.Type.IMPOSSIBLE, file.get("ISADG_FA_TITLE"), "items of the finding aid are not items of a unit");
            assertEquals(RulItemType.Type.IMPOSSIBLE, file.get("ISADG_CONTAINER_NUMBER"), "items of the container are not items of a unit");
            assertEquals(List.of("ISADG_LEVEL_SERIES", "ISADG_LEVEL_SUBSERIES", "ISADG_LEVEL_FILE", "ISADG_LEVEL_ITEM"),
                         specs(fileId, "ISADG_LEVEL"), "the levels allowed below a series");
            assertEquals(LANGUAGES, specs(fileId, "ISADG_LANGUAGE").size());
            assertEquals(List.of("ISADG_ACCESS_OPEN", "ISADG_ACCESS_RESTRICTED", "ISADG_ACCESS_CLOSED"),
                         specs(fileId, "ISADG_ACCESS_STATUS"));
        });
    }

    /** The hierarchy and the containers are checked; the messages are rendered in the reader's language. */
    @Test
    @Order(4)
    void validationChecksTheHierarchyAndTheContainers() {
        tx(() -> {
            assertEquals(List.of(), keys(rootId), "the fonds has nothing to report");
            assertEquals(List.of(), keys(seriesId), "a series needs no container");
            assertEquals(List.of("ISADG/CONTAINER_MISSING"), keys(itemId), "the item at the end of the branch has no container");

            // a series turned into a fonds does not fit below the fonds
            setLevel(seriesId, "ISADG_LEVEL_FONDS");
            assertEquals(List.of("ISADG/LEVEL_ORDER"), keys(seriesId));
            setLevel(seriesId, "ISADG_LEVEL_SERIES");
            assertEquals(List.of(), keys(seriesId));

            StaticDataProvider sdp = staticDataService.getData();
            String line = results(itemId).get(0).getMessage();
            assertEquals("The unit and the units above it have no container.",
                         packageTexts.render(line, sdp.getSysLanguageByTag("en")));
            assertEquals("Jednotka popisu ani jednotky nad ní nemají ukládací jednotku.",
                         packageTexts.render(line, sdp.getSysLanguageByTag("cs")));
        });
    }

    /** CZ_BASE and ZP2015 in the same installation: a Czech fonds next to the ISAD(G) one. */
    @Test
    @Order(5)
    void worksNextToCzBaseAndZp2015() {
        helperTestService.loadPackage("CZ_BASE", "package-cz-base");
        helperTestService.loadPackage("ZP2015", "rules-cz-zp2015");
        tx(() -> {
            StaticDataProvider sdp = staticDataService.getData();
            RulRuleSet zp2015 = ruleSetRepository.findByCode("ZP2015");
            ArrFundVersion czech = arrangementService.createFundWithScenario("Český fond", zp2015, "CZ", institution,
                    null, null, null, null, null, null, null, null);
            assertEquals("ZP2015_LEVEL_ROOT", levelOf(czech.getRootNode().getNodeId(), "ZP2015_LEVEL_TYPE"));

            List<String> isadg = ruleService.getItemTypeCodesByRuleSet(sdp.getRuleSetByCode(CODE).getEntity());
            List<String> czechItems = ruleService.getItemTypeCodesByRuleSet(zp2015);
            assertTrue(Collections.disjoint(isadg, czechItems), "each rule set lists its own items");
            // the shared languages exist once, owned by the package that declared them first
            assertEquals("ISAAR_CPF", itemSpecRepository.findOneByCode("LNG_eng").getPackage().getCode());
            assertEquals(1, itemSpecRepository.findAll().stream().filter(s -> s.getCode().equals("LNG_eng")).count());

            assertEquals(List.of(), keys(rootId), "the ISAD(G) fonds still validates");
            assertEquals(List.of("Subfonds", "Series", "File", "Item"), scenarios(rootId));
            assertEquals(LANGUAGES, specs(fileId, "ISADG_LANGUAGE").size(), "CAM's own languages are not offered");
        });
    }

    // helpers

    private String level(Integer nodeId) {
        return levelOf(nodeId, "ISADG_LEVEL");
    }

    private String levelOf(Integer nodeId, String itemType) {
        return descriptionItemService.findByNodeIdsAndDeleteChangeIsNull(List.of(nodeId)).stream()
                .filter(i -> i.getItemType().getCode().equals(itemType))
                .map(i -> i.getItemSpec().getCode())
                .findFirst().orElse(null);
    }

    private void setLevel(Integer nodeId, String specCode) {
        ArrDescItem item = descriptionItemService.findByNodeIdsAndDeleteChangeIsNull(List.of(nodeId)).stream()
                .filter(i -> i.getItemType().getCode().equals("ISADG_LEVEL"))
                .findFirst().orElseThrow();
        ArrItemEnumVO vo = new ArrItemEnumVO();
        vo.setId(item.getItemId());
        vo.setDescItemObjectId(item.getDescItemObjectId());
        vo.setPosition(item.getPosition());
        vo.setItemTypeId(item.getItemTypeId());
        vo.setDescItemSpecId(itemSpecRepository.findOneByCode(specCode).getItemSpecId());
        ArrDescItem changed = factoryDO.createDescItem(vo, item.getItemTypeId());
        changed.setItemId(item.getItemId());
        changed.setDescItemObjectId(item.getDescItemObjectId());
        changed.setPosition(item.getPosition() == null ? 1 : item.getPosition());
        ArrNode node = nodeRepository.findById(nodeId).orElseThrow();
        descriptionItemService.updateDescriptionItem(changed, node.getVersion(), nodeId, version.getFundVersionId(), true,
                                                     false);
        levelTreeCacheService.invalidateFundVersion(version);
    }

    private List<String> scenarios(Integer nodeId) {
        return rulesExecutor.executeScenarioOfNewLevelRules(arrLevel(nodeId), DirectionLevel.CHILD, version).stream()
                .map(ScenarioOfNewLevel::getName).toList();
    }

    private Integer addChild(Integer parentNodeId, String scenario) {
        ArrNode parent = nodeRepository.findById(parentNodeId).orElseThrow();
        List<ArrLevel> levels = fundLevelService.addNewLevel(version, parent, parent, AddLevelDirection.CHILD, scenario,
                null, null, null, null);
        // the newly created level has the highest level id
        Integer nodeId = levels.stream().max(Comparator.comparing(ArrLevel::getLevelId)).orElseThrow().getNode().getNodeId();
        levelTreeCacheService.invalidateFundVersion(version);
        return nodeId;
    }

    private List<RulItemTypeExt> itemTypes(Integer nodeId) {
        return ruleService.getDescriptionItemTypes(version, nodeRepository.findById(nodeId).orElseThrow());
    }

    private Map<String, RulItemType.Type> available(Integer nodeId) {
        Map<String, RulItemType.Type> types = new HashMap<>();
        for (RulItemTypeExt type : itemTypes(nodeId)) {
            types.put(type.getCode(), type.getType());
        }
        return types;
    }

    /** Codes of the specifications of the item type that the level may use, in their order. */
    private List<String> specs(Integer nodeId, String itemType) {
        RulItemTypeExt type = itemTypes(nodeId).stream().filter(t -> t.getCode().equals(itemType)).findFirst().orElseThrow();
        return type.getRulItemSpecList().stream()
                .filter(s -> s.getType() != RulItemSpec.Type.IMPOSSIBLE)
                .map(RulItemSpecExt::getCode).toList();
    }

    private List<DataValidationResult> results(Integer nodeId) {
        return rulesExecutor.executeDescItemValidationRules(arrLevel(nodeId), version);
    }

    private List<String> keys(Integer nodeId) {
        return results(nodeId).stream().map(r -> ValidationMessage.decode(r.getMessage()).getKey()).sorted().toList();
    }

    private ArrLevel arrLevel(Integer nodeId) {
        return levelRepository.findByNode(nodeRepository.findById(nodeId).orElseThrow(), version.getLockChange());
    }

    private void authorizeAsAdmin() {
        UserDetail userDetail = userService.createUserDetail((Integer) null);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("", "", null);
        auth.setDetails(userDetail);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * The institution keeping the fonds: a corporate body of an ISAAR(CPF) scope with the institution
     * type the package declares (without CZ_BASE there is no other).
     */
    private ParInstitution createInstitution() {
        Integer scopeId = createScope("ISADG_TEST", "ISAAR_CPF");
        Integer archive = txGet(() -> apService.createAccessPoint(scopeRepository.findById(scopeId).orElseThrow(),
                type("PARTY_GROUP"), form("PT_NAME", string("NM_MAIN", "Smith County Archives"))).getAccessPointId());
        return txGet(() -> {
            Institution dto = new Institution();
            dto.setInternalCode("SCA");
            dto.setAccessPointId(archive);
            dto.setInstitutionTypeId(institutionTypeRepository.findAll().stream()
                    .filter(t -> t.getCode().equals("ISADG_ARCHIVE")).findFirst().orElseThrow()
                    .getInstitutionTypeId());
            return institutionService.create(dto);
        });
    }

    /** Creates the scope, or gives an existing one (scopes survive the removal of packages) the rule set. */
    private Integer createScope(String code, String ruleSetCode) {
        return txGet(() -> {
            ApScope scope = scopeRepository.findAll().stream().filter(s -> code.equals(s.getCode())).findFirst()
                    .orElseGet(ApScope::new);
            scope.setCode(code);
            scope.setName(code);
            scope.setRulRuleSet(staticDataService.getData().getRuleSetByCode(ruleSetCode).getEntity());
            return scopeRepository.save(scope).getScopeId();
        });
    }

    private ApPartFormVO form(String partType, ApItemVO... items) {
        ApPartFormVO form = new ApPartFormVO();
        form.setPartTypeCode(partType);
        form.setItems(new ArrayList<>(List.of(items)));
        return form;
    }

    private ApItemVO string(String itemType, String value) {
        ApItemStringVO item = new ApItemStringVO();
        item.setTypeId(itemTypeRepository.findOneByCode(itemType).getItemTypeId());
        item.setValue(value);
        return item;
    }

    private ApType type(String code) {
        ApType type = staticDataService.getData().getApTypeByCode(code);
        assertNotNull(type, code);
        return type;
    }

    private void tx(Runnable body) {
        new TransactionTemplate(txManager).executeWithoutResult(status -> body.run());
    }

    private <T> T txGet(Supplier<T> body) {
        return new TransactionTemplate(txManager).execute(status -> body.get());
    }
}
