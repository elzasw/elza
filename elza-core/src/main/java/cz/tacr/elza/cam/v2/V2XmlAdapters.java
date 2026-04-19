package cz.tacr.elza.cam.v2;

import cz.tacr.cam.v2.schema.cam.CodeXml;
import cz.tacr.cam.v2.schema.cam.ItemBinaryXml;
import cz.tacr.cam.v2.schema.cam.ItemBooleanXml;
import cz.tacr.cam.v2.schema.cam.ItemEntityRefXml;
import cz.tacr.cam.v2.schema.cam.ItemEnumXml;
import cz.tacr.cam.v2.schema.cam.ItemIntegerXml;
import cz.tacr.cam.v2.schema.cam.ItemLinkXml;
import cz.tacr.cam.v2.schema.cam.ItemStringXml;
import cz.tacr.cam.v2.schema.cam.ItemUnitDateXml;
import cz.tacr.elza.cam.adapter.XmlAdapterFactory;
import cz.tacr.elza.cam.adapter.XmlBinaryItemAdapter;
import cz.tacr.elza.cam.adapter.XmlBooleanItemAdapter;
import cz.tacr.elza.cam.adapter.XmlCodeAdapter;
import cz.tacr.elza.cam.adapter.XmlEntityRefItemAdapter;
import cz.tacr.elza.cam.adapter.XmlEnumItemAdapter;
import cz.tacr.elza.cam.adapter.XmlIntegerItemAdapter;
import cz.tacr.elza.cam.adapter.XmlItemAdapter;
import cz.tacr.elza.cam.adapter.XmlLinkItemAdapter;
import cz.tacr.elza.cam.adapter.XmlStringItemAdapter;
import cz.tacr.elza.cam.adapter.XmlUnitDateItemAdapter;

/**
 * Adaptery pro CAM v2 schéma.
 *
 * Obal nad JAXB typy z {@code cz.tacr.cam.v2.schema.cam}, který skrývá
 * verzově specifické názvy getterů (getType/getSpec/getFrom/getFormat/...).
 */
public final class V2XmlAdapters implements XmlAdapterFactory {

    public static final V2XmlAdapters INSTANCE = new V2XmlAdapters();

    private V2XmlAdapters() {
    }

    @Override
    public XmlItemAdapter wrapItem(Object xmlItem) {
        if (xmlItem instanceof ItemBinaryXml) {
            return new V2BinaryItem((ItemBinaryXml) xmlItem);
        } else if (xmlItem instanceof ItemBooleanXml) {
            return new V2BooleanItem((ItemBooleanXml) xmlItem);
        } else if (xmlItem instanceof ItemEntityRefXml) {
            return new V2EntityRefItem((ItemEntityRefXml) xmlItem);
        } else if (xmlItem instanceof ItemEnumXml) {
            return new V2EnumItem((ItemEnumXml) xmlItem);
        } else if (xmlItem instanceof ItemIntegerXml) {
            return new V2IntegerItem((ItemIntegerXml) xmlItem);
        } else if (xmlItem instanceof ItemLinkXml) {
            return new V2LinkItem((ItemLinkXml) xmlItem);
        } else if (xmlItem instanceof ItemStringXml) {
            return new V2StringItem((ItemStringXml) xmlItem);
        } else if (xmlItem instanceof ItemUnitDateXml) {
            return new V2UnitDateItem((ItemUnitDateXml) xmlItem);
        }
        throw new IllegalArgumentException("Unsupported v2 item type: "
                + (xmlItem == null ? "null" : xmlItem.getClass().getName()));
    }

    public static XmlCodeAdapter wrap(CodeXml code) {
        return code == null ? null : new V2Code(code);
    }

    private record V2Code(CodeXml raw) implements XmlCodeAdapter {
        @Override public String getValue() { return raw.getValue(); }
    }

    private record V2BinaryItem(ItemBinaryXml raw) implements XmlBinaryItemAdapter {
        @Override public String getUuid() { return raw.getUuid().getValue(); }
        @Override public XmlCodeAdapter getType() { return wrap(raw.getType()); }
        @Override public XmlCodeAdapter getSpec() { return wrap(raw.getSpec()); }
        @Override public Object getRaw() { return raw; }
        @Override public byte[] getBinaryValue() { return raw.getValue().getValue(); }
    }

    private record V2BooleanItem(ItemBooleanXml raw) implements XmlBooleanItemAdapter {
        @Override public String getUuid() { return raw.getUuid().getValue(); }
        @Override public XmlCodeAdapter getType() { return wrap(raw.getType()); }
        @Override public XmlCodeAdapter getSpec() { return wrap(raw.getSpec()); }
        @Override public Object getRaw() { return raw; }
        @Override public Boolean getBoolValue() { return raw.getValue().isValue(); }
    }

    private record V2EntityRefItem(ItemEntityRefXml raw) implements XmlEntityRefItemAdapter {
        @Override public String getUuid() { return raw.getUuid().getValue(); }
        @Override public XmlCodeAdapter getType() { return wrap(raw.getType()); }
        @Override public XmlCodeAdapter getSpec() { return wrap(raw.getSpec()); }
        @Override public Object getRaw() { return raw; }
        @Override public String getRefIdOrUuid() { return CamHelper.getEntityIdorUuid(raw); }
    }

    private record V2EnumItem(ItemEnumXml raw) implements XmlEnumItemAdapter {
        @Override public String getUuid() { return raw.getUuid().getValue(); }
        @Override public XmlCodeAdapter getType() { return wrap(raw.getType()); }
        @Override public XmlCodeAdapter getSpec() { return wrap(raw.getSpec()); }
        @Override public Object getRaw() { return raw; }
    }

    private record V2IntegerItem(ItemIntegerXml raw) implements XmlIntegerItemAdapter {
        @Override public String getUuid() { return raw.getUuid().getValue(); }
        @Override public XmlCodeAdapter getType() { return wrap(raw.getType()); }
        @Override public XmlCodeAdapter getSpec() { return wrap(raw.getSpec()); }
        @Override public Object getRaw() { return raw; }
        @Override public int getIntValue() { return raw.getValue().getValue().intValue(); }
    }

    private record V2LinkItem(ItemLinkXml raw) implements XmlLinkItemAdapter {
        @Override public String getUuid() { return raw.getUuid().getValue(); }
        @Override public XmlCodeAdapter getType() { return wrap(raw.getType()); }
        @Override public XmlCodeAdapter getSpec() { return wrap(raw.getSpec()); }
        @Override public Object getRaw() { return raw; }
        @Override public String getUrl() { return raw.getUrl().getValue(); }
        @Override public String getDescription() {
            return raw.getName() == null ? null : raw.getName().getValue();
        }
    }

    private record V2StringItem(ItemStringXml raw) implements XmlStringItemAdapter {
        @Override public String getUuid() { return raw.getUuid().getValue(); }
        @Override public XmlCodeAdapter getType() { return wrap(raw.getType()); }
        @Override public XmlCodeAdapter getSpec() { return wrap(raw.getSpec()); }
        @Override public Object getRaw() { return raw; }
        @Override public String getStringValue() { return raw.getValue().getValue(); }
    }

    private record V2UnitDateItem(ItemUnitDateXml raw) implements XmlUnitDateItemAdapter {
        @Override public String getUuid() { return raw.getUuid().getValue(); }
        @Override public XmlCodeAdapter getType() { return wrap(raw.getType()); }
        @Override public XmlCodeAdapter getSpec() { return wrap(raw.getSpec()); }
        @Override public Object getRaw() { return raw; }
        @Override public String getValueFrom() { return raw.getFrom(); }
        @Override public String getValueTo() { return raw.getTo(); }
        @Override public String getFormat() { return raw.getFormat(); }
        @Override public Boolean isFromEstimate() { return raw.isFromEstimate(); }
        @Override public Boolean isToEstimate() { return raw.isToEstimate(); }
    }
}
