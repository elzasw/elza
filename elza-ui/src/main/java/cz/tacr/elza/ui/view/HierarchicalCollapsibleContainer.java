package cz.tacr.elza.ui.view;

import java.util.HashSet;
import java.util.Set;

import com.vaadin.data.Collapsible;
import com.vaadin.data.util.HierarchicalContainer;


/**
 * @author Martin Šlapa, martin.slapa@marbes.cz.
 * @since 5.6.2015
 */
public class HierarchicalCollapsibleContainer extends HierarchicalContainer implements Collapsible {

    Set<Integer> isCollapsedSet = new HashSet<>();

    @Override
    public void setCollapsed(Object itemId, boolean collapsed) {
        if (collapsed) {
            isCollapsedSet.remove(itemId);
        } else {
            isCollapsedSet.add((Integer) itemId);
        }
    }

    @Override
    public boolean isCollapsed(Object itemId) {
        return !isCollapsedSet.contains(itemId);
    }
}
