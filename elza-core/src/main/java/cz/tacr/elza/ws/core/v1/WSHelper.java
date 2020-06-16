package cz.tacr.elza.ws.core.v1;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.data.CalendarType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.ws.types.v1.FundIdentifiers;
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
        } else {
            Validate.isTrue(false, "Cannot convert srcItem to trgItem: {}", srcItem);
        }
        
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
            Validate.isTrue(false, "Cannot convert string to data type: {}, item type: {}", itemType.getDataType(),
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
            Validate.notNull(spec, "Missing specification for item type: {}", type);
            RulItemSpec itemSpec = itemType.getItemSpecByCode(spec);
            Validate.notNull(itemSpec, "Cannot find specification for item type: {}, spec code: {}", type, spec);

            trgItem.setItemSpec(itemSpec);
        } else {
            Validate.isTrue(spec == null, "Item type cannot have specification: {}, value: {}", type, spec);
        }
        return itemType;
    }
}
