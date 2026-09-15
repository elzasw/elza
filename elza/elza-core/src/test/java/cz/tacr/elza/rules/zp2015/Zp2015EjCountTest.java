package cz.tacr.elza.rules.zp2015;

import static cz.tacr.elza.rules.zp2015.Zp2015EjFixture.node;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import cz.tacr.elza.ElzaCoreMain;
import cz.tacr.elza.bulkaction.BulkActionConfigManager;
import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.bulkaction.generator.MultiActionConfig;
import cz.tacr.elza.bulkaction.generator.multiple.TypeLevel;
import cz.tacr.elza.bulkaction.generator.multiple.UnitCountAction;
import cz.tacr.elza.bulkaction.generator.multiple.UnitCountConfig;
import cz.tacr.elza.bulkaction.generator.result.UnitCountActionResult;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.other.HelperTestService;
import cz.tacr.elza.packageimport.PackageService;
import cz.tacr.elza.service.StartupService;
import cz.tacr.elza.service.StructObjService;
import cz.tacr.elza.domain.table.ElzaRow;

/**
 * Tests the evidence-unit counting of the ZP2015 rules.
 *
 * <p>The counters are configured in ZP2015_INTRO.yaml as an ordered list of aggregators, and the
 * order decides the result: an aggregator marked {@code stopLevelProcessing} claims the level and
 * the ones after it never see it. Nothing expressed that in a test, so a change to the ordering -
 * or to whether an aggregator stops the level or the whole subtree - stayed invisible until
 * someone read the counts on a real fund.
 *
 * <p>The package is imported once for the class and the levels are built in memory, so each further
 * scenario costs nothing. The only thing reached for outside is the content of a storage unit,
 * which the counters read through {@link StructObjService} - stubbed here so that a packet is one
 * line of the scenario rather than a row in the database.
 *
 * <p>Context configuration is deliberately identical to {@code AbstractTest}: the Spring test
 * context is cached by its configuration, so matching it keeps this class on the context the rest
 * of the suite already started. It is not derived from AbstractTest because that class reloads the
 * packages in {@code @BeforeEach}, which would repeat the import for every scenario.
 */
@ContextConfiguration(classes = ElzaCoreMain.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Zp2015EjCountTest {

    /** Storage unit ids used by the scenarios; the stub answers what each one contains. */
    private static final int PACKET_CARTON = 101;
    private static final int PACKET_FASCICLE = 102;
    private static final int PACKET_BOX = 103;

    @Autowired
    private ApplicationContext appCtx;
    @Autowired
    private HelperTestService helperTestService;
    @Autowired
    private StaticDataService staticDataService;
    @Autowired
    private StartupService startupService;
    @Autowired
    private BulkActionConfigManager bulkActionConfigManager;
    @Autowired
    private PackageService packageService;

    private StaticDataProvider sdp;
    private UnitCountConfig unitCountConfig;
    private StructObjService structObjStub;

    @BeforeAll
    void loadRulesOnce() {
        helperTestService.deleteTables(false);
        startupService.startNow();
        helperTestService.loadPackage("CZ_BASE", "package-cz-base");
        helperTestService.loadPackage("ZP2015", "rules-cz-zp2015");
        sdp = staticDataService.getData();

        MultiActionConfig intro = (MultiActionConfig) bulkActionConfigManager.get("ZP2015_INTRO");
        assertNotNull(intro, "ZP2015_INTRO bulk action is not registered");
        unitCountConfig = intro.getActions().stream()
                .filter(UnitCountConfig.class::isInstance)
                .map(UnitCountConfig.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("ZP2015_INTRO has no UnitCount action"));

        structObjStub = stubStorageUnits();
    }

    /**
     * Removes ZP2015 again. The rule packages are not among the tables the shared helper wipes, so
     * they live for the whole run of the suite - a package left behind here would be visible to
     * every class after this one, and the classes that follow are written against the rules they
     * load themselves.
     */
    @AfterAll
    void unloadRules() {
        packageService.deletePackage("ZP2015");
        staticDataService.refreshForCurrentThread();
        startupService.stop();
    }

    /** Each storage unit holds one packet type - what the counters map onto an evidence unit. */
    private StructObjService stubStorageUnits() {
        ItemType packetType = sdp.getItemTypeByCode("ZP2015_PACKET_TYPE");
        Map<Integer, List<ArrStructuredItem>> content = new HashMap<>();
        content.put(PACKET_CARTON, List.of(packetItem(packetType, "ZP2015_PACKET_TYPE_KAR")));
        content.put(PACKET_FASCICLE, List.of(packetItem(packetType, "ZP2015_PACKET_TYPE_FAS")));
        content.put(PACKET_BOX, List.of(packetItem(packetType, "ZP2015_PACKET_TYPE_BAL")));

        StructObjService stub = mock(StructObjService.class);
        when(stub.findByStructObjIdAndDeleteChangeIsNullFetchData(anyInt()))
                .thenAnswer(inv -> content.getOrDefault(inv.getArgument(0), List.of()));
        return stub;
    }

    private ArrStructuredItem packetItem(ItemType packetType, String specCode) {
        RulItemSpec spec = packetType.getItemSpecByCode(specCode);
        assertNotNull(spec, "Unknown packet specification: " + specCode);
        ArrStructuredItem item = new ArrStructuredItem();
        item.setItemTypeId(packetType.getItemTypeId());
        item.setItemSpecId(spec.getItemSpecId());
        item.setPosition(1);
        return item;
    }

    /**
     * Runs the configured counters over the levels and returns the resulting table as pairs of
     * unit name and count, which is what the intro of a finding aid shows.
     */
    private Map<String, String> countsOf(List<LevelWithItems> levels) {
        UnitCountAction action = appCtx.getBean(UnitCountAction.class, unitCountConfig);
        ReflectionTestUtils.setField(action, "structObjService", structObjStub);

        ArrBulkActionRun run = new ArrBulkActionRun();
        run.setChange(new ArrChange());
        action.init(null, run);

        for (LevelWithItems level : levels) {
            action.apply(level, TypeLevel.CHILD);
        }

        UnitCountActionResult result = (UnitCountActionResult) action.getResult();
        Map<String, String> counts = new LinkedHashMap<>();
        for (ElzaRow row : result.getTable().getRows()) {
            Map<String, String> v = row.getValues();
            counts.put(v.get(unitCountConfig.getOutputColumnUnitName()),
                       v.get(unitCountConfig.getOutputColumnUnitCount()));
        }
        return counts;
    }

    /**
     * An unregistered single item in a carton is counted as the carton it lies in, not as an item
     * of its own - it is the storage unit that forms the evidence unit here.
     */
    @Test
    void anUnregisteredItemInACartonIsCountedAsTheCarton() {
        Zp2015EjFixture f = new Zp2015EjFixture(sdp);
        Zp2015EjFixture.Node root = node("root")
                .child(node("unregistered item")
                        .with(f.enumItem("ZP2015_LEVEL_TYPE", "ZP2015_LEVEL_ITEM"))
                        .with(f.enumItem("ZP2015_UNIT_TYPE", "ZP2015_UNIT_TYPE_OTHER"))
                        .with(f.storageRef("ZP2015_STORAGE_ID", PACKET_CARTON)));

        assertEquals(Map.of("kar", "1"), countsOf(f.levels(root)));
    }

    /**
     * A single item under a series is counted by the type of archival material it is, not by what
     * it is stored in - a different aggregator, reached only because the one above it did not
     * claim the level.
     */
    @Test
    void aSingleItemUnderASeriesIsCountedByItsUnitType() {
        Zp2015EjFixture f = new Zp2015EjFixture(sdp);
        Zp2015EjFixture.Node root = node("root")
                .child(node("series")
                        .with(f.enumItem("ZP2015_LEVEL_TYPE", "ZP2015_LEVEL_SERIES"))
                        .child(node("item")
                                .with(f.enumItem("ZP2015_LEVEL_TYPE", "ZP2015_LEVEL_ITEM"))
                                .with(f.enumItem("ZP2015_UNIT_TYPE", "ZP2015_UNIT_TYPE_LIO"))));

        assertEquals(Map.of("lio", "1"), countsOf(f.levels(root)));
    }

    /**
     * Under a folder holding a quantity, the storage units of the nodes inside it are counted and
     * the nodes themselves are not counted as single items - the behaviour #9766 introduced by
     * replacing "skip the whole subtree" with "stop this level only".
     */
    @Test
    void storageUnitsUnderAQuantityFolderAreCounted() {
        Zp2015EjFixture f = new Zp2015EjFixture(sdp);
        Zp2015EjFixture.Node root = node("root")
                .child(node("quantity folder")
                        .with(f.enumItem("ZP2015_FOLDER_TYPE", "ZP2015_FOLDER_UNITS"))
                        .with(f.storageRef("ZP2015_STORAGE_ID", PACKET_CARTON))
                        .child(node("attachment")
                                .with(f.enumItem("ZP2015_LEVEL_TYPE", "ZP2015_LEVEL_ITEM"))
                                .with(f.storageRef("ZP2015_STORAGE_ID", PACKET_FASCICLE))));

        Map<String, String> counts = countsOf(f.levels(root));
        assertEquals("1", counts.get("kar"), "the carton of the folder itself");
        assertEquals("1", counts.get("fas"), "the fascicle of the attachment, counted as bulk");
    }

    /** The same storage unit referenced by two levels is one evidence unit, not two. */
    @Test
    void oneStorageUnitSharedByTwoLevelsIsCountedOnce() {
        Zp2015EjFixture f = new Zp2015EjFixture(sdp);
        Zp2015EjFixture.Node root = node("root")
                .child(node("first")
                        .with(f.enumItem("ZP2015_LEVEL_TYPE", "ZP2015_LEVEL_ITEM"))
                        .with(f.enumItem("ZP2015_UNIT_TYPE", "ZP2015_UNIT_TYPE_OTHER"))
                        .with(f.storageRef("ZP2015_STORAGE_ID", PACKET_CARTON)))
                .child(node("second")
                        .with(f.enumItem("ZP2015_LEVEL_TYPE", "ZP2015_LEVEL_ITEM"))
                        .with(f.enumItem("ZP2015_UNIT_TYPE", "ZP2015_UNIT_TYPE_OTHER"))
                        .with(f.storageRef("ZP2015_STORAGE_ID", PACKET_CARTON)));

        assertEquals(Map.of("kar", "1"), countsOf(f.levels(root)));
    }

    /**
     * A record marked invalid forms no evidence unit, whatever it is stored in. The flag carries no
     * specification - the rules match it by its presence alone.
     */
    @Test
    void anInvalidRecordIsNotCounted() {
        Zp2015EjFixture f = new Zp2015EjFixture(sdp);
        Zp2015EjFixture.Node root = node("root")
                .child(node("invalid")
                        .with(f.enumItem("ZP2015_LEVEL_TYPE", "ZP2015_LEVEL_ITEM"))
                        .with(f.enumItem("ZP2015_UNIT_TYPE", "ZP2015_UNIT_TYPE_OTHER"))
                        .with(f.flagItem("ZP2015_INVALID_RECORD"))
                        .with(f.storageRef("ZP2015_STORAGE_ID", PACKET_BOX)));

        assertEquals(new ArrayList<String>(), new ArrayList<>(countsOf(f.levels(root)).keySet()));
    }
}
