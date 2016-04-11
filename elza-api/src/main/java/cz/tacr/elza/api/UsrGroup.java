package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Uživatelká skupina.
 *
 * @author Martin Šlapa
 * @since 11.04.2016
 */
public interface UsrGroup extends Serializable {

    /**
     * @return identifikátor entity
     */
    Integer getGroupId();

    /**
     * @param groupId identifikátor entity
     */
    void setGroupId(Integer groupId);

    /**
     * @return kód skupiny
     */
    String getCode();

    /**
     * @param code kód skupiny
     */
    void setCode(String code);

    /**
     * @return název skupiny
     */
    String getName();

    /**
     * @param name název skupiny
     */
    void setName(String name);

    /**
     * @return popis skupiny
     */
    String getDescription();

    /**
     * @param description popis skupiny
     */
    void setDescription(String description);
}
