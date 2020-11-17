package cz.tacr.elza.dataexchange.output.items;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.ObjectFactory;
import org.apache.commons.lang.Validate;
import org.hibernate.Hibernate;

import java.util.Map;

/**
 * Converter for description item from domain to XML object.
 */
public class ItemApConvertor {

    private final StaticDataProvider staticDataProvider;

    private final Map<DataType, ItemDataConvertor> dataConvertors;

    private final ObjectFactory objectFactory = new ObjectFactory();

    public ItemApConvertor(StaticDataProvider staticDataProvider, ItemDataConvertorFactory convertorFactory) {
        this.staticDataProvider = staticDataProvider;
        this.dataConvertors = convertorFactory.createAll();
    }

    /**
     * Converts domain object ApItem to XML DescriptionItem object.
     *
     * @param item not-null
     * @return converted object
     */
    public final DescriptionItem convert(ApItem item) {
        DescriptionItem converted = convert(item.getData());

        ItemType itemType = staticDataProvider.getItemTypeById(item.getItemTypeId());
        converted.setT(itemType.getCode());
        if (item.getItemSpecId() != null) {
            RulItemSpec itemSpec = staticDataProvider.getItemSpecById(item.getItemSpecId());
            if (itemSpec == null) {
                throw new BusinessException("Missing item specification: " + item.getItemSpecId(),
                        BaseCode.DB_INTEGRITY_PROBLEM)
                                .set("ITEM_SPEC_ID", item.getItemSpecId());
            }
            converted.setS(itemSpec.getCode());
        }
        return converted;
    }

    private DescriptionItem convert(ArrData data) {
        if (data == null) {
            return objectFactory.createDescriptionItemUndefined();
        }
       /* Hibernate.initialize(data);
        // data must be initialized - no hidden fetches per item
        Validate.isTrue(HibernateUtils.isInitialized(data));*/
        //ArrData unproxydata = HibernateUtils.unproxy(data);
        ItemDataConvertor convertor = dataConvertors.get(data.getType());
        if (convertor == null) {
            throw new IllegalStateException("Unsupported data type:" + data.getDataType());
        }
        DescriptionItem item = convertor.convert(data, objectFactory);
        return item;
    }
}
