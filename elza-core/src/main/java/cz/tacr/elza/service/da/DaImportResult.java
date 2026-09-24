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
 * What the DA_IMPORT script decides about one div (variable {@code RESULT}): whether it becomes a
 * level, and with which items.
 *
 * The script names item types and specifications by code; they are resolved and the values are
 * read here, so a wrong code fails at once with the script named, and a value not written the
 * way it can be read fails with the file, the unit and the element named.
 */
public class DaImportResult {

    /** What becomes of a div. */
    public enum Decision {
        /** The div becomes a level of the archival description, with the items of the result. */
        LEVEL,
        /** The div becomes no level; its digital entity is attached to the level above it. */
        ATTACH,
        /** The div is left out; the divs below it are imported as if they were in its place. */
        SKIP
    }

    /** One item of the level; the data are not stored yet. */
    public record Item(RulItemType itemType, @Nullable RulItemSpec itemSpec, ArrData data) {
    }

    private final StaticDataProvider sdp;
    private final DaImportLevel level;
    private final String eadHref;

    private Decision decision;
    private final List<RulItemType> matchBy = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();

    DaImportResult(StaticDataProvider sdp, DaImportLevel level, @Nullable String eadHref) {
        this.sdp = sdp;
        this.level = level;
        this.eadHref = eadHref;
    }

    /** The div becomes a level. */
    public DaImportResult level() {
        decision = Decision.LEVEL;
        return this;
    }

    /** The div becomes no level; its digital entity is attached to the level above it. */
    public DaImportResult attach() {
        decision = Decision.ATTACH;
        return this;
    }

    /** The div is left out, the divs below it take its place. */
    public DaImportResult skip() {
        decision = Decision.SKIP;
        return this;
    }

    /**
     * Items by which the level is recognized as a level that exists already - imported from
     * another package, or described by hand: a level below the same parent with the same values
     * of all these items is that level. E.g. the level type and the name of a group of the file
     * plan. None (the default) for a level that is never shared, such as a document; such a level
     * is recognized only by its UUID.
     */
    public DaImportResult matchBy(String... itemTypeCodes) {
        for (String itemTypeCode : itemTypeCodes) {
            matchBy.add(resolveType(itemTypeCode));
        }
        return this;
    }

    /** An item without a value - an item type of data type ENUM, e.g. the level type. */
    public DaImportResult item(String itemTypeCode, String itemSpecCode) {
        RulItemType itemType = resolveType(itemTypeCode);
        requireDataType(itemType, DataType.ENUM);
        return add(itemType, itemSpecCode, new ArrDataNull());
    }

    /**
     * An item with the given text, e.g. the name of the level taken from the div. Blank text adds
     * no item.
     */
    public DaImportResult item(String itemTypeCode, @Nullable String itemSpecCode, @Nullable String text) {
        if (StringUtils.isBlank(text)) {
            return this;
        }
        RulItemType itemType = resolveType(itemTypeCode);
        DataType dataType = requireDataType(itemType, DataType.STRING, DataType.TEXT);
        ArrData data;
        if (dataType == DataType.TEXT) {
            data = new ArrDataText(text.strip());
        } else {
            String value = StringUtils.normalizeSpace(text);
            if (value.length() > StringLength.LENGTH_1000) {
                throw invalid("hodnota prvku popisu " + itemTypeCode + " je delší než " + StringLength.LENGTH_1000
                        + " znaků", null);
            }
            data = new ArrDataString(value);
        }
        return add(itemType, itemSpecCode, data);
    }

    /**
     * An item with the value of the element, read according to the profile of EAD. An element
     * carrying no value adds no item.
     */
    public DaImportResult item(String itemTypeCode, @Nullable String itemSpecCode, DaImportElement element) {
        RulItemType itemType = resolveType(itemTypeCode);
        RulItemSpec itemSpec = resolveSpec(itemType, itemSpecCode);
        ArrData data;
        try {
            data = DidElementConverters.convert(element.getSource(),
                                                new DidElementConverters.Mapping(itemType, itemSpec));
        } catch (EadContentException e) {
            throw invalid("element " + element + " nelze převzít: " + e.getMessage(), e);
        }
        if (data != null) {
            items.add(new Item(itemType, itemSpec, data));
        }
        return this;
    }

    private DaImportResult add(RulItemType itemType, @Nullable String itemSpecCode, ArrData data) {
        RulItemSpec itemSpec = resolveSpec(itemType, itemSpecCode);
        data.setDataType(DataType.fromId(itemType.getDataTypeId()).getEntity());
        items.add(new Item(itemType, itemSpec, data));
        return this;
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

    private SystemException scriptError(String reason) {
        return new SystemException("Skript DA_IMPORT u " + level + ": " + reason, BaseCode.INVALID_STATE);
    }

    private AipProblemException invalid(String reason, @Nullable Throwable cause) {
        return AipProblemException.metadata("Inherentní archivní popis v souboru '" + eadHref
                + "' obsahuje u jednotky popisu '" + level.getId() + "' " + reason + ".", eadHref, cause);
    }

    @Nullable
    Decision getDecision() {
        return decision;
    }

    List<RulItemType> getMatchBy() {
        return matchBy;
    }

    List<Item> getItems() {
        return items;
    }
}
