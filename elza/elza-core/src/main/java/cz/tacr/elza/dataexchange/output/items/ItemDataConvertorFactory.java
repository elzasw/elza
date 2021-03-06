package cz.tacr.elza.dataexchange.output.items;

import cz.tacr.elza.core.data.DataType;

import java.util.EnumMap;
import java.util.Map;

public class ItemDataConvertorFactory {

    public IntValueConvertor createIntValueConvertor() {
        return new IntValueConvertor();
    }

    public DecimalValueConvertor createDecimalValueConvertor() {
        return new DecimalValueConvertor();
    }

    public StringValueConvertor createStringValueConvertor() {
        return new StringValueConvertor();
    }

    public TextValueConvertor createTextValueConvertor() {
        return new TextValueConvertor();
    }

    public EnumValueConvertor createEnumValueConvertor() {
        return new EnumValueConvertor();
    }

    public UnitDateValueConvertor createUnitDateValueConvertor() {
        return new UnitDateValueConvertor();
    }

    public APRefConvertor createAPRefConvertor() {
        return new APRefConvertor();
    }

    public GeoLocationConvertor createGeLocationConvertor() {
        return new GeoLocationConvertor();
    }

    public UnitidValueConvertor createUnitidConvertor() {
        return new UnitidValueConvertor();
    }

    private JsonTableConvertor createJsonTableConvertor() {
        return new JsonTableConvertor();
    }

    public StructObjRefConvertor createStructObjectRefConvertor() {
        return new StructObjRefConvertor();
    }

    public FileRefConvertor createFileRefConvertor() {
        return new FileRefConvertor();
    }

    public UriRefConvertor createUriRefConvertor() {
        return new UriRefConvertor();
    }

    private ItemDataConvertor createBitConvertor() {
        return new BitValueConvertor();
    }

    private ItemDataConvertor createDateConvertor() {
        return new DateValueConvertor();
    }

    public final Map<DataType, ItemDataConvertor> createAll() {
        Map<DataType, ItemDataConvertor> map = new EnumMap<>(DataType.class);
        map.put(DataType.STRING, createStringValueConvertor());
        map.put(DataType.TEXT, createTextValueConvertor());
        map.put(DataType.COORDINATES, createGeLocationConvertor());
        map.put(DataType.FORMATTED_TEXT, createTextValueConvertor());
        map.put(DataType.UNITID, createUnitidConvertor());
        map.put(DataType.INT, createIntValueConvertor());
        map.put(DataType.DECIMAL, createDecimalValueConvertor());
        map.put(DataType.ENUM, createEnumValueConvertor());
        map.put(DataType.UNITDATE, createUnitDateValueConvertor());
        map.put(DataType.RECORD_REF, createAPRefConvertor());
        map.put(DataType.STRUCTURED, createStructObjectRefConvertor());
        map.put(DataType.JSON_TABLE, createJsonTableConvertor());
        map.put(DataType.FILE_REF, createFileRefConvertor());
        map.put(DataType.URI_REF, createUriRefConvertor());
        map.put(DataType.BIT, createBitConvertor());
        map.put(DataType.DATE, createDateConvertor());
        return map;
    }

}
