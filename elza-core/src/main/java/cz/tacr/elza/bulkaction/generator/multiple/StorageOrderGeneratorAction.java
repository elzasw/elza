package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.bulkaction.generator.LevelWithItems;
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
import cz.tacr.elza.service.DescriptionItemService;

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
            storeLastUsedOrder(structObjId, orderItem);
        } else {
            // try to add new value
            Integer lastUsedValue = lastOrderValues.get(structObjId);
            if (lastUsedValue == null) {
                // storage without defined last value -> ignore
                return;
            }
            // add new item
            lastUsedValue++;
            addOrderWithValue(level, lastUsedValue);
            lastOrderValues.put(structObjId, lastUsedValue);
        }
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
        di.setValue(lastUsedValue);

        ArrDescItem descItem = new ArrDescItem();
        descItem.setItemType(orderItemType.getEntity());
        descItem.setData(di);
        descriptionItemService.createDescriptionItem(descItem, level.getNodeId(), fundVersion, change);
    }

    private void storeLastUsedOrder(Integer structObjId, ArrDescItem orderItem) {
        ArrData data = orderItem.getData();
        // value is unknown -> return
        if (data == null) {
            return;
        }
        Validate.isTrue(data.getDataTypeId() == DataType.INT.getId(), "Unexpected data type: %i", data.getDataTypeId());
        ArrDataInteger dataInt = HibernateUtils.unproxy(data);
        lastOrderValues.put(structObjId, dataInt.getValue());
    }

    @Override
    public ActionResult getResult() {
        // TODO Auto-generated method stub
        return null;
    }

}
