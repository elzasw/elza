package cz.tacr.elza.ui;

import cz.req.ax.AxView;
import cz.tacr.elza.ui.view.FindingAidView;
import cz.tacr.elza.ui.view.TestView;

import javax.annotation.PostConstruct;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
public abstract class ElzaView extends AxView {

    @PostConstruct
    public void init() {
        menuActions(action(FindingAidView.class), action(TestView.class));
    }

}