package cz.tacr.elza.rules.zp2015;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.archivists.ead3.schema.C;
import org.archivists.ead3.schema.Dsc;
import org.archivists.ead3.schema.Ead;
import org.archivists.ead3.schema.Unitid;
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
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.other.HelperTestService;
import cz.tacr.elza.packageimport.PackageService;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.ArrDaLinkRepository;
import cz.tacr.elza.repository.DaChangeRepository;
import cz.tacr.elza.repository.DaDaoRepository;
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
import cz.tacr.elza.service.da.DaImportBuilder;
import cz.tacr.elza.service.da.DaImportPackage;
import cz.tacr.elza.service.da.DaImportService;
import cz.tacr.elza.service.da.DaImportPlan;
import cz.tacr.elza.service.da.DaImportPlanner;
import cz.tacr.elza.service.da.DaService;
import cz.tacr.elza.utils.EadReaderWriter;
import gov.loc.mets.v1_11.schema.DivType;
import gov.loc.mets.v1_11.schema.MetsType;
import gov.loc.mets.v1_11.schema.StructMapType;

/**
 * Carrying out the import plan of an NSESSS package in a real ZP2015 fund: the levels are created
 * once, the same package matches them by UUID, and another package from the same file plan shares
 * its groups by name.
 *
 * The scenarios build on each other and run in order. Setup and cleanup follow
 * {@link Zp2015AccessRestrictExportTest}: the validation queued by the created levels is waited for
 * before the tables are wiped and the package is unloaded.
 */
@ContextConfiguration(classes = ElzaCoreMain.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Zp2015DaImportBuildTest {

    private static final String DIR = "/zp2015/da-nsesss/";
    private static final String GROUP_44 = "uuid-ac73f202-a2a0-4309-9ce7-6c1e426be654";
    private static final String GROUP_44_4 = "uuid-89a2ecb1-6951-4f97-b1ad-beff25e6cf2d";
    private static final String DOCUMENT = "uuid-082a283c-0731-4160-9490-32921f2f88d2";

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
    private DaImportPlanner planner;
    @Autowired
    private DaImportBuilder builder;
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
    private DaAipAutoLinkService autoLinkService;
    @Autowired
    private DaImportService daImportService;

    private Integer ruleSetId;
    private Integer rootNodeId;
    private Integer firstAipId;
    private Integer manuallyImportedAipId;

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
            ruleSetId = ruleSet.getRuleSetId();
            ArrFundVersion version = arrangementService.createFundWithScenario("DA import", ruleSet, "DAI",
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
                daoRepository.deleteAll();
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
    void firstImport_createsTheLevels_andAttachesTheComponents() throws Exception {
        MetsType mets = mets(ids -> { });
        DaImportBuilder.Outcome outcome = importPackage("aip-first", mets, ead(ids -> { }));
        firstAipId = aipRepository.findByCode("aip-first").getAipId();

        assertEquals(3, outcome.created());
        assertEquals(2, outcome.attached());
        tx().executeWithoutResult(status -> {
            ArrNode group = onlyChild(rootNodeId);
            assertEquals(uuid(GROUP_44), group.getUuid(), "a created level takes the UUID of its div");
            assertEquals("Název věcné skupiny 44 - např. HOSPODÁŘSKÉ PROVOZY, SLUŽBY", name(group));
            ArrNode subgroup = onlyChild(group.getNodeId());
            ArrNode document = onlyChild(subgroup.getNodeId());
            assertEquals(uuid(DOCUMENT), document.getUuid());
            assertEquals("Název dokumentu, věc-doručený dokument", name(document));

            List<ArrDaLink> links = daLinkRepository.findByAipIdAndDeleteChangeIsNull(firstAipId);
            assertEquals(2, links.size());
            assertTrue(links.stream().allMatch(l -> l.getNodeId().equals(document.getNodeId())),
                       "the components are attached to the document");
        });
    }

    @Test
    @Order(2)
    void samePackageAgain_matchesByUuid_andCreatesNothing() throws Exception {
        DaAip aip = tx().execute(status -> aipRepository.findByCode("aip-first"));
        DaImportPlan plan = plan(mets(ids -> { }), ead(ids -> { }));

        DaImportBuilder.Outcome outcome = tx().execute(status -> builder.build(
                aip, nodeRepository.findById(rootNodeId).orElseThrow(), plan));

        assertEquals(0, outcome.created());
        assertEquals(3, outcome.matched());
        assertTrue(outcome.conflicts().isEmpty(), outcome.conflicts().toString());
        tx().executeWithoutResult(status -> assertEquals(2,
                daLinkRepository.findByAipIdAndDeleteChangeIsNull(firstAipId).size(), "links are not duplicated"));
    }

    @Test
    @Order(3)
    void packageFromTheSameFilePlan_sharesTheGroupsByName() throws Exception {
        // another package: its own UUIDs, the same groups of the file plan, a different source id
        Map<String, String> ids = Map.of(GROUP_44, newId(), GROUP_44_4, newId(), DOCUMENT, newId());
        MetsType mets = mets(div -> div.setID(ids.getOrDefault(div.getID(), div.getID())));
        Ead ead = ead(c -> {
            if (GROUP_44.equals(c.getId())) {
                setUnitid(c, "ZDROJ_ID", "ERMS:jiny_identifikator");
            }
            c.setId(ids.getOrDefault(c.getId(), c.getId()));
        });

        DaImportBuilder.Outcome outcome = importPackage("aip-second", mets, ead);

        assertEquals(1, outcome.created(), "only the document of the second package is new");
        assertEquals(2, outcome.matched());
        assertEquals(1, outcome.conflicts().size(), outcome.conflicts().toString());
        assertTrue(outcome.conflicts().get(0).contains("ZP2015_OTHER_ID"), outcome.conflicts().get(0));
        tx().executeWithoutResult(status -> {
            ArrNode group = onlyChild(rootNodeId);
            ArrNode subgroup = onlyChild(group.getNodeId());
            List<ArrNode> documents = children(subgroup.getNodeId());
            assertEquals(2, documents.size());
            assertEquals(uuid(ids.get(DOCUMENT)), documents.get(1).getUuid());
        });
    }

    @Test
    @Order(4)
    void automaticProcessing_matchesAGroupByUuid_andImportsTheLevelsBelowIt() throws Exception {
        // a package whose group 44 is the group created by the first import; below it, a subgroup of
        // the same name under another UUID and a new document
        String subgroup = newId();
        String document = newId();
        String metsXml = resource("METS.xml").replace(GROUP_44_4, subgroup).replace(DOCUMENT, document);
        String eadXml = resource("pruvodka.xml").replace(GROUP_44_4, subgroup).replace(DOCUMENT, document);
        Integer aipId = storedAip(metsXml, eadXml);
        String packageUuid = tx().execute(status -> AipNodeUuids.normalize(aipRepository.findById(aipId).orElseThrow().getCode()));
        List<String> uuids = List.of(packageUuid, uuid("uuid-82f42018-e998-4878-bc48-c951251cf454"), uuid(GROUP_44),
                                     uuid(subgroup), uuid(document));

        assertEquals(1, autoLinkService.linkReceivedAips(Map.of(aipId, uuids)));

        tx().executeWithoutResult(status -> {
            ArrNode group = onlyChild(rootNodeId);
            ArrNode sharedSubgroup = onlyChild(group.getNodeId());
            List<ArrNode> documents = children(sharedSubgroup.getNodeId());
            assertEquals(3, documents.size(), "the document of the third package is added to the shared subgroup");
            ArrNode created = documents.get(2);
            assertEquals(uuid(document), created.getUuid());

            List<ArrDaLink> links = daLinkRepository.findByAipIdAndDeleteChangeIsNull(aipId);
            assertEquals(2, links.size());
            assertTrue(links.stream().allMatch(l -> l.getNodeId().equals(created.getNodeId())
                    && l.getDaDao() != null), "the components are attached to the new document, not the whole AIP");
        });
    }

    @Test
    @Order(5)
    void manualImport_withTheFileplanAsRoot_createsTheRootSeries() throws Exception {
        Map<String, String> ids = Map.of(GROUP_44, newId(), GROUP_44_4, newId(), DOCUMENT, newId());
        String metsXml = replaceAll(resource("METS.xml"), ids);
        String eadXml = replaceAll(resource("pruvodka.xml"), ids);
        Integer aipId = storedAip(metsXml, eadXml);
        manuallyImportedAipId = aipId;

        DaImportBuilder.Outcome outcome = daImportService.importPackage(aipId, rootNodeId, true);

        assertEquals(4, outcome.created(), "file plan, two groups and the document");
        assertEquals(2, outcome.attached());
        tx().executeWithoutResult(status -> {
            List<ArrNode> roots = children(rootNodeId);
            assertEquals(2, roots.size(), "the file plan is a new root series next to the group imported before");
            assertEquals("Spisový plán_název", name(roots.get(1)));
            assertEquals("Název věcné skupiny 44 - např. HOSPODÁŘSKÉ PROVOZY, SLUŽBY",
                         name(onlyChild(roots.get(1).getNodeId())));
        });
    }

    @Test
    @Order(6)
    void manualImport_ofAnAttachedAip_isRefused() {
        Integer attachedAipId = manuallyImportedAipId;

        BusinessException e = assertThrows(BusinessException.class,
                () -> daImportService.importPackage(attachedAipId, rootNodeId, false));
        assertTrue(e.getMessage().contains("již připojen"), e.getMessage());
    }

    /** Gives the divs and units of the package new ids, as another package would have. */
    private static String replaceAll(String xml, Map<String, String> ids) {
        for (Map.Entry<String, String> id : ids.entrySet()) {
            xml = xml.replace(id.getKey(), id.getValue());
        }
        return xml;
    }

    /** An AIP with its metadata package stored in ELZA, as downloading it leaves it. */
    private Integer storedAip(String metsXml, String eadXml) throws IOException {
        Path zip = zip(Map.of("aip/METS.xml", metsXml, "aip/metadata/descriptive/pruvodka.xml", eadXml));
        return tx().execute(status -> {
            MetsType mets;
            try (InputStream is = new java.io.ByteArrayInputStream(metsXml.getBytes(StandardCharsets.UTF_8))) {
                mets = MetsReaderWriter.unmarshal(is);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            DaAip aip = createAip("uuid-" + UUID.randomUUID(), mets);
            DaAipState state = new DaAipState();
            state.setDaAip(aip);
            state.setCreateChange(daChangeRepository.findAll().stream()
                    .filter(c -> c.getDaAip().getAipId().equals(aip.getAipId())).findFirst().orElseThrow());
            state.setAipVersion("1");
            state.setFund(nodeRepository.findById(rootNodeId).orElseThrow().getFund());
            state.setContentType("NSESSS");
            aipStateRepository.save(state);
            daService.createImportLocalCache(state, aip.getDigitalRepository(), AipType.METADATA_BASE, zip, null);
            return aip.getAipId();
        });
    }

    private String resource(String name) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(DIR + name)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Path zip(Map<String, String> entries) throws IOException {
        Path zip = Files.createTempFile("da-import", ".zip");
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

    private DaImportBuilder.Outcome importPackage(String aipCode, MetsType mets, Ead ead) {
        DaImportPlan plan = plan(mets, ead);
        return tx().execute(status -> {
            DaAip aip = createAip(aipCode, mets);
            return builder.build(aip, nodeRepository.findById(rootNodeId).orElseThrow(), plan);
        });
    }

    private DaImportPlan plan(MetsType mets, Ead ead) {
        return tx().execute(status -> planner.plan(mets, ead, "metadata/descriptive/pruvodka.xml", ruleSetId,
                new DaImportPackage("NSESSS", "https://stands.nacr.cz/da/2023/aip.xml", false)).orElseThrow());
    }

    /** The AIP with a logical digital entity for every div, as processing its metadata creates them. */
    private DaAip createAip(String code, MetsType mets) {
        ArrDigitalRepository repository = digitalRepositoryRepository.findAll().stream().findFirst().orElseGet(() -> {
            ArrDigitalRepository r = new ArrDigitalRepository();
            r.setCode("DA-IMPORT");
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
        forEachDiv(mets, div -> daService.createDaDao(aip, change, div.getID(), div.getLABEL(), DaDao.DaoType.LOGICAL));
        return aip;
    }

    private MetsType mets(Consumer<DivType> adjust) throws Exception {
        MetsType mets;
        try (InputStream is = getClass().getResourceAsStream(DIR + "METS.xml")) {
            mets = MetsReaderWriter.unmarshal(is);
        }
        forEachDiv(mets, adjust);
        return mets;
    }

    private Ead ead(Consumer<C> adjust) throws Exception {
        Ead ead;
        try (InputStream is = getClass().getResourceAsStream(DIR + "pruvodka.xml")) {
            ead = EadReaderWriter.unmarshal(is);
        }
        for (Object o : ead.getArchdesc().getAccessrestrictOrAccrualsOrAcqinfo()) {
            if (o instanceof Dsc dsc) {
                dsc.getC().forEach(c -> forEachC(c, adjust));
            }
        }
        return ead;
    }

    private static void forEachDiv(MetsType mets, Consumer<DivType> action) {
        for (StructMapType structMap : mets.getStructMap()) {
            if ("LOGICAL".equals(structMap.getTYPE())) {
                forEachDiv(structMap.getDiv(), action);
            }
        }
    }

    private static void forEachDiv(DivType div, Consumer<DivType> action) {
        action.accept(div);
        div.getDiv().forEach(d -> forEachDiv(d, action));
    }

    private static void forEachC(C c, Consumer<C> action) {
        action.accept(c);
        for (Object o : c.getTheadAndC()) {
            if (o instanceof C child) {
                forEachC(child, action);
            }
        }
    }

    private static void setUnitid(C c, String localType, String value) {
        for (Object o : c.getDid().getMDid()) {
            if (o instanceof Unitid unitid && localType.equals(unitid.getLocaltype())) {
                unitid.getContent().clear();
                unitid.getContent().add((Serializable) value);
            }
        }
    }

    private List<ArrNode> children(Integer nodeId) {
        return levelRepository.findByParentNodeAndDeleteChangeIsNullOrderByPositionAsc(
                nodeRepository.findById(nodeId).orElseThrow()).stream()
                .map(ArrLevel::getNode).collect(Collectors.toList());
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

    private static String newId() {
        return "uuid-" + UUID.randomUUID();
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
