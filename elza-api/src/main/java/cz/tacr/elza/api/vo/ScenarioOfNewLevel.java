package cz.tacr.elza.api.vo;

import java.io.Serializable;
import java.util.List;

import cz.tacr.elza.api.ArrDescItem;


/**
 * Scénář založení nového uzlu.
 *
 * @author Martin Šlapa
 * @since 9.12.2015
 */
public interface ScenarioOfNewLevel<DI extends ArrDescItem> extends Serializable {

    /**
     * @return název scénáře
     */
    String getName();


    /**
     * @param name název scénáře
     */
    void setName(String name);


    /**
     * @return seznam hodnot atributů k vytvoření
     */
    List<DI> getDescItems();


    /**
     * @param descItems seznam hodnot atributů k vytvoření
     */
    void setDescItems(List<DI> descItems);

}
