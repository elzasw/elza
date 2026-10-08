package cz.tacr.elza.rules.addon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

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

import cz.tacr.elza.AbstractTest;
import cz.tacr.elza.ElzaCoreMain;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.RuleSet;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.dataexchange.input.DEImportParams;
import cz.tacr.elza.dataexchange.input.DEImportService;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.RulArrangementRule;
import cz.tacr.elza.domain.RulArrangementRule.RuleType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemSpecExt;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulItemTypeSpecAssign;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.UISettings.EntityType;
import cz.tacr.elza.domain.UISettings.SettingsType;
import cz.tacr.elza.domain.vo.DataValidationResult;
import cz.tacr.elza.domain.vo.DataValidationResult.ValidationResultType;
import cz.tacr.elza.drools.RulesExecutor;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.PackageCode;
import cz.tacr.elza.other.HelperTestService;
import cz.tacr.elza.packageimport.PackageService;
import cz.tacr.elza.packageimport.xml.SettingOutputDefaults;
import cz.tacr.elza.repository.ArrangementRuleRepository;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.ItemTypeSpecAssignRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.OutputFilterRepository;
import cz.tacr.elza.repository.PackageRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.service.FundLevelService.AddLevelDirection;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.SettingsService;
import cz.tacr.elza.service.StartupService;
import cz.tacr.elza.service.UserService;

/**
 * Proves that a dependent package can extend the ZP2015 rule set without owning it - the
 * "addon" path of the package importer.
 *
 * <p>The test package {@code rules-addon-test} declares a dependency on ZP2015 and contributes an
 * item type of its own, a specification attached to the ZP2015 item type {@code ZP2015_OTHER_ID}
 * and placed after one of its ZP2015 specifications ({@code view-after}), and rule files placed
 * under {@code rul_rule_set/ZP2015/}. The importer registers such a directory for a rule set the
 * package does not own as an addon; its rules are stored against the addon package and executed
 * after the ZP2015 rules because of their higher priority, each rule file in a session of its own.
 * That ordering is what lets an addon both add to and override what the base rules decided. An
 * {@code ITEM_TYPE_FILTER} rule adds the addon item type to the rule set's list of item types.
 *
 * <p>A rule-set {@code ui_setting.xml} from a second package is still refused by the importer; the
 * test package ships none.
 *
 * <p>Packages are imported once for the class and removed again afterwards, because the rule
 * packages are not among the tables the shared helper wipes and would otherwise stay visible to
 * every later test class. The fund used to evaluate the rules lives only inside a rolled-back
 * transaction, so no fund version references ZP2015 when the package is deleted. Context
 * configuration matches {@code AbstractTest} so the cached Spring context is reused; the class is
 * not derived from it because its {@code @BeforeEach} would re-import the packages per scenario.
 */
@ContextConfiguration(classes = ElzaCoreMain.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AddonPackageTest {

    private static final String BASE_CODE = "ZP2015";
    private static final String ADDON_CODE = "ADDON_TEST";
    private static final String ADDON_DIR = "rules-addon-test";

    private static final String ADDON_ITEM_TYPE = "ADT_STAGE";
    private static final String ADDON_SPEC = "ADT_OTHERID_TEST";
    private static final String FOREIGN_ITEM_TYPE = "ZP2015_OTHER_ID";
    /** ZP2015 specification the addon places its specification after (view-after). */
    private static final String ANCHOR_SPEC = "ZP2015_OTHERID_SIG";
    private static final String OVERRIDDEN_ITEM_TYPE = "ZP2015_INTERNAL_NOTE";
    private static final String ADDON_MISSING_MESSAGE = "Addon stage is missing (rule ADT_001).";
    private static final String BASE_DEFAULT_OUTPUT_FILTER = "ZP_ACCESS_RESTRICT";
    private static final String ADDON_OUTPUT_FILTER = "ADT_FILTER";

    @Autowired
    private HelperTestService helperTestService;
    @Autowired
    private StaticDataService staticDataService;
    @Autowired
    private StartupService startupService;
    @Autowired
    private PackageService packageService;
    @Autowired
    private PackageRepository packageRepository;
    @Autowired
    private RuleSetRepository ruleSetRepository;
    @Autowired
    private ArrangementRuleRepository arrangementRuleRepository;
    @Autowired
    private ItemTypeRepository itemTypeRepository;
    @Autowired
    private ItemTypeSpecAssignRepository itemTypeSpecAssignRepository;
    @Autowired
    private ResourcePathResolver resourcePathResolver;
    @Autowired
    private RuleService ruleService;
    @Autowired
    private RulesExecutor rulesExecutor;
    @Autowired
    private ArrangementService arrangementService;
    @Autowired
    private FundLevelService fundLevelService;
    @Autowired
    private LevelTreeCacheService levelTreeCacheService;
    @Autowired
    private LevelRepository levelRepository;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private InstitutionRepository institutionRepository;
    @Autowired
    private DEImportService deImportService;
    @Autowired
    private AccessPointService apService;
    @Autowired
    private UserService userService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private OutputFilterRepository outputFilterRepository;
    @Autowired
    @Qualifier("transactionManager")
    private PlatformTransactionManager txManager;

    /** Number of ATTRIBUTE_TYPES and CONFORMITY_INFO rules ZP2015 has on its own. */
    private int baseAttributeRules;
    private int baseValidationRules;
    /** Default output filter of ZP2015 before the addon was imported. */
    private String baseDefaultOutputFilter;

    private ParInstitution institution;

    /** Rule file of the addon as resolved by the running system; checked again after removal. */
    private Path addonAttributeRulesFile;

    @BeforeAll
    void loadPackagesOnce() throws IOException {
        helperTestService.deleteTables(false);
        startupService.startNow();
        helperTestService.loadPackage("CZ_BASE", "package-cz-base");
        helperTestService.loadPackage(BASE_CODE, "rules-cz-zp2015");

        RuleSet base = staticDataService.getData().getRuleSetByCode(BASE_CODE);
        baseAttributeRules = base.getRulesByType(RuleType.ATTRIBUTE_TYPES).size();
        baseValidationRules = base.getRulesByType(RuleType.CONFORMITY_INFO).size();
        tx(() -> baseDefaultOutputFilter = outputFilterCode(settingsService.getOutputDefaults(base.getRuleSetId())));

        helperTestService.loadPackage(ADDON_CODE, ADDON_DIR);

        authorizeAsAdmin();
        institution = importInstitution();
    }

    @AfterAll
    void unloadPackages() {
        if (packageRepository.findByCode(ADDON_CODE) != null) {
            packageService.deletePackage(ADDON_CODE);
        }
        packageService.deletePackage(BASE_CODE);
        staticDataService.refreshForCurrentThread();
        startupService.stop();
    }

    /** The addon owns its item type; its specification hangs on an item type owned by ZP2015. */
    @Test
    @Order(1)
    void addonOwnsItsItemTypeAndAttachesASpecificationToAForeignType() {
        tx(() -> {
            RulPackage addon = addonPackage();
            StaticDataProvider sdp = staticDataService.getData();

            ItemType stage = sdp.getItemTypeByCode(ADDON_ITEM_TYPE);
            assertNotNull(stage, "addon item type was not imported");
            List<RulItemType> ownedByAddon = itemTypeRepository.findByRulPackage(addon);
            assertEquals(1, ownedByAddon.size());
            assertEquals(ADDON_ITEM_TYPE, ownedByAddon.get(0).getCode());

            ItemType otherId = sdp.getItemTypeByCode(FOREIGN_ITEM_TYPE);
            assertNotNull(otherId.getItemSpecByCode(ADDON_SPEC), "addon specification not attached");
            RulItemType otherIdEntity = itemTypeRepository.findOneByCode(FOREIGN_ITEM_TYPE);
            assertEquals(BASE_CODE, otherIdEntity.getRulPackage().getCode(),
                         "attaching a specification must not change who owns the item type");

            assertSpecFollowsAnchor(otherIdEntity);
        });
    }

    /**
     * The anchor survives a re-import of the package that owns the item type: the ordering pass
     * runs over all item types on every import, so ZP2015 cannot push the addon spec back to the end.
     */
    @Test
    @Order(2)
    void specificationPositionSurvivesReimportOfTheBasePackage() {
        Boolean testing = packageService.getTesting();
        packageService.setTesting(true); // same version may be imported again
        try {
            helperTestService.loadPackage(BASE_CODE, "rules-cz-zp2015");
        } finally {
            packageService.setTesting(testing);
        }
        staticDataService.refreshForCurrentThread();

        tx(() -> assertSpecFollowsAnchor(itemTypeRepository.findOneByCode(FOREIGN_ITEM_TYPE)));
    }

    /** Addon rules are appended to the ZP2015 rule set, run last, and are read from the addon's directory. */
    @Test
    @Order(3)
    void addonRulesRunAfterTheBaseRulesFromTheAddonPackageDirectory() {
        tx(() -> {
            RulPackage addon = addonPackage();
            RuleSet base = staticDataService.getData().getRuleSetByCode(BASE_CODE);

            List<RulArrangementRule> attributeRules = base.getRulesByType(RuleType.ATTRIBUTE_TYPES);
            assertEquals(baseAttributeRules + 1, attributeRules.size());
            RulArrangementRule addonRule = attributeRules.get(attributeRules.size() - 1);
            assertEquals(addon.getPackageId(), addonRule.getPackageId());
            assertEquals(200, addonRule.getPriority());
            assertTrue(attributeRules.get(0).getPriority() < addonRule.getPriority(),
                       "base rules must run before the addon rules");

            List<RulArrangementRule> validationRules = base.getRulesByType(RuleType.CONFORMITY_INFO);
            assertEquals(baseValidationRules + 1, validationRules.size());
            assertEquals(addon.getPackageId(), validationRules.get(validationRules.size() - 1).getPackageId());

            addonAttributeRulesFile = resourcePathResolver.getDroolFile(addonRule);
            assertTrue(Files.isRegularFile(addonAttributeRulesFile), "addon rule file not stored");
            assertTrue(addonAttributeRulesFile.startsWith(resourcePathResolver.getPackageDir(addon)),
                       "addon rule file must live in the addon package directory");
        });
    }

    /**
     * On a ZP2015 fund the addon item type is offered on folders only, the addon specification is
     * offered on the foreign item type, a base decision is overridden on folders and left alone on
     * the root, and the addon validation rule reports its missing item in its own words.
     */
    @Test
    @Order(4)
    void addonRulesExtendAndOverrideAvailabilityAndValidateFolders() {
        TransactionTemplate template = new TransactionTemplate(txManager);
        template.executeWithoutResult(status -> {
            try {
                RulRuleSet base = ruleSetRepository.findByCode(BASE_CODE);
                ArrFundVersion version = arrangementService.createFundWithScenario("Addon test fund", base, "ADT",
                        institution, null, null, null, null, null, null, null, null);
                ArrNode root = version.getRootNode();
                ArrNode series = addChild(version, root, "Série");
                ArrNode folder = addChild(version, series, "Složka");

                List<RulItemTypeExt> folderTypes = ruleService.getDescriptionItemTypes(version, folder);
                assertEquals(RulItemType.Type.POSSIBLE, typeOf(folderTypes, ADDON_ITEM_TYPE).getType(),
                             "addon item type must be offered on a folder");
                assertEquals(RulItemType.Type.IMPOSSIBLE, typeOf(folderTypes, OVERRIDDEN_ITEM_TYPE).getType(),
                             "addon must be able to override a base decision");
                assertEquals(RulItemSpec.Type.POSSIBLE,
                             specOf(typeOf(folderTypes, FOREIGN_ITEM_TYPE), ADDON_SPEC).getType(),
                             "addon specification must be offered on the foreign item type");

                List<RulItemTypeExt> rootTypes = ruleService.getDescriptionItemTypes(version, root);
                assertEquals(RulItemType.Type.IMPOSSIBLE, typeOf(rootTypes, ADDON_ITEM_TYPE).getType(),
                             "addon item type is folder-only");
                assertNotEquals(RulItemType.Type.IMPOSSIBLE, typeOf(rootTypes, OVERRIDDEN_ITEM_TYPE).getType(),
                                "base decision must stand where the addon is silent");

                ArrLevel folderLevel = levelRepository.findByNodeAndDeleteChangeIsNull(folder);
                DataValidationResult missing = rulesExecutor.executeDescItemValidationRules(folderLevel, version)
                        .stream()
                        .filter(r -> ADDON_ITEM_TYPE.equals(r.getTypeCode()))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("addon validation rule did not fire on the folder"));
                assertEquals(ValidationResultType.MISSING, missing.getResultType());
                assertEquals(ADDON_MISSING_MESSAGE, missing.getMessage());
                assertEquals("ZP2015_POL_BASIC", missing.getPolicyTypeCode());

                ArrLevel rootLevel = levelRepository.findByNodeAndDeleteChangeIsNull(root);
                assertTrue(rulesExecutor.executeDescItemValidationRules(rootLevel, version).stream()
                                   .noneMatch(r -> ADDON_ITEM_TYPE.equals(r.getTypeCode())),
                           "addon validation rule is folder-only");
            } finally {
                // The fund exists only for this evaluation; nothing may reference ZP2015 at unload.
                status.setRollbackOnly();
            }
        });
    }

    /**
     * The addon's ITEM_TYPE_FILTER rule adds its item type to the rule set's list of item types,
     * which feeds the grid, search, the add-item dialog and the AI dictionary.
     */
    @Test
    @Order(5)
    void addonFilterRuleAddsItsItemTypeToTheRuleSet() {
        tx(() -> {
            RulRuleSet base = ruleSetRepository.findByCode(BASE_CODE);
            List<String> codes = ruleService.getItemTypeCodesByRuleSet(base);
            assertTrue(codes.contains("ZP2015_NAME"), "the rule set's own filter still applies");
            assertTrue(codes.contains(ADDON_ITEM_TYPE), "the addon filter rule adds its item type");
            assertFalse(codes.contains("SRD_TITLE"), "item types named by no filter stay out");
        });
    }

    /**
     * OUTPUT_DEFAULTS is layered: the addon states it for ZP2015 next to ZP2015 itself (no
     * OTHER_PACKAGE refusal) and wins with its own output filter.
     */
    @Test
    @Order(6)
    void addonOutputDefaultsReplaceTheBaseDefaultFilter() {
        assertEquals(BASE_DEFAULT_OUTPUT_FILTER, baseDefaultOutputFilter, "ZP2015 states its default filter");
        Integer ruleSetId = staticDataService.getData().getRuleSetByCode(BASE_CODE).getRuleSetId();
        tx(() -> {
            assertEquals(2, settingsService.getGlobalSettings(SettingsType.OUTPUT_DEFAULTS.toString(),
                    EntityType.RULE, ruleSetId).size(), "both packages keep their own setting");
            assertEquals(ADDON_OUTPUT_FILTER, outputFilterCode(settingsService.getOutputDefaults(ruleSetId)),
                    "the addon setting wins");
        });
    }

    /**
     * Output filters are imported per rule set: the addon's filters in ZP2015 and in CAM both
     * survive, the second rule set does not delete the filters of the first.
     */
    @Test
    @Order(6)
    void outputFiltersOfEveryRuleSetOfThePackageSurviveTheImport() {
        Integer zp2015 = staticDataService.getData().getRuleSetByCode(BASE_CODE).getRuleSetId();
        Integer cam = staticDataService.getData().getRuleSetByCode("CAM").getRuleSetId();
        tx(() -> {
            assertNotNull(outputFilterRepository.findByRuleSetIdAndCode(zp2015, ADDON_OUTPUT_FILTER));
            assertNotNull(outputFilterRepository.findByRuleSetIdAndCode(cam, "ADT_CAM_FILTER"));
            assertNotNull(outputFilterRepository.findByRuleSetIdAndCode(zp2015, BASE_DEFAULT_OUTPUT_FILTER),
                    "filters of the base package are untouched");
        });
    }

    /** The base package is protected while an addon depends on it. */
    @Test
    @Order(7)
    void basePackageCannotBeDeletedWhileTheAddonDependsOnIt() {
        BusinessException ex = assertThrows(BusinessException.class, () -> packageService.deletePackage(BASE_CODE));
        assertEquals(PackageCode.FOREIGN_DEPENDENCY, ex.getErrorCode());
        assertNotNull(packageRepository.findByCode(BASE_CODE), "base package must survive the refused delete");
    }

    /** Removing the addon takes its item type, specification, rules and files away and leaves ZP2015 as it was. */
    @Test
    @Order(8)
    void deletingTheAddonRestoresTheBaseRuleSet() {
        RulPackage addon = addonPackage();

        packageService.deletePackage(ADDON_CODE);
        staticDataService.refreshForCurrentThread();

        assertNull(packageRepository.findByCode(ADDON_CODE));
        assertTrue(arrangementRuleRepository.findByRulPackage(addon).isEmpty());
        assertTrue(itemTypeRepository.findByRulPackage(addon).isEmpty());
        if (addonAttributeRulesFile != null) {
            assertFalse(Files.exists(addonAttributeRulesFile), "addon rule file must be removed with the package");
        }

        StaticDataProvider sdp = staticDataService.getData();
        assertNull(sdp.getItemTypeByCode(ADDON_ITEM_TYPE));
        assertNull(sdp.getItemTypeByCode(FOREIGN_ITEM_TYPE).getItemSpecByCode(ADDON_SPEC));
        RuleSet base = sdp.getRuleSetByCode(BASE_CODE);
        assertEquals(baseAttributeRules, base.getRulesByType(RuleType.ATTRIBUTE_TYPES).size());
        assertEquals(baseValidationRules, base.getRulesByType(RuleType.CONFORMITY_INFO).size());
        assertNotNull(packageRepository.findByCode(BASE_CODE));
        tx(() -> {
            assertEquals(BASE_DEFAULT_OUTPUT_FILTER,
                    outputFilterCode(settingsService.getOutputDefaults(base.getRuleSetId())),
                    "the default filter of ZP2015 applies again");
            assertNull(outputFilterRepository.findByRuleSetIdAndCode(base.getRuleSetId(), ADDON_OUTPUT_FILTER));
        });
    }

    // ---------------------------------------------------------------------------------------------

    /** The addon spec sits right after its anchor, not at the end where its package would put it. */
    private void assertSpecFollowsAnchor(RulItemType otherIdEntity) {
        List<String> order = itemTypeSpecAssignRepository.findByItemTypeSorted(otherIdEntity).stream()
                .map(a -> a.getItemSpec().getCode())
                .toList();
        int anchor = order.indexOf(ANCHOR_SPEC);
        assertTrue(anchor >= 0, "anchor specification missing: " + order);
        assertEquals(ADDON_SPEC, order.get(anchor + 1), "addon specification must follow its anchor: " + order);
        assertNotEquals(ADDON_SPEC, order.get(order.size() - 1), "anchor must move the spec from the end");
    }

    private RulPackage addonPackage() {
        RulPackage addon = packageRepository.findByCode(ADDON_CODE);
        assertNotNull(addon, "addon package is not installed");
        return addon;
    }

    private static RulItemTypeExt typeOf(List<RulItemTypeExt> types, String code) {
        return types.stream()
                .filter(t -> code.equals(t.getCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("item type not evaluated: " + code));
    }

    private static RulItemSpecExt specOf(RulItemTypeExt type, String code) {
        return type.getRulItemSpecList().stream()
                .filter(s -> code.equals(s.getCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("specification not evaluated: " + code));
    }

    /** Adds a child through the rule set's own new-level scenario so the level carries its level type. */
    private ArrNode addChild(ArrFundVersion version, ArrNode parent, String scenario) {
        ArrNode parentNode = nodeRepository.findById(parent.getNodeId()).orElseThrow();
        List<ArrLevel> levels = fundLevelService.addNewLevel(version, parentNode, parentNode, AddLevelDirection.CHILD,
                scenario, null, null, null, null);
        // the tree cache must know the new node before it can become a parent or be evaluated
        levelTreeCacheService.invalidateFundVersion(version);
        return levels.stream().max(Comparator.comparing(ArrLevel::getLevelId)).orElseThrow().getNode();
    }

    private static String outputFilterCode(SettingOutputDefaults defaults) {
        return defaults != null ? defaults.outputFilterCode() : null;
    }

    private void tx(Runnable body) {
        new TransactionTemplate(txManager).executeWithoutResult(status -> body.run());
    }

    private void authorizeAsAdmin() {
        UserDetail userDetail = userService.createUserDetail((Integer) null);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("", "", null);
        auth.setDetails(userDetail);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /** Funds need an institution; the shared fixture file provides one. */
    private ParInstitution importInstitution() throws IOException {
        ApScope scope = apService.getApScope(1);
        assertNotNull(scope);
        File instFile = AbstractTest.getResourceFile("institution-import.xml");
        try (FileInputStream fis = new FileInputStream(instFile)) {
            deImportService.importData(fis, new DEImportParams(scope.getScopeId(), 1000, 10000, null, null));
        }
        List<ParInstitution> institutions = institutionRepository.findAll();
        assertFalse(institutions.isEmpty(), "no institution imported");
        return institutions.get(0);
    }
}
