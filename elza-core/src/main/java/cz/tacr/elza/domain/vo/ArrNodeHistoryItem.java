package cz.tacr.elza.domain.vo;

import java.io.Serializable;
import java.util.List;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;


/**
 * Obsahuje změnu v historii.
 *
 * @author Martin Šlapa
 * @since 22.9.2015
 */
public class ArrNodeHistoryItem implements cz.tacr.elza.api.vo.ArrNodeHistoryItem<ArrChange>, Serializable {

    private Type type;
    private ArrChange change;
    private List<ArrDescItem> descItems;

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public void setType(final Type type) {
        this.type = type;
    }

    @Override
    public ArrChange getChange() {
        return change;
    }

    @Override
    public void setChange(final ArrChange change) {
        this.change = change;
    }
}
