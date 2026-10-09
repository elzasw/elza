package cz.tacr.elza.service.imp;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntConsumer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.common.GeometryConvertor;
import cz.tacr.elza.common.UuidUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataBit;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataDate;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.FundFileRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.StructuredObjectRepository;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.service.arrangement.MultipleItemChangeContext;

/**
 * Imports description items from a CSV file. Shared by the import into a fund (Table view)
 * and by the batch import.
 *
 * <p>Each row is {@code <node>,<type code>,[<spec code>,]<value...>[,<type code>,...]}:
 * <ul>
 * <li>node: UUID or numeric node ID of a node that is not deleted; a row with an empty first
 * column is skipped,</li>
 * <li>a spec code follows when the type uses specifications (always for ENUM),</li>
 * <li>ENUM has no value, URI_REF has two (URL, description - may be empty or omitted at the
 * end of the row), every other type has one; see {@link #buildData} for the value formats,</li>
 * <li>empty type columns are skipped (trailing separators from spreadsheets).</li>
 * </ul>
 *
 * <p>Rows are written in chunks of {@link #ROWS_PER_TRANSACTION}, each in its own transaction
 * with one change per archival fund. A failure rolls back its chunk only; the chunks before it
 * stay imported.
 */
@Service
public class CsvDescItemsImporter {

    public static final int ROWS_PER_TRANSACTION = 1000;

    /** Chunk size; tests lower it. */
    private int rowsPerTransaction = ROWS_PER_TRANSACTION;

    /**
     * @param nodesUpdated number of distinct nodes that received at least one description item
     * @param fundVersionIds open version of every fund the file changed, by fund ID, in the order
     *                       of first appearance
     */
    public record Result(int nodesUpdated, Map<Integer, Integer> fundVersionIds) { }

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private DescriptionItemService descriptionItemService;

    @Autowired
    private ArrangementInternalService arrangementInternalService;

    @Autowired
    private ApAccessPointRepository apAccessPointRepository;

    @Autowired
    private StructuredObjectRepository structuredObjectRepository;

    @Autowired
    private FundFileRepository fundFileRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * @param separator        field separator; the first character is used, empty means the default
     * @param defaultSeparator separator used when none is given
     * @param encoding         charset name, empty means UTF-8; a byte order mark is skipped
     * @param fundCheck        called once for each fund the file touches, inside the chunk
     *                         transaction; throws to refuse the fund
     * @param dryRun           parse and resolve every row without writing anything
     */
    public Result importCsv(InputStream in,
                            String separator,
                            char defaultSeparator,
                            String encoding,
                            IntConsumer fundCheck,
                            boolean dryRun) throws IOException {
        Objects.requireNonNull(fundCheck);
        Charset charset = StringUtils.isBlank(encoding) ? StandardCharsets.UTF_8 : Charset.forName(encoding);
        CSVFormat format = CSVFormat.EXCEL.builder()
                .setDelimiter(StringUtils.isEmpty(separator) ? defaultSeparator : separator.charAt(0))
                .setIgnoreSurroundingSpaces(true)
                .setIgnoreEmptyLines(true)
                .build();

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        // runs nested in the caller's transaction (batch item) as well as on its own (fund import)
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        Set<Integer> updatedNodeIds = new HashSet<>();
        Map<Integer, Integer> fundVersionIds = new LinkedHashMap<>();
        long lastImportedRow = 0;

        try (Reader reader = new InputStreamReader(BOMInputStream.builder().setInputStream(in).get(), charset);
             CSVParser parser = format.parse(reader)) {
            Iterator<CSVRecord> records = parser.iterator();
            while (records.hasNext()) {
                List<CSVRecord> chunk = nextChunk(records);
                if (chunk.isEmpty()) {
                    break;
                }
                try {
                    tx.executeWithoutResult(status -> {
                        importChunk(chunk, fundCheck, dryRun, updatedNodeIds, fundVersionIds);
                        if (dryRun) {
                            status.setRollbackOnly();
                        }
                    });
                } catch (RuntimeException e) {
                    throw withImportedRows(e, dryRun ? 0 : lastImportedRow);
                }
                lastImportedRow = chunk.get(chunk.size() - 1).getRecordNumber();
            }
        }
        return new Result(updatedNodeIds.size(), fundVersionIds);
    }

    private List<CSVRecord> nextChunk(Iterator<CSVRecord> records) {
        List<CSVRecord> chunk = new ArrayList<>(rowsPerTransaction);
        while (chunk.size() < rowsPerTransaction && records.hasNext()) {
            CSVRecord record = records.next();
            if (record.size() > 0 && !record.get(0).isBlank()) {
                chunk.add(record);
            }
        }
        return chunk;
    }

    /**
     * Change of one fund within a chunk.
     */
    private static class FundTarget {
        final ArrFundVersion version;
        final ArrChange change;
        final MultipleItemChangeContext changeContext;

        FundTarget(ArrFundVersion version, ArrChange change, MultipleItemChangeContext changeContext) {
            this.version = version;
            this.change = change;
            this.changeContext = changeContext;
        }
    }

    private void importChunk(List<CSVRecord> chunk,
                             IntConsumer fundCheck,
                             boolean dryRun,
                             Set<Integer> updatedNodeIds,
                             Map<Integer, Integer> fundVersionIds) {
        StaticDataProvider sdp = staticDataService.getData();
        Map<Integer, FundTarget> targets = new HashMap<>();

        for (CSVRecord record : chunk) {
            ArrNode node = resolveNode(record);
            Integer fundId = node.getFundId();
            FundTarget target = targets.get(fundId);
            if (target == null) {
                target = openTarget(record, fundId, fundCheck, dryRun);
                targets.put(fundId, target);
                fundVersionIds.putIfAbsent(fundId, target.version.getFundVersionId());
            }

            List<ArrDescItem> items = parseItems(record, fundId, sdp);
            if (items.isEmpty() || dryRun) {
                continue;
            }
            node = descriptionItemService.saveNode(node, target.change);
            for (ArrDescItem item : items) {
                descriptionItemService.createDescriptionItemInBatch(item, node, target.version,
                        target.change, target.changeContext);
            }
            target.changeContext.flushIfNeeded();
            updatedNodeIds.add(node.getNodeId());
        }

        for (FundTarget target : targets.values()) {
            if (target.changeContext != null) {
                target.changeContext.flush();
            }
        }
    }

    private FundTarget openTarget(CSVRecord record, Integer fundId, IntConsumer fundCheck, boolean dryRun) {
        fundCheck.accept(fundId);
        ArrFundVersion version = fundVersionRepository.findByFundIdAndLockChangeIsNull(fundId);
        if (version == null) {
            throw rowError(record, "the archival fund of the node has no open version (fund ID " + fundId + ")");
        }
        if (dryRun) {
            return new FundTarget(version, null, null);
        }
        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.IMPORT, null);
        return new FundTarget(version, change,
                descriptionItemService.createChangeContext(version.getFundVersionId()));
    }

    private ArrNode resolveNode(CSVRecord record) {
        String nodeId = record.get(0).trim();
        ArrNode node;
        if (UuidUtils.isUUID(nodeId)) {
            node = nodeRepository.findOneByUuid(nodeId);
        } else if (StringUtils.isNumeric(nodeId)) {
            node = nodeRepository.findById(Integer.valueOf(nodeId)).orElse(null);
        } else {
            throw rowError(record, "invalid node identifier '" + nodeId
                    + "', expected UUID or numeric ID. Check the CSV field separator.")
                    .set("value", nodeId);
        }
        if (node == null) {
            throw rowError(record, "node '" + nodeId + "' not found").set("value", nodeId);
        }
        // a node without an active level was deleted from the fund
        if (levelRepository.findByNodeIdAndDeleteChangeIsNull(node.getNodeId()) == null) {
            throw rowError(record, "node '" + nodeId + "' was deleted").set("value", nodeId);
        }
        return node;
    }

    private List<ArrDescItem> parseItems(CSVRecord record, Integer fundId, StaticDataProvider sdp) {
        List<ArrDescItem> items = new ArrayList<>();
        int i = 1;
        while (i < record.size()) {
            int typeColumn = i;
            String typeCode = record.get(i++).trim();
            if (typeCode.isEmpty()) {
                continue;
            }
            ItemType itemType = sdp.getItemTypeByCode(typeCode);
            if (itemType == null) {
                throw rowError(record, "unknown item type '" + typeCode + "' (column " + (typeColumn + 1) + ")");
            }
            DataType dataType = itemType.getDataType();

            RulItemSpec spec = null;
            if (itemType.hasSpecifications() || dataType == DataType.ENUM) {
                String specCode = i < record.size() ? record.get(i++).trim() : "";
                if (specCode.isEmpty()) {
                    throw rowError(record, "missing specification for type " + typeCode);
                }
                spec = itemType.getItemSpecByCode(specCode);
                if (spec == null) {
                    throw rowError(record, "unknown specification '" + specCode + "' for type " + typeCode);
                }
            }

            ArrData data;
            if (dataType == DataType.ENUM) {
                data = new ArrDataNull();
            } else {
                if (i >= record.size() || record.get(i).isEmpty()) {
                    throw rowError(record, "missing value for type " + typeCode);
                }
                String value = record.get(i++);
                String description = null;
                if (dataType == DataType.URI_REF && i < record.size()) {
                    description = record.get(i++);
                }
                try {
                    data = buildData(itemType, value, description, fundId);
                } catch (RuntimeException e) {
                    throw rowError(record, "invalid value '" + value + "' for type " + typeCode
                            + ": " + e.getMessage()).set("value", value);
                }
            }

            ArrDescItem descItem = new ArrDescItem();
            descItem.setItemType(itemType.getEntity());
            descItem.setItemSpec(spec);
            descItem.setData(data);
            items.add(descItem);
        }
        return items;
    }

    /**
     * Value formats: INT, DECIMAL (dot as decimal separator), BIT ({@code true}/{@code false},
     * {@code 1}/{@code 0}), DATE (ISO {@code yyyy-mm-dd}), UNITDATE (unit-date expression),
     * COORDINATES (WKT), JSON_TABLE (JSON of the table), RECORD_REF (entity ID), STRUCTURED (ID
     * of a structured object of the fund), FILE_REF (ID of a file of the fund); text types as is.
     */
    private ArrData buildData(ItemType itemType, String value, String description, Integer fundId) {
        DataType dataType = itemType.getDataType();
        switch (dataType) {
            case STRING: {
                ArrDataString d = new ArrDataString();
                d.setStringValue(value);
                return d;
            }
            case TEXT:
            case FORMATTED_TEXT: {
                ArrDataText d = new ArrDataText();
                d.setTextValue(value);
                return d;
            }
            case UNITID: {
                ArrDataUnitid d = new ArrDataUnitid();
                d.setUnitId(value);
                return d;
            }
            case INT: {
                ArrDataInteger d = new ArrDataInteger();
                d.setIntegerValue(Integer.valueOf(value.trim()));
                return d;
            }
            case DECIMAL: {
                ArrDataDecimal d = new ArrDataDecimal();
                d.setValue(new BigDecimal(value.trim()));
                return d;
            }
            case BIT:
                return new ArrDataBit(parseBit(value.trim()));
            case DATE: {
                ArrDataDate d = new ArrDataDate();
                d.setValue(LocalDate.parse(value.trim()));
                return d;
            }
            case UNITDATE:
                return ArrDataUnitdate.valueOf(value);
            case COORDINATES: {
                ArrDataCoordinates d = new ArrDataCoordinates();
                d.setValue(GeometryConvertor.convert(value));
                return d;
            }
            case JSON_TABLE: {
                ArrDataJsonTable d = new ArrDataJsonTable();
                d.setValue(ElzaTable.fromJsonString(value));
                return d;
            }
            case URI_REF: {
                ArrDataUriRef d = new ArrDataUriRef();
                d.setSchema(ArrDataUriRef.createSchema(value));
                d.setUriRefValue(value);
                if (StringUtils.isNotEmpty(description)) {
                    d.setDescription(description);
                }
                return d;
            }
            case RECORD_REF: {
                Integer apId = parseId(value);
                ApAccessPoint ap = apAccessPointRepository.findById(apId)
                        .orElseThrow(() -> new IllegalArgumentException("entity " + apId + " not found"));
                ArrDataRecordRef d = new ArrDataRecordRef();
                d.setRecord(ap);
                return d;
            }
            case STRUCTURED: {
                Integer soId = parseId(value);
                ArrStructuredObject so = structuredObjectRepository.findById(soId)
                        .filter(o -> o.getDeleteChange() == null && fundId.equals(o.getFundId()))
                        .orElseThrow(() -> new IllegalArgumentException(
                                "structured object " + soId + " not found in the archival fund"));
                if (!Objects.equals(so.getStructuredTypeId(), itemType.getEntity().getStructuredTypeId())) {
                    throw new IllegalArgumentException("structured object " + soId + " is of another type");
                }
                ArrDataStructureRef d = new ArrDataStructureRef();
                d.setStructuredObject(so);
                return d;
            }
            case FILE_REF: {
                Integer fileId = parseId(value);
                ArrFile file = fundFileRepository.findById(fileId)
                        .filter(f -> fundId.equals(f.getFundId()))
                        .orElseThrow(() -> new IllegalArgumentException(
                                "file " + fileId + " not found in the archival fund"));
                ArrDataFileRef d = new ArrDataFileRef();
                d.setFile(file);
                return d;
            }
            default:
                throw new IllegalArgumentException("data type " + dataType.getCode() + " is not supported");
        }
    }

    private static Integer parseId(String value) {
        String trimmed = value.trim();
        if (!StringUtils.isNumeric(trimmed)) {
            throw new IllegalArgumentException("expected a numeric ID");
        }
        return Integer.valueOf(trimmed);
    }

    private static boolean parseBit(String value) {
        switch (value.toLowerCase()) {
            case "true":
            case "1":
                return true;
            case "false":
            case "0":
                return false;
            default:
                throw new IllegalArgumentException("expected true/false or 1/0");
        }
    }

    private static AbstractException rowError(CSVRecord record, String message) {
        return new BusinessException("CSV row " + record.getRecordNumber() + ": " + message, BaseCode.IMPORT_FAILED)
                .set("row", record.getRecordNumber());
    }

    /**
     * Tells the operator that the chunks before the failed one stay in the fund.
     */
    private static RuntimeException withImportedRows(RuntimeException e, long lastImportedRow) {
        if (lastImportedRow == 0) {
            return e;
        }
        BusinessException be = new BusinessException(e.getMessage() + " Rows up to CSV row "
                + lastImportedRow + " were already imported.", e, BaseCode.IMPORT_FAILED);
        if (e instanceof AbstractException src) {
            src.getProperties().forEach(be::set);
        }
        be.set("importedUpToRow", lastImportedRow);
        return be;
    }
}
