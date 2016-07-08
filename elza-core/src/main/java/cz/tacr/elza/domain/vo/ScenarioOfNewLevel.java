package cz.tacr.elza.domain.vo;

import java.util.List;

import cz.tacr.elza.domain.ArrDescItem;


/**
 * Implementace scénáře založení nového uzlu.
 *
 * @author Martin Šlapa
 * @since 9.12.2015
 */
public class ScenarioOfNewLevel implements cz.tacr.elza.api.vo.ScenarioOfNewLevel<ArrDescItem> {

    private String name;

    private List<ArrDescItem> descItems;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public List<ArrDescItem> getDescItems() {
        return descItems;
    }

    @Override
    public void setDescItems(final List<ArrDescItem> descItems) {
        this.descItems = descItems;
    }
}
