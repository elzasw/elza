package cz.tacr.elza.ui.view;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ru.xpoft.vaadin.VaadinView;

import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FontAwesome;

import cz.req.ax.AxAction;
import cz.tacr.elza.ui.ElzaView;
import cz.tacr.elza.ui.controller.TestingDataController;
import cz.tacr.elza.ui.utils.ElzaNotifications;


/**
 * Stránka pro práci s testovacími daty.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 3. 9. 2015
 */
@Component
@Scope("prototype")
@VaadinView("TestData")
public class TestingDataView extends ElzaView {

    @Autowired
    private TestingDataController testingDataController;

    @Override
    public void enter(final ViewChangeListener.ViewChangeEvent event) {
        super.enter(event);

        pageTitle("Testovací data");

        actions(new AxAction().caption("Vygenerovat testovací data").icon(FontAwesome.PLUS_CIRCLE)
                .run(() -> {
                    testingDataController.createData();
                    ElzaNotifications.show("Data byla vygenerována.");
                    }),
                new AxAction().caption("Odstranit všechna data v databázi").icon(FontAwesome.TIMES)
                .run(() -> {
                    testingDataController.removeData();
                    ElzaNotifications.show("Data byla smazána.");
                }));
    }
}
