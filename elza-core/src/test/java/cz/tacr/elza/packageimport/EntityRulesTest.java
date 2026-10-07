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
        } finally {
            Files.deleteIfExists(zip);
        }
    }

    @Test
    @Order(4)
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
        // member classes: an unknown class, a class listed twice
        assertRefused(Map.of(RULE_SET_DIR + PackageService.RULE_SET_AP_TYPE_XML,
                             "<ap-types><ap-type code=\"NO_SUCH_CLASS\"/></ap-types>"));
        assertRefused(Map.of(RULE_SET_DIR + PackageService.RULE_SET_AP_TYPE_XML,
                             "<ap-types><ap-type code=\"PERSON\"/><ap-type code=\"PERSON\"/></ap-types>"));
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
        assertEquals(List.of("PERSON_INDIVIDUAL"), tree.get(0).getChildren().stream().map(ApTypeVO::getCode).toList());
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

    @Test
    @Order(8)
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
