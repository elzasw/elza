package cz.tacr.elza.drools.model;

import java.util.ArrayList;
import java.util.List;


/**
 * Value object scénáøe.
 *
 * @author Martin Šlapa
 * @since 9.12.2015
 */
public class NewLevelApproach {

    /**
     * jméno scénáøe
     */
    private String name;

    /**
     * seznam hodnot atrubutù
     */
    private List<DescItem> descItems = new ArrayList<>();

    public NewLevelApproach(final String name) {
        this.name = name;
    }

    public DescItem addDescItem(final String type, final String spec) {
        DescItem descItem = new DescItem(type, spec);
        descItems.add(descItem);
        return descItem;
    }

    public void addDescItem(final DescItem descItem) {
        descItems.add(descItem);
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public List<DescItem> getDescItems() {
        return descItems;
    }

    public void setDescItems(final List<DescItem> descItems) {
        this.descItems = descItems;
    }
}
