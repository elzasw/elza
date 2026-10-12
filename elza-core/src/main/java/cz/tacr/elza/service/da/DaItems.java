package cz.tacr.elza.service.da;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StructType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Items a script plans: the items of a level ({@link DaLevelItems}) or a group of items beside
 * them ({@link DaItemGroup}) - the items of a new structured object, or the items a level gets
 * only when it is created.
 *
 * The script names item types and specifications by code; they are resolved and the values are
 * read here, so a wrong code fails at once with the script named, and a value not written the
 * way it can be read fails with the file, the unit and the element named.
 *
 * @param <T> the concrete class, so that the calls can be chained
 */
public abstract class DaItems<T extends DaItems<T>> {

    /**
     * One planned item; the data are not stored yet. A structured item
     * ({@link #structuredItem(String)}) refers to a structured object that does not exist yet:
     * {@code structured} holds its items, and the object is created together with the item.
     */
    public record Item(RulItemType itemType, @Nullable RulItemSpec itemSpec, ArrData data,
                       @Nullable DaItemGroup structured) {
    }

    private final StaticDataProvider sdp;
    private final String scriptName;
    private final String eadHref;

    private final List<Item> items = new ArrayList<>();

    DaItems(StaticDataProvider sdp, String scriptName, @Nullable String eadHref) {
        this.sdp = sdp;
        this.scriptName = scriptName;
        this.eadHref = eadHref;
    }

    /** What the items belong to, for the messages - e.g. the div the level stands for. */
    protected abstract String subject();

    @SuppressWarnings("unchecked")
    private T self() {
        return (T) this;
    }

    /** An item without a value - an item type of data type ENUM, e.g. the level type. */
    public T item(String itemTypeCode, String itemSpecCode) {
        RulItemType itemType = resolveType(itemTypeCode);
        requireDataType(itemType, DataType.ENUM);
        return add(itemType, itemSpecCode, new ArrDataNull());
    }

    /**
     * An item with the given text, e.g. the name of the level taken from the div. Blank text adds
     * no item.
     */
    public T item(String itemTypeCode, @Nullable String itemSpecCode, @Nullable String text) {
        if (StringUtils.isBlank(text)) {
            return self();
        }
        RulItemType itemType = resolveType(itemTypeCode);
        DataType dataType = requireDataType(itemType, DataType.STRING, DataType.TEXT);
        ArrData data;
        if (dataType == DataType.TEXT) {
            data = new ArrDataText(text.strip());
        } else {
            String value = StringUtils.normalizeSpace(text);
            if (value.length() > StringLength.LENGTH_1000) {
                // the text comes from the package (a label, a value of the EAD) - a problem of the package
                throw AipProblemException.metadata("Balíček obsahuje u " + subject() + " hodnotu prvku popisu "
                        + itemTypeCode + " delší než " + StringLength.LENGTH_1000 + " znaků.", eadHref, null);
            }
            data = new ArrDataString(value);
        }
        return add(itemType, itemSpecCode, data);
    }

    /**
     * An item with the value of the element, read according to the profile of EAD. An element
     * carrying no value adds no item.
     */
    public T item(String itemTypeCode, @Nullable String itemSpecCode, DaImportElement element) {
        RulItemType itemType = resolveType(itemTypeCode);
        RulItemSpec itemSpec = resolveSpec(itemType, itemSpecCode);
        ArrData data;
        try {
            data = DidElementConverters.convert(element.getSource(),
                                                new DidElementConverters.Mapping(itemType, itemSpec));
        } catch (EadContentException e) {
            throw AipProblemException.metadata("Inherentní archivní popis v souboru '" + eadHref
                    + "' obsahuje u jednotky popisu '" + element.getUnitId() + "' element " + element
                    + ", který nelze převzít: " + e.getMessage() + ".", eadHref, e);
        }
        if (data != null) {
            items.add(new Item(itemType, itemSpec, data, null));
        }
        return self();
    }

    /**
     * An item referring to a new structured object - e.g. a restriction of access - whose items
     * are added to the returned group. The item type must refer to an anonymous structured type:
     * the object belongs to the item alone and goes with it. An item of a shared structured type
     * (e.g. a storage unit) cannot be planned - such an object would have to be found, not made.
     *
     * A structured object has no value before it is created, so the item is not compared with
     * the items of an existing level: a level that has an item of the type keeps it, and the
     * item type cannot be a match key.
     */
    public DaItemGroup structuredItem(String itemTypeCode) {
        RulItemType itemType = resolveType(itemTypeCode);
        requireDataType(itemType, DataType.STRUCTURED);
        StructType structType = sdp.getStructuredTypeById(itemType.getStructuredTypeId());
        RulStructuredType structuredType = structType.getStructuredType();
        if (!Boolean.TRUE.equals(structuredType.getAnonymous())) {
            throw scriptError("prvek popisu " + itemTypeCode + " odkazuje na sdílený strukturovaný typ "
                    + structType.getCode() + "; skript může založit jen anonymní strukturovaný objekt, "
                    + "který patří pouze tomuto prvku popisu");
        }
        RulItemSpec itemSpec = resolveSpec(itemType, null);
        DaItemGroup group = group("strukturovaného objektu " + itemTypeCode + " u " + subject(), structuredType);
        ArrDataStructureRef data = new ArrDataStructureRef();
        data.setDataType(DataType.STRUCTURED.getEntity());
        items.add(new Item(itemType, itemSpec, data, group));
        return group;
    }

    private T add(RulItemType itemType, @Nullable String itemSpecCode, ArrData data) {
        RulItemSpec itemSpec = resolveSpec(itemType, itemSpecCode);
        data.setDataType(DataType.fromId(itemType.getDataTypeId()).getEntity());
        items.add(new Item(itemType, itemSpec, data, null));
        return self();
    }

    /** A group of items beside these, resolved the same way. */
    DaItemGroup group(String subject, @Nullable RulStructuredType structuredType) {
        return new DaItemGroup(sdp, scriptName, eadHref, subject, structuredType);
    }

    RulItemType resolveType(String itemTypeCode) {
        ItemType itemType = sdp.getItemTypeByCode(itemTypeCode);
        if (itemType == null) {
            throw scriptError("neznámý prvek popisu " + itemTypeCode);
        }
        return itemType.getEntity();
    }

    @Nullable
    RulItemSpec resolveSpec(RulItemType itemType, @Nullable String itemSpecCode) {
        if (itemSpecCode == null) {
            if (Boolean.TRUE.equals(itemType.getUseSpecification())) {
                throw scriptError("prvek popisu " + itemType.getCode() + " vyžaduje specifikaci");
            }
            return null;
        }
        RulItemSpec itemSpec = sdp.getItemTypeByCode(itemType.getCode()).getItemSpecByCode(itemSpecCode);
        if (itemSpec == null) {
            throw scriptError("specifikace " + itemSpecCode + " nepatří k prvku popisu " + itemType.getCode());
        }
        return itemSpec;
    }

    private DataType requireDataType(RulItemType itemType, DataType... allowed) {
        DataType dataType = DataType.fromId(itemType.getDataTypeId());
        for (DataType a : allowed) {
            if (a == dataType) {
                return dataType;
            }
        }
        throw scriptError("prvek popisu " + itemType.getCode() + " má datový typ " + dataType
                + ", do kterého nelze zapsat tuto hodnotu");
    }

    SystemException scriptError(String reason) {
        return new SystemException("Skript " + scriptName + " u " + subject() + ": " + reason, BaseCode.INVALID_STATE);
    }

    /** The planned items, in the order of planning. */
    public List<Item> getItems() {
        return items;
    }
}
