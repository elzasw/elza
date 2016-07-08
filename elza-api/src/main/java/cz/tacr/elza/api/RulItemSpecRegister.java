package cz.tacr.elza.api;

import java.io.Serializable;


/**
 * Vazební tabulka mezi entitami {@link RegRegisterType} a {@link RulItemSpec}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 19.10.2015
 */
public interface RulItemSpecRegister<RT extends RegRegisterType, DIS extends RulItemSpec>
        extends
            Serializable {

    void setItemSpec(DIS descItemSpec);

    DIS getItemSpec();

    void setRegisterType(RT registerType);

    RT getRegisterType();

    void setItemSpecRegisterId(Integer descItemSpecRegisterId);

    Integer getItemSpecRegisterId();
}
