package cz.tacr.elza.rules.zp2015;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.lightcomp.kads.mets.MetsReaderWriter;

import cz.tacr.elza.AbstractTest;
import cz.tacr.elza.ElzaCoreMain;
import cz.tacr.elza.api.AipLinkState;
import cz.tacr.elza.api.AipType;
import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.dataexchange.input.DEImportParams;
import cz.tacr.elza.dataexchange.input.DEImportService;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrDaLink;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaChange;
import cz.tacr.elza.domain.DaChangeType;
import cz.tacr.elza.domain.DaDao;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.other.HelperTestService;
import cz.tacr.elza.packageimport.PackageService;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.ArrDaLinkRepository;
import cz.tacr.elza.repository.DaChangeRepository;
import cz.tacr.elza.repository.DaDaoRelationRepository;
import cz.tacr.elza.repository.DaDaoRepository;
import cz.tacr.elza.repository.DaLevelViewRepository;
import cz.tacr.elza.repository.DaLocalCacheRepository;
import cz.tacr.elza.repository.DaSyncQueueItemRepository;
import cz.tacr.elza.repository.DigitalRepositoryRepository;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.service.StartupService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.da.AipNodeUuids;
import cz.tacr.elza.service.da.DaAipAutoLinkService;
import cz.tacr.elza.service.da.DaAipLinkStateResolver;
import cz.tacr.elza.service.da.DaService;
import gov.loc.mets.v1_11.schema.DivType;
import gov.loc.mets.v1_11.schema.FileGrpType;
import gov.loc.mets.v1_11.schema.FileType;
import gov.loc.mets.v1_11.schema.MetsType;
import gov.loc.mets.v1_11.schema.StructMapType;

/**
 * The automatic processing of received packages of loose files ("Volné soubory") in a ZP2015
 * fund: a package matching no unit of description by UUID is placed by the DA_MATCH script under
 * the series "Importováno" and a series per storage unit (or per day of import), and imported
 * there by the DA_IMPORT script.
 *
 * The scenarios build on each other and run in order. Setup and cleanup follow
 * {@link Zp2015DaImportBuildTest}.
 */
@ContextConfiguration(classes = ElzaCoreMain.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Zp2015DaLooseFilesPlacementTest {

    private static final String DIR = Zp2015DaLooseFilesPlanTest.DIR;
    private static final String IMPORTED = "Importováno";
    private static final String FOLDER_B = "uuid-f793d499-430b-4342-b5b6-139343262bf8";

    @Autowired
    private HelperTestService helperTestService;
    @Autowired
    private StaticDataService staticDataService;
    @Autowired
    private StartupService startupService;
    @Autowired
    private PackageService packageService;
    @Autowired
    private PlatformTransactionManager txManager;
    @Autowired
    private UserService userService;
    @Autowired
    private DEImportService deImportService;
    @Autowired
    private AccessPointService apService;
    @Autowired
    private InstitutionRepository institutionRepository;
    @Autowired
    private RuleSetRepository ruleSetRepository;
    @Autowired
    private ArrangementService arrangementService;
    @Autowired
    private DigitalRepositoryRepository digitalRepositoryRepository;
    @Autowired
    private AipRepository aipRepository;
    @Autowired
    private DaChangeRepository daChangeRepository;
    @Autowired
    private DaService daService;
    @Autowired
    private LevelRepository levelRepository;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private DescriptionItemService descriptionItemService;
    @Autowired
    private ArrDaLinkRepository daLinkRepository;
    @Autowired
    private DaDaoRepository daoRepository;
    @Autowired
    private AipStateRepository aipStateRepository;
    @Autowired
    private DaLocalCacheRepository localCacheRepository;
    @Autowired
    private DaSyncQueueItemRepository syncQueueItemRepository;
    @Autowired
    private DaLevelViewRepository levelViewRepository;
    @Autowired
    private DaDaoRelationRepository daoRelationRepository;
    @Autowired
    private DaAipAutoLinkService autoLinkService;
    @Autowired
    private DaAipLinkStateResolver linkStateResolver;

    private Integer rootNodeId;
    private Integer itemA;
    private Integer itemB;

    @BeforeAll
    void createFund() throws Exception {
        helperTestService.deleteTables(false);
        startupService.startNow();
        helperTestService.loadPackage("CZ_BASE", "package-cz-base");
        helperTestService.loadPackage("ZP2015", "rules-cz-zp2015");
        authorizeAsAdmin();
        ParInstitution institution = importInstitution();

        tx().executeWithoutResult(status -> {
            RulRuleSet ruleSet = ruleSetRepository.findByCode("ZP2015");
            ArrFundVersion version = arrangementService.createFundWithScenario("Volné soubory", ruleSet, "VS",
                    institution, null, null, null, null, null, null, null, null);
            rootNodeId = version.getRootNode().getNodeId();
        });
    }

    @AfterAll
    void unloadRules() {
        try {
            helperTestService.waitForWorkers();
            // the shared helper does not know the tables of the digital archive
            tx().executeWithoutResult(status -> {
                daLinkRepository.deleteAll();
                localCacheRepository.deleteAll();
                syncQueueItemRepository.deleteAll();
                aipStateRepository.deleteAll();
                daoRelationRepository.deleteAll();
                daoRepository.deleteAll();
                levelViewRepository.deleteAll();
                daChangeRepository.deleteAll();
                aipRepository.deleteAll();
                digitalRepositoryRepository.deleteAll();
            });
            helperTestService.deleteTables(false);
            packageService.deletePackage("ZP2015");
            staticDataService.refreshForCurrentThread();
        } finally {
            startupService.stop();
        }
    }

    @Test
    @Order(1)
    void packageWithAContainer_isPlacedUnderItsStorageSeries() throws Exception {
        Integer aipId = storedAip("b", Zp2015DaLooseFilesPlanTest.EAD_B, Zp2015DaLooseFilesPlanTest.LOOSE_FILES);

        assertEquals(1, receive(aipId));

        tx().executeWithoutResult(status -> {
            ArrNode imported = onlyChild(rootNodeId);
            assertEquals(IMPORTED, name(imported));
            ArrNode storage = onlyChild(imported.getNodeId());
            assertEquals("samostatně 1", name(storage), "the series is named by the <container> of the package");
            ArrNode item = onlyChild(storage.getNodeId());
            assertEquals("Posudek vojáka; 1938; 5030", name(item));
            assertEquals(uuid(FOLDER_B), item.getUuid(), "the item takes the UUID of its div");
            itemB = item.getNodeId();

            List<ArrDaLink> links = daLinkRepository.findByAipIdAndDeleteChangeIsNull(aipId);
            assertEquals(1, links.size());
            assertEquals(itemB, links.get(0).getNodeId());
            assertEquals(FOLDER_B, links.get(0).getDaDao().getCode());
            assertEquals(AipLinkState.FULLY_LINKED,
                         linkStateResolver.computeLinkState(aipRepository.findById(aipId).orElseThrow()));
        });
    }

    @Test
    @Order(2)
    void packageWithoutAContainer_isPlacedUnderTheSeriesOfTheDay() throws Exception {
        Integer aipId = storedAip("a", Zp2015DaLooseFilesPlanTest.EAD_A, Zp2015DaLooseFilesPlanTest.LOOSE_FILES);

        assertEquals(1, receive(aipId));

        tx().executeWithoutResult(status -> {
            ArrNode imported = onlyChild(rootNodeId);
            List<ArrNode> series = children(imported.getNodeId());
            assertEquals(2, series.size(), "the series Importováno is shared");
            assertEquals(LocalDate.now().toString(), name(series.get(1)));
            ArrNode item = onlyChild(series.get(1).getNodeId());
            assertEquals("Digitální fotografie 3", name(item));
            itemA = item.getNodeId();

            List<ArrDaLink> links = daLinkRepository.findByAipIdAndDeleteChangeIsNull(aipId);
            assertEquals(1, links.size(), "the package is the item - its own part covers the components");
            assertEquals(itemA, links.get(0).getNodeId());
            assertEquals("ds_1", links.get(0).getDaDao().getCode());
        });
    }

    @Test
    @Order(3)
    void samePackageAgain_withoutUuids_findsItsItemBySourceId() throws Exception {
        Integer aipId = storedAip("a", Zp2015DaLooseFilesPlanTest.EAD_A, Zp2015DaLooseFilesPlanTest.LOOSE_FILES);

        assertEquals(1, receive(aipId));

        tx().executeWithoutResult(status -> {
            ArrNode imported = onlyChild(rootNodeId);
            ArrNode day = children(imported.getNodeId()).get(1);
            assertEquals(itemA, onlyChild(day.getNodeId()).getNodeId(), "no second item for the same unit");
            List<ArrDaLink> links = daLinkRepository.findByAipIdAndDeleteChangeIsNull(aipId);
            assertEquals(1, links.size());
            assertEquals(itemA, links.get(0).getNodeId());
        });
    }

    @Test
    @Order(4)
    void samePackageAgain_withUuids_isMatchedByUuid() throws Exception {
        Integer aipId = storedAip("b", Zp2015DaLooseFilesPlanTest.EAD_B, Zp2015DaLooseFilesPlanTest.LOOSE_FILES);

        assertEquals(1, receive(aipId));

        tx().executeWithoutResult(status -> {
            ArrNode storage = children(onlyChild(rootNodeId).getNodeId()).get(0);
            assertEquals(itemB, onlyChild(storage.getNodeId()).getNodeId());
            List<ArrDaLink> links = daLinkRepository.findByAipIdAndDeleteChangeIsNull(aipId);
            assertEquals(1, links.size());
            assertEquals(itemB, links.get(0).getNodeId(), "the component is attached to the item it matched");
        });
    }

    @Test
    @Order(5)
    void packageOfAnotherType_isLeftToTheUser() throws Exception {
        Integer aipId = storedAip("a", Zp2015DaLooseFilesPlanTest.EAD_A, "NSESSS");

        assertEquals(0, receive(aipId));

        tx().executeWithoutResult(status -> {
            assertEquals(1, children(rootNodeId).size());
            assertTrue(daLinkRepository.findByAipIdAndDeleteChangeIsNull(aipId).isEmpty());
        });
    }

    /** What the automatic processing does with a package whose metadata arrived. */
    private int receive(Integer aipId) {
        List<String> uuids = tx().execute(status -> {
            DaAip aip = aipRepository.findById(aipId).orElseThrow();
            List<String> divIds = daoRepository.findByAipAndTypeAndDeleteChangeIsNull(aip, DaDao.DaoType.LOGICAL)
                    .stream().map(DaDao::getCode).collect(Collectors.toList());
            return AipNodeUuids.inMatchingOrder(aip.getCode(), divIds, List.of());
        });
        return autoLinkService.linkReceivedAips(Map.of(aipId, uuids));
    }

    /** An AIP of the sample with its metadata package stored in ELZA, as downloading it leaves it. */
    private Integer storedAip(String sample, String eadHref, String contentType) throws Exception {
        String metsXml = resource(sample + "/METS.xml");
        String eadXml = resource(sample + "/" + eadHref.substring(eadHref.lastIndexOf('/') + 1));
        Path zip = zip(Map.of("aip/METS.xml", metsXml, "aip/" + eadHref, eadXml));
        MetsType mets = MetsReaderWriter.unmarshal(new ByteArrayInputStream(metsXml.getBytes(StandardCharsets.UTF_8)));
        return tx().execute(status -> {
            DaAip aip = createAip("uuid-" + UUID.randomUUID(), mets);
            DaAipState state = aipStateRepository.findByDaAipAndDeleteChangeIsNull(aip);
            state.setFund(nodeRepository.findById(rootNodeId).orElseThrow().getFund());
            state.setContentType(contentType);
            state.setProfile(Zp2015DaLooseFilesPlanTest.PROFILE);
            aipStateRepository.save(state);
            daService.createImportLocalCache(state, aip.getDigitalRepository(), AipType.METADATA_BASE, zip, null);
            return aip.getAipId();
        });
    }

    /**
     * The AIP with a logical digital entity for every div and a file entity for every file a div
     * points to, related as processing the metadata relates them.
     */
    private DaAip createAip(String code, MetsType mets) {
        ArrDigitalRepository repository = digitalRepositoryRepository.findAll().stream().findFirst().orElseGet(() -> {
            ArrDigitalRepository r = new ArrDigitalRepository();
            r.setCode("DA-VOLNE");
            r.setName("Digitalni archiv");
            r.setDigitalRepositoryType(DigitalRepositoryType.DA);
            r.setSendNotification(false);
            return digitalRepositoryRepository.save(r);
        });
        DaAip aip = new DaAip();
        aip.setCode(code);
        aip.setDigitalRepository(repository);
        aipRepository.save(aip);

        DaChange change = new DaChange();
        change.setChangeDate(LocalDateTime.now());
        change.setDaAip(aip);
        change.setType(DaChangeType.AIP_CREATE);
        daChangeRepository.save(change);

        DaAipState state = new DaAipState();
        state.setDaAip(aip);
        state.setCreateChange(change);
        state.setAipVersion("1");
        aipStateRepository.save(state);

        Map<String, DaDao> files = new HashMap<>();
        for (StructMapType structMap : mets.getStructMap()) {
            if ("LOGICAL".equals(structMap.getTYPE())) {
                createDaos(aip, change, structMap.getDiv(), null, files);
            }
        }
        return aip;
    }

    private void createDaos(DaAip aip, DaChange change, DivType div, DaDao parent, Map<String, DaDao> files) {
        DaDao dao = daService.createDaDao(aip, change, div.getID(), div.getLABEL(), DaDao.DaoType.LOGICAL);
        if (parent != null) {
            daService.createDaDaoRelation(dao, parent, change);
        }
        for (DivType.Fptr fptr : div.getFptr()) {
            Object target = fptr.getFILEID();
            String fileId = target instanceof FileType file ? file.getID() : ((FileGrpType) target).getID();
            DaDao file = files.computeIfAbsent(fileId,
                    id -> daService.createDaDao(aip, change, id, id, DaDao.DaoType.FILE));
            daService.createDaDaoRelation(file, dao, change);
        }
        for (DivType child : div.getDiv()) {
            createDaos(aip, change, child, dao, files);
        }
    }

    private String resource(String name) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(DIR + name)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Path zip(Map<String, String> entries) throws IOException {
        Path zip = Files.createTempFile("da-volne", ".zip");
        zip.toFile().deleteOnExit();
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return zip;
    }

    private List<ArrNode> children(Integer nodeId) {
        return new ArrayList<>(levelRepository.findByParentNodeAndDeleteChangeIsNullOrderByPositionAsc(
                nodeRepository.findById(nodeId).orElseThrow()).stream()
                .map(ArrLevel::getNode).toList());
    }

    private ArrNode onlyChild(Integer nodeId) {
        List<ArrNode> children = children(nodeId);
        assertEquals(1, children.size(), "children of node " + nodeId);
        return children.get(0);
    }

    private String name(ArrNode node) {
        List<ArrDescItem> items = descriptionItemService.findByNodeIdsAndDeleteChangeIsNull(List.of(node.getNodeId()));
        ArrDescItem name = items.stream()
                .filter(i -> i.getItemType().getCode().equals("ZP2015_NAME"))
                .findFirst().orElseThrow();
        return name.getData().getFulltextValue();
    }

    private static String uuid(String divId) {
        return divId.substring("uuid-".length());
    }

    private TransactionTemplate tx() {
        return new TransactionTemplate(txManager);
    }

    private void authorizeAsAdmin() {
        UserDetail userDetail = userService.createUserDetail((Integer) null);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("", "", null);
        auth.setDetails(userDetail);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

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
