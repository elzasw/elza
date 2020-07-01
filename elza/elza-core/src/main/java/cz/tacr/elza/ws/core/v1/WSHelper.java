package cz.tacr.elza.ws.core.v1;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.data.CalendarType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.ws.types.v1.FundIdentifiers;
import cz.tacr.elza.ws.types.v1.ItemEnum;
import cz.tacr.elza.ws.types.v1.ItemLong;
import cz.tacr.elza.ws.types.v1.ItemString;

@Component
public class WSHelper {
    @Autowired
    ArrangementService arrangementService;

    @Autowired
    StaticDataService staticDataService;

    public Integer getFundId(FundIdentifiers fundInfo) {
        Validate.notNull(fundInfo);
        if (fundInfo.getId() != null) {
            return Integer.valueOf(fundInfo.getId());
        } else {
            Validate.notNull(fundInfo.getUuid(), "Fund ID or UUID have to be specified");
            ArrNode node = arrangementService.findNodeByUuid(fundInfo.getUuid());
            return node.getFundId();
        }
    }

    public ArrFund getFund(FundIdentifiers fundInfo) {
        Integer fundId = getFundId(fundInfo);
        ArrFund fund = arrangementService.getFund(fundId);
        return fund;
    }

    public void convertItem(ArrItem trgItem, Object srcItem) {
        if (srcItem instanceof ItemString) {
            convertItemString(trgItem, (ItemString) srcItem);
        } else if (srcItem instanceof ItemLong) {
            convertItemLong(trgItem, (ItemLong) srcItem);
        } else if (srcItem instanceof ItemEnum) {
            convertItemEnum(trgItem, (ItemEnum) srcItem);
        }
        else {
            Validate.isTrue(false, "Cannot convert srcItem to trgItem: %s", srcItem);
        }
        
    }

    private void convertItemEnum(ArrItem trgItem, ItemEnum srcItem) {
        ItemType itemType = prepareItem(trgItem, srcItem.getType(), srcItem.getSpec());
        ArrData data = null;
        switch (itemType.getDataType()) {
        case ENUM:
            ArrDataNull dn = new ArrDataNull();
            data = dn;
            break;
        default:
            Validate.isTrue(false, "Cannot convert enum to data type: %s, item type: %s", itemType.getDataType(),
                            srcItem.getType());
        }
        data.setDataType(itemType.getDataType().getEntity());
        trgItem.setData(data);

    }

    /**
     * Convert long item to ArrItem
     * 
     * @param trgItem
     * @param srcItem
     */
    private void convertItemLong(ArrItem trgItem, ItemLong srcItem) {
        ItemType itemType = prepareItem(trgItem, srcItem.getType(), srcItem.getSpec());
        ArrData data = null;
        switch (itemType.getDataType()) {
        case STRING:
            ArrDataString ds = new ArrDataString();
            ds.setValue(Long.toString(srcItem.getValue()));
            data = ds;
            break;
        case TEXT:
            ArrDataText dt = new ArrDataText();
            dt.setValue(Long.toString(srcItem.getValue()));
            data = dt;
            break;
        case INT:
            ArrDataInteger di = new ArrDataInteger();
            di.setValue((int) srcItem.getValue());
            data = di;
            break;
        default:
            Validate.isTrue(false, "Cannot convert long to data type: %s, item type: %s", itemType.getDataType(),
                            srcItem.getType());
        }
        data.setDataType(itemType.getDataType().getEntity());
        trgItem.setData(data);
    }

    /**
     * Convert string item to ArrItem
     * 
     * @param trgItem
     * @param srcItem
     */
    private void convertItemString(ArrItem trgItem, ItemString srcItem) {
        ItemType itemType = prepareItem(trgItem, srcItem.getType(), srcItem.getSpec());
        ArrData data = null;
        switch (itemType.getDataType()) {
        case STRING:
            ArrDataString ds = new ArrDataString();
            ds.setValue(srcItem.getValue());
            data = ds;
            break;
        case TEXT:
            ArrDataText dt = new ArrDataText();
            dt.setValue(srcItem.getValue());
            data = dt;
            break;
        case INT:
            ArrDataInteger di = new ArrDataInteger();
            di.setValue(Integer.valueOf(srcItem.getValue()));
            data = di;
            break;
        case UNITDATE:
            ArrDataUnitdate du = ArrDataUnitdate.valueOf(CalendarType.GREGORIAN, srcItem.getValue());
            data = du;
            break;
        default:
            Validate.isTrue(false, "Cannot convert string to data type: %s, item type: %s", itemType.getDataType(),
                            srcItem.getType());
        }
        data.setDataType(itemType.getDataType().getEntity());
        trgItem.setData(data);
    }

    private ItemType prepareItem(ArrItem trgItem, String type, String spec) {
        StaticDataProvider sdp = staticDataService.getData();
        ItemType itemType = sdp.getItemTypeByCode(type);
        Validate.notNull(itemType, "Item type not found: {}", type);

        trgItem.setItemType(itemType.getEntity());

        if (itemType.hasSpecifications()) {
            Validate.notNull(spec, "Missing specification for item type: %s", type);
            RulItemSpec itemSpec = itemType.getItemSpecByCode(spec);
            Validate.notNull(itemSpec, "Cannot find specification for item type: %s, spec code: %s", type, spec);

            trgItem.setItemSpec(itemSpec);
        } else {
            Validate.isTrue(spec == null, "Item type cannot have specification: %s, value: %s", type, spec);
        }
        return itemType;
    }

    /**
     * Iterate all items and fill in position
     * 
     * @param result
     */
    public void countPositions(List<? extends ArrItem> result) {
        final Map<RulItemType, Integer> positionMap = new HashMap<>();
        result.stream().forEach(item -> {
            Integer position = positionMap.compute(item.getItemType(), (k, v) -> v == null ? 1 : ++v);
            item.setPosition(position);
        });

    }
}
