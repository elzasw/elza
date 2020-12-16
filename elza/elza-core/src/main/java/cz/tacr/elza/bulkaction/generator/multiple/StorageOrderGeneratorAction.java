package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.bulkaction.generator.multiple.StorageOrderGeneratorConfig.WhenCondition;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.service.StructObjService;

@Component
@Scope("prototype")
public class StorageOrderGeneratorAction extends Action {

    final StorageOrderGeneratorConfig config;
    private ItemType storageItemType;
    private ItemType orderItemType;

    final Map<Integer, Integer> lastOrderValues = new HashMap<>();

    private ArrFundVersion fundVersion;
    private ArrChange change;

    @Autowired
    private DescriptionItemService descriptionItemService;

    @Autowired
    private StructObjService structObjService;

    public StorageOrderGeneratorAction(final StorageOrderGeneratorConfig config) {
        this.config = config;
    }

    @Override
    public void init(ArrBulkActionRun bulkActionRun) {
        fundVersion = bulkActionRun.getFundVersion();
        change = bulkActionRun.getChange();

        StaticDataProvider sdp = getStaticDataProvider();
        this.storageItemType = sdp.getItemTypeByCode(config.getStorageItemType());
        Validate.notNull(this.storageItemType);
        Validate.isTrue(storageItemType.getDataType() == DataType.STRUCTURED);

        this.orderItemType = sdp.getItemTypeByCode(config.getOrderItemType());
        Validate.notNull(this.orderItemType);
        Validate.isTrue(orderItemType.getDataType() == DataType.INT);

    }

    @Override
    public void apply(LevelWithItems level, TypeLevel typeLevel) {
        List<ArrDescItem> storageItems = level.getDescItems(storageItemType, null);
        // only single storage items are supported 
        if (storageItems == null || storageItems.size() != 1) {
            return;
        }
        // check storage type
        ArrDescItem storageItem = storageItems.get(0);
        ArrData storageData = storageItem.getData();
        if (storageData == null) {
            return;
        }
        ArrDataStructureRef structRef = HibernateUtils.unproxy(storageData);
        Integer structObjId = structRef.getStructuredObjectId();

        // check if item count exists
        List<ArrDescItem> orderItems = level.getDescItems(orderItemType, null);
        if (orderItems != null && orderItems.size() > 0) {
            // store new value
            ArrDescItem orderItem = orderItems.get(0);
            takeLastUsedOrder(structObjId, orderItem);
        } else {
            // try to add new value
            Integer lastUsedValue = lastOrderValues.get(structObjId);
            if (lastUsedValue == null) {
                // check if structured object suitable for item order
                ArrStructuredObject structuredObject = structRef.getStructuredObject();
                if (!canHaveOrderValue(structuredObject)) {
                    return;
                }
                // storage without defined last value -> set as 1
                lastUsedValue = 1;
            } else {
                // increment counter
                lastUsedValue++;
            }
            // add new item            
            addOrderWithValue(level, lastUsedValue);
            lastOrderValues.put(structObjId, lastUsedValue);
        }
    }

    /**
     * Check if structured object is supported for order value
     * 
     * @param structuredObject
     * @return
     */
    private boolean canHaveOrderValue(ArrStructuredObject structuredObject) {
        List<WhenCondition> whenStorageConds = config.getWhenStorage();
        if (whenStorageConds != null && whenStorageConds.size() > 0) {
            // read structured object
            List<ArrStructuredItem> items = structObjService.findStructureItems(structuredObject);
            // check condition
            for (WhenCondition whenStorageCond : whenStorageConds) {
                if (!checkCondition(whenStorageCond, items)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkCondition(WhenCondition whenStorageCond, List<ArrStructuredItem> items) {
        String itemTypeCode = whenStorageCond.getItemType();
        // check if itemType exists
        if (StringUtils.isNotBlank(itemTypeCode)) {
            StaticDataProvider sdp = this.staticDataService.getData();
            ItemType itemType = sdp.getItemTypeByCode(itemTypeCode);
            Validate.notNull(itemType, "Item type not found: %s", itemTypeCode);

            // get required specs
            Set<Integer> requiredSpecIds = null;
            List<String> requiredSpecCodes = whenStorageCond.getItemSpecs();
            if (CollectionUtils.isNotEmpty(requiredSpecCodes)) {
                Validate.isTrue(itemType.hasSpecifications(), "Item type does not support specifications: %s",
                                itemTypeCode);
                requiredSpecIds = requiredSpecCodes.stream().map(itemType::getItemSpecByCode).
                        map(RulItemSpec::getItemSpecId).collect(Collectors.toSet());
            }

            // check items if contains required item
            boolean found = false;
            for (ArrStructuredItem item : items) {
                if(item.getItemTypeId().equals(itemType.getItemTypeId())) {
                    if (requiredSpecIds == null) {
                        found = true;
                        break;
                    } else {
                        // check spec code
                        if (requiredSpecIds.contains(item.getItemSpecId())) {
                            found = true;
                            break;
                        }
                    }
                }
            }
            if (!found) {
                return false;
            }

        }

        return true;
    }

    /**
     * Add order
     * 
     * @param level
     * 
     * @param lastUsedValue
     */
    private void addOrderWithValue(LevelWithItems level, Integer lastUsedValue) {
        // Connect struct obj to the level
        ArrDataInteger di = new ArrDataInteger();
        di.setDataType(DataType.INT.getEntity());
        di.setIntegerValue(lastUsedValue);

        ArrDescItem descItem = new ArrDescItem();
        descItem.setItemType(orderItemType.getEntity());
        descItem.setData(di);
        descriptionItemService.createDescriptionItem(descItem, level.getNodeId(), fundVersion, change);
    }

    /**
     * Take last used order from existing item
     * 
     * @param structObjId
     * @param orderItem
     */
    private void takeLastUsedOrder(Integer structObjId, ArrDescItem orderItem) {
        ArrData data = orderItem.getData();
        // value is unknown -> return
        if (data == null) {
            return;
        }
        Validate.isTrue(data.getDataTypeId() == DataType.INT.getId(), "Unexpected data type: %i", data.getDataTypeId());
        ArrDataInteger dataInt = HibernateUtils.unproxy(data);
        lastOrderValues.put(structObjId, dataInt.getIntegerValue());
    }

    @Override
    public ActionResult getResult() {
        // TODO Auto-generated method stub
        return null;
    }

}
