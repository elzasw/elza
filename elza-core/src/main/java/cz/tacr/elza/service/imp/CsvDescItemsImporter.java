package cz.tacr.elza.service.imp;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.DescriptionItemService;

/**
 * Imports description items from a CSV file into an existing fund version. Each row is:
 * {@code <node UUID>,<type code>,[<spec code>,]<value>[,<type code>,...]}. The parsing decides
 * whether a spec code follows by looking at the type's {@code useSpecification} flag; ENUM types
 * carry only a spec, no value.
 */
@Service
public class CsvDescItemsImporter {

    /**
     * @param nodesUpdated number of nodes for which at least one description item was added.
     * @param touchedFunds every archival file whose node was updated; order is preserved to keep
     *                     the operator view predictable across runs.
     */
    public record Result(int nodesUpdated, Set<ArrFund> touchedFunds) { }

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private DescriptionItemService descriptionItemService;

    @Autowired
    private ApAccessPointRepository apAccessPointRepository;

    public Result importCsv(InputStream in,
                            String separator,
                            String encoding) throws IOException {
        Charset charset = Charset.forName(encoding == null || encoding.isBlank() ? "UTF-8" : encoding);
        char sep = (separator == null || separator.isEmpty()) ? ',' : separator.charAt(0);
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(sep)
                .setIgnoreSurroundingSpaces(true)
                .build();

        StaticDataProvider staticData = staticDataService.getData();
        int nodesUpdated = 0;

        // Resolving open version and fund once per fundId is enough - a CSV file typically
        // targets one fund, sometimes a few.
        Map<Integer, ArrFundVersion> openVersionByFund = new HashMap<>();
        Set<ArrFund> touchedFunds = new LinkedHashSet<>();

        try (Reader reader = new InputStreamReader(in, charset);
             CSVParser parser = CSVParser.parse(reader, format)) {

            for (CSVRecord record : parser) {
                if (record.size() == 0) {
                    continue;
                }
                String uuid = record.get(0).trim();
                if (uuid.isEmpty()) {
                    continue;
                }

                ArrNode node = nodeRepository.findOneByUuid(uuid);
                if (node == null) {
                    throw new BusinessException(
                            "Node with UUID '" + uuid + "' not found",
                            BaseCode.INVALID_STATE);
                }
                ArrFundVersion openVersion = openVersionByFund.computeIfAbsent(node.getFundId(),
                        fundId -> fundVersionRepository.findByFundIdAndLockChangeIsNull(fundId));
                if (openVersion == null) {
                    throw new BusinessException(
                            "Fund of node '" + uuid + "' has no open version",
                            BaseCode.INVALID_STATE);
                }

                List<ArrDescItem> items = parseRow(record, staticData);
                if (items.isEmpty()) {
                    continue;
                }
                descriptionItemService.createDescriptionItems(items, node.getNodeId(),
                        node.getVersion(), openVersion.getFundVersionId());
                nodesUpdated++;
                touchedFunds.add(openVersion.getFund());
            }
        }
        return new Result(nodesUpdated, touchedFunds);
    }

    private List<ArrDescItem> parseRow(CSVRecord record, StaticDataProvider staticData) {
        List<ArrDescItem> items = new ArrayList<>();
        int i = 1;
        while (i < record.size()) {
            String typeCode = record.get(i++).trim();
            if (typeCode.isEmpty()) {
                continue;
            }
            ItemType itemType = staticData.getItemTypeByCode(typeCode);
            if (itemType == null) {
                throw new BusinessException("Unknown item type: " + typeCode, BaseCode.INVALID_STATE);
            }
            DataType dataType = itemType.getDataType();

            String specCode = null;
            String value = null;
            boolean isEnum = dataType == DataType.ENUM;
            boolean useSpec = Boolean.TRUE.equals(itemType.getEntity().getUseSpecification()) || isEnum;

            if (useSpec) {
                if (i >= record.size()) {
                    throw new BusinessException("Missing spec code for type " + typeCode, BaseCode.INVALID_STATE);
                }
                specCode = record.get(i++).trim();
            }
            if (!isEnum) {
                if (i >= record.size()) {
                    throw new BusinessException("Missing value for type " + typeCode, BaseCode.INVALID_STATE);
                }
                value = record.get(i++);
            }

            RulItemSpec spec = null;
            if (specCode != null && !specCode.isEmpty()) {
                spec = itemType.getItemSpecByCode(specCode);
                if (spec == null) {
                    throw new BusinessException(
                            "Unknown spec '" + specCode + "' for type " + typeCode, BaseCode.INVALID_STATE);
                }
            }

            ArrDescItem descItem = new ArrDescItem();
            descItem.setItemType(itemType.getEntity());
            descItem.setItemSpec(spec);
            descItem.setData(buildData(dataType, value, typeCode));
            items.add(descItem);
        }
        return items;
    }

    private ArrData buildData(DataType dataType, String value, String typeCode) {
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
            case INT: {
                ArrDataInteger d = new ArrDataInteger();
                d.setIntegerValue(Integer.parseInt(value.trim()));
                return d;
            }
            case UNITID: {
                ArrDataUnitid d = new ArrDataUnitid();
                d.setUnitId(value);
                return d;
            }
            case RECORD_REF: {
                String trimmed = value == null ? "" : value.trim();
                if (trimmed.isEmpty()) {
                    throw new BusinessException(
                            "Missing entity ID for type " + typeCode, BaseCode.INVALID_STATE);
                }
                int apId;
                try {
                    apId = Integer.parseInt(trimmed);
                } catch (NumberFormatException e) {
                    throw new BusinessException(
                            "Invalid entity ID '" + trimmed + "' for type " + typeCode,
                            BaseCode.INVALID_STATE);
                }
                ApAccessPoint ap = apAccessPointRepository.findById(apId).orElse(null);
                if (ap == null) {
                    throw new BusinessException(
                            "Entity with ID '" + apId + "' not found (type " + typeCode + ")",
                            BaseCode.INVALID_STATE);
                }
                ArrDataRecordRef d = new ArrDataRecordRef();
                d.setRecord(ap);
                return d;
            }
            case ENUM:
                return new ArrDataNull();
            default:
                throw new BusinessException(
                        "CSV import for data type '" + dataType.getCode() + "' is not supported yet (type " + typeCode + ")",
                        BaseCode.INVALID_STATE);
        }
    }
}
