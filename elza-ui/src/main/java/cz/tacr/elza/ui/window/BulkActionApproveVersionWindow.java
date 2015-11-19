package cz.tacr.elza.ui.window;

import java.util.List;

import org.springframework.util.Assert;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;

import cz.req.ax.AxAction;
import cz.req.ax.AxWindow;
import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.controller.BulkActionManager;
import cz.tacr.elza.ui.utils.ElzaNotifications;


/**
 * Zobrazení okna pro spuštění povinných hromadných akcí, které ještě nebyly spuštěny
 *
 * @author Martin Šlapa
 * @since 19.11.2015
 */
public class BulkActionApproveVersionWindow extends AxWindow {

    private final BulkActionManager bulkActionManager;

    public BulkActionApproveVersionWindow(final BulkActionManager bulkActionManager) {
        Assert.notNull(bulkActionManager);
        this.bulkActionManager = bulkActionManager;
    }

    public AxWindow show(final Integer findingAidId,
                         final List<BulkActionConfig> bulkActionConfigs,
                         final Runnable runApproveVersion) {

        CssLayout layout = new CssLayout();
        layout.addStyleName("bulkaction-box");
        layout.setSizeUndefined();

        for (BulkActionConfig bulkActionConfig : bulkActionConfigs) {
            CssLayout bulkActionLayout = new CssLayout();
            bulkActionLayout.addStyleName("bulkaction-line");

            AxAction runBulkAction = new AxAction().run(() -> {
                try {
                    bulkActionManager.run(bulkActionConfig, findingAidId);
                    ElzaNotifications.show("Spuštěno...", 2000);
                } catch (Exception e) {
                    e.printStackTrace();
                    ElzaNotifications.showError(e.getMessage());
                }
            }).caption("Spustit").icon(FontAwesome.PLAY);

            bulkActionLayout.addComponent(runBulkAction.button());
            String name = (String) bulkActionConfig.getProperty("name");
            Label bulkActionLabel = newLabel(name == null ? "[" + bulkActionConfig.getCode() + "]" : name + " [" + bulkActionConfig.getCode() + "]");
            bulkActionLabel.addStyleName("bulkaction-name");
            bulkActionLayout.addComponent(bulkActionLabel);

            layout.addComponent(bulkActionLayout);
        }

        AxAction runAllBulkActions = new AxAction().run(() -> {
            try {
                for (BulkActionConfig bulkActionConfig : bulkActionConfigs) {
                    bulkActionManager.run(bulkActionConfig, findingAidId);
                }
                ElzaNotifications.show("Spuštěno vše...", 2000);
            } catch (Exception e) {
                e.printStackTrace();
                ElzaNotifications.showError(e.getMessage());
            }
        }).caption("Spustit vše").icon(FontAwesome.PLAY_CIRCLE);

        addComponent(layout);

        caption("Hromadné akce, které je nutné spustit před uzavřením verze").components(layout).buttonClose().modal()
                .style(
                        "bulkaction-window-detail").buttonAndClose(
                new AxAction().run(runApproveVersion).caption("Vynutit uzavření")).buttonPrimary(runAllBulkActions);

        return super.show();
    }

}
