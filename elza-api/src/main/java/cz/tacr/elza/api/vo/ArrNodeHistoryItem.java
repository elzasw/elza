package cz.tacr.elza.api.vo;

import java.io.Serializable;

import cz.tacr.elza.api.ArrChange;


/**
 * Obsahuje změnu v historii.
 *
 * @author Martin Šlapa
 * @since 22.9.2015
 */
public interface ArrNodeHistoryItem<CH extends ArrChange> extends Serializable {

    /**
     * Typ změny.
     */
    public enum Type {
        LEVEL_CREATE,
        LEVEL_CHANGE,
        LEVEL_DELETE,
        ATTRIBUTE_CHANGE;
    }


    Type getType();


    void setType(Type type);


    CH getChange();


    void setChange(CH change);
}
