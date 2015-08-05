package cz.tacr.elza.ui.view;

import com.vaadin.navigator.ViewChangeListener;
import cz.tacr.elza.ui.ElzaView;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.xpoft.vaadin.VaadinView;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Component
@Scope("prototype")
@VaadinView("FindingAid")
public class FindingAidView extends ElzaView {

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        super.enter(event);
        components(newLabel("FindingAid", "h2"));
    }
}
