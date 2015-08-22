package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Číselník externích zdrojů rejstříkových hesel.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface RegExternalSource extends Serializable {
    Integer getExternalSourceId();

    void setExternalSourceId(Integer externalSourceId);

    String getCode();

    void setCode(String code);

    String getName();

    void setName(String name);
}
