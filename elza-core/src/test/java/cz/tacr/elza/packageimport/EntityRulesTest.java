package cz.tacr.elza.packageimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.ElzaCoreMain;
import cz.tacr.elza.controller.vo.ApAccessPointCreateVO;
import cz.tacr.elza.controller.vo.ApPartFormVO;
import cz.tacr.elza.core.data.RuleSet;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.RulEntityRule;
import cz.tacr.elza.drools.model.ItemType;
import cz.tacr.elza.drools.model.ModelAvailable;
import cz.tacr.elza.drools.model.RequiredType;
import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.exception.codes.PackageCode;
import cz.tacr.elza.other.HelperTestService;
import cz.tacr.elza.repository.PackageRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.service.AccessPointDataService;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.core.data.PackageTexts;
import cz.tacr.elza.groovy.GroovyItem;
import cz.tacr.elza.groovy.GroovyItems;
import cz.tacr.elza.groovy.GroovyPart;
import cz.tacr.elza.groovy.GroovyResult;
import cz.tacr.elza.service.GroovyScriptService;
import cz.tacr.elza.service.GroovyService;
import cz.tacr.elza.domain.RulApTypeDeclaration;
import cz.tacr.elza.domain.TranslationEntityType;
import cz.tacr.elza.repository.ApTypeDeclarationRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import java.util.Set;
import java.util.stream.Collectors;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.controller.ApController;
import cz.tacr.elza.controller.vo.ApTypeVO;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.exception.codes.RegistryCode;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import cz.tacr.elza.service.StartupService;
import cz.tacr.elza.service.PartService;
import cz.tacr.elza.service.AccessPointItemService;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeDeclaration;
import cz.tacr.elza.repository.ItemTypeDeclarationRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemSpecDeclaration;
import cz.tacr.elza.domain.RulItemTypeSpecAssign;
import cz.tacr.elza.repository.ItemSpecDeclarationRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeSpecAssignRepository;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.RulPartTypeDeclaration;
import cz.tacr.elza.repository.PartTypeDeclarationRepository;
import cz.tacr.elza.repository.PartTypeRepository;
import cz.tacr.elza.packageimport.xml.SettingPartsOrder;

/**
 * Entity rules belong to their rule set: CZ_BASE (rule set CAM) and the test package
 * {@code entity-rules-test} (rule set ENT_TEST) both have a rule for the name part in a file of the
 * same name; an entity is evaluated only by the rules of the rule set of its scope.
 *
 * <p>Packages are imported once for the class and the test package is removed afterwards; context
 * configuration matches {@code AbstractTest} so the cached Spring context is reused.
 */
@ContextConfiguration(classes = ElzaCoreMain.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EntityRulesTest {

    private static final String TEST_CODE = "ENTITY_RULES_TEST";
    private static final String TEST_DIR = "entity-rules-test";
    private static final String RULE_SET_DIR = "rul_rule_set/ENT_TEST/";

    @Autowired
    private HelperTestService helperTestService;
    @Autowired
    private StartupService startupService;
    @Autowired
    private StaticDataService staticDataService;
    @Autowired
    private PackageService packageService;
    @Autowired
    private PackageRepository packageRepository;
    @Autowired
    private ScopeRepository scopeRepository;
    @Autowired
    private RuleService ruleService;
    @Autowired
    private AccessPointService accessPointService;
    @Autowired
    private AccessPointDataService accessPointDataService;
    @Autowired
    private ApController apController;
    @Autowired
    private UserService userService;
    @Autowired
    private ApTypeRepository apTypeRepository;
    @Autowired
    private ApTypeDeclarationRepository apTypeDeclarationRepository;
    @Autowired
    private PackageTexts packageTexts;
    @Autowired
    private PartTypeRepository partTypeRepository;
    @Autowired
    private PartTypeDeclarationRepository partTypeDeclarationRepository;
    @Autowired
    private PartService partService;
    @Autowired
    private AccessPointItemService accessPointItemService;
    @Autowired
    private ItemTypeRepository itemTypeRepository;
    @Autowired
    private ItemTypeDeclarationRepository itemTypeDeclarationRepository;
    @Autowired
    private ItemSpecRepository itemSpecRepository;
    @Autowired
    private ItemSpecDeclarationRepository itemSpecDeclarationRepository;
    @Autowired
    private ItemTypeSpecAssignRepository itemTypeSpecAssignRepository;
    @Autowired
    private cz.tacr.elza.repository.ItemAptypeRepository itemAptypeRepository;
    @Autowired
    private GroovyService groovyService;
    @Autowired
    private GroovyScriptService groovyScriptService;
    @Autowired
    @Qualifier("transactionManager")
    private PlatformTransactionManager txManager;

    private final List<Integer> scopeIds = new ArrayList<>();

    @BeforeAll
    void loadPackagesOnce() {
        helperTestService.deleteTables(false);
        startupService.startNow();
        authorizeAsAdmin();
        helperTestService.loadPackage("CZ_BASE", "package-cz-base");
        helperTestService.loadPackage(TEST_CODE, TEST_DIR);
    }

    @AfterAll
    void unloadPackages() {
        try {
            // entities created by the tests
            helperTestService.deleteTables(false);
            tx(() -> {
                scopeRepository.deleteAllById(scopeIds);
                // scopes of other tests without a rule set got ENT_TEST at its import: restore them
                for (ApScope scope : scopeRepository.findAll()) {
                    if (scope.getRulRuleSet() != null && "ENT_TEST".equals(scope.getRulRuleSet().getCode())) {
                        scope.setRulRuleSet(null);
                        scopeRepository.save(scope);
                    }
                }
            });
            if (packageRepository.findByCode(TEST_CODE) != null) {
                packageService.deletePackage(TEST_CODE);
            }
        } finally {
            staticDataService.refreshForCurrentThread();
            startupService.stop();
        }
    }

    /**
     * Rules for all classes first, then from the root class down; rules without a part type before
     * the rules of the part.
     */
    @Test
    @Order(1)
    void eachRuleSetHasOnlyItsOwnRulesInOrder() {
        tx(() -> {
            StaticDataProvider sdp = staticDataService.getData();
            List<String> classes = List.of("PERSON_INDIVIDUAL", "PERSON");

            assertEquals(List.of("available_items/GLOBAL.drl",
                                 "available_items/PT_NAME.drl",
                                 "available_items/PERSON/PT_NAME.drl",
                                 "available_items/PERSON_INDIVIDUAL/PT_NAME.drl"),
                         filenames(sdp.getRuleSetByCode("CAM"), RulEntityRule.Kind.AVAILABLE_ITEMS, classes, "PT_NAME"));
            assertEquals(List.of("available_items/PT_NAME.drl"),
                         filenames(sdp.getRuleSetByCode("ENT_TEST"), RulEntityRule.Kind.AVAILABLE_ITEMS, classes,
                                   "PT_NAME"));
            assertEquals(List.of(),
                         filenames(sdp.getRuleSetByCode("ENT_TEST"), RulEntityRule.Kind.VALIDATION, classes, null));
            // validation: all part types
            assertEquals(List.of("validation/GLOBAL.drl", "validation/PERSON/GLOBAL.drl",
                                 "validation/PERSON_INDIVIDUAL/GLOBAL.drl"),
                         filenames(sdp.getRuleSetByCode("CAM"), RulEntityRule.Kind.VALIDATION, classes, null));
        });
    }

    /** The name part of a person is evaluated by the rule set of the scope only. */
    @Test
    @Order(2)
    void availableItemsFollowTheRuleSetOfTheScope() {
        Integer camScope = createScope("ENT_RULES_CAM", "CAM");
        Integer testScope = createScope("ENT_RULES_TEST", "ENT_TEST");

        Map<String, RequiredType> cam = availableNameItems(camScope);
        assertEquals(RequiredType.REQUIRED, cam.get("NM_MAIN"));
        assertTrue(cam.get("NM_MINOR") != RequiredType.REQUIRED, "CAM: " + cam.get("NM_MINOR"));

        Map<String, RequiredType> test = availableNameItems(testScope);
        assertEquals(RequiredType.REQUIRED, test.get("NM_MINOR"));
        assertTrue(test.get("NM_MAIN") != RequiredType.REQUIRED, "ENT_TEST: " + test.get("NM_MAIN"));
    }

    @Test
    @Order(3)
    void exportWritesTheEntityRules() throws IOException {
        Path zip = txGet(() -> {
            try {
                return packageService.exportPackage(TEST_CODE);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
        try (ZipFile zipFile = new ZipFile(zip.toFile())) {
            ZipEntry rules = zipFile.getEntry(RULE_SET_DIR + PackageService.ENTITY_RULE_XML);
            assertNotNull(rules);
            String xml = new String(zipFile.getInputStream(rules).readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(xml.contains("kind=\"AVAILABLE_ITEMS\"") && xml.contains("part-type=\"PT_NAME\""), xml);
            assertNotNull(zipFile.getEntry(RULE_SET_DIR + "rules/available_items/PT_NAME.drl"));

            // the package's own declarations of classes, PERSON with its English name
            String apTypes = new String(zipFile.getInputStream(zipFile.getEntry(APTypeUpdater.AP_TYPE_XML))
                    .readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(apTypes.contains("<name>Person</name>") && apTypes.contains("ENT_PERSON_LOCAL")
                    && !apTypes.contains("osoba"), apTypes);

            ZipEntry members = zipFile.getEntry(RULE_SET_DIR + PackageService.RULE_SET_AP_TYPE_XML);
            assertNotNull(members);
            String membersXml = new String(zipFile.getInputStream(members).readAllBytes(), StandardCharsets.UTF_8);
            // the package declares PERSON assignable, the rule set not: written; PERSON_INDIVIDUAL keeps
            // the default of its class: not written
            assertTrue(membersXml.contains("code=\"PERSON\" assignable=\"false\"")
                    && membersXml.contains("<ap-type code=\"PERSON_INDIVIDUAL\"/>"), membersXml);
            assertTrue(membersXml.indexOf("PERSON_INDIVIDUAL") < membersXml.indexOf("ENT_PERSON_LOCAL"), membersXml);

            // own declarations of part types (PT_NAME with its English name) and the list of the rule set
            String partTypes = new String(zipFile.getInputStream(zipFile.getEntry(PackageService.PART_TYPE_XML))
                    .readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(partTypes.contains("<name>Name</name>") && partTypes.contains("PT_ENT_NOTE")
                    && !partTypes.contains("PT_BODY"), partTypes);
            // own item type and the declaration of NOTE of CZ_BASE, with the package's texts
            String itemTypes = new String(zipFile.getInputStream(zipFile.getEntry(PackageService.ITEM_TYPE_XML))
                    .readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(itemTypes.contains("<name>Note</name>") && itemTypes.contains("ENT_LOCAL_ID")
                    && itemTypes.contains("<string-length-limit>50</string-length-limit>")
                    && !itemTypes.contains("NOTE_INTERNAL") && !itemTypes.contains("Poznámka"), itemTypes);
            assertTrue(itemTypes.indexOf("ENT_LOCAL_ID") < itemTypes.indexOf("\"NOTE\""), itemTypes);

            // own specification and the declaration of NT_PSEUDONYM of CZ_BASE, with the package's texts
            String itemSpecs = new String(zipFile.getInputStream(zipFile.getEntry(PackageService.ITEM_SPEC_XML))
                    .readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(itemSpecs.contains("<name>Pseudonym</name>") && itemSpecs.contains("ENT_NT_LOCAL")
                    && itemSpecs.contains("<item-type-assign code=\"NM_TYPE\"/>")
                    && !itemSpecs.contains("NT_OFFICIAL"), itemSpecs);

            ZipEntry partList = zipFile.getEntry(RULE_SET_DIR + PackageService.RULE_SET_PART_TYPE_XML);
            assertNotNull(partList);
            String partListXml = new String(zipFile.getInputStream(partList).readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(partListXml.indexOf("PT_ENT_NOTE") >= 0
                    && partListXml.indexOf("PT_ENT_NOTE") < partListXml.indexOf("PT_NAME"), partListXml);
        } finally {
            Files.deleteIfExists(zip);
        }
    }

    @Test
    @Order(18)
    void invalidEntityRulesAreRefused() throws Exception {
        assertRefused(Map.of(RULE_SET_DIR + "rul_extension_rule.xml", "<extension-rules/>"));
        assertRefused(Map.of(RULE_SET_DIR + "rul_arrangement_extension.xml", "<arrangement-extensions/>"));
        assertRefused(Map.of("rul_rule_set.xml", "<rule-sets><rule-set code=\"ENT_TEST\"><name>x</name>"
                + "<rule-type>ARRANGEMENT</rule-type></rule-set></rule-sets>"));
        // references are foreign keys: an unknown class or part type is refused
        assertRefused(Map.of(RULE_SET_DIR + PackageService.ENTITY_RULE_XML, "<entity-rules><entity-rule"
                + " filename=\"available_items/PT_NAME.drl\" kind=\"AVAILABLE_ITEMS\" ap-type=\"NO_SUCH_CLASS\""
                + " priority=\"100\"/></entity-rules>"));
        assertRefused(Map.of(RULE_SET_DIR + PackageService.ENTITY_RULE_XML, "<entity-rules><entity-rule"
                + " filename=\"available_items/PT_NAME.drl\" kind=\"AVAILABLE_ITEMS\" part-type=\"PT_NO_SUCH\""
                + " priority=\"100\"/></entity-rules>"));
        // computed items apply to the whole entity; they are entity rules, not arrangement rules
        assertRefused(Map.of(RULE_SET_DIR + PackageService.ENTITY_RULE_XML, "<entity-rules><entity-rule"
                + " filename=\"auto_items/GLOBAL.groovy\" kind=\"AUTO_ITEMS\" part-type=\"PT_NAME\""
                + " priority=\"100\"/></entity-rules>"));
        assertRefused(Map.of(RULE_SET_DIR + PackageService.ARRANGEMENT_RULE_XML, "<arrangement-rules>"
                + "<arrangement-rule filename=\"auto_items/GLOBAL.groovy\"><rule-type>AUTO_ITEMS</rule-type>"
                + "<priority>100</priority></arrangement-rule></arrangement-rules>"));
        // an INDEX rule needs a Groovy file, the other kinds a DRL file
        assertRefused(Map.of(RULE_SET_DIR + PackageService.ENTITY_RULE_XML, "<entity-rules><entity-rule"
                + " filename=\"available_items/PT_NAME.drl\" kind=\"INDEX\" priority=\"100\"/></entity-rules>"));
        // member classes: an unknown class, a class listed twice
        assertRefused(Map.of(RULE_SET_DIR + PackageService.RULE_SET_AP_TYPE_XML,
                             "<ap-types><ap-type code=\"NO_SUCH_CLASS\"/></ap-types>"));
        assertRefused(Map.of(RULE_SET_DIR + PackageService.RULE_SET_AP_TYPE_XML,
                             "<ap-types><ap-type code=\"PERSON\"/><ap-type code=\"PERSON\"/></ap-types>"));
        // part types of the rule set: an unknown part type, a part type listed twice
        assertRefused(Map.of(RULE_SET_DIR + PackageService.RULE_SET_PART_TYPE_XML,
                             "<part-types><part-type code=\"PT_NO_SUCH\"/></part-types>"));
        assertRefused(Map.of(RULE_SET_DIR + PackageService.RULE_SET_PART_TYPE_XML,
                             "<part-types><part-type code=\"PT_NAME\"/><part-type code=\"PT_NAME\"/></part-types>"));
        // a declaration of NOTE of CZ_BASE must keep how its data are stored
        String note = "<item-types><item-type code=\"NOTE\" data-type=\"%s\"><name>Note</name><shortcut>Note</shortcut>"
                + "<use-specification>%s</use-specification>%s</item-type></item-types>";
        assertRefused(PackageCode.ITEM_TYPE_CONFLICT,
                      Map.of(PackageService.ITEM_TYPE_XML, String.format(note, "STRING", "false", "")));
        assertRefused(PackageCode.ITEM_TYPE_CONFLICT,
                      Map.of(PackageService.ITEM_TYPE_XML, String.format(note, "TEXT", "true", "")));
        assertRefused(PackageCode.ITEM_TYPE_CONFLICT, Map.of(PackageService.ITEM_TYPE_XML, String.format(note, "TEXT",
                "false", "<item-aptypes><item-aptype register-type=\"PERSON\"/></item-aptypes>")));
        // RECORD_REF classes of a specification of CZ_BASE stay with CZ_BASE: a declaration may repeat
        // them (RT_RELATED in the test package), not state others
        assertRefused(PackageCode.ITEM_SPEC_CONFLICT, Map.of(PackageService.ITEM_SPEC_XML, "<item-specs>"
                + "<item-spec code=\"NT_PSEUDONYM\"><name>Pseudonym</name><description>Pseudonym</description>"
                + "<shortcut>Pseudonym</shortcut><item-type-assign code=\"NM_TYPE\"/>"
                + "<item-aptypes><item-aptype register-type=\"PERSON\"/></item-aptypes></item-spec></item-specs>"));
        assertRefused(PackageCode.ITEM_SPEC_CONFLICT, Map.of(PackageService.ITEM_SPEC_XML, "<item-specs>"
                + "<item-spec code=\"RT_RELATED\"><name>Related</name><description>Related</description>"
                + "<shortcut>Related</shortcut><item-type-assign code=\"REL_ENTITY\"/>"
                + "<item-aptypes><item-aptype register-type=\"PERSON\"/></item-aptypes></item-spec></item-specs>"));
        // part type declarations: a code declared twice, an unknown child part
        assertRefused(Map.of(PackageService.PART_TYPE_XML, "<part-types>"
                + "<part-type code=\"PT_ENT_NOTE\"><name>Note</name></part-type>"
                + "<part-type code=\"PT_ENT_NOTE\"><name>Note</name></part-type></part-types>"));
        assertRefused(Map.of(PackageService.PART_TYPE_XML, "<part-types>"
                + "<part-type code=\"PT_ENT_NOTE\"><name>Note</name><child_part>PT_NO_SUCH</child_part>"
                + "</part-type></part-types>"));
    }

    /** A specification used by entities cannot be removed by a new version of its package. */
    @Test
    @Order(15)
    void aUsedSpecificationCannotBeRemoved() throws Exception {
        tx(() -> {
            ApChange change = accessPointDataService.createChange(ApChange.Type.AP_CREATE);
            ApState state = accessPointService.createAccessPoint(scope(scopeIds.get(1)), type("PERSON_INDIVIDUAL"),
                                                                 ApState.StateApproval.NEW, change, null);
            ApPart part = partService.createPart(partTypeRepository.findByCode("PT_NAME"), state.getAccessPoint(),
                                                 change, null);
            ArrDataNull data = new ArrDataNull();
            data.setDataType(DataType.ENUM.getEntity());
            accessPointItemService.createItemWithSave(part, data, itemTypeRepository.findOneByCode("NM_TYPE"),
                                                      itemSpecRepository.findOneByCode("ENT_NT_LOCAL"), change,
                                                      new ArrayList<>(), null, null);
        });
        assertRefused(PackageCode.ITEM_SPEC_IN_USE, Map.of(PackageService.ITEM_SPEC_XML, "<item-specs>"
                + "<item-spec code=\"NT_PSEUDONYM\"><name>Pseudonym</name><description>Pseudonym</description>"
                + "<shortcut>Pseudonym</shortcut><item-type-assign code=\"NM_TYPE\"/></item-spec></item-specs>"));
        // the specification stays, but its used assignment to NM_TYPE would be removed
        assertRefused(PackageCode.ITEM_SPEC_IN_USE, Map.of(PackageService.ITEM_SPEC_XML, "<item-specs>"
                + "<item-spec code=\"ENT_NT_LOCAL\"><name>Local form</name><description>Local</description>"
                + "<shortcut>Local form</shortcut></item-spec></item-specs>"));
    }

    /** An item type used by entities cannot be removed by a new version of its package. */
    @Test
    @Order(16)
    void aUsedItemTypeCannotBeRemoved() throws Exception {
        tx(() -> {
            ApChange change = accessPointDataService.createChange(ApChange.Type.AP_CREATE);
            ApState state = accessPointService.createAccessPoint(scope(scopeIds.get(1)), type("PERSON_INDIVIDUAL"),
                                                                 ApState.StateApproval.NEW, change, null);
            ApPart part = partService.createPart(partTypeRepository.findByCode("PT_NAME"), state.getAccessPoint(),
                                                 change, null);
            ArrDataString data = new ArrDataString("A-1");
            data.setDataType(DataType.STRING.getEntity());
            accessPointItemService.createItemWithSave(part, data, itemTypeRepository.findOneByCode("ENT_LOCAL_ID"),
                                                      null, change, new ArrayList<>(), null, null);
        });
        assertRefused(PackageCode.ITEM_TYPE_IN_USE, Map.of(PackageService.ITEM_TYPE_XML,
                "<item-types><item-type code=\"NOTE\" data-type=\"TEXT\"><name>Note</name><shortcut>Note</shortcut>"
                + "<use-specification>false</use-specification></item-type></item-types>"));
    }

    /** A part type used by parts of entities cannot be removed by a new version of its package. */
    @Test
    @Order(17)
    void aUsedPartTypeCannotBeRemoved() throws Exception {
        tx(() -> {
            ApChange change = accessPointDataService.createChange(ApChange.Type.AP_CREATE);
            ApState state = accessPointService.createAccessPoint(scope(scopeIds.get(1)), type("PERSON_INDIVIDUAL"),
                                                                 ApState.StateApproval.NEW, change, null);
            partService.createPart(partTypeRepository.findByCode("PT_ENT_NOTE"), state.getAccessPoint(), change, null);
        });
        assertRefused(PackageCode.PART_TYPE_IN_USE, Map.of(
                PackageService.PART_TYPE_XML, "<part-types><part-type code=\"PT_NAME\"><name>Name</name>"
                        + "<repeatable>true</repeatable></part-type></part-types>",
                RULE_SET_DIR + PackageService.RULE_SET_PART_TYPE_XML,
                "<part-types><part-type code=\"PT_NAME\"/></part-types>"));
    }

    /**
     * ENT_TEST declares PERSON (not assignable) and PERSON_INDIVIDUAL; CAM declares no classes and
     * offers all of them, assignable when not read-only.
     */
    @Test
    @Order(5)
    void ruleSetsOfferTheirClasses() {
        tx(() -> {
            StaticDataProvider sdp = staticDataService.getData();
            RuleSet test = sdp.getRuleSetByCode("ENT_TEST");
            RuleSet cam = sdp.getRuleSetByCode("CAM");
            assertTrue(test.hasApTypeMembers());
            assertTrue(test.offersApType(sdp.getApTypeByCode("PERSON")));
            assertTrue(!test.isApTypeAssignable(sdp.getApTypeByCode("PERSON")));
            assertTrue(test.isApTypeAssignable(sdp.getApTypeByCode("PERSON_INDIVIDUAL")));
            assertTrue(!test.offersApType(sdp.getApTypeByCode("GEO_UNIT")));

            assertTrue(!cam.hasApTypeMembers());
            assertTrue(cam.offersApType(sdp.getApTypeByCode("GEO_UNIT")));
            assertTrue(!cam.isApTypeAssignable(sdp.getApTypeByCode("PERSON")));
            assertTrue(cam.isApTypeAssignable(sdp.getApTypeByCode("PERSON_INDIVIDUAL")));
        });

        // the class tree of a scope: members with their parents, assignable ones selectable
        Integer testScope = scopeIds.get(1);
        List<ApTypeVO> tree = txGet(() -> apController.getApTypes(testScope));
        assertEquals(List.of("PERSON"), tree.stream().map(ApTypeVO::getCode).toList());
        assertTrue(!tree.get(0).getAddRecord());
        // in the order of the rule set, not by name ("Local person" before "osoba")
        assertEquals(List.of("PERSON_INDIVIDUAL", "ENT_PERSON_LOCAL"),
                     tree.get(0).getChildren().stream().map(ApTypeVO::getCode).toList());
        assertTrue(tree.get(0).getChildren().get(0).getAddRecord());
        assertTrue(txGet(() -> apController.getApTypes(null)).size() > 1);
    }

    @Test
    @Order(6)
    void classesOutsideTheRuleSetOfTheScopeAreRefused() {
        Integer camScope = scopeIds.get(0);
        Integer testScope = scopeIds.get(1);
        assertRefusedClass("GEO_UNIT", testScope, true);
        assertRefusedClass("GEO_UNIT", testScope, false);
        // a member that is not assignable cannot be chosen, but entities of it may be in the scope
        assertRefusedClass("PERSON", testScope, true);
        tx(() -> accessPointService.checkApTypeInScope(type("PERSON"), scope(testScope), false));
        tx(() -> accessPointService.checkApTypeInScope(type("PERSON_INDIVIDUAL"), scope(testScope), true));
        // CAM keeps the read-only roots
        assertRefusedClass("PERSON", camScope, true);

        // a scope with an entity of a class the new rule set does not offer keeps its rule set
        tx(() -> {
            ApChange change = accessPointDataService.createChange(ApChange.Type.AP_CREATE);
            accessPointService.createAccessPoint(scope(camScope), type("GEO_UNIT"), ApState.StateApproval.NEW,
                                                 change, null);
        });
        AbstractException e = assertThrows(AbstractException.class, () -> tx(() -> {
            // as the scope form sends it: a new object with the id and the new rule set
            ApScope moved = new ApScope();
            moved.setScopeId(camScope);
            moved.setCode("ENT_RULES_CAM");
            moved.setName("ENT_RULES_CAM");
            moved.setRulRuleSet(staticDataService.getData().getRuleSetByCode("ENT_TEST").getEntity());
            accessPointService.checkScopeRuleSet(moved);
        }));
        assertEquals(RegistryCode.AP_TYPE_NOT_IN_RULE_SET, e.getErrorCode());
    }

    /**
     * PERSON is declared by CZ_BASE (Czech, read-only) and by the test package (English, assignable):
     * one class, two declarations; the name follows the language of the reader, the stored name is
     * the one in the language of the installation; CAM keeps PERSON read-only.
     */
    @Test
    @Order(7)
    void aClassDeclaredByTwoPackagesExistsOnce() {
        tx(() -> {
            ApType person = apTypeRepository.findAll().stream().filter(t -> t.getCode().equals("PERSON"))
                    .reduce((a, b) -> {
                        throw new AssertionError("PERSON twice");
                    }).orElseThrow();
            List<RulApTypeDeclaration> declarations = apTypeDeclarationRepository.findByApTypes(List.of(person));
            assertEquals(Set.of("CZ_BASE", TEST_CODE),
                         declarations.stream().map(d -> d.getRulPackage().getCode()).collect(Collectors.toSet()));
            assertEquals("osoba / bytost", person.getName());

            StaticDataProvider sdp = staticDataService.getData();
            assertEquals("Person", packageTexts.text(TranslationEntityType.AP_TYPE, "PERSON", TranslationEntityType.NAME, "source",
                                                     sdp.getSysLanguageByTag("en")));
            assertEquals("osoba / bytost", packageTexts.text(TranslationEntityType.AP_TYPE, "PERSON", TranslationEntityType.NAME, "source",
                                                             sdp.getSysLanguageByTag("cs")));
            assertTrue(!sdp.getRuleSetByCode("CAM").isApTypeAssignable(sdp.getApTypeByCode("PERSON")));
            assertEquals("PERSON", sdp.getApTypeByCode("ENT_PERSON_LOCAL").getParentApType().getCode());
        });
    }

    /**
     * The script building the name of a part is the most specific INDEX rule of the rule set of the
     * scope: CAM has scripts per part and per class, the test rule set one script for names.
     */
    @Test
    @Order(4)
    void indexScriptsFollowTheRuleSetOfTheScope() {
        Integer camScope = scopeIds.get(0);
        Integer testScope = scopeIds.get(1);
        tx(() -> {
            String camName = groovyService.getIndexScriptPath(scope(camScope), groovyPart("PERSON_INDIVIDUAL", "PT_NAME"));
            assertTrue(camName.replace('\\', '/').endsWith("/CAM/drools/index/PERSON/PT_NAME.groovy")
                    || camName.replace('\\', '/').endsWith("index/PERSON/PT_NAME.groovy"), camName);
            String camBody = groovyService.getIndexScriptPath(scope(camScope), groovyPart("PERSON_INDIVIDUAL", "PT_BODY"));
            assertTrue(camBody.replace('\\', '/').endsWith("index/PT_BODY.groovy"), camBody);

            String testName = groovyService.getIndexScriptPath(scope(testScope), groovyPart("PERSON_INDIVIDUAL", "PT_NAME"));
            assertTrue(testName.replace('\\', '/').endsWith("index/PT_NAME.groovy")
                    && testName.contains(TEST_CODE), testName);

            // no script: a clear error naming the part and the class
            AbstractException e = assertThrows(AbstractException.class, () -> groovyService
                    .getIndexScriptPath(scope(testScope), groovyPart("PERSON_INDIVIDUAL", "PT_BODY")));
            assertTrue(e.getMessage().contains("No script"), e.getMessage());
            // CZ_BASE has no name script of DYNASTY itself, only of its subclasses
            assertThrows(AbstractException.class, () -> groovyService
                    .getIndexScriptPath(scope(camScope), groovyPart("DYNASTY", "PT_NAME")));

            // the script of the test rule set builds the name from the main name
            StaticDataProvider sdp = staticDataService.getData();
            GroovyItems items = new GroovyItems();
            items.addItem(new GroovyItem(sdp.getItemTypeByCode("NM_MAIN"), null, "Novák"));
            GroovyPart part = new GroovyPart(sdp, sdp.getApTypeByCode("PERSON_INDIVIDUAL"),
                                             sdp.getPartTypeByCode("PT_NAME"), true, items, List.of());
            GroovyResult result = groovyScriptService.process(part, testName);
            assertEquals("ENT Novák", result.getIndexes().get("DISPLAY_NAME"));
        });
    }

    /**
     * PT_NAME is declared by CZ_BASE (Czech) and by the test package (English): one part type, two
     * declarations, the name follows the language of the reader.
     */
    @Test
    @Order(8)
    void aPartTypeDeclaredByTwoPackagesExistsOnce() {
        tx(() -> {
            RulPartType name = partTypeRepository.findByCode("PT_NAME");
            List<RulPartTypeDeclaration> declarations = partTypeDeclarationRepository.findByPartTypes(List.of(name));
            assertEquals(Set.of("CZ_BASE", TEST_CODE),
                         declarations.stream().map(d -> d.getRulPackage().getCode()).collect(Collectors.toSet()));
            assertEquals("Označení", name.getName());
            assertTrue(name.getRepeatable());

            StaticDataProvider sdp = staticDataService.getData();
            assertEquals("Name", packageTexts.text(TranslationEntityType.PART_TYPE, "PT_NAME", TranslationEntityType.NAME,
                                                   "source", sdp.getSysLanguageByTag("en")));
            assertEquals("Označení", packageTexts.text(TranslationEntityType.PART_TYPE, "PT_NAME",
                                                       TranslationEntityType.NAME, "source",
                                                       sdp.getSysLanguageByTag("cs")));
            assertEquals(TEST_CODE, partTypeRepository.findByCode("PT_ENT_NOTE").getRulPackage().getCode());
        });
    }

    /**
     * A rule set lists its part types (rul_part_type.xml of the rule set); the parts of an entity are
     * shown in that order.
     */
    @Test
    @Order(9)
    void ruleSetsListTheirPartTypes() {
        tx(() -> {
            StaticDataProvider sdp = staticDataService.getData();
            assertEquals(List.of("PT_ENT_NOTE", "PT_NAME"), partCodes(sdp.getRuleSetByCode("ENT_TEST").getPartTypeOrder()));
            assertEquals(List.of("PT_NAME", "PT_CRE", "PT_EXT", "PT_BODY", "PT_EVENT", "PT_REL", "PT_IDENT"),
                         partCodes(sdp.getRuleSetByCode("CAM").getPartTypeOrder()));
        });
        Integer testRuleSetId = staticDataService.getData().getRuleSetByCode("ENT_TEST").getRuleSetId();
        List<SettingPartsOrder.Part> partsOrder = txGet(() -> apController.getApTypeViewSettings()).getRules()
                .get(testRuleSetId).getPartsOrder();
        assertEquals(List.of("PT_ENT_NOTE", "PT_NAME"), partsOrder.stream().map(SettingPartsOrder.Part::getCode).toList());
    }

    /**
     * NOTE is declared by CZ_BASE (Czech) and by the test package (English): one item type owned by
     * CZ_BASE, which keeps its place; the texts follow the language of the reader.
     */
    @Test
    @Order(10)
    void anItemTypeDeclaredByTwoPackagesExistsOnce() {
        tx(() -> {
            RulItemType note = itemTypeRepository.findOneByCode("NOTE");
            List<RulItemTypeDeclaration> declarations = itemTypeDeclarationRepository.findByItemTypes(List.of(note));
            assertEquals(Set.of("CZ_BASE", TEST_CODE),
                         declarations.stream().map(d -> d.getRulPackage().getCode()).collect(Collectors.toSet()));
            assertEquals("CZ_BASE", note.getRulPackage().getCode());
            assertEquals("Poznámka", note.getName());
            RulItemType noteInternal = itemTypeRepository.findOneByCode("NOTE_INTERNAL");
            assertEquals(note.getViewOrder() + 1, noteInternal.getViewOrder());

            StaticDataProvider sdp = staticDataService.getData();
            assertEquals("Note", packageTexts.text(TranslationEntityType.ITEM_TYPE, "NOTE", TranslationEntityType.NAME,
                                                   "source", sdp.getSysLanguageByTag("en")));
            assertEquals("General note", packageTexts.text(TranslationEntityType.ITEM_TYPE, "NOTE",
                                                           TranslationEntityType.DESCRIPTION, "source",
                                                           sdp.getSysLanguageByTag("en")));
            assertEquals("Poznámka", packageTexts.text(TranslationEntityType.ITEM_TYPE, "NOTE",
                                                       TranslationEntityType.NAME, "source",
                                                       sdp.getSysLanguageByTag("cs")));
            assertEquals(TEST_CODE, itemTypeRepository.findOneByCode("ENT_LOCAL_ID").getRulPackage().getCode());
        });
    }

    /**
     * The items computed for an entity come from the most specific AUTO_ITEMS entity rule of the rule set
     * of its scope.
     */
    @Test
    @Order(11)
    void autoItemsFollowTheRuleSetAndClass() {
        Integer testScope = scopeIds.get(1);
        assertEquals(List.of("ENT auto"), autoItemValues(testScope, "PERSON_INDIVIDUAL"));
        assertEquals(List.of("ENT local auto"), autoItemValues(testScope, "ENT_PERSON_LOCAL"));
    }

    private List<String> autoItemValues(Integer scopeId, String apType) {
        return txGet(() -> {
            ApChange change = accessPointDataService.createChange(ApChange.Type.AP_CREATE);
            ApState state = accessPointService.createAccessPoint(scope(scopeId), type(apType),
                                                                 ApState.StateApproval.NEW, change, null);
            return groovyService.getAutoItems(state).stream().map(GroovyItem::getValue).toList();
        });
    }

    /**
     * NT_PSEUDONYM is declared by CZ_BASE (Czech) and by the test package (English): one specification
     * owned by CZ_BASE; the specifications of NM_TYPE are the union of both packages, CZ_BASE's first in
     * their order, the own specification of the test package last.
     */
    @Test
    @Order(12)
    void aSpecificationDeclaredByTwoPackagesExistsOnce() {
        tx(() -> {
            RulItemSpec pseudonym = itemSpecRepository.findOneByCode("NT_PSEUDONYM");
            List<RulItemSpecDeclaration> declarations = itemSpecDeclarationRepository.findByItemSpecs(List.of(pseudonym));
            assertEquals(Set.of("CZ_BASE", TEST_CODE),
                         declarations.stream().map(d -> d.getRulPackage().getCode()).collect(Collectors.toSet()));
            assertEquals("CZ_BASE", pseudonym.getPackage().getCode());
            // the shared relation specification keeps CZ_BASE's classes of related entities
            RulItemSpec related = itemSpecRepository.findOneByCode("RT_RELATED");
            assertEquals("CZ_BASE", related.getPackage().getCode());
            assertEquals(6, itemAptypeRepository.findByItemSpec(related).size());
            assertEquals("pseudonym", pseudonym.getName());
            assertEquals(TEST_CODE, itemSpecRepository.findOneByCode("ENT_NT_LOCAL").getPackage().getCode());

            List<String> specs = nameTypeSpecs();
            assertEquals(33, specs.size(), specs.toString());
            assertEquals(1, specs.stream().filter("NT_PSEUDONYM"::equals).count());
            assertEquals("NT_OFFICIAL", specs.get(0));
            assertEquals("ENT_NT_LOCAL", specs.get(specs.size() - 1));

            StaticDataProvider sdp = staticDataService.getData();
            assertEquals("Pseudonym", packageTexts.text(TranslationEntityType.ITEM_SPEC, "NT_PSEUDONYM",
                                                        TranslationEntityType.NAME, "source",
                                                        sdp.getSysLanguageByTag("en")));
            assertEquals("pseudonym", packageTexts.text(TranslationEntityType.ITEM_SPEC, "NT_PSEUDONYM",
                                                        TranslationEntityType.NAME, "source",
                                                        sdp.getSysLanguageByTag("cs")));
        });
    }

    private List<String> nameTypeSpecs() {
        return itemTypeSpecAssignRepository.findByItemTypeSorted(itemTypeRepository.findOneByCode("NM_TYPE")).stream()
                .map(RulItemTypeSpecAssign::getItemSpec)
                .map(RulItemSpec::getCode)
                .toList();
    }

    private List<String> partCodes(List<Integer> partTypeIds) {
        StaticDataProvider sdp = staticDataService.getData();
        return partTypeIds.stream().map(id -> sdp.getPartTypeById(id).getCode()).toList();
    }

    private GroovyPart groovyPart(String apType, String partType) {
        StaticDataProvider sdp = staticDataService.getData();
        return new GroovyPart(sdp, sdp.getApTypeByCode(apType), sdp.getPartTypeByCode(partType), true,
                              new GroovyItems(), List.of());
    }

    @Test
    @Order(19)
    void aClassDeclaredWithAnotherParentIsRefused() throws Exception {
        assertRefused(PackageCode.AP_TYPE_CONFLICT, Map.of(APTypeUpdater.AP_TYPE_XML,
                "<ap-types><ap-type code=\"PERSON\" parent-ap-type=\"DYNASTY\"><name>Person</name>"
                + "<hierarchical>false</hierarchical><read-only>false</read-only></ap-type></ap-types>"));
    }

    /** Deleting the package removes its own class and keeps the shared one, declared by CZ_BASE only. */
    @Test
    @Order(20)
    void deletingThePackageKeepsTheSharedClass() {
        helperTestService.deleteTables(false);
        tx(() -> {
            scopeRepository.deleteAllById(scopeIds);
            for (ApScope scope : scopeRepository.findAll()) {
                if (scope.getRulRuleSet() != null && "ENT_TEST".equals(scope.getRulRuleSet().getCode())) {
                    scope.setRulRuleSet(null);
                    scopeRepository.save(scope);
                }
            }
        });
        scopeIds.clear();
        packageService.deletePackage(TEST_CODE);
        staticDataService.refreshForCurrentThread();
        tx(() -> {
            ApType person = apTypeRepository.findAll().stream().filter(t -> t.getCode().equals("PERSON"))
                    .findFirst().orElseThrow();
            List<RulApTypeDeclaration> declarations = apTypeDeclarationRepository.findByApTypes(List.of(person));
            assertEquals(List.of("CZ_BASE"), declarations.stream().map(d -> d.getRulPackage().getCode()).toList());
            assertEquals("CZ_BASE", person.getRulPackage().getCode());
            assertTrue(person.isReadOnly());
            assertTrue(apTypeRepository.findAll().stream().noneMatch(t -> t.getCode().equals("ENT_PERSON_LOCAL")));

            // the shared part type stays with the declaration of CZ_BASE, the own one is removed
            RulPartType name = partTypeRepository.findByCode("PT_NAME");
            assertEquals(List.of("CZ_BASE"), partTypeDeclarationRepository.findByPartTypes(List.of(name)).stream()
                    .map(d -> d.getRulPackage().getCode()).toList());
            assertEquals("CZ_BASE", name.getRulPackage().getCode());
            assertEquals(null, partTypeRepository.findByCode("PT_ENT_NOTE"));
            assertEquals(7, staticDataService.getData().getRuleSetByCode("CAM").getPartTypeOrder().size());

            // the shared item type stays with CZ_BASE, the own one is removed
            RulItemType note = itemTypeRepository.findOneByCode("NOTE");
            assertEquals(List.of("CZ_BASE"), itemTypeDeclarationRepository.findByItemTypes(List.of(note)).stream()
                    .map(d -> d.getRulPackage().getCode()).toList());
            assertEquals("Poznámka", note.getName());
            assertEquals(null, itemTypeRepository.findOneByCode("ENT_LOCAL_ID"));

            // the shared specification stays with CZ_BASE, the own one and its assignment are removed
            RulItemSpec pseudonym = itemSpecRepository.findOneByCode("NT_PSEUDONYM");
            assertEquals(List.of("CZ_BASE"), itemSpecDeclarationRepository.findByItemSpecs(List.of(pseudonym)).stream()
                    .map(d -> d.getRulPackage().getCode()).toList());
            assertEquals("pseudonym", pseudonym.getName());
            assertEquals(null, itemSpecRepository.findOneByCode("ENT_NT_LOCAL"));
            List<String> specs = nameTypeSpecs();
            assertEquals(32, specs.size(), specs.toString());
            assertTrue(specs.contains("NT_PSEUDONYM"));
        });
    }

    private void assertRefusedClass(String apType, Integer scopeId, boolean assign) {
        AbstractException e = assertThrows(AbstractException.class,
                () -> tx(() -> accessPointService.checkApTypeInScope(type(apType), scope(scopeId), assign)));
        assertEquals(RegistryCode.AP_TYPE_NOT_IN_RULE_SET, e.getErrorCode(), apType);
    }

    private ApType type(String code) {
        return staticDataService.getData().getApTypeByCode(code);
    }

    private ApScope scope(Integer scopeId) {
        return scopeRepository.findById(scopeId).orElseThrow();
    }

    private void authorizeAsAdmin() {
        UserDetail userDetail = userService.createUserDetail((Integer) null);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("", "", null);
        auth.setDetails(userDetail);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Map<String, RequiredType> availableNameItems(Integer scopeId) {
        return txGet(() -> {
            ApAccessPointCreateVO form = new ApAccessPointCreateVO();
            form.setTypeId(staticDataService.getData().getApTypeByCode("PERSON_INDIVIDUAL").getApTypeId());
            form.setScopeId(scopeId);
            ApPartFormVO partForm = new ApPartFormVO();
            partForm.setPartTypeCode("PT_NAME");
            partForm.setItems(List.of());
            form.setPartForm(partForm);
            ModelAvailable result = ruleService.executeAvailable(form);
            Map<String, RequiredType> types = new HashMap<>();
            for (ItemType itemType : result.getItemTypes()) {
                types.put(itemType.getCode(), itemType.getRequiredType());
            }
            return types;
        });
    }

    private Integer createScope(String code, String ruleSetCode) {
        Integer id = txGet(() -> {
            ApScope scope = new ApScope();
            scope.setCode(code);
            scope.setName(code);
            scope.setRulRuleSet(staticDataService.getData().getRuleSetByCode(ruleSetCode).getEntity());
            return scopeRepository.save(scope).getScopeId();
        });
        scopeIds.add(id);
        return id;
    }

    private List<String> filenames(RuleSet ruleSet, RulEntityRule.Kind kind, List<String> classes, String partType) {
        StaticDataProvider sdp = staticDataService.getData();
        List<Integer> classIds = classes.stream().map(c -> sdp.getApTypeByCode(c).getApTypeId()).toList();
        Integer partTypeId = partType != null ? sdp.getPartTypeByCode(partType).getPartTypeId() : null;
        return ruleSet.getEntityRules(kind, classIds, partTypeId).stream()
                .map(r -> r.getComponent().getFilename())
                .toList();
    }

    private void assertRefused(Map<String, String> replacedFiles) throws Exception {
        assertRefused(PackageCode.INVALID_ENTITY_RULE, replacedFiles);
    }

    private void assertRefused(PackageCode expected, Map<String, String> replacedFiles) throws Exception {
        File zip = buildVariant(replacedFiles);
        Boolean testing = packageService.getTesting();
        packageService.setTesting(true);
        try {
            AbstractException e = assertThrows(AbstractException.class, () -> {
                packageService.preImportPackage();
                packageService.importPackageInternal(zip, true);
            });
            assertEquals(expected, e.getErrorCode(), e.getMessage());
        } finally {
            packageService.setTesting(testing);
            staticDataService.refreshForCurrentThread();
            Files.deleteIfExists(zip.toPath());
        }
    }

    /** The test package with some files replaced or added, as a ZIP. */
    private static File buildVariant(Map<String, String> replacedFiles) throws IOException, URISyntaxException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(TEST_DIR);
        assertNotNull(url, TEST_DIR);
        Path dir = Path.of(url.toURI());
        Map<String, byte[]> files = new HashMap<>();
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path p : paths.filter(Files::isRegularFile).toList()) {
                files.put(dir.relativize(p).toString().replace('\\', '/'), Files.readAllBytes(p));
            }
        }
        replacedFiles.forEach((name, content) -> files.put(name, content.getBytes(StandardCharsets.UTF_8)));
        File zip = File.createTempFile("entity-rules-variant_", ".zip");
        try (OutputStream os = Files.newOutputStream(zip.toPath()); ZipOutputStream zos = new ZipOutputStream(os)) {
            for (Map.Entry<String, byte[]> f : files.entrySet()) {
                zos.putNextEntry(new ZipEntry(f.getKey()));
                zos.write(f.getValue());
                zos.closeEntry();
            }
        }
        return zip;
    }

    private void tx(Runnable body) {
        new TransactionTemplate(txManager).executeWithoutResult(status -> body.run());
    }

    private <T> T txGet(Supplier<T> body) {
        return new TransactionTemplate(txManager).execute(status -> body.get());
    }
}
