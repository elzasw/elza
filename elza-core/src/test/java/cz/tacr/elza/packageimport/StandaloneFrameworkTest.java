package cz.tacr.elza.packageimport;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.ElzaCoreMain;
import cz.tacr.elza.controller.AccessPointController;
import cz.tacr.elza.controller.ApController;
import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.ApTypeVO;
import cz.tacr.elza.controller.vo.ApPartFormVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemStringVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemTextVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemVO;
import cz.tacr.elza.controller.vo.Institution;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.controller.vo.ApValidationIssues;
import cz.tacr.elza.groovy.GroovyResult;
import cz.tacr.elza.other.HelperTestService;
import cz.tacr.elza.repository.ApIndexRepository;
import cz.tacr.elza.repository.InstitutionTypeRepository;
import cz.tacr.elza.repository.PackageRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.security.oauth2.JwtUserDetailProvider;
import cz.tacr.elza.security.oauth2.OAuth2Properties;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.InstitutionService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.StartupService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.repository.ItemTypeRepository;

/**
 * An entity description framework without CZ_BASE: the test package {@code entity-standalone-test}
 * declares PERSON (as the parent only), PERSON_INDIVIDUAL, PT_NAME, PT_BODY, PT_REL, NM_MAIN, its own
 * name items and NOTE itself, has no AUTO_ITEMS script and none of the CAM item types. The core must
 * create, index and validate its entities, create users from tokens and institutions, and rank the
 * search by the index the package states in {@code index-search}.
 *
 * <p>Packages stay installed across test classes, so the class removes all of them first; later test
 * classes import the packages they need again. Context configuration matches {@code AbstractTest} so
 * the cached Spring context is reused.
 */
@ContextConfiguration(classes = ElzaCoreMain.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StandaloneFrameworkTest {

    private static final String TEST_CODE = "ENTITY_STANDALONE_TEST";

    @Autowired
    private HelperTestService helperTestService;
    @Autowired
    private StartupService startupService;
    @Autowired
    private StaticDataService staticDataService;
    @Autowired
    private PackageRepository packageRepository;
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
    private InstitutionService institutionService;
    @Autowired
    private InstitutionTypeRepository institutionTypeRepository;
    @Autowired
    private ApIndexRepository indexRepository;
    @Autowired
    private ItemTypeRepository itemTypeRepository;
    @Autowired
    @Qualifier("transactionManager")
    private PlatformTransactionManager txManager;

    private Integer accessPointId;

    @BeforeAll
    void loadPackage() {
        startupService.startNow();
        authorizeAsAdmin();
        helperTestService.deleteAllPackages();
        helperTestService.loadPackage(TEST_CODE, "entity-standalone-test");
    }

    @AfterAll
    void unloadPackage() {
        try {
            helperTestService.deleteAllPackages();
        } finally {
            startupService.stop();
        }
    }

    @Test
    @Order(1)
    void noCamDefinitions() {
        assertNull(packageRepository.findByCode("CZ_BASE"));
        StaticDataProvider sdp = staticDataService.getData();
        assertNull(sdp.getItemTypeByCode("REL_ENTITY"));
        assertNull(sdp.getItemTypeByCode("NM_TYPE"));
        assertNull(sdp.getItemTypeByCode("NM_SUP_PRIV"));
        assertNotNull(sdp.getItemTypeByCode("NM_MAIN"));
    }

    /** An entity with a name, a description and a relation is created, indexed and validated. */
    @Test
    @Order(2)
    void entityIsCreatedAndValidated() {
        ApScope scope = txGet(() -> scopeRepository.save(newScope("STD_SCOPE")));
        ApState state = txGet(() -> accessPointService.createAccessPoint(scope,
                staticDataService.getData().getApTypeByCode("PERSON_INDIVIDUAL"), form("PT_NAME", string("NM_MAIN", "Novak"))));
        // the tree of the scope and of the installation: the person class as a root, its parent is not a member
        for (List<ApTypeVO> tree : List.of(txGet(() -> apController.getApTypes(scope.getScopeId())),
                                           txGet(() -> apController.getApTypes(null)))) {
            assertEquals(List.of("PERSON_INDIVIDUAL"), tree.stream().map(ApTypeVO::getCode).toList());
            assertTrue(tree.get(0).getChildren() == null || tree.get(0).getChildren().isEmpty());
        }
        accessPointId = state.getAccessPointId();

        tx(() -> {
            ApAccessPoint ap = accessPointService.getAccessPointInternal(accessPointId);
            accessPointController.accessPointCreatePart(accessPointId, form("PT_BODY", text("NOTE", "A person")),
                                                        ap.getVersion());
        });
        tx(() -> {
            ApAccessPoint ap = accessPointService.getAccessPointInternal(accessPointId);
            accessPointController.accessPointCreatePart(accessPointId, form("PT_REL", text("NOTE", "Related")),
                                                        ap.getVersion());
        });

        tx(() -> {
            ApAccessPoint ap = accessPointService.getAccessPointInternal(accessPointId);
            ApIndex name = indexRepository.findPreferredPartIndexByAccessPointAndIndexType(ap, GroovyResult.DISPLAY_NAME);
            assertEquals("Novak", name.getIndexValue());
            ApValidationIssues issues = ruleService.executeValidation(accessPointService.getStateInternal(ap), false);
            assertTrue(issues.getErrors() == null || issues.getErrors().isEmpty(), String.valueOf(issues.getErrors()));
            assertTrue(ap.getErrorDescription() == null || ap.getErrorDescription().isBlank(), ap.getErrorDescription());
        });
    }

    /** A user created from a token gets an entity in the configured scope, without CAM's private name. */
    @Test
    @Order(3)
    void userFromTokenIsCreated() {
        txGet(() -> scopeRepository.save(newScope("STD_USERS")));
        OAuth2Properties properties = new OAuth2Properties();
        properties.setUserScope("STD_USERS");
        properties.setUserApType("PERSON_INDIVIDUAL");
        JwtUserDetailProvider provider = new JwtUserDetailProvider(null, txManager, userService, accessPointService,
                itemTypeRepository, properties, null);

        UsrUser user = txGet(() -> {
            SecurityContext previous = SecurityContextHolder.getContext();
            SecurityContextHolder.setContext(userService.createSecurityContextSystem());
            try {
                return ReflectionTestUtils.invokeMethod(provider, "createJWTUser", "jdoe", "John Doe");
            } finally {
                SecurityContextHolder.setContext(previous);
            }
        });
        tx(() -> {
            ApAccessPoint ap = accessPointService.getAccessPointInternal(user.getAccessPointId());
            ApIndex name = indexRepository.findPreferredPartIndexByAccessPointAndIndexType(ap, GroovyResult.DISPLAY_NAME);
            assertEquals("John Doe", name.getIndexValue());
        });
    }

    /** The short name of an institution is the SHORT_NAME index of its entity's preferred name. */
    @Test
    @Order(4)
    void institutionGetsTheShortNameOfItsEntity() {
        ParInstitution institution = txGet(() -> {
            Institution dto = new Institution();
            dto.setInternalCode("STD-1");
            dto.setAccessPointId(accessPointId);
            dto.setInstitutionTypeId(institutionTypeRepository.findAll().stream()
                    .filter(t -> t.getCode().equals("STD_ARCHIVE")).findFirst().orElseThrow()
                    .getInstitutionTypeId());
            return institutionService.create(dto);
        });
        assertEquals("Novak", institution.getName());
        assertEquals("Nov", institution.getShortName());
    }

    /**
     * The whole search text matched against the REAL_NAME index (stated by the package in
     * {@code index-search}, read by the index bridge at its import) ranks the entity whose common
     * name the user typed above an entity whose shorter catalogue name merely contains the words;
     * the match ignores the order of the words and the diacritics.
     */
    @Test
    @Order(5)
    void realNameRanksTheCommonNameFirst() {
        ApScope scope = txGet(() -> scopeRepository.save(newScope("STD_SEARCH")));
        // "Rumburak, Petr (zlý čaroděj)", REAL_NAME "Petr Rumburak"
        Integer wanted = createPerson(scope, string("NM_MAIN", "Rumburak"), string("STD_MINOR", "Petr"),
                                      string("STD_SUP", "zlý čaroděj"));
        // "Rumburak, Petr Abel", REAL_NAME "Petr Abel Rumburak": the shorter catalogue name
        Integer other = createPerson(scope, string("NM_MAIN", "Rumburak"), string("STD_MINOR", "Petr Abel"));

        // indexing is asynchronous (outbox polling) and the validation queue reindexes a new entity once more
        awaitSearch(List.of(wanted, other), "petr rumburak", scope);
        awaitSearch(List.of(wanted, other), "Rumburák, Petr", scope);
        // every word must match a field of the entity: the index of the common name adds no recall
        awaitSearch(List.of(other), "petr abel rumburak", scope);
    }

    private void awaitSearch(List<Integer> expected, String text, ApScope scope) {
        await().atMost(30, SECONDS).pollInterval(200, MILLISECONDS).untilAsserted(
                () -> assertEquals(expected, search(text, scope), text));
    }

    private Integer createPerson(ApScope scope, ApItemVO... items) {
        return txGet(() -> accessPointService.createAccessPoint(scope,
                staticDataService.getData().getApTypeByCode("PERSON_INDIVIDUAL"), form("PT_NAME", items))
                .getAccessPointId());
    }

    /** Ids of the entities of the scope found by the text search, in the order of the search. */
    private List<Integer> search(String text, ApScope scope) {
        return txGet(() -> accessPointService.findUseLuceneQueries(text, null, null, null, scope.getScopeId(),
                                                                    null, null, 0, 10, staticDataService.getData())
                .getRows().stream().map(ApAccessPointVO::getId).toList());
    }

    private ApScope newScope(String code) {
        ApScope scope = new ApScope();
        scope.setCode(code);
        scope.setName(code);
        scope.setRulRuleSet(staticDataService.getData().getRuleSetByCode("STD_TEST").getEntity());
        return scope;
    }

    private ApPartFormVO form(String partType, ApItemVO... items) {
        ApPartFormVO form = new ApPartFormVO();
        form.setPartTypeCode(partType);
        form.setItems(new ArrayList<>(List.of(items)));
        return form;
    }

    private ApItemVO string(String itemType, String value) {
        ApItemStringVO item = new ApItemStringVO();
        item.setTypeId(staticDataService.getData().getItemTypeByCode(itemType).getItemTypeId());
        item.setValue(value);
        return item;
    }

    private ApItemVO text(String itemType, String value) {
        ApItemTextVO item = new ApItemTextVO();
        item.setTypeId(staticDataService.getData().getItemTypeByCode(itemType).getItemTypeId());
        item.setValue(value);
        return item;
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
