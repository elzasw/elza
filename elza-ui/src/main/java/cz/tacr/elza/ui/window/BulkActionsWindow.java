package cz.tacr.elza.ui.window;

import java.util.List;

import org.springframework.util.Assert;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;

import cz.req.ax.AxAction;
import cz.req.ax.AxWindow;
import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.bulkaction.BulkActionState;
import cz.tacr.elza.controller.BulkActionManager;
import cz.tacr.elza.ui.utils.ElzaNotifications;


/**
 * Zobrazení okna pro spuštění všech hromadných akcí, které jsou k dispozici.
 *
 * @author Martin Šlapa
 * @since 1.12.2015
 */
public class BulkActionsWindow extends AxWindow {

    private final BulkActionManager bulkActionManager;

    private List<BulkActionConfig> bulkActionConfigs;

    private Integer findingAidId;

    private List<BulkActionState> bulkActionStates;

    private CssLayout layout = new CssLayout();

    public BulkActionsWindow(final BulkActionManager bulkActionManager) {
        Assert.notNull(bulkActionManager);
        this.bulkActionManager = bulkActionManager;
    }

    /**
     * Zobrazení okna pro spuštění hromadných akcí.
     *
     * @param findingAidVersionId identifikátor verze
     */
    public AxWindow show(final Integer findingAidVersionId) {
        this.findingAidId = findingAidVersionId;


        reload();

        addComponent(layout);

        AxAction runAllBulkActions = new AxAction().run(() -> {
            try {
                for (BulkActionConfig bulkActionConfig : bulkActionConfigs) {
                    bulkActionManager.run(bulkActionConfig, findingAidVersionId);
                }
                ElzaNotifications.show("Spuštěno vše...", 2000);
                reload();
            } catch (Exception e) {
                e.printStackTrace();
                ElzaNotifications.showError(e.getMessage());
            }
        }).caption("Spustit vše").icon(FontAwesome.PLAY_CIRCLE);

        caption("Hromadné akce, které je možné spustit").components(layout).buttonClose().modal()
                .style(
                        "bulkaction-window-detail");

        AxAction refreshBulkActions = new AxAction().run(() -> {
            reload();
        }).caption("Aktualizovat").icon(FontAwesome.REFRESH);

        menuActions(refreshBulkActions);
        menuActions(runAllBulkActions);

        return super.show();
    }

    /**
     * Vyhledá stav podle kódu.
     *
     * @param bulkActionCode kód hromadné akce
     * @return nalezený stav
     */
    private BulkActionState getStateByCode(final String bulkActionCode) {
        for (BulkActionState bulkActionState : bulkActionStates) {
            if (bulkActionCode.equals(bulkActionState.getBulkActionCode())) {
                return bulkActionState;
            }
        }
        return null;
    }

    /**
     * Přenačte stavy hromadných akcí.
     */
    private void reload() {
        bulkActionConfigs = bulkActionManager.getBulkActions(findingAidId);
        bulkActionStates = bulkActionManager.getBulkActionState(findingAidId);

        layout.removeAllComponents();

        layout.addStyleName("bulkaction-box");
        layout.setSizeUndefined();

        for (BulkActionConfig bulkActionConfig : bulkActionConfigs) {

            BulkActionState state = getStateByCode(bulkActionConfig.getCode());

            CssLayout bulkActionLayout = new CssLayout();
            bulkActionLayout.addStyleName("bulkaction-line");

            AxAction runBulkAction;

            if (state == null
                    || state.getState().equals(cz.tacr.elza.api.vo.BulkActionState.State.FINISH)
                    || state.getState().equals(cz.tacr.elza.api.vo.BulkActionState.State.ERROR)) {

                runBulkAction = new AxAction().run(() -> {
                    try {
                        bulkActionManager.run(bulkActionConfig, findingAidId);
                        ElzaNotifications.show("Spuštěno...", 2000);
                        reload();
                    } catch (Exception e) {
                        e.printStackTrace();
                        ElzaNotifications.showError(e.getMessage());
                    }
                }).caption("Spustit").icon(FontAwesome.PLAY);

            } else {

                String caption;
                FontAwesome font;

                switch (state.getState()) {
                    case PLANNED:
                        caption = "Naplánován";
                        font = FontAwesome.SPINNER;
                        break;
                    case RUNNING:
                        caption = "Běží";
                        font = FontAwesome.STOP;
                        break;
                    case WAITING:
                        caption = "Ve frontě";
                        font = FontAwesome.PAUSE;
                        break;
                    default:
                        throw new IllegalStateException("Neplatný stav");
                }

                runBulkAction = new AxAction().run(() -> {
                    reload();
                }).caption(caption).icon(font);

            }
            runBulkAction.style("action-button");
            bulkActionLayout.addComponent(runBulkAction.button());


            Label bulkActionLabel = newLabel(bulkActionConfig.getCode());
            bulkActionLabel.addStyleName("bulkaction-code");
            bulkActionLayout.addComponent(bulkActionLabel);

            layout.addComponent(bulkActionLayout);
        }

    }

}
