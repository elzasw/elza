package cz.tacr.elza.drools.model;

import java.util.List;


/**
 * Value object scénáøe.
 *
 * @author Martin Šlapa
 * @since 9.12.2015
 */
public class VOScenarioOfNewLevel {

    /**
     * jméno scénáøe
     */
    private String name;

    /**
     * seznam hodnot atrubutù
     */
    private List<DescItemVO> descItems;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public List<DescItemVO> getDescItems() {
        return descItems;
    }

    public void setDescItems(final List<DescItemVO> descItems) {
        this.descItems = descItems;
    }
}
