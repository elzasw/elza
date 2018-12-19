package cz.tacr.elza.dataexchange.output.items;

import java.util.Map;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.ObjectFactory;

/**
 * Converter for description item from domain to XML object.
 */
public class ItemConvertor {

    private final StaticDataProvider staticDataProvider;

    private final Map<DataType, ItemDataConvertor> dataConvertors;

    private final ObjectFactory objectFactory = new ObjectFactory();

    public ItemConvertor(StaticDataProvider staticDataProvider, ItemDataConvertorFactory convertorFactory) {
        this.staticDataProvider = staticDataProvider;
        this.dataConvertors = convertorFactory.createAll();
    }

    /**
     * Converts domain object ArrItem to XML DescriptionItem object.
     *
     * @param item not-null
     * @return converted object
     */
    public final DescriptionItem convert(ArrItem item) {
        DescriptionItem converted = convert(item.getData());

        ItemType itemType = staticDataProvider.getItemTypeById(item.getItemTypeId());
        converted.setT(itemType.getCode());
        if (item.getItemSpecId() != null) {
            RulItemSpec itemSpec = itemType.getItemSpecById(item.getItemSpecId());
            converted.setS(itemSpec.getCode());
        }
        return converted;
    }

    private DescriptionItem convert(ArrData data) {
        if (data == null) {
            return objectFactory.createDescriptionItemUndefined();
        }
        // data must be initialized - no hidden fetches per item
        Validate.isTrue(HibernateUtils.isInitialized(data));

        ItemDataConvertor convertor = dataConvertors.get(data.getType());
        if (convertor == null) {
            throw new IllegalStateException("Unsupported data type:" + data.getDataType());
        }
        DescriptionItem item = convertor.convert(data, objectFactory);
        return item;
    }
}
