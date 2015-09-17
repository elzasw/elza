package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Číselník typů rejstříkových hesel.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface RegRegisterType extends Serializable {

    /**
     * Vlastní ID.
     * @return  id
     */
    Integer getRegisterTypeId();

    /**
     * Vlastní ID.
     * @param registerTypeId id
     */
    void setRegisterTypeId(Integer registerTypeId);

    /**
     * Kód typu.
     * @return kód typu
     */
    String getCode();

    /**
     * Kód typu.
     * @param code kód typu
     */
    void setCode(String code);

    /**
     * Název typu.
     * @return název typu
     */
    String getName();

    /**
     * Název typu.
     * @param name název typu
     */
    void setName(String name);
}
