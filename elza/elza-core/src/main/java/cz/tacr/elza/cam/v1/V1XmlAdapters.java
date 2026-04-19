package cz.tacr.elza.cam.v1;

import cz.tacr.cam.v1.schema.cam.CodeXml;
import cz.tacr.cam.v1.schema.cam.ItemBinaryXml;
import cz.tacr.cam.v1.schema.cam.ItemBooleanXml;
import cz.tacr.cam.v1.schema.cam.ItemEntityRefXml;
import cz.tacr.cam.v1.schema.cam.ItemEnumXml;
import cz.tacr.cam.v1.schema.cam.ItemIntegerXml;
import cz.tacr.cam.v1.schema.cam.ItemLinkXml;
import cz.tacr.cam.v1.schema.cam.ItemStringXml;
import cz.tacr.cam.v1.schema.cam.ItemUnitDateXml;
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
 * Adaptery pro CAM v1 schéma.
 *
 * Obal nad JAXB typy z {@code cz.tacr.cam.v1.schema.cam}, který skrývá
 * verzově specifické názvy getterů (getT/getS/getF/getFmt/...).
 */
public final class V1XmlAdapters implements XmlAdapterFactory {

    public static final V1XmlAdapters INSTANCE = new V1XmlAdapters();

    private V1XmlAdapters() {
    }

    @Override
    public XmlItemAdapter wrapItem(Object xmlItem) {
        if (xmlItem instanceof ItemBinaryXml) {
            return new V1BinaryItem((ItemBinaryXml) xmlItem);
        } else if (xmlItem instanceof ItemBooleanXml) {
            return new V1BooleanItem((ItemBooleanXml) xmlItem);
        } else if (xmlItem instanceof ItemEntityRefXml) {
            return new V1EntityRefItem((ItemEntityRefXml) xmlItem);
        } else if (xmlItem instanceof ItemEnumXml) {
            return new V1EnumItem((ItemEnumXml) xmlItem);
        } else if (xmlItem instanceof ItemIntegerXml) {
            return new V1IntegerItem((ItemIntegerXml) xmlItem);
        } else if (xmlItem instanceof ItemLinkXml) {
            return new V1LinkItem((ItemLinkXml) xmlItem);
        } else if (xmlItem instanceof ItemStringXml) {
            return new V1StringItem((ItemStringXml) xmlItem);
        } else if (xmlItem instanceof ItemUnitDateXml) {
            return new V1UnitDateItem((ItemUnitDateXml) xmlItem);
        }
        throw new IllegalArgumentException("Unsupported v1 item type: "
                + (xmlItem == null ? "null" : xmlItem.getClass().getName()));
    }

    public static XmlCodeAdapter wrap(CodeXml code) {
        return code == null ? null : new V1Code(code);
    }

    private record V1Code(CodeXml raw) implements XmlCodeAdapter {
        @Override public String getValue() { return raw.getValue(); }
    }

    private record V1BinaryItem(ItemBinaryXml raw) implements XmlBinaryItemAdapter {
        @Override public String getUuid() { return raw.getUuid().getValue(); }
        @Override public XmlCodeAdapter getType() { return wrap(raw.getT()); }
        @Override public XmlCodeAdapter getSpec() { return wrap(raw.getS()); }
        @Override public Object getRaw() { return raw; }
        @Override public byte[] getBinaryValue() { return raw.getValue().getValue(); }
    }

    private record V1BooleanItem(ItemBooleanXml raw) implements XmlBooleanItemAdapter {
        @Override public String getUuid() { return raw.getUuid().getValue(); }
        @Override public XmlCodeAdapter getType() { return wrap(raw.getT()); }
        @Override public XmlCodeAdapter getSpec() { return wrap(raw.getS()); }
        @Override public Object getRaw() { return raw; }
        @Override public Boolean getBoolValue() { return raw.getValue().isValue(); }
    }

    private record V1EntityRefItem(ItemEntityRefXml raw) implements XmlEntityRefItemAdapter {
        @Override public String getUuid() { return raw.getUuid().getValue(); }
        @Override public XmlCodeAdapter getType() { return wrap(raw.getT()); }
        @Override public XmlCodeAdapter getSpec() { return wrap(raw.getS()); }
        @Override public Object getRaw() { return raw; }
        @Override public String getRefIdOrUuid() { return CamHelper.getEntityIdorUuid(raw); }
    }

    private record V1EnumItem(ItemEnumXml raw) implements XmlEnumItemAdapter {
        @Override public String getUuid() { return raw.getUuid().getValue(); }
        @Override public XmlCodeAdapter getType() { return wrap(raw.getT()); }
        @Override public XmlCodeAdapter getSpec() { return wrap(raw.getS()); }
        @Override public Object getRaw() { return raw; }
    }

    private record V1IntegerItem(ItemIntegerXml raw) implements XmlIntegerItemAdapter {
        @Override public String getUuid() { return raw.getUuid().getValue(); }
        @Override public XmlCodeAdapter getType() { return wrap(raw.getT()); }
        @Override public XmlCodeAdapter getSpec() { return wrap(raw.getS()); }
        @Override public Object getRaw() { return raw; }
        @Override public int getIntValue() { return raw.getValue().getValue().intValue(); }
    }

    private record V1LinkItem(ItemLinkXml raw) implements XmlLinkItemAdapter {
        @Override public String getUuid() { return raw.getUuid().getValue(); }
        @Override public XmlCodeAdapter getType() { return wrap(raw.getT()); }
        @Override public XmlCodeAdapter getSpec() { return wrap(raw.getS()); }
        @Override public Object getRaw() { return raw; }
        @Override public String getUrl() { return raw.getUrl().getValue(); }
        @Override public String getDescription() {
            return raw.getNm() == null ? null : raw.getNm().getValue();
        }
    }

    private record V1StringItem(ItemStringXml raw) implements XmlStringItemAdapter {
        @Override public String getUuid() { return raw.getUuid().getValue(); }
        @Override public XmlCodeAdapter getType() { return wrap(raw.getT()); }
        @Override public XmlCodeAdapter getSpec() { return wrap(raw.getS()); }
        @Override public Object getRaw() { return raw; }
        @Override public String getStringValue() { return raw.getValue().getValue(); }
    }

    private record V1UnitDateItem(ItemUnitDateXml raw) implements XmlUnitDateItemAdapter {
        @Override public String getUuid() { return raw.getUuid().getValue(); }
        @Override public XmlCodeAdapter getType() { return wrap(raw.getT()); }
        @Override public XmlCodeAdapter getSpec() { return wrap(raw.getS()); }
        @Override public Object getRaw() { return raw; }
        @Override public String getValueFrom() { return raw.getF(); }
        @Override public String getValueTo() { return raw.getTo(); }
        @Override public String getFormat() { return raw.getFmt(); }
        @Override public Boolean isFromEstimate() { return raw.isFe(); }
        @Override public Boolean isToEstimate() { return raw.isToe(); }
    }
}
