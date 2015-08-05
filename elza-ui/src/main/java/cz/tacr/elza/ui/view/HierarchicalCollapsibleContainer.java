package cz.tacr.elza.ui.view;

import java.util.List;

import com.vaadin.data.Collapsible;
import com.vaadin.data.util.HierarchicalContainer;

import cz.tacr.elza.domain.FaLevel;


/**
 * Created by slapa on 5.8.2015.
 */
public class HierarchicalCollapsibleContainer extends HierarchicalContainer implements Collapsible {

    public void add(List<FaLevel> list) {

    }

    @Override
    public void setCollapsed(Object itemId, boolean collapsed) {

    }

    @Override
    public boolean isCollapsed(Object itemId) {
        return false;
    }
}
