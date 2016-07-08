package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Typ formy jména.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParPartyNameFormType extends Serializable {

    /**
     * Primární ID.
     * @return      id objektu
     */
    Integer getNameFormTypeId();

    void setNameFormTypeId(Integer nameFormTypeId);

    String getCode();

    void setCode(String code);

    String getName();

    void setName(String name);
}
