package cz.tacr.elza.ui;

import javax.annotation.PostConstruct;

import cz.req.ax.AxView;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
public abstract class ElzaView extends AxView {

    @PostConstruct
    public void init() {
        //        menuActions(action(FindingAidView.class), action(TestView.class));
    }

}