package cz.tacr.elza.bulkaction.generator.multiple;

import static cz.tacr.elza.repository.ExceptionThrow.ap;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.bulkaction.generator.result.AccessPointAggregationResult;
import cz.tacr.elza.bulkaction.generator.result.AccessPointAggregationStructResult;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataBit;
import cz.tacr.elza.domain.ArrDataDate;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApPartRepository;

@Component
@Scope("prototype")
public class AccessPointAggregationAction extends Action {

    private final static Logger logger = LoggerFactory.getLogger(BulkActionService.class);

    @Autowired
    private ApPartRepository partRepository;

    @Autowired
    private ApItemRepository itemRepository;

    @Autowired
    private ApAccessPointRepository apAccessPointRepository;

    /**
     * Vstupní atributy
     */
    private final Map<Integer, ItemType> inputItemTypes = new HashMap<>();

    /**
     * Výstupní atribut
     */
    private ItemType outputItemType;

    private ItemType outputItemTypeApRef;

    protected AccessPointAggregationConfig config;

    private Map<Integer, ApResult> apStructItems;

    private StaticDataProvider ruleSystem;

    public AccessPointAggregationAction(final AccessPointAggregationConfig config) {
        Validate.notNull(config);
        this.config = config;
    }

    @Override
    public void init(ArrBulkActionRun bulkActionRun) {
        apStructItems = new LinkedHashMap<>();
        ruleSystem = getStaticDataProvider();
        StaticDataProvider sdp = StaticDataProvider.getInstance();

        String outputType = config.getOutputType();
        outputItemType = sdp.getItemTypeByCode(outputType);
        checkValidDataType(outputItemType, DataType.STRUCTURED);

        for (String inputTypeCode : config.getInputTypes()) {
            ItemType inputType = sdp.getItemTypeByCode(inputTypeCode);
            checkValidDataType(inputType, DataType.RECORD_REF);
            inputItemTypes.put(inputType.getItemTypeId(), inputType);
        }

        String outputTypeApRef = config.getOutputTypeApRef();
        outputItemTypeApRef = sdp.getItemTypeByCode(outputTypeApRef);
        checkValidDataType(outputItemTypeApRef, DataType.RECORD_REF);

        if (config.getMappingPartValue() != null) {
            for (ApAggregationPartConfig partConfig : config.getMappingPartValue()) {
                Assert.notNull(ruleSystem.getPartTypeByCode(partConfig.getFromPart()),
                               "Neexistujíci typ Partu pro mapování: " + partConfig.getFromPart());
                Assert.notNull(sdp.getItemTypeByCode(partConfig.getToItem()), "Neexistující typ Itemu pro mapování: "
                        + partConfig.getToItem());
            }
        }

    }

    @Override
    public void apply(LevelWithItems level, TypeLevel typeLevel) {
        List<ArrDescItem> items = level.getDescItems();
        for (ArrItem item : items) {
            ItemType itemType = inputItemTypes.get(item.getItemTypeId());
            if (itemType != null && itemType.getDataType().equals(DataType.RECORD_REF)) {
                ArrDataRecordRef data = (ArrDataRecordRef) item.getData();
                processAP(data.getRecordId());
            }
        }
    }

    /**
     * Zpracování AP
     *
     * @param apId identifikátor dat
     */
    private void processAP(Integer apId) {
        logger.debug("Zpracovnání AP ID : " + apId);

        ApResult apResult = apStructItems.get(apId);
        if (apResult != null) {
            logger.debug("Konec zpracování AP : " + apId + " - již byl přidán");
            return;
        }
        ApAccessPoint ap = apAccessPointRepository.findById(apId).orElseThrow(ap(apId));
        List<ApPart> parts = partRepository.findValidPartByAccessPoint(ap);
        List<ApItem> items = itemRepository.findValidItemsByAccessPointMultiFetch(ap);

        apResult = new ApResult();
        apStructItems.put(ap.getAccessPointId(), apResult);

        // Procházení prvků PART_VALUE
        createPartValueResults(apResult, ap, parts);

        //Procházení prvků PART_ITEM
        createPartItemResults(apResult, ap, items);

        //Procházení prvků PART_ITEMS
        createPartItemsResults(apResult, ap, items);

        //Vložení odkazu na zdrojový AP
        createApRefItem(apResult, ap);

        logger.debug("Konec zpracování AP : " + ap.getAccessPointId());
    }

    private void createPartValueResults(final ApResult apResult, ApAccessPoint ap, List<ApPart> parts) {
        if (config.getMappingPartValue() == null) {
            return;
        }
        for (ApAggregationPartConfig partConfig : config.getMappingPartValue()) {
            RulPartType fromPart = ruleSystem.getPartTypeByCode(partConfig.getFromPart());
            // pro value jen z fromPrefferedName
            if (partConfig.fromPrefferedName) {
                createResultItem(apResult, partConfig.getToItem(), ap.getPreferredPart().getValue());
            }
            //pro všechny value z Partů daného typu
            else {
                List<String> foundPartValues = parts.stream()
                        .filter(apPart -> apPart.getPartType().getPartTypeId().equals(fromPart.getPartTypeId()))
                        .map(ApPart::getValue)
                        .collect(Collectors.toList());
                if (partConfig.group && foundPartValues.size() > 0) {
                    createResultItem(apResult, partConfig.getToItem(), String.join(partConfig.groupSeparator, foundPartValues));
                } else if (foundPartValues.size() > 0) {
                    for (String partValue : foundPartValues) {
                        createResultItem(apResult, partConfig.getToItem(), partValue);
                    }
                }
            }
        }
    }

    private void createPartItemResults(final ApResult apResult, ApAccessPoint ap, List<ApItem> items) {
        if (config.getMappingPartItem() == null) {
            return;
        }
        for (ApAggregationItemConfig itemConfig : config.getMappingPartItem()) {
            // pro value jen z fromPrefferedName
            if (itemConfig.fromPrefferedName) {
                createResultItem(apResult, itemConfig.getToItem(), ap.getPreferredPart().getValue());
            }
            //pro všechny value z Itemů daného typu
            else {
                List<String> fromPartCodeList = createPartCodeList(itemConfig.fromPart);

                List<String> foundItemValues = items.stream()
                        .filter(apItem -> fromPartCodeList.contains(apItem.getPart().getPartType().getCode()))
                        .filter(apItem -> apItem.getItemType().getCode().equals(itemConfig.fromType))
                        .map(ApItem::getData)
                        .map(ArrData::getFulltextValue)
                        .collect(Collectors.toList());
                if (itemConfig.group && foundItemValues.size() > 0) {
                    createResultItem(apResult, itemConfig.getToItem(), String.join(itemConfig.groupSeparator, foundItemValues));
                } else if (foundItemValues.size() > 0) {
                    for (String partValue : foundItemValues) {
                        createResultItem(apResult, itemConfig.getToItem(), partValue);
                    }
                }
            }
        }
    }

    private void createPartItemsResults(final ApResult apResult, ApAccessPoint ap, List<ApItem> items) {
        if (config.getMappingPartItems() == null) {
            return;
        }

        for (ApAggregationItemsConfig itemsConfig : config.getMappingPartItems()) {
            // pro value jen z fromPrefferedName
            if (itemsConfig.fromPrefferedName) {
                createResultItem(apResult, itemsConfig.getToItem(), ap.getPreferredPart().getValue());
            }
            //pro všechny value z Itemů daných typu
            else {
                List<String> fromPartCodeList = createPartCodeList(itemsConfig.fromPart);
                List<String> fromTypeCodeList = createTypeCodeList(itemsConfig.fromType);

                List<String> foundItemValues = items.stream()
                        .filter(apItem -> fromPartCodeList.contains(apItem.getPart().getPartType().getCode()))
                        .filter(apItem -> fromTypeCodeList.contains(apItem.getItemType().getCode()))
                        .map(ApItem::getData)
                        .map(ArrData::getFulltextValue)
                        .collect(Collectors.toList());
                if (foundItemValues.size() > 0) {
                    createResultItem(apResult, itemsConfig.getToItem(), String.join(itemsConfig.groupSeparator, foundItemValues));
                }
            }
        }
    }

    private List<String> createPartCodeList(List<String> fromParts) {
        List<String> fromPartCodeList;
        if (fromParts == null || fromParts.isEmpty()) {
            fromPartCodeList = ruleSystem.getPartTypes().stream()
                    .map(RulPartType::getCode).collect(Collectors.toList());
        } else {
            fromPartCodeList = ruleSystem.getPartTypes().stream()
                    .filter(rulPartType -> fromParts.contains(rulPartType.getCode()))
                    .map(RulPartType::getCode)
                    .collect(Collectors.toList());
        }
        return fromPartCodeList;
    }

    private List<String> createTypeCodeList(List<String> fromTypes) {
        List<String> fromTypeCodeList;
        if (fromTypes == null || fromTypes.isEmpty()) {
            fromTypeCodeList = ruleSystem.getItemTypes().stream().map(ItemType::getCode).collect(Collectors.toList());
        } else {
            fromTypeCodeList = ruleSystem.getItemTypes().stream()
                    .filter(itemType -> fromTypes.contains(itemType.getCode()))
                    .map(ItemType::getCode)
                    .collect(Collectors.toList());
        }
        return fromTypeCodeList;
    }

    private void createApRefItem(final ApResult apResult, ApAccessPoint ap) {
        ArrStructuredItem item = new ArrStructuredItem();
        RulItemType entity = outputItemTypeApRef.getEntity();
        item.setItemType(entity);
        ArrDataRecordRef data = new ArrDataRecordRef();
        data.setDataType(entity.getDataType());
        data.setRecord(ap);
        item.setData(data);
        apResult.addItem(item);
    }

    private void createResultItem(final ApResult apResult, String itemCode, String value) {
        ArrStructuredItem item = new ArrStructuredItem();
        StaticDataProvider sdp = StaticDataProvider.getInstance();
        item.setItemType(sdp.getItemTypeByCode(itemCode).getEntity());
        ArrData data = createItemData(sdp.getItemTypeByCode(itemCode), value);
        item.setData(data);
        apResult.addItem(item);
    }

    @Override
    public ActionResult getResult() {
        AccessPointAggregationResult accessPointAggregationResult = new AccessPointAggregationResult();
        accessPointAggregationResult.setOutputType(outputItemType.getCode());
        List<AccessPointAggregationStructResult> structs = new ArrayList<>();
        for (ApResult apResult : apStructItems.values()) {
            AccessPointAggregationStructResult struct = new AccessPointAggregationStructResult();
            struct.setItems(apResult.items);
            structs.add(struct);
        }
        accessPointAggregationResult.setStructs(structs);
        return accessPointAggregationResult;
    }

    protected ArrData createItemData(ItemType itemType, String value) {
        ArrData data;
        DataType dataType = itemType.getDataType();
        switch (dataType) {
            case FORMATTED_TEXT:
            case TEXT:
                ArrDataText dataText = new ArrDataText();
                dataText.setValue(value);
                data = dataText;
                break;
            case STRING:
                ArrDataString itemString = new ArrDataString();
                itemString.setValue(value);
                data = itemString;
                break;
            case INT:
                ArrDataInteger itemInteger = new ArrDataInteger();
                itemInteger.setValue(Integer.valueOf(value));
                data = itemInteger;
                break;
            case DATE:
                ArrDataDate dataDate = new ArrDataDate();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate localDate = LocalDate.parse(value, formatter);
                dataDate.setValue(localDate);
                dataDate.setDataType(DataType.DATE.getEntity());
                data = dataDate;
                break;
            case UNITID:
                ArrDataUnitid itemUnitid = new ArrDataUnitid();
                itemUnitid.setUnitId(value);
                data = itemUnitid;
                break;
            case BIT:
                ArrDataBit itemBit = new ArrDataBit();
                itemBit.setValue(Boolean.valueOf(value));
                data = itemBit;
                break;
            case URI_REF:
                ArrDataUriRef itemUriRef = new ArrDataUriRef();
                itemUriRef.setValue(value);
                data = itemUriRef;
                break;
            case ENUM:
                data = new ArrDataNull();
                break;
            case COORDINATES:
            case DECIMAL:
            case RECORD_REF:
            case UNITDATE:
            case JSON_TABLE:
            case FILE_REF:
            case APFRAG_REF:
            case STRUCTURED:
                throw new NotImplementedException("Data nejsou podporovány: " + dataType);
            default:
                throw new SystemException("Není implementováno, nebo neplatný typ atributu " + dataType.getCode(), BaseCode.INVALID_STATE);
        }
        data.setDataType(dataType.getEntity());
        return data;
    }

    private static class ApResult {

        List<ArrStructuredItem> items = new ArrayList<>();

        void addItem(ArrStructuredItem item) {
            items.add(item);
        }

    }
}

