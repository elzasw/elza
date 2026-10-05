package cz.tacr.elza.rules.zp2015;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
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
import cz.tacr.elza.service.da.DaLevelItems;
import cz.tacr.elza.utils.EadReaderWriter;
import gov.loc.mets.v1_11.schema.MetsType;

/**
 * The DA_IMPORT script of ZP2015 on packages of loose files ("Volné soubory"): the package
 * describes one unit, which becomes an item with the components attached.
 *
 * Sample A describes itself in the {@code <archdesc>} paired with its top div; sample B is a
 * wrapper whose only {@code slozka} carries the description. Setup follows
 * {@link Zp2015DaImportPlanTest}.
 */
@ContextConfiguration(classes = ElzaCoreMain.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Zp2015DaLooseFilesPlanTest {

    static final String DIR = "/zp2015/da-volne/";
    static final String LOOSE_FILES = "Volné soubory";
    static final String PROFILE = "https://stands.nacr.cz/da/2023/aip.xml";
    static final String EAD_A = "metadata/descriptive/EAD-INHERENT.xml";
    static final String EAD_B = "metadata/descriptive/DOK_9f38e61d-3761-4ab2-925e-ba3042751564.xml";

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

    private Integer ruleSetId;

    @BeforeAll
    void loadRulesOnce() {
        helperTestService.deleteTables(false);
        startupService.startNow();
        helperTestService.loadPackage("CZ_BASE", "package-cz-base");
        helperTestService.loadPackage("ZP2015", "rules-cz-zp2015");
        ruleSetId = staticDataService.getData().getRuleSetByCode("ZP2015").getRuleSetId();
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

    @Test
    void packageDescribingItself_isTheItem() throws Exception {
        DaImportPlan plan = plan("a", EAD_A);

        assertEquals(1, plan.getRoots().size());
        DaImportPlan.Node item = plan.getRoots().get(0);
        assertEquals("ds_1", item.getDivId());
        assertEquals(DaImportResult.Decision.LEVEL, item.getDecision());
        assertEquals("ZP2015_LEVEL_ITEM", spec(item, "ZP2015_LEVEL_TYPE"));
        assertEquals("Digitální fotografie 3", string(item, "ZP2015_NAME"), "the title of the archdesc");
        assertEquals(List.of("ZP2015_OTHERID_SOURCEID:eskartace_2150:d39c947a-7870-4c00-aa6b-aff7f4c9e01d"),
                     otherIds(item));
        assertEquals("2010-01-01T00:00:00",
                     ((ArrDataUnitdate) item(item, "ZP2015_UNIT_DATE").data()).getValueFrom());
        assertEquals(List.of("ZP2015_OTHER_ID:ZP2015_OTHERID_SOURCEID"), matchBy(item),
                     "the next version of the package finds the unit by its source id");

        assertEquals(2, item.getChildren().size());
        assertTrue(item.getChildren().stream()
                           .allMatch(c -> c.getDecision() == DaImportResult.Decision.ATTACH));
    }

    @Test
    void wrapperPackage_isSkipped_itsFolderIsTheItem() throws Exception {
        DaImportPlan plan = plan("b", EAD_B);

        assertEquals(1, plan.getRoots().size(), "the wrapper has no description and is skipped");
        DaImportPlan.Node item = plan.getRoots().get(0);
        assertEquals("uuid-f793d499-430b-4342-b5b6-139343262bf8", item.getDivId());
        assertEquals("ZP2015_LEVEL_ITEM", spec(item, "ZP2015_LEVEL_TYPE"));
        assertEquals("Posudek vojáka; 1938; 5030", string(item, "ZP2015_NAME"));
        assertEquals(List.of("ZP2015_OTHERID_SIG:sig"), otherIds(item));
        assertTrue(item.getMatchBy().isEmpty(), "no source id - the unit is recognized by its UUID");
        assertEquals(1, item.getChildren().size());
        assertEquals(DaImportResult.Decision.ATTACH, item.getChildren().get(0).getDecision());
    }

    private DaImportPlan plan(String sample, String eadHref) throws Exception {
        MetsType mets;
        try (InputStream is = getClass().getResourceAsStream(DIR + sample + "/METS.xml")) {
            mets = MetsReaderWriter.unmarshal(is);
        }
        Ead ead;
        String eadFile = eadHref.substring(eadHref.lastIndexOf('/') + 1);
        try (InputStream is = getClass().getResourceAsStream(DIR + sample + "/" + eadFile)) {
            ead = EadReaderWriter.unmarshal(is);
        }
        return new TransactionTemplate(txManager).execute(status -> planner
                .plan(mets, ead, eadHref, ruleSetId, new DaImportPackage(LOOSE_FILES, PROFILE, false))
                .orElseThrow());
    }

    private static DaLevelItems.Item item(DaImportPlan.Node node, String itemTypeCode) {
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
        return node.getMatchBy().stream().map(Object::toString).collect(Collectors.toList());
    }

    private static List<String> otherIds(DaImportPlan.Node node) {
        return node.getItems().stream()
                .filter(i -> i.itemType().getCode().equals("ZP2015_OTHER_ID"))
                .map(i -> i.itemSpec().getCode() + ":" + ((ArrDataString) i.data()).getStringValue())
                .collect(Collectors.toList());
    }
}
