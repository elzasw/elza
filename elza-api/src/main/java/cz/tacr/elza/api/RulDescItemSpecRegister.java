package cz.tacr.elza.api;

import java.io.Serializable;


/**
 * Vazební tabulka mezi entitami {@link RegRegisterType} a {@link RulDescItemSpec}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 19.10.2015
 */
public interface RulDescItemSpecRegister<RT extends RegRegisterType, DIS extends RulDescItemSpec>
        extends
            Serializable {

    void setDescItemSpec(DIS descItemSpec);

    DIS getDescItemSpec();

    void setRegisterType(RT registerType);

    RT getRegisterType();

    void setDescItemSpecRegisterId(Integer descItemSpecRegisterId);

    Integer getDescItemSpecRegisterId();
}
