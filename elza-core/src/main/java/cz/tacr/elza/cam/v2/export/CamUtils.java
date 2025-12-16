package cz.tacr.elza.cam.v2.export;

import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import cz.tacr.cam.v2.schema.cam.CodeXml;
import cz.tacr.cam.v2.schema.cam.EntityXml;
import cz.tacr.cam.v2.schema.cam.IntegerXml;
import cz.tacr.cam.v2.schema.cam.ItemEnumXml;
import cz.tacr.cam.v2.schema.cam.ItemIntegerXml;
import cz.tacr.cam.v2.schema.cam.ItemStringXml;
import cz.tacr.cam.v2.schema.cam.ItemsXml;
import cz.tacr.cam.v2.schema.cam.ObjectFactory;
import cz.tacr.cam.v2.schema.cam.PartXml;
import cz.tacr.cam.v2.schema.cam.PartsXml;
import cz.tacr.cam.v2.schema.cam.StringXml;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.writer.cam.CamDataType;
import cz.tacr.elza.dataexchange.output.writer.cam.CamItemType;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.exception.SystemException;

public class CamUtils {

    final protected static ObjectFactory objectFactory = new ObjectFactory();

    public static ObjectFactory getObjectFactory() {
        return objectFactory;
    }

    private static void addItemToPart(PartXml part, Object item) {
    	Objects.requireNonNull(item);

        ItemsXml itm = part.getItems();
        Objects.requireNonNull(itm);
        List<Object> items = itm.getItems();

        items.add(item);
    }

    public static void addItemEnum(PartXml part, CamItemType itemType, String specType) {

        Validate.isTrue(itemType.getDataType() == CamDataType.ENUM);
        // check if specification is valid
        Validate.isTrue(itemType.isValidSpec(specType), "Invalid specification: %s", specType);

        CodeXml codeSpec = specType != null ? new CodeXml(specType) : null;

        ItemEnumXml itemEnum = objectFactory.createItemEnumXml();
        itemEnum.setType(new CodeXml(itemType.name()));
        itemEnum.setSpec(codeSpec);

        addItemToPart(part, itemEnum);
    }

    public static void addItemNumber(PartXml part, CamItemType itemType, Long value) {
        Validate.isTrue(itemType.getDataType() == CamDataType.NUMBER);
        // ignore empty value
        if (value == null) {
            return;
        }
        Validate.isTrue(!itemType.isUseSpecification());

        ItemIntegerXml itemInt = new ItemIntegerXml();
        itemInt.setType(new CodeXml(itemType.name()));
        itemInt.setValue(new IntegerXml(value));

        addItemToPart(part, itemInt);
    }

    public static void addItemString(PartXml part, CamItemType itemType, String value) {
        Validate.isTrue(!itemType.isUseSpecification());
        // Empty values are ignored
        if (StringUtils.isEmpty(value)) {
            return;
        }

        switch (itemType.getDataType()) {
        case TEXT_50:
            Validate.isTrue(value.length() <= 50);
            break;
        case TEXT_250:
            Validate.isTrue(value.length() <= 250);
            break;
        case MEMO:
            break;
        default:
            throw new SystemException("Unexpected type for string value: " + itemType.getDataType());
        }

        ItemStringXml itemString = objectFactory.createItemStringXml();
        itemString.setType(new CodeXml(itemType.name()));
        itemString.setValue(new StringXml(value));

        addItemToPart(part, itemString);
    }

    public static ApBindingItem findBindingItemById(List<ApBindingItem> bindingParts, Integer partId) {
        if (CollectionUtils.isNotEmpty(bindingParts)) {
            for (ApBindingItem bindingItem : bindingParts) {
                if (bindingItem.getPart() != null && bindingItem.getPart().getPartId().equals(partId)) {
                    return bindingItem;
                }
            }
        }
        return null;
    }

    public static PartXml getPrefName(EntityXml entityXml) {
        PartsXml parts = entityXml.getParts();
        List<PartXml> partList = parts.getPart();
        for (PartXml partXml : partList) {
            if (StaticDataProvider.DEFAULT_PART_TYPE.equals(partXml.getType().value())) {
                return partXml;
            }
        }
        return null;
    }
}
