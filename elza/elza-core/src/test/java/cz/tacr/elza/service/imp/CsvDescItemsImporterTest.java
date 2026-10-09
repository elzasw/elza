package cz.tacr.elza.service.imp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.AbstractServiceTest;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataBit;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataDate;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.BatchState;
import cz.tacr.elza.domain.ImpBatchDescCsv;
import cz.tacr.elza.domain.ImpItem;
import cz.tacr.elza.domain.ItemState;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.service.FundLevelService.AddLevelDirection;
import cz.tacr.elza.service.ImpBatchService;
import cz.tacr.elza.service.ImpItemService;

/**
 * Shared CSV import of description items (Table view and batch import), against the
 * SIMPLE-DEV rules.
 */
public class CsvDescItemsImporterTest extends AbstractServiceTest {

    private static final IntConsumer ANY_FUND = fundId -> { };

    private static final String BOM = "﻿";

    @Autowired
    private CsvDescItemsImporter importer;

    @Autowired
    private ImpBatchService batchService;

    @Autowired
    private ImpItemService itemService;

    @Autowired
    private FundLevelService fundLevelService;

    @Test
    public void importsEveryDataTypeInOneChange() throws IOException {
        FundInfo fund = newFund("CSV-all-types");
        ArrNode root = fund.getRootNode();

        String csv = BOM + root.getUuid()
                + ";SRD_LEVEL_TYPE;SRD_LEVEL_ROOT"
                + ";SRD_NAME;Name"
                + ";SRD_TITLE;Title"
                + ";SRD_LEGEND;Legend"
                + ";SRD_SERIAL_NUMBER;42"
                + ";SRD_COLL_EXTENT_LENGTH;1.25"
                + ";SRD_UNIT_ID;U-1"
                + ";SRD_UNIT_DATE;1920"
                + ";SRD_SIMPLE_DATE;2020-05-17"
                + ";SRD_POSITION;POINT (14.4 50.08)"
                + ";SRD_STATISTICS;\"{\"\"rows\"\":[{\"\"values\"\":{\"\"KEY\"\":\"\"a\"\",\"\"VALUE\"\":\"\"1\"\"}}]}\""
                + ";ODKAZ_OBECNY;https://example.org;Example"
                + ";ZVEREJNENO;true"
                + ";;\r\n"
                // numeric node ID, URI without description at the end of the row
                + root.getNodeId() + ";ODKAZ_OBECNY;https://example.com\r\n";

        CsvDescItemsImporter.Result result = run(csv, ';', ANY_FUND, false);

        assertEquals(1, result.nodesUpdated());
        assertEquals(Map.of(fund.getFund().getFundId(), fund.getFundVersionId()), result.fundVersionIds());

        Map<String, List<ArrData>> data = readData(root);
        assertInstanceOf(ArrDataNull.class, data.get("SRD_LEVEL_TYPE").get(0));
        assertEquals("Name", ((ArrDataString) data.get("SRD_NAME").get(0)).getStringValue());
        assertEquals("Title", ((ArrDataText) data.get("SRD_TITLE").get(0)).getTextValue());
        assertEquals("Legend", ((ArrDataText) data.get("SRD_LEGEND").get(0)).getTextValue());
        assertEquals(42, ((ArrDataInteger) data.get("SRD_SERIAL_NUMBER").get(0)).getIntegerValue());
        assertEquals(0, new BigDecimal("1.25").compareTo(
                ((ArrDataDecimal) data.get("SRD_COLL_EXTENT_LENGTH").get(0)).getValue()));
        assertEquals("U-1", ((ArrDataUnitid) data.get("SRD_UNIT_ID").get(0)).getUnitId());
        assertInstanceOf(ArrDataUnitdate.class, data.get("SRD_UNIT_DATE").get(0));
        assertEquals(LocalDate.of(2020, 5, 17), ((ArrDataDate) data.get("SRD_SIMPLE_DATE").get(0)).getValue());
        assertEquals("Point", ((ArrDataCoordinates) data.get("SRD_POSITION").get(0)).getValue().getGeometryType());
        assertEquals(1, ((ArrDataJsonTable) data.get("SRD_STATISTICS").get(0)).getValue().getRows().size());
        assertTrue(((ArrDataBit) data.get("ZVEREJNENO").get(0)).isBitValue());

        Map<String, String> uris = data.get("ODKAZ_OBECNY").stream().map(ArrDataUriRef.class::cast)
                .collect(Collectors.toMap(ArrDataUriRef::getUriRefValue,
                        u -> u.getDescription() == null ? "" : u.getDescription()));
        assertEquals(Map.of("https://example.org", "Example", "https://example.com", ""), uris);

        // both rows went into a single IMPORT change of the fund
        List<ArrChange> changes = readChanges(root);
        assertEquals(1, changes.size());
        assertEquals(ArrChange.Type.IMPORT, changes.get(0).getType());
    }

    @Test
    public void oneChangePerFundAndChunk() throws IOException {
        FundInfo first = newFund("CSV-chunk-1");
        FundInfo second = newFund("CSV-chunk-2");
        String a = first.getRootNode().getUuid();
        String b = second.getRootNode().getUuid();

        // chunk 1: a, b, a -> one change per fund; chunk 2: a -> another change of fund 1
        String csv = a + ",SRD_NAME,1\n" + b + ",SRD_NAME,2\n" + a + ",SRD_NAME,3\n" + a + ",SRD_NAME,4\n";
        CsvDescItemsImporter.Result result = withChunkSize(3, () -> run(csv, ',', ANY_FUND, false));

        assertEquals(2, result.nodesUpdated());
        assertEquals(List.of(first.getFund().getFundId(), second.getFund().getFundId()),
                List.copyOf(result.fundVersionIds().keySet()));
        assertEquals(2, readChanges(first.getRootNode()).size());
        assertEquals(1, readChanges(second.getRootNode()).size());
        assertEquals(4, readData(first.getRootNode()).get("SRD_NAME").size()
                + readData(second.getRootNode()).get("SRD_NAME").size());
    }

    @Test
    public void failedChunkKeepsTheChunksBefore() {
        FundInfo fund = newFund("CSV-partial");
        String a = fund.getRootNode().getUuid();
        String csv = a + ",SRD_NAME,1\n" + a + ",SRD_NAME,2\n" + a + ",SRD_NAME,3\n" + a + ",SRD_NO_SUCH_TYPE,4\n";

        BusinessException e = assertThrows(BusinessException.class,
                () -> withChunkSize(2, () -> run(csv, ',', ANY_FUND, false)));
        assertTrue(e.getMessage().contains("CSV row 4"), e.getMessage());
        assertTrue(e.getMessage().contains("Rows up to CSV row 2 were already imported"), e.getMessage());
        assertEquals(4L, e.getProperties().get("row"));

        // first chunk committed, the failing one rolled back
        assertEquals(2, readData(fund.getRootNode()).get("SRD_NAME").size());
    }

    @Test
    public void dryRunWritesNothing() throws IOException {
        FundInfo fund = newFund("CSV-dry-run");
        String csv = fund.getRootNode().getUuid() + ",SRD_NAME,x\n";

        CsvDescItemsImporter.Result result = run(csv, ',', ANY_FUND, true);

        assertEquals(0, result.nodesUpdated());
        assertFalse(readData(fund.getRootNode()).containsKey("SRD_NAME"));
    }

    @Test
    public void fundCheckRefusesForeignFund() {
        FundInfo own = newFund("CSV-own");
        FundInfo foreign = newFund("CSV-foreign");
        Integer ownFundId = own.getFund().getFundId();
        String csv = foreign.getRootNode().getUuid() + ",SRD_NAME,x\n";

        assertThrows(IllegalStateException.class, () -> run(csv, ',', fundId -> {
            if (!ownFundId.equals(fundId)) {
                throw new IllegalStateException("foreign fund");
            }
        }, false));
        assertFalse(readData(foreign.getRootNode()).containsKey("SRD_NAME"));
    }

    /**
     * The batch runner holds the item in its own transaction; the importer's chunk
     * transactions run nested in it.
     */
    @Test
    public void batchRunsCsvItem() throws IOException {
        FundInfo fund = newFund("CSV-batch");
        byte[] csv = (BOM + fund.getRootNode().getUuid() + ",SRD_NAME,from batch\n").getBytes(StandardCharsets.UTF_8);

        ImpBatchDescCsv batch = batchService.createDescCsvBatch("CSV batch", ",", "UTF-8", false, false);
        ImpItem item = itemService.uploadItem(batch, "items.csv", "text/csv", csv.length,
                new ByteArrayInputStream(csv));
        batchService.changeState(batch, BatchState.IN_PROGRESS);
        batchService.runBatch(batch.getBatchId());

        ImpItem done = itemService.findById(item.getItemId());
        assertEquals(ItemState.FINISHED, done.getState(), done.getError());
        assertEquals(1, done.getNodesUpdated());
        assertEquals(BatchState.FINISHED, batchService.findById(batch.getBatchId()).getState());
        assertEquals(1, readData(fund.getRootNode()).get("SRD_NAME").size());
    }

    @Test
    public void refusesDeletedNode() {
        FundInfo fund = newFund("CSV-deleted-node");
        ArrNode child = new TransactionTemplate(txManager).execute(s -> {
            ArrNode root = nodeRepository.findById(fund.getRootNodeId()).orElseThrow();
            List<ArrLevel> levels = fundLevelService.addNewLevel(fund.getFundVersion(), root, root,
                    AddLevelDirection.CHILD, null, null, null, null, null);
            ArrNode node = levels.stream().max(Comparator.comparing(ArrLevel::getLevelId))
                    .orElseThrow().getNode();
            fundLevelService.deleteLevel(fund.getFundVersion(), node, root, false);
            return node;
        });

        assertRowError("1", child.getUuid() + ",SRD_NAME,x\n", "was deleted");
        assertRowError("1", child.getNodeId() + ",SRD_NAME,x\n", "was deleted");
        assertFalse(readData(child).containsKey("SRD_NAME"));
    }

    @Test
    public void reportsRowErrors() {
        FundInfo fund = newFund("CSV-errors");
        String uuid = fund.getRootNode().getUuid();

        assertRowError("1", uuid + ",SRD_LEVEL_TYPE\n", "missing specification");
        assertRowError("1", uuid + ",SRD_LEVEL_TYPE,NO_SUCH_SPEC\n", "unknown specification");
        assertRowError("1", uuid + ",SRD_NAME\n", "missing value");
        assertRowError("1", uuid + ",SRD_SERIAL_NUMBER,abc\n", "invalid value 'abc'");
        assertRowError("1", uuid + ",SRD_ORIGINATOR,999999999\n", "entity 999999999 not found");
        assertRowError("1", uuid + ",SRD_STORAGE_ID,999999999\n", "structured object 999999999 not found");
        assertRowError("1", uuid + ",SRD_FILE,999999999\n", "file 999999999 not found");
        assertRowError("2", "\n,ignored\n" + "not-a-node,SRD_NAME,x\n", "invalid node identifier");
        assertRowError("1", "00000000-0000-0000-0000-000000000000,SRD_NAME,x\n", "not found");
    }

    /** Commits the fund so that the importer's own transactions see it. */
    private FundInfo newFund(String name) {
        return new TransactionTemplate(txManager).execute(s -> createFund(name));
    }

    private void assertRowError(String row, String csv, String expected) {
        BusinessException e = assertThrows(BusinessException.class, () -> run(csv, ',', ANY_FUND, false));
        assertTrue(e.getMessage().startsWith("CSV row " + row + ": "), e.getMessage());
        assertTrue(e.getMessage().contains(expected), e.getMessage());
    }

    private CsvDescItemsImporter.Result run(String csv, char separator, IntConsumer fundCheck, boolean dryRun)
            throws IOException {
        try (InputStream in = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))) {
            return importer.importCsv(in, String.valueOf(separator), ',', "UTF-8", fundCheck, dryRun);
        }
    }

    private interface Import {
        CsvDescItemsImporter.Result run() throws IOException;
    }

    private CsvDescItemsImporter.Result withChunkSize(int size, Import action) throws IOException {
        ReflectionTestUtils.setField(importer, "rowsPerTransaction", size);
        try {
            return action.run();
        } finally {
            ReflectionTestUtils.setField(importer, "rowsPerTransaction", CsvDescItemsImporter.ROWS_PER_TRANSACTION);
        }
    }

    private Map<String, List<ArrData>> readData(ArrNode node) {
        return new TransactionTemplate(txManager).execute(s -> {
            Map<String, List<ArrData>> result = new HashMap<>();
            for (ArrDescItem item : items(node)) {
                result.computeIfAbsent(item.getItemType().getCode(), k -> new ArrayList<>())
                        .add((ArrData) Hibernate.unproxy(item.getData()));
            }
            return result;
        });
    }

    private List<ArrChange> readChanges(ArrNode node) {
        return new TransactionTemplate(txManager).execute(s -> List.copyOf(items(node).stream()
                .map(ArrDescItem::getCreateChange)
                .filter(c -> c.getType() == ArrChange.Type.IMPORT)
                .collect(Collectors.toMap(ArrChange::getChangeId, c -> c, (x, y) -> x))
                .values()));
    }

    private List<ArrDescItem> items(ArrNode node) {
        return descItemRepository.findByNodeAndDeleteChangeIsNull(nodeRepository.getReferenceById(node.getNodeId()));
    }
}
