package cz.tacr.elza.ui.components;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;

import cz.req.ax.AxAction;
import cz.req.ax.Components;
import cz.tacr.elza.domain.ArrFaLevel;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.8.2015
 */
public class LevelInlineDetail extends CssLayout implements Components {

    private Runnable onClose;
    private CssLayout detailContent;

    public LevelInlineDetail(final Runnable onClose) {
        setSizeUndefined();
        addStyleName("level-detail");
        this.onClose = onClose;

        init();
    }

    private void init(){
        Button closeButton = new AxAction().icon(FontAwesome.TIMES).right().run(()->{
            LevelInlineDetail.this.addStyleName("hidden");
            onClose.run();
        }).button();
        detailContent = cssLayout("detail-content");

        addComponent(newLabel("Detail atributu", "h2"));
        addComponent(closeButton);
        addComponent(detailContent);
    }


    public void showLevelDetail(final ArrFaLevel level) {
        removeStyleName("hidden");
        detailContent.removeAllComponents();
        detailContent.addComponent(newLabel("Zobrazen level s nodeId "+level.getNodeId()));
        detailContent.addComponent(newLabel("Pozice: "+level.getPosition()));
    }



}
