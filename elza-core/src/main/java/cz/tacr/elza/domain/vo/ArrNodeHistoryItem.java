package cz.tacr.elza.domain.vo;

import java.util.List;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;


/**
 * Obsahuje změnu v historii.
 *
 * @author Martin Šlapa
 * @since 22.9.2015
 */
public class ArrNodeHistoryItem {

    private Type type;
    private ArrChange change;
    private List<ArrDescItem> descItems;

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public ArrChange getChange() {
        return change;
    }

    public void setChange(final ArrChange change) {
        this.change = change;
    }

    /**
     * Typ změny.
     */
    public enum Type {
        LEVEL_CREATE,
        LEVEL_CHANGE,
        LEVEL_DELETE,
        ATTRIBUTE_CHANGE;
    }
}
