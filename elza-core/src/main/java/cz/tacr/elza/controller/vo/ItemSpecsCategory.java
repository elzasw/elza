package cz.tacr.elza.controller.vo;

import java.util.List;

/**
 * Kategorie specifikace.
 *
 * @author Martin Šlapa
 * @since 27.10.2016
 */
public class ItemSpecsCategory {

    /**
     * Potomci stromové kategorie.
     */
    private List<ItemSpecsCategory> children;

    /**
     * Název kategorie.
     */
    private String name;

    /**
     * Identifikátory specifikací, které do kategorie patří.
     */
    private List<Integer> specIds;

    public List<ItemSpecsCategory> getChildren() {
        return children;
    }

    public void setChildren(final List<ItemSpecsCategory> children) {
        this.children = children;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public List<Integer> getSpecIds() {
        return specIds;
    }

    public void setSpecIds(final List<Integer> specIds) {
        this.specIds = specIds;
    }
}
