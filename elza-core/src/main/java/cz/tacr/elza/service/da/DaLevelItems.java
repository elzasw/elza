package cz.tacr.elza.service.da;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;

/**
 * A level a script plans: its items and the key by which an existing level is recognized as
 * the same level. Shared by the DA_IMPORT script ({@link DaImportResult}, a level for a div of
 * the package) and the DA_MATCH script ({@link DaMatchLevel}, a level the package is placed
 * under).
 *
 * The items ({@link #item}) are those of the level: a created level gets them all, a level found
 * by the match key gets those it has none of yet. What a found level must not get again - what
 * the user is meant to remove, e.g. a restriction of access on a new series - is planned with
 * {@link #whenCreated()}.
 *
 * @param <T> the concrete class, so that the calls can be chained
 */
public abstract class DaLevelItems<T extends DaLevelItems<T>> extends DaItems<T> {

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

    private final List<MatchKey> matchBy = new ArrayList<>();
    private DaItemGroup createdItems;

    DaLevelItems(StaticDataProvider sdp, String scriptName, @Nullable String eadHref) {
        super(sdp, scriptName, eadHref);
    }

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
            matchBy.add(new MatchKey(resolveKeyType(itemTypeCode), null));
        }
        return self();
    }

    /**
     * As {@link #matchBy(String...)}, but only the items of the given specification are compared
     * - e.g. the identifier in the source system among the other identifiers of a unit, which
     * the user may add to.
     */
    public T matchBySpec(String itemTypeCode, String itemSpecCode) {
        RulItemType itemType = resolveKeyType(itemTypeCode);
        matchBy.add(new MatchKey(itemType, resolveSpec(itemType, itemSpecCode)));
        return self();
    }

    /** An item type the level can be recognized by: one with a value to compare. */
    private RulItemType resolveKeyType(String itemTypeCode) {
        RulItemType itemType = resolveType(itemTypeCode);
        if (DataType.fromId(itemType.getDataTypeId()) == DataType.STRUCTURED) {
            throw scriptError("prvek popisu " + itemTypeCode + " je strukturovaný, podle něj nelze úroveň hledat");
        }
        return itemType;
    }

    /**
     * Items the level gets only when it is created - not when an existing level is found by the
     * match key, which gets only the items it lacks of those planned with {@link #item}. For what
     * the user is meant to review and remove, e.g. a restriction of access on a new series: once
     * the user removes it, the next package does not bring it back. Every call returns the same
     * group.
     */
    public DaItemGroup whenCreated() {
        if (createdItems == null) {
            createdItems = group(subject(), null);
        }
        return createdItems;
    }

    List<MatchKey> getMatchBy() {
        return matchBy;
    }

    /** Items the level gets only when it is created; none without {@link #whenCreated()}. */
    List<Item> getCreatedItems() {
        return createdItems == null ? List.of() : createdItems.getItems();
    }
}
