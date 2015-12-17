package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Číselník typů doplňků jmen osob.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParComplementType extends Serializable {

    /**
     * Primární ID.
     * @return      id objektu
     */
    Integer getComplementTypeId();

    void setComplementTypeId(Integer complementTypeId);

    String getCode();

    void setCode(String code);

    String getName();

    void setName(String name);

    Integer getViewOrder();

    void setViewOrder(Integer name);
}
