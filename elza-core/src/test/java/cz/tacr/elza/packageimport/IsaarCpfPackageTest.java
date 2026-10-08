package cz.tacr.elza.packageimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
import cz.tacr.elza.controller.AccessPointController;
import cz.tacr.elza.controller.ApController;
import cz.tacr.elza.controller.vo.ApAccessPointCreateVO;
import cz.tacr.elza.controller.vo.ApPartFormVO;
import cz.tacr.elza.controller.vo.ApTypeVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemAccessPointRefVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemEnumVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemStringVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemVO;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.TranslationEntityType;
import cz.tacr.elza.drools.model.ItemSpec;
import cz.tacr.elza.drools.model.ItemType;
import cz.tacr.elza.drools.model.ModelAvailable;
import cz.tacr.elza.drools.model.RequiredType;
import cz.tacr.elza.groovy.GroovyResult;
import cz.tacr.elza.core.data.PackageTexts;
import cz.tacr.elza.other.HelperTestService;
import cz.tacr.elza.repository.ApIndexRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.ItemAptypeRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.ItemTypeSpecAssignRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.StartupService;
import cz.tacr.elza.service.UserService;

/**
 * The international entity description package ISAAR_CPF: alone (it owns the shared definitions) and
 * together with CZ_BASE in both orders of import (shared classes, part types, item types and
 * specifications exist once; each scope shows the tree of its rule set).
 *
 * <p>Packages stay installed across test classes, so the class removes all of them first and at the end;
 * later test classes import the packages they need again.
 */
@ContextConfiguration(classes = ElzaCoreMain.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IsaarCpfPackageTest {

    private static final String CODE = "ISAAR_CPF";
    private static final String DIR = "package-isaar-cpf";
    private static final List<String> ROOTS = List.of("PERSON_INDIVIDUAL", "FAMILY", "PARTY_GROUP", "GEO", "TERM");

    @Autowired
    private HelperTestService helperTestService;
    @Autowired
    private StartupService startupService;
    @Autowired
    private StaticDataService staticDataService;
    @Autowired
    private PackageService packageService;
    @Autowired
    private ScopeRepository scopeRepository;
    @Autowired
    private AccessPointService accessPointService;
    @Autowired
    private AccessPointController accessPointController;
    @Autowired
    private ApController apController;
    @Autowired
    private RuleService ruleService;
    @Autowired
    private UserService userService;
    @Autowired
    private PackageTexts packageTexts;
    @Autowired
    private ApIndexRepository indexRepository;
    @Autowired
    private ApTypeRepository apTypeRepository;
    @Autowired
    private ItemTypeRepository itemTypeRepository;
    @Autowired
    private ItemSpecRepository itemSpecRepository;
    @Autowired
    private ItemTypeSpecAssignRepository itemTypeSpecAssignRepository;
    @Autowired
    private ItemAptypeRepository itemAptypeRepository;
    @Autowired
    @Qualifier("transactionManager")
    private PlatformTransactionManager txManager;

    private Integer isaarScope;

    @BeforeAll
    void loadPackage() {
        startupService.startNow();
        authorizeAsAdmin();
        // validation workers of a preceding test class may still write; they would collide with the delete
        helperTestService.waitForWorkers();
        helperTestService.deleteAllPackages();
        helperTestService.loadPackage(CODE, DIR);
        isaarScope = createScope("ISAAR_TEST", CODE);
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

    /** Alone, the package owns the shared definitions and offers five flat root classes. */
    @Test
    @Order(1)
    void packageAloneOffersFlatRoots() {
        tx(() -> {
            StaticDataProvider sdp = staticDataService.getData();
            assertEquals(CODE, sdp.getApTypeByCode("PERSON_INDIVIDUAL").getRulPackage().getCode());
            assertEquals(CODE, itemTypeRepository.findOneByCode("NM_MAIN").getRulPackage().getCode());
            assertEquals(CODE, itemSpecRepository.findOneByCode("NT_PSEUDONYM").getPackage().getCode());
            assertEquals(List.of("PT_NAME", "PT_IDENT", "PT_BODY", "PT_CRE", "PT_EXT", "PT_EVENT", "PT_REL"),
                         sdp.getRuleSetByCode(CODE).getPartTypeOrder().stream()
                                 .map(id -> sdp.getPartTypeById(id).getCode()).toList());
        });
        assertEquals(ROOTS, roots(isaarScope));
        assertEquals(ROOTS, roots(null));
        for (ApTypeVO root : txGet(() -> apController.getApTypes(isaarScope))) {
            assertTrue(root.getAddRecord(), root.getCode());
            assertTrue(root.getChildren() == null || root.getChildren().isEmpty(), root.getCode());
        }
    }

    /** A corporate body, a person related to it and a place: created, named and validated by the rules. */
    @Test
    @Order(2)
    void entitiesAreCreatedAndValidated() {
        Integer archives = create(isaarScope, "PARTY_GROUP", "PT_NAME", string("NM_MAIN", "National Archives"));
        addPart(archives, "PT_BODY", spec("ISAAR_CORP_TYPE", "ISAAR_CORP_TYPE_GOVERNMENT"),
                spec("ISAAR_LEGAL_STATUS", "ISAAR_LEGAL_STATUS_PUBLIC_LAW"), string("BRIEF_DESC", "The national archives"));
        Integer person = create(isaarScope, "PERSON_INDIVIDUAL", "PT_NAME", string("NM_MAIN", "Novak"), string("NM_MINOR", "Jan"));
        addPart(person, "PT_IDENT", spec("IDN_TYPE", "VIAF"), string("IDN_VALUE", "123456789"));
        addPart(person, "PT_REL", ref("REL_ENTITY", "RT_EMPLOYER", archives));
        Integer prague = create(isaarScope, "GEO", "PT_NAME", string("NM_MAIN", "Prague"));
        addPart(prague, "PT_BODY", spec("ISAAR_PLACE_TYPE", "ISAAR_PLACE_TYPE_POPULATED"));

        tx(() -> {
            assertEquals("Novak, Jan", displayName(person));
            assertEquals("National Archives", displayName(archives));
            assertEquals("The national archives", bodyName(archives));
            for (Integer id : List.of(archives, person, prague)) {
                ApAccessPoint ap = accessPointService.getAccessPointInternal(id);
                assertTrue(ap.getErrorDescription() == null || ap.getErrorDescription().isBlank(), ap.getErrorDescription());
            }
        });

        // available items follow the rules of the rule set and the class
        Map<String, RequiredType> name = available(isaarScope, "PERSON_INDIVIDUAL", "PT_NAME");
        assertEquals(RequiredType.REQUIRED, name.get("NM_MAIN"));
        assertEquals(RequiredType.POSSIBLE, name.get("NM_MINOR"));
        assertEquals(RequiredType.POSSIBLE, name.get("NM_TYPE/NT_PSEUDONYM"));
        assertEquals(RequiredType.POSSIBLE, name.get("NM_LANG/LNG_eng"));
        assertEquals(16, name.keySet().stream().filter(k -> k.startsWith("NM_LANG/")).count());
        Map<String, RequiredType> body = available(isaarScope, "PARTY_GROUP", "PT_BODY");
        assertEquals(RequiredType.POSSIBLE, body.get("ISAAR_CORP_TYPE/ISAAR_CORP_TYPE_PARTY"));
        assertEquals(RequiredType.POSSIBLE, body.get("ISAAR_LEGAL_STATUS/ISAAR_LEGAL_STATUS_COMPANY"));
        Map<String, RequiredType> personBody = available(isaarScope, "PERSON_INDIVIDUAL", "PT_BODY");
        assertTrue(personBody.get("ISAAR_CORP_TYPE") != RequiredType.POSSIBLE, String.valueOf(personBody.get("ISAAR_CORP_TYPE")));
        assertEquals(RequiredType.POSSIBLE, personBody.get("ISAAR_FUNCTIONS"));
    }

    /** The package exports and imports again as it was. */
    @Test
    @Order(3)
    void exportsAndImportsAgain() throws IOException {
        Path zip = txGet(() -> {
            try {
                return packageService.exportPackage(CODE);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
        try {
            helperTestService.deleteAllPackages();
            packageService.preImportPackage();
            try {
                packageService.importPackageInternal(zip.toFile(), true);
            } finally {
                staticDataService.refreshForCurrentThread();
            }
            isaarScope = createScope("ISAAR_TEST", CODE);
            assertEquals(ROOTS, roots(isaarScope));
            tx(() -> assertEquals(121, itemSpecRepository.findAll().size()));
        } finally {
            Files.deleteIfExists(zip);
        }
    }

    /**
     * CZ_BASE imported after ISAAR_CPF, then the other order: the shared definitions exist once, CAM
     * scopes see CAM's tree, ISAAR scopes the flat roots, and an ISAAR person relates to a CAM place.
     */
    @Test
    @Order(4)
    void worksTogetherWithCzBaseInBothOrders() {
        helperTestService.loadPackage("CZ_BASE", "package-cz-base");
        checkShared(CODE);

        helperTestService.deleteAllPackages();
        helperTestService.loadPackage("CZ_BASE", "package-cz-base");
        helperTestService.loadPackage(CODE, DIR);
        checkShared("CZ_BASE");
    }

    private void checkShared(String ownerOfSharedCodes) {
        Integer isaar = createScope("ISAAR_SHARED", CODE);
        Integer cam = createScope("CAM_SHARED", "CAM");
        tx(() -> {
            StaticDataProvider sdp = staticDataService.getData();
            assertEquals(1, apTypeRepository.findAll().stream().filter(t -> t.getCode().equals("PERSON_INDIVIDUAL")).count());
            assertEquals(ownerOfSharedCodes, itemTypeRepository.findOneByCode("NM_MAIN").getRulPackage().getCode());
            assertEquals(ownerOfSharedCodes, itemSpecRepository.findOneByCode("NT_PSEUDONYM").getPackage().getCode());
            // the specifications of NM_TYPE are CAM's 32; ISAAR_CPF adds none
            assertEquals(32, itemTypeSpecAssignRepository.findByItemTypeSorted(itemTypeRepository.findOneByCode("NM_TYPE")).size());
            RulItemSpec father = itemSpecRepository.findOneByCode("RT_FATHER");
            assertEquals(1, itemAptypeRepository.findByItemSpec(father).size());
            assertEquals("Pseudonym", packageTexts.text(TranslationEntityType.ITEM_SPEC, "NT_PSEUDONYM",
                                                        TranslationEntityType.NAME, "x", sdp.getSysLanguageByTag("en")));
            assertEquals("pseudonym", packageTexts.text(TranslationEntityType.ITEM_SPEC, "NT_PSEUDONYM",
                                                        TranslationEntityType.NAME, "x", sdp.getSysLanguageByTag("cs")));
        });
        assertEquals(ROOTS, roots(isaar));

        // the ISAAR rules open every language of a name, but a rule set sees only the specifications
        // assigned by packages related to its own: ISAAR_CPF's 16 languages, not the 150 CZ_BASE adds
        // to NM_LANG; the CAM rule set (same package as the shared codes) keeps every language
        Map<String, RequiredType> isaarName = available(isaar, "PERSON_INDIVIDUAL", "PT_NAME");
        assertEquals(RequiredType.POSSIBLE, isaarName.get("NM_LANG/LNG_eng"));
        assertNull(isaarName.get("NM_LANG/LNG_heb"));
        assertEquals(16, isaarName.keySet().stream().filter(k -> k.startsWith("NM_LANG/")).count());
        Map<String, RequiredType> camName = available(cam, "PERSON_INDIVIDUAL", "PT_NAME");
        assertEquals(RequiredType.POSSIBLE, camName.get("NM_LANG/LNG_heb"));
        assertEquals(RequiredType.POSSIBLE, camName.get("NM_LANG/LNG_eng"));
        int camLanguages = txGet(() -> itemTypeSpecAssignRepository
                .findByItemTypeSorted(itemTypeRepository.findOneByCode("NM_LANG")).size());
        assertEquals(camLanguages, camName.keySet().stream().filter(k -> k.startsWith("NM_LANG/")).count());

        List<ApTypeVO> camTree = txGet(() -> apController.getApTypes(cam));
        ApTypeVO person = camTree.stream().filter(t -> t.getCode().equals("PERSON")).findFirst().orElseThrow();
        assertTrue(!person.getAddRecord());
        assertTrue(person.getChildren().stream().anyMatch(c -> c.getCode().equals("PERSON_INDIVIDUAL")));
        assertTrue(camTree.size() > ROOTS.size());

        // an ISAAR person seated in a CAM place (class GEO_UNIT, a subclass of the shared root GEO)
        Integer camPlace = create(cam, "GEO_UNIT", "PT_NAME", string("NM_MAIN", "Praha"));
        Integer person2 = create(isaar, "PERSON_INDIVIDUAL", "PT_NAME", string("NM_MAIN", "Dvorak"));
        addPart(person2, "PT_REL", ref("REL_ENTITY", "RT_RESIDENCE", camPlace));
        tx(() -> {
            ApAccessPoint ap = accessPointService.getAccessPointInternal(person2);
            assertTrue(ap.getErrorDescription() == null || ap.getErrorDescription().isBlank(), ap.getErrorDescription());
        });
    }

    // helpers

    private List<String> roots(Integer scopeId) {
        return txGet(() -> apController.getApTypes(scopeId)).stream().map(ApTypeVO::getCode).toList();
    }

    private Integer create(Integer scopeId, String apType, String partType, ApItemVO... items) {
        return txGet(() -> accessPointService.createAccessPoint(scope(scopeId), type(apType), form(partType, items))
                .getAccessPointId());
    }

    private void addPart(Integer accessPointId, String partType, ApItemVO... items) {
        tx(() -> {
            ApAccessPoint ap = accessPointService.getAccessPointInternal(accessPointId);
            accessPointController.accessPointCreatePart(accessPointId, form(partType, items), ap.getVersion());
        });
    }

    private String displayName(Integer accessPointId) {
        ApIndex index = indexRepository.findPreferredPartIndexByAccessPointAndIndexType(
                accessPointService.getAccessPointInternal(accessPointId), GroovyResult.DISPLAY_NAME);
        return index != null ? index.getIndexValue() : null;
    }

    private String bodyName(Integer accessPointId) {
        return indexRepository.findIndicesByAccessPoint(accessPointId).stream()
                .filter(i -> GroovyResult.DISPLAY_NAME.equals(i.getIndexType())
                        && "PT_BODY".equals(i.getPart().getPartType().getCode()))
                .map(ApIndex::getIndexValue).findFirst().orElse(null);
    }

    private Map<String, RequiredType> available(Integer scopeId, String apType, String partType) {
        return txGet(() -> {
            ApAccessPointCreateVO form = new ApAccessPointCreateVO();
            form.setTypeId(type(apType).getApTypeId());
            form.setScopeId(scopeId);
            ApPartFormVO partForm = new ApPartFormVO();
            partForm.setPartTypeCode(partType);
            partForm.setItems(List.of());
            form.setPartForm(partForm);
            ModelAvailable result = ruleService.executeAvailable(form);
            Map<String, RequiredType> types = new HashMap<>();
            for (ItemType itemType : result.getItemTypes()) {
                types.put(itemType.getCode(), itemType.getRequiredType());
                for (ItemSpec spec : itemType.getSpecs()) {
                    types.put(itemType.getCode() + "/" + spec.getCode(), spec.getRequiredType());
                }
            }
            return types;
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

    private ApItemVO spec(String itemType, String specCode) {
        ApItemEnumVO item = new ApItemEnumVO();
        item.setTypeId(itemTypeRepository.findOneByCode(itemType).getItemTypeId());
        item.setSpecId(itemSpecRepository.findOneByCode(specCode).getItemSpecId());
        return item;
    }

    private ApItemVO ref(String itemType, String specCode, Integer accessPointId) {
        ApItemAccessPointRefVO item = new ApItemAccessPointRefVO();
        item.setTypeId(itemTypeRepository.findOneByCode(itemType).getItemTypeId());
        item.setSpecId(itemSpecRepository.findOneByCode(specCode).getItemSpecId());
        item.setValue(accessPointId);
        return item;
    }

    private cz.tacr.elza.domain.ApType type(String code) {
        cz.tacr.elza.domain.ApType type = staticDataService.getData().getApTypeByCode(code);
        assertNotNull(type, code);
        return type;
    }

    private ApScope scope(Integer scopeId) {
        return scopeRepository.findById(scopeId).orElseThrow();
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

    private void authorizeAsAdmin() {
        UserDetail userDetail = userService.createUserDetail((Integer) null);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("", "", null);
        auth.setDetails(userDetail);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void tx(Runnable body) {
        new TransactionTemplate(txManager).executeWithoutResult(status -> body.run());
    }

    private <T> T txGet(Supplier<T> body) {
        return new TransactionTemplate(txManager).execute(status -> body.get());
    }
}
