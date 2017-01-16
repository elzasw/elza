package cz.tacr.elza.domain.vo;

import java.util.List;

import cz.tacr.elza.domain.ArrDescItem;


/**
 * Scénář založení nového uzlu.
 *
 * @author Martin Šlapa
 * @since 9.12.2015
 */
public class ScenarioOfNewLevel {

    private String name;

    private List<ArrDescItem> descItems;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return seznam hodnot atributů k vytvoření
     */
    public List<ArrDescItem> getDescItems() {
        return descItems;
    }

    /**
     * @param descItems seznam hodnot atributů k vytvoření
     */
    public void setDescItems(final List<ArrDescItem> descItems) {
        this.descItems = descItems;
    }
}
