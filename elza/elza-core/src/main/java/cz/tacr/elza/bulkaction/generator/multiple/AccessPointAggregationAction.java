package cz.tacr.elza.bulkaction.generator.multiple;

import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.bulkaction.generator.result.AccessPointAggregationResult;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApPartRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class AccessPointAggregationAction extends Action {

    private final static Logger logger = LoggerFactory.getLogger(BulkActionService.class);

    @Autowired
    ApPartRepository partRepository;

    @Autowired
    ApItemRepository itemRepository;

    @Autowired
    ItemTypeRepository itemTypeRepository;

    @Autowired
    ApAccessPointRepository apAccessPointRepository;

    /**
     * Vstupní atributy
     */
    private Map<Integer, ItemType> inputItemTypes = new HashMap<>();

    /**
     * Výstupní atribut
     */
    private ItemType outputItemType;

    private ItemType outputItemTypeApRef;

    protected AccessPointAggregationConfig config;

    private List<ArrStructuredItem> resultItems;

    private StaticDataProvider ruleSystem;

    public AccessPointAggregationAction(final AccessPointAggregationConfig config) {
        Validate.notNull(config);
        this.config = config;
    }

    @Override
    public void init(ArrBulkActionRun bulkActionRun) {
        resultItems = new ArrayList<>();
        ruleSystem = getStaticDataProvider();

        String outputType = config.getOutputType();
        outputItemType = ruleSystem.getItemTypeByCode(outputType);
        checkValidDataType(outputItemType, DataType.STRUCTURED);

        for (String inputTypeCode : config.getInputTypes()) {
            ItemType inputType = ruleSystem.getItemTypeByCode(inputTypeCode);
            checkValidDataType(inputType, DataType.RECORD_REF);
            inputItemTypes.put(inputType.getItemTypeId(), inputType);
        }

        String outputTypeApRef = config.getOutputTypeApRef();
        outputItemTypeApRef = ruleSystem.getItemTypeByCode(outputTypeApRef);
        checkValidDataType(outputItemTypeApRef, DataType.RECORD_REF);

        for (ApAggregationPartConfig partConfig : config.getMappingPartValue()) {
            Assert.notNull(ruleSystem.getPartTypeByCode(partConfig.getFromPart()), "Neexistujíci typ Partu pro mapování");
            Assert.notNull(ruleSystem.getItemTypeByCode(partConfig.getToItem()), "Neexistující typ Itemu pro mapování");
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
     * @param dataId
     */
    private void processAP(Integer dataId) {
        logger.debug("Zpracovnání AP ID : " + dataId);

        ApAccessPoint ap = apAccessPointRepository.findOne(dataId);
        List<ApPart> parts = partRepository.findValidPartByAccessPoint(ap);
        List<ApItem> items = itemRepository.findValidItemsByAccessPointMultiFetch(ap);

        // Procházení prvků PART_VALUE
        createPartValueResults(ap, parts);

        //Procházení prvků PART_ITEM
        createPartItemResults(ap, items);

        //Procházení prvků PART_ITEMS
        createPartItemsResults(ap, items);

        logger.debug("Konec zpracování AP : " + ap.getAccessPointId());
    }

    private void createPartValueResults(ApAccessPoint ap, List<ApPart> parts) {
        for (ApAggregationPartConfig partConfig : config.getMappingPartValue()) {
            RulPartType fromPart = ruleSystem.getPartTypeByCode(partConfig.getFromPart());
            // pro value jen z fromPrefferedName
            if (partConfig.fromPrefferedName) {
                createResultItem(partConfig.getToItem(), ap.getPreferredPart().getValue());
            }
            //pro všechny value z Partů daného typu
            else {
                List<String> foundPartValues = parts.stream()
                        .filter(apPart -> apPart.getPartType().getPartTypeId().equals(fromPart.getPartTypeId()))
                        .map(ApPart::getValue)
                        .collect(Collectors.toList());
                if (partConfig.group && foundPartValues != null && foundPartValues.size() > 0) {
                    createResultItem(partConfig.getToItem(), String.join(partConfig.groupSeparator, foundPartValues));
                } else if (foundPartValues != null && foundPartValues.size() > 0) {
                    for (String partValue : foundPartValues) {
                        createResultItem(partConfig.getToItem(), partValue);
                    }
                }
            }
        }
    }

    private void createPartItemResults(ApAccessPoint ap, List<ApItem> items) {
        for (ApAggregationItemConfig itemConfig : config.getMappingPartItem()) {
            // pro value jen z fromPrefferedName
            if (itemConfig.fromPrefferedName) {
                createResultItem(itemConfig.getToItem(), ap.getPreferredPart().getValue());
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
                if (itemConfig.group && foundItemValues != null && foundItemValues.size() > 0) {
                    createResultItem(itemConfig.getToItem(), String.join(itemConfig.groupSeparator, foundItemValues));
                } else if (foundItemValues != null && foundItemValues.size() > 0) {
                    for (String partValue : foundItemValues) {
                        createResultItem(itemConfig.getToItem(), partValue);
                    }
                }
            }
        }
    }

    private void createPartItemsResults(ApAccessPoint ap, List<ApItem> items) {
        for (ApAggregationItemsConfig itemsConfig : config.getMappingPartItems()) {
            // pro value jen z fromPrefferedName
            if (itemsConfig.fromPrefferedName) {
                createResultItem(itemsConfig.getToItem(), ap.getPreferredPart().getValue());
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
                if (foundItemValues != null && foundItemValues.size() > 0) {
                    createResultItem(itemsConfig.getToItem(), String.join(itemsConfig.groupSeparator, foundItemValues));
                }
            }
        }
    }

    private List<String> createPartCodeList(List<String> fromParts) {
        List<String> fromPartCodeList;
        if (fromParts == null || fromParts.isEmpty()) {
            fromPartCodeList = ruleSystem.getPartTypes().stream()
                    .map(rulPartType -> rulPartType.getCode()).collect(Collectors.toList());
        } else {
            fromPartCodeList = ruleSystem.getPartTypes().stream()
                    .filter(rulPartType -> fromParts.contains(rulPartType.getCode()))
                    .map(rulPartType -> rulPartType.getCode())
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

    private void createResultItem(String itemCode, String value) {
        ArrStructuredItem item = new ArrStructuredItem();
        item.setItemType(itemTypeRepository.findOneByCode(itemCode));
        ArrData data = createItemData(ruleSystem.getItemTypeByCode(itemCode), value);
        item.setData(data);
        resultItems.add(item);
    }

    @Override
    public ActionResult getResult() {
        AccessPointAggregationResult accessPointAggregationResult = new AccessPointAggregationResult();
        accessPointAggregationResult.setOutputType(outputItemType.getCode());
        accessPointAggregationResult.setOutputTypeApRef(outputItemTypeApRef.getCode());
        accessPointAggregationResult.setDataItems(resultItems);
        return accessPointAggregationResult;
    }

    protected ArrData createItemData(ItemType itemType, String value) {
        ArrData data = null;
        switch (itemType.getDataType().getCode()) {
            case "FORMATTED_TEXT":
            case "TEXT":
                ArrDataText dataText = new ArrDataText();
                dataText.setValue(value);
                data = dataText;
                break;
            case "STRING":
                ArrDataString itemString = new ArrDataString();
                itemString.setValue(value);
                data = itemString;
                break;
            case "INT":
                ArrDataInteger itemInteger = new ArrDataInteger();
                itemInteger.setValue(Integer.valueOf(value));
                data = itemInteger;
                break;
            case "DATE":
                ArrDataDate dataDate = new ArrDataDate();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate localDate = LocalDate.parse(value, formatter);
                dataDate.setValue(localDate);
                dataDate.setDataType(DataType.DATE.getEntity());
                data = dataDate;
                break;
            case "UNITID":
                ArrDataUnitid itemUnitid = new ArrDataUnitid();
                itemUnitid.setUnitId(value);
                data = itemUnitid;
                break;
            case "COORDINATES":
                break;
            case "BIT":
                ArrDataBit itemBit = new ArrDataBit();
                itemBit.setValue(Boolean.valueOf(value));
                data = itemBit;
                break;
            case "URI-REF":
                ArrDataUriRef itemUriRef = new ArrDataUriRef();
                itemUriRef.setValue(value);
                data = itemUriRef;
                break;
            case "DECIMAL":
                break;
            case "STRUCTURED":
                break;
            case "ENUM":
                ArrDataNull itemNull = new ArrDataNull();
                data = itemNull;
                break;
            default:
                throw new SystemException("Není implementováno, nebo neplatný typ atributu " + itemType.getDataType().getCode(), BaseCode.INVALID_STATE);
        }
        data.setDataType(itemType.getDataType().getEntity());
        return data;
    }
}

