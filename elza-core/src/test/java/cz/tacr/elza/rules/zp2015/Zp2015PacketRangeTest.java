package cz.tacr.elza.rules.zp2015;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import cz.tacr.elza.ElzaCoreMain;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.other.HelperTestService;
import cz.tacr.elza.packageimport.PackageService;
import cz.tacr.elza.repository.PackageRepository;
import cz.tacr.elza.service.GroovyScriptService.GroovyScriptFile;
import cz.tacr.elza.service.StartupService;
import cz.tacr.elza.service.StructObjValueService;
import cz.tacr.elza.service.StructObjValueService.Result;

/**
 * Tests the value and sort value which the ZP2015 scripts generate for a storage unit with a range
 * ("9-10 karton").
 *
 * <p>The sort value is also the key of the duplicate check of storage units: two units of a fund
 * with the same sort value are both marked as an error. A range therefore has to differ from a
 * single unit with the same end number, while two identical ranges still have to collide.
 *
 * <p>Both serialization scripts are covered - the base one and the one of the extension
 * "ZP2015_PACKET_EXTTYPE", which adds the packet type to the value.
 */
@ContextConfiguration(classes = ElzaCoreMain.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Zp2015PacketRangeTest {

    private static final List<String> SCRIPTS = List.of("Packet.groovy", "Packet_ExtType.groovy");

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
    private ResourcePathResolver resourcePathResolver;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private StaticDataProvider sdp;
    private Path scriptDir;

    @BeforeAll
    void loadRulesOnce() {
        helperTestService.deleteTables(false);
        startupService.startNow();
        helperTestService.loadPackage("CZ_BASE", "package-cz-base");
        helperTestService.loadPackage("ZP2015", "rules-cz-zp2015");
        sdp = staticDataService.getData();
        scriptDir = new TransactionTemplate(transactionManager)
                .execute(tx -> resourcePathResolver.getGroovyDir(packageRepository.findByCode("ZP2015")));
    }

    /** Removes ZP2015 again, so that the classes after this one see only the packages they load. */
    @AfterAll
    void unloadRules() {
        packageService.deletePackage("ZP2015");
        staticDataService.refreshForCurrentThread();
        startupService.stop();
    }

    @Test
    void aRangeDiffersFromTheUnitWithItsEndNumber() {
        for (String script : SCRIPTS) {
            Result range = generate(script, 9, 10);
            Result single = generate(script, null, 10);

            assertTrue(range.getValue().startsWith("9-10"), script + ": " + range.getValue());
            assertTrue(single.getValue().startsWith("10"), script + ": " + single.getValue());
            assertNotEquals(single.getSortValue(), range.getSortValue(), script);
        }
    }

    @Test
    void twoIdenticalRangesAreStillDuplicates() {
        for (String script : SCRIPTS) {
            assertEquals(generate(script, 9, 10).getSortValue(), generate(script, 9, 10).getSortValue(), script);
        }
    }

    @Test
    void aRangeSortsBetweenItsStartAndTheNextUnit() {
        for (String script : SCRIPTS) {
            String nine = generate(script, null, 9).getSortValue();
            String range = generate(script, 9, 10).getSortValue();
            String ten = generate(script, null, 10).getSortValue();
            String eight = generate(script, null, 8).getSortValue();

            assertTrue(eight.compareTo(range) < 0, script + ": " + eight + " < " + range);
            assertTrue(nine.compareTo(range) < 0, script + ": " + nine + " < " + range);
            assertTrue(range.compareTo(ten) < 0, script + ": " + range + " < " + ten);
        }
    }

    /** Runs the serialization script for a carton with the given range start and number. */
    private Result generate(String script, Integer startNumber, int number) {
        List<ArrStructuredItem> items = new ArrayList<>();
        if (startNumber != null) {
            items.add(integerItem("ZP2015_PACKET_START_NUMBER", startNumber));
        }
        items.add(integerItem("ZP2015_PACKET_NUMBER", number));
        items.add(cartonItem());

        File file = scriptDir.resolve(script).toFile();
        assertTrue(file.isFile(), "Missing script: " + file);

        Result result = new Result();
        Map<String, Object> input = new HashMap<>();
        input.put("ITEMS", items);
        input.put("RESULT", result);
        input.put(StructObjValueService.GROOVY_STRUCTURE_TYPE_SETTINGS, null);
        new GroovyScriptFile(file).evaluate(input);
        return result;
    }

    private ArrStructuredItem integerItem(String itemTypeCode, int value) {
        ArrStructuredItem item = new ArrStructuredItem();
        item.setItemType(sdp.getItemTypeByCode(itemTypeCode).getEntity());
        item.setData(new ArrDataInteger(value));
        return item;
    }

    private ArrStructuredItem cartonItem() {
        ItemType packetType = sdp.getItemTypeByCode("ZP2015_PACKET_TYPE");
        RulItemSpec spec = packetType.getItemSpecByCode("ZP2015_PACKET_TYPE_KAR");
        assertNotNull(spec);
        ArrStructuredItem item = new ArrStructuredItem();
        item.setItemType(packetType.getEntity());
        item.setItemSpec(spec);
        return item;
    }
}
