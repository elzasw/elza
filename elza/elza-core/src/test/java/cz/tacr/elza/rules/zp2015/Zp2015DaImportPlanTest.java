package cz.tacr.elza.rules.zp2015;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.archivists.ead3.schema.Ead;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.lightcomp.kads.mets.MetsReaderWriter;

import cz.tacr.elza.ElzaCoreMain;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.other.HelperTestService;
import cz.tacr.elza.packageimport.PackageService;
import cz.tacr.elza.service.StartupService;
import cz.tacr.elza.service.da.DaImportPackage;
import cz.tacr.elza.service.da.DaImportPlan;
import cz.tacr.elza.service.da.DaImportPlanner;
import cz.tacr.elza.service.da.DaImportResult;
import cz.tacr.elza.utils.EadReaderWriter;
import gov.loc.mets.v1_11.schema.MetsType;

/**
 * The DA_IMPORT script of ZP2015 on an NSESSS package (the sample of the digital archive): which
 * divs become levels, which are attached, and what the levels get from the EAD.
 *
 * Context configuration is identical to {@code AbstractTest} so the cached Spring context is
 * shared; the package is loaded once for the class and unloaded again, see
 * {@link Zp2015EjCountTest}.
 */
@ContextConfiguration(classes = ElzaCoreMain.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Zp2015DaImportPlanTest {

    private static final String DIR = "/zp2015/da-nsesss/";
    private static final String NSESSS = "NSESSS";
    private static final String PROFILE = "https://stands.nacr.cz/da/2023/aip.xml";

    @Autowired
    private HelperTestService helperTestService;
    @Autowired
    private StaticDataService staticDataService;
    @Autowired
    private StartupService startupService;
    @Autowired
    private PackageService packageService;
    @Autowired
    private DaImportPlanner planner;
    @Autowired
    private PlatformTransactionManager txManager;

    private MetsType mets;
    private Ead ead;
    private Integer ruleSetId;

    @BeforeAll
    void loadRulesOnce() throws Exception {
        helperTestService.deleteTables(false);
        startupService.startNow();
        helperTestService.loadPackage("CZ_BASE", "package-cz-base");
        helperTestService.loadPackage("ZP2015", "rules-cz-zp2015");
        ruleSetId = staticDataService.getData().getRuleSetByCode("ZP2015").getRuleSetId();

        try (InputStream is = getClass().getResourceAsStream(DIR + "METS.xml")) {
            mets = MetsReaderWriter.unmarshal(is);
        }
        try (InputStream is = getClass().getResourceAsStream(DIR + "pruvodka.xml")) {
            ead = EadReaderWriter.unmarshal(is);
        }
    }

    @AfterAll
    void unloadRules() {
        try {
            packageService.deletePackage("ZP2015");
            staticDataService.refreshForCurrentThread();
        } finally {
            startupService.stop();
        }
    }

    private DaImportPlan plan(boolean fileplanAsRoot) {
        return new TransactionTemplate(txManager).execute(status -> planner
                .plan(mets, ead, "metadata/descriptive/pruvodka.xml", ruleSetId,
                      new DaImportPackage(NSESSS, PROFILE, fileplanAsRoot))
                .orElseThrow());
    }

    @Test
    void fileplanIsSkipped_groupsBelowItTakeItsPlace() {
        DaImportPlan plan = plan(false);

        assertEquals(1, plan.getRoots().size());
        DaImportPlan.Node group = plan.getRoots().get(0);
        assertEquals("uuid-ac73f202-a2a0-4309-9ce7-6c1e426be654", group.getDivId());
        assertEquals(DaImportResult.Decision.LEVEL, group.getDecision());
        assertEquals("ZP2015_LEVEL_SERIES", spec(group, "ZP2015_LEVEL_TYPE"));
        assertEquals("Název věcné skupiny 44 - např. HOSPODÁŘSKÉ PROVOZY, SLUŽBY", string(group, "ZP2015_NAME"));
        assertEquals(List.of("ZP2015_LEVEL_TYPE", "ZP2015_NAME"), matchBy(group),
                     "a group of the file plan is recognized by its name");
        assertEquals(List.of("ZP2015_OTHERID_STORAGE_ID:44", "ZP2015_OTHERID_SOURCEID:ERMS:identifikátor_vecna_skupina_44"),
                     otherIds(group));
    }

    @Test
    void documentIsAnItem_itsComponentsAreAttached() {
        DaImportPlan.Node subgroup = plan(false).getRoots().get(0).getChildren().get(0);
        assertEquals("ZP2015_LEVEL_SERIES", spec(subgroup, "ZP2015_LEVEL_TYPE"));

        DaImportPlan.Node document = subgroup.getChildren().get(0);
        assertEquals("uuid-082a283c-0731-4160-9490-32921f2f88d2", document.getDivId());
        assertEquals("ZP2015_LEVEL_ITEM", spec(document, "ZP2015_LEVEL_TYPE"));
        assertEquals("Název dokumentu, věc-doručený dokument", string(document, "ZP2015_NAME"));
        assertTrue(document.getMatchBy().isEmpty(), "a document is never shared by packages");
        // PORADOVE_CISLO_PUVODNI has no counterpart in ZP2015 and is not taken over
        assertEquals(List.of("ZP2015_OTHERID_CJ:č.j.DDFN-101/2009"), otherIds(document));

        ArrDataUnitdate date = (ArrDataUnitdate) item(document, "ZP2015_UNIT_DATE").data();
        assertEquals("YM-D", date.getFormat());
        assertEquals("2009-08-01T00:00:00", date.getValueFrom());
        assertEquals("2009-12-13T23:59:59", date.getValueTo());

        assertEquals(2, document.getChildren().size());
        assertTrue(document.getChildren().stream()
                           .allMatch(c -> c.getDecision() == DaImportResult.Decision.ATTACH && c.getItems().isEmpty()));
    }

    @Test
    void fileplanAsRoot_isTheRootSeries() {
        DaImportPlan plan = plan(true);

        assertEquals(1, plan.getRoots().size());
        DaImportPlan.Node fileplan = plan.getRoots().get(0);
        assertEquals("uuid-82f42018-e998-4878-bc48-c951251cf454", fileplan.getDivId());
        assertEquals("ZP2015_LEVEL_SERIES", spec(fileplan, "ZP2015_LEVEL_TYPE"));
        assertEquals("Spisový plán_název", string(fileplan, "ZP2015_NAME"));
        assertEquals(List.of("ZP2015_LEVEL_TYPE", "ZP2015_NAME"), matchBy(fileplan));
        assertEquals("uuid-ac73f202-a2a0-4309-9ce7-6c1e426be654", fileplan.getChildren().get(0).getDivId());
    }

    /**
     * The shape of a real NSESSS package: the divs are typed by readable names ("věcná skupina")
     * instead of codes, and the <fileplan> has no id to pair it with its div.
     */
    @Test
    void readableDivTypes_andFileplanWithoutId() throws Exception {
        DaImportPlan plan = realShapePlan(null);

        assertEquals(1, plan.getRoots().size(), "the file plan is skipped even without its id");
        DaImportPlan.Node group = plan.getRoots().get(0);
        assertEquals("uuid-ac73f202-a2a0-4309-9ce7-6c1e426be654", group.getDivId());
        assertEquals("ZP2015_LEVEL_SERIES", spec(group, "ZP2015_LEVEL_TYPE"));
        DaImportPlan.Node document = group.getChildren().get(0).getChildren().get(0);
        assertEquals("ZP2015_LEVEL_ITEM", spec(document, "ZP2015_LEVEL_TYPE"));
        assertTrue(document.getChildren().stream()
                           .allMatch(c -> c.getDecision() == DaImportResult.Decision.ATTACH));
    }

    /** What failed on the real package: the structure below the selected file plan level. */
    @Test
    void readableDivTypes_belowTheFileplan() throws Exception {
        DaImportPlan plan = realShapePlan("82f42018-e998-4878-bc48-c951251cf454");

        assertEquals(1, plan.getRoots().size());
        assertEquals("ZP2015_LEVEL_SERIES", spec(plan.getRoots().get(0), "ZP2015_LEVEL_TYPE"));
    }

    /**
     * The package says what its parts are: the TYPE of the div in METS decides; the EAD of the
     * originator is only supporting evidence.
     */
    @Test
    void metsDecides_whenTheEadSaysOtherwise() throws Exception {
        String metsXml = resource("METS.xml").replace(
                "LABEL=\"Název věcné skupiny 44 - např. HOSPODÁŘSKÉ PROVOZY, SLUŽBY\" TYPE=\"vecnaskp\"",
                "LABEL=\"Název věcné skupiny 44 - např. HOSPODÁŘSKÉ PROVOZY, SLUŽBY\" TYPE=\"spis\"");
        MetsType changed = MetsReaderWriter.unmarshal(new ByteArrayInputStream(metsXml.getBytes(StandardCharsets.UTF_8)));

        DaImportPlan plan = new TransactionTemplate(txManager).execute(status -> planner
                .plan(changed, ead, "metadata/descriptive/pruvodka.xml", ruleSetId,
                      new DaImportPackage(NSESSS, PROFILE, false))
                .orElseThrow());

        assertEquals("ZP2015_LEVEL_FOLDER", spec(plan.getRoots().get(0), "ZP2015_LEVEL_TYPE"),
                     "the div says spis, the EAD says vecnaskp - the div wins");
    }

    private DaImportPlan realShapePlan(String startUuid) throws Exception {
        String metsXml = resource("METS.xml")
                .replace("TYPE=\"spisplan\"", "TYPE=\"spisový plán\"")
                .replace("TYPE=\"vecnaskp\"", "TYPE=\"věcná skupina\"");
        String eadXml = resource("pruvodka.xml")
                .replace(" id=\"uuid-82f42018-e998-4878-bc48-c951251cf454\"", "");
        MetsType realMets = MetsReaderWriter.unmarshal(new ByteArrayInputStream(metsXml.getBytes(StandardCharsets.UTF_8)));
        Ead realEad = EadReaderWriter.unmarshal(new ByteArrayInputStream(eadXml.getBytes(StandardCharsets.UTF_8)));
        DaImportPackage importPackage = new DaImportPackage(NSESSS, PROFILE, false);
        return new TransactionTemplate(txManager).execute(status -> (startUuid == null
                ? planner.plan(realMets, realEad, "metadata/descriptive/pruvodka.xml", ruleSetId, importPackage)
                : planner.planBelow(realMets, realEad, "metadata/descriptive/pruvodka.xml", ruleSetId, importPackage,
                                    startUuid))
                .orElseThrow());
    }

    private String resource(String name) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(DIR + name)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static DaImportResult.Item item(DaImportPlan.Node node, String itemTypeCode) {
        return node.getItems().stream()
                .filter(i -> i.itemType().getCode().equals(itemTypeCode))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no " + itemTypeCode + " on " + node.getDivId()));
    }

    private static String spec(DaImportPlan.Node node, String itemTypeCode) {
        return item(node, itemTypeCode).itemSpec().getCode();
    }

    private static String string(DaImportPlan.Node node, String itemTypeCode) {
        return ((ArrDataString) item(node, itemTypeCode).data()).getStringValue();
    }

    private static List<String> matchBy(DaImportPlan.Node node) {
        return node.getMatchBy().stream().map(t -> t.getCode()).collect(Collectors.toList());
    }

    private static List<String> otherIds(DaImportPlan.Node node) {
        return node.getItems().stream()
                .filter(i -> i.itemType().getCode().equals("ZP2015_OTHER_ID"))
                .map(i -> i.itemSpec().getCode() + ":" + ((ArrDataString) i.data()).getStringValue())
                .collect(Collectors.toList());
    }
}
