package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Číselník externích zdrojů rejstříkových hesel.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface RegExternalSource extends Serializable {

    /**
     * Vlastní ID.
     * @return  id
     */
    Integer getExternalSourceId();

    /**
     * Vlastní ID.
     * @param externalSourceId id
     */
    void setExternalSourceId(Integer externalSourceId);

    /**
     * Kód zdroje.
     * @return  kód zdroje
     */
    String getCode();

    /**
     * Kód zdroje.
     * @param code kód zdroje
     */
    void setCode(String code);

    /**
     * Název zdroje.
     * @return název zdroje
     */
    String getName();

    /**
     * Název zdroje.
     * @param name název zdroje
     */
    void setName(String name);
}
