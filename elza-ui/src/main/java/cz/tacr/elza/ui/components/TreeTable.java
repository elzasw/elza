package cz.tacr.elza.ui.components;

import org.apache.commons.lang.BooleanUtils;
import org.springframework.util.Assert;

import com.vaadin.data.Container;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinSession;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 2.9.2015
 */
public class TreeTable extends com.vaadin.ui.TreeTable {

    public TreeTable() {
    }

    public TreeTable(final String caption) {
        super(caption);
    }

    public TreeTable(final String caption, final Container dataSource) {
        super(caption, dataSource);
    }

    public void resizeOnPageResize(final String tableId){
        Assert.notNull(tableId);

        Object lastRegisteredPage = VaadinSession.getCurrent().getAttribute(tableId);

        if(Page.getCurrent() != lastRegisteredPage) {
            PageResizeListener listener = new PageResizeListener();
            Page.getCurrent().addBrowserWindowResizeListener(listener);
        }

        VaadinSession.getCurrent().setAttribute(tableId, Page.getCurrent());
    }


    @Override
    public void setSelectable(final boolean selectable) {
        super.setSelectable(selectable);

        if (selectable) {
            addStyleName("v-selectable");
        } else {
            removeStyleName("v-selectable");
        }
    }

    private class PageResizeListener implements Page.BrowserWindowResizeListener{

        @Override
        public void browserWindowResized(final Page.BrowserWindowResizeEvent event) {
            TreeTable.this.markAsDirty();
        }

    }
}
