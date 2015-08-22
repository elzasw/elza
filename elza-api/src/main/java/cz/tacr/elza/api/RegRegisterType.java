package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Číselník typů rejstříků.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface RegRegisterType extends Serializable {

    Integer getRegisterTypeId();

    void setRegisterTypeId(Integer registerTypeId);

    String getCode();

    void setCode(String code);

    String getName();

    void setName(String name);
}
