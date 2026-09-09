package cz.tacr.elza.dataexchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.AbstractServiceTest;
import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.dataexchange.input.DEImportParams;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApPartRepository;

/**
 * Import of access point parts with a parent part and with relations to another access
 * point created by the same file.
 *
 * Both cases have to be resolved while the file is being read, i.e. before the queued
 * entities are stored:
 * <ul>
 * <li>a part declares its parent part by {@code pid} and the parent part may be written
 * before its child parts (this is the order used by the export),</li>
 * <li>a description item references an access point by {@code apid}, which may be created
 * by the same file as long as it is declared earlier.</li>
 * </ul>
 * Import batch size is intentionally small, so that both the access points and the parent
 * part references are stored while the access point is still being read.
 */
public class DataExchangeApPartsTest extends AbstractServiceTest {

    private static final String AP_PARTS_XML = "ap-parts-import.xml";

    /** Access points of {@link #AP_PARTS_XML}. */
    private static final String AP_UUID_1 = "8f1d9b3a-0000-4000-8000-00000000a001";

    private static final String AP_UUID_2 = "8f1d9b3a-0000-4000-8000-00000000a002";

    private static final int BATCH_SIZE = 2;

    @Autowired
    private ApAccessPointRepository accessPointRepository;

    @Autowired
    private ApPartRepository partRepository;

    @Autowired
    private ApItemRepository itemRepository;

    @Test
    public void importApPartsTest() {
        importApParts();

        txExec(() -> {
            ApAccessPoint ap1 = accessPointRepository.findAccessPointByUuid(AP_UUID_1);
            ApAccessPoint ap2 = accessPointRepository.findAccessPointByUuid(AP_UUID_2);
            assertNotNull(ap1);
            assertNotNull(ap2);

            List<ApPart> parts = partRepository.findValidPartByAccessPoint(ap2);
            assertEquals(6, parts.size());

            List<ApPart> namePats = partsOfType(parts, "PT_NAME");
            List<ApPart> eventParts = partsOfType(parts, "PT_EVENT");
            List<ApPart> relParts = partsOfType(parts, "PT_REL");
            assertEquals(1, namePats.size());
            assertEquals(2, eventParts.size());
            assertEquals(3, relParts.size());

            namePats.forEach(part -> assertNull(part.getParentPartId()));
            eventParts.forEach(part -> assertNull(part.getParentPartId()));

            // relation parts are subordinate to the event parts of the same access point
            Map<Integer, Long> relCountByParent = relParts.stream()
                    .collect(Collectors.groupingBy(this::parentPartId, Collectors.counting()));
            assertEquals(2, relCountByParent.size());
            eventParts.forEach(eventPart -> assertNotNull(relCountByParent.get(eventPart.getPartId())));
            assertEquals(3, relCountByParent.values().stream().mapToLong(Long::longValue).sum());

            // every relation references the access point created by the same file
            for (ApPart relPart : relParts) {
                List<ApItem> items = itemRepository.findValidItemsByPart(relPart);
                assertEquals(1, items.size());
                ArrDataRecordRef data = HibernateUtils.unproxy(items.get(0).getData());
                assertEquals(ap1.getAccessPointId(), data.getRecordId());
            }
            return null;
        });
    }

    private void importApParts() {
        ApScope scope = apService.getApScope(1);
        Objects.requireNonNull(scope);

        File file = getResourceFile(AP_PARTS_XML);
        try (FileInputStream fis = new FileInputStream(file)) {
            deImportService.importData(fis, new DEImportParams(scope.getScopeId(), BATCH_SIZE, 10000, null, null));
        } catch (IOException e) {
            Assertions.fail(e.fillInStackTrace().toString());
        }
    }

    private Integer parentPartId(ApPart part) {
        Integer parentPartId = part.getParentPartId();
        assertNotNull(parentPartId);
        return parentPartId;
    }

    private List<ApPart> partsOfType(List<ApPart> parts, String partTypeCode) {
        return parts.stream().filter(part -> partTypeCode.equals(part.getPartType().getCode())).toList();
    }

    private <T> T txExec(Supplier<T> work) {
        return new TransactionTemplate(txManager).execute(tx -> work.get());
    }
}
