package cz.tacr.elza.dataexchange.output.writer.cam;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import cz.tacr.cam.schema.cam.CodeXml;
import cz.tacr.cam.schema.cam.IntegerXml;
import cz.tacr.cam.schema.cam.ItemEnumXml;
import cz.tacr.cam.schema.cam.ItemIntegerXml;
import cz.tacr.cam.schema.cam.ItemStringXml;
import cz.tacr.cam.schema.cam.ItemsXml;
import cz.tacr.cam.schema.cam.ObjectFactory;
import cz.tacr.cam.schema.cam.PartXml;
import cz.tacr.cam.schema.cam.StringXml;
import org.apache.commons.lang3.Validate;
import org.drools.core.util.StringUtils;

import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.exception.SystemException;

public class CamUtils {

    final protected static ObjectFactory objectFactory = new ObjectFactory();

    public final static String CAM_SCHEMA = "http://cam.tacr.cz/2019";

    public static ObjectFactory getObjectFactory() {
        return objectFactory;
    }

    private static void addItemToPart(PartXml part, Object item) {
        Validate.notNull(item);

        ItemsXml itm = part.getItms();
        Validate.notNull(itm);
        List<Object> items = itm.getItems();

        items.add(item);
    }

    public static void addItemEnum(PartXml part, CamItemType itemType, String specType) {

        Validate.isTrue(itemType.getDataType() == CamDataType.ENUM);
        // check if specification is valid
        Validate.isTrue(itemType.isValidSpec(specType), "Invalid specification: %s", specType);

        CodeXml codeSpec = specType != null ? new CodeXml(specType) : null;

        ItemEnumXml itemEnum = objectFactory.createItemEnumXml();
        itemEnum.setT(new CodeXml(itemType.name()));
        itemEnum.setS(codeSpec);

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
        itemInt.setT(new CodeXml(itemType.name()));
        itemInt.setValue(new IntegerXml(value));

        addItemToPart(part, itemInt);
    }

    /*public static void addItemUnitDate(Part part, CamItemType itemType, ParUnitdate unitDate) {
        Validate.isTrue(!itemType.isUseSpecification());
        if (unitDate == null) {
            return;
        }
        // empty string cannot be converted
        if (StringUtils.isEmpty(unitDate.getValueFrom()) &&
                StringUtils.isEmpty(unitDate.getValueTo())) {
            return;
        }
        String valueFrom = unitDate.getValueFrom();
        String valueTo = unitDate.getValueTo();
        if (StringUtils.isEmpty(valueFrom) ||StringUtils.isEmpty(valueTo)) {
            throw new SystemException("Value from or value to is empty: " + itemType.getDataType());
        }
        // Try to parse dates
        LocalDateTime.parse(valueFrom, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        LocalDateTime.parse(valueTo, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String format = unitDate.getFormat();
        String camFormat = convertUnitDateFormat(format);

        ItemUnitDate itemUnitDate = new ItemUnitDate();
        itemUnitDate.setT(itemType.name());

        itemUnitDate.setF(valueFrom);
        itemUnitDate.setFe(unitDate.getValueFromEstimated());
        itemUnitDate.setTo(valueTo);
        itemUnitDate.setToe(unitDate.getValueToEstimated());
        itemUnitDate.setFmt(camFormat);

        addItemToPart(part, itemUnitDate);
    }*/

    /**
     * convert elze unit date format to CAM format
     *
     * @param format
     * @return
     */
    private static String convertUnitDateFormat(String format) {
        String[] parts = format.split("-");
        if (parts.length == 1) {
            return convertUnitDateFormatPart(parts[0]);
        } else if (parts.length == 2) {
            StringBuilder sb = new StringBuilder();
            sb.append(convertUnitDateFormatPart(parts[0]))
                    .append("-")
                    .append(convertUnitDateFormatPart(parts[1]));
            return sb.toString();
        }
        throw new IllegalStateException("Incorrect date format: " + format);
    }

    private static String convertUnitDateFormatPart(String partValue) {
        switch (partValue) {
        case UnitDateConvertor.CENTURY:
            return "C";
        case UnitDateConvertor.YEAR:
            return "Y";
        case UnitDateConvertor.YEAR_MONTH:
            // CAM nepodporuje tento format primo
            return "D";
        case UnitDateConvertor.DATE:
            return "D";
        case UnitDateConvertor.DATE_TIME:
            return "DT";
        }
        throw new IllegalStateException("Incorrect date format: " + partValue);
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
        itemString.setT(new CodeXml(itemType.name()));
        itemString.setValue(new StringXml(value));

        addItemToPart(part, itemString);
    }

}
