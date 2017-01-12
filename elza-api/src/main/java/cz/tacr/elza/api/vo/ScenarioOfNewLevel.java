package cz.tacr.elza.api.vo;

import java.io.Serializable;


/**
 * Scénář založení nového uzlu.
 *
 * @author Martin Šlapa
 * @since 9.12.2015
 */
public interface ScenarioOfNewLevel extends Serializable {

    /**
     * @return název scénáře
     */
    String getName();


    /**
     * @param name název scénáře
     */
    void setName(String name);
}
