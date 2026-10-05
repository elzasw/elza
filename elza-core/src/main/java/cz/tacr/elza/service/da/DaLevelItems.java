package cz.tacr.elza.service.da;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * A level a script plans: its items and the key by which an existing level is recognized as
 * the same level. Shared by the DA_IMPORT script ({@link DaImportResult}, a level for a div of
 * the package) and the DA_MATCH script ({@link DaMatchLevel}, a level the package is placed
 * under).
 *
 * The script names item types and specifications by code; they are resolved and the values are
 * read here, so a wrong code fails at once with the script named, and a value not written the
 * way it can be read fails with the file, the unit and the element named.
 *
 * @param <T> the concrete class, so that the calls can be chained
 */
public abstract class DaLevelItems<T extends DaLevelItems<T>> {

    /** One item of the level; the data are not stored yet. */
    public record Item(RulItemType itemType, @Nullable RulItemSpec itemSpec, ArrData data) {
    }

    /**
     * One part of the key by which an existing level is recognized: the items of the type - and
     * of the specification, when given - must have the same values.
     */
    public record MatchKey(RulItemType itemType, @Nullable RulItemSpec itemSpec) {

        /** Whether the item of the given type and specification is compared by this key. */
        public boolean covers(Integer itemTypeId, @Nullable Integer itemSpecId) {
            return itemType.getItemTypeId().equals(itemTypeId)
                    && (itemSpec == null || itemSpec.getItemSpecId().equals(itemSpecId));
        }

        @Override
        public String toString() {
            return itemSpec == null ? itemType.getCode() : itemType.getCode() + ":" + itemSpec.getCode();
        }
    }

    private final StaticDataProvider sdp;
    private final String scriptName;
    private final String eadHref;

    private final List<MatchKey> matchBy = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();

    DaLevelItems(StaticDataProvider sdp, String scriptName, @Nullable String eadHref) {
        this.sdp = sdp;
        this.scriptName = scriptName;
        this.eadHref = eadHref;
    }

    /** What the level is, for the messages - e.g. the div it stands for. */
    protected abstract String subject();

    @SuppressWarnings("unchecked")
    private T self() {
        return (T) this;
    }

    /**
     * Item types by which the level is recognized as a level that exists already - imported
     * from another package, or described by hand: a level below the same parent with the same
     * values of all these items (whatever their specification) is that level. E.g. the level
     * type and the name of a group of the file plan. None (the default) for a level that is never
     * shared, such as a document; such a level is recognized only by its UUID.
     */
    public T matchBy(String... itemTypeCodes) {
        for (String itemTypeCode : itemTypeCodes) {
            matchBy.add(new MatchKey(resolveType(itemTypeCode), null));
        }
        return self();
    }

    /**
     * As {@link #matchBy(String...)}, but only the items of the given specification are compared
     * - e.g. the identifier in the source system among the other identifiers of a unit, which
     * the user may add to.
     */
    public T matchBySpec(String itemTypeCode, String itemSpecCode) {
        RulItemType itemType = resolveType(itemTypeCode);
        matchBy.add(new MatchKey(itemType, resolveSpec(itemType, itemSpecCode)));
        return self();
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
            items.add(new Item(itemType, itemSpec, data));
        }
        return self();
    }

    private T add(RulItemType itemType, @Nullable String itemSpecCode, ArrData data) {
        RulItemSpec itemSpec = resolveSpec(itemType, itemSpecCode);
        data.setDataType(DataType.fromId(itemType.getDataTypeId()).getEntity());
        items.add(new Item(itemType, itemSpec, data));
        return self();
    }

    private RulItemType resolveType(String itemTypeCode) {
        ItemType itemType = sdp.getItemTypeByCode(itemTypeCode);
        if (itemType == null) {
            throw scriptError("neznámý prvek popisu " + itemTypeCode);
        }
        return itemType.getEntity();
    }

    @Nullable
    private RulItemSpec resolveSpec(RulItemType itemType, @Nullable String itemSpecCode) {
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

    List<MatchKey> getMatchBy() {
        return matchBy;
    }

    List<Item> getItems() {
        return items;
    }
}
