package cz.tacr.elza.ui.window;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.util.Assert;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;

import cz.req.ax.AxAction;
import cz.req.ax.AxWindow;
import cz.tacr.elza.controller.ArrangementManager;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.ui.utils.ConcurrentUpdateExceptionHandler;


/**
 * Okno pri pridavani levelu do stromu s nabidkou scenaru.
 *
 * @author Martin Šlapa
 * @since 10.12.2015
 */
public class AddLevelScenariosWindow extends AxWindow {

    private ArrangementManager arrangementManager;

    private Consumer<ScenarioOfNewLevel> consumer;

    public AddLevelScenariosWindow(final ArrangementManager arrangementManager) {
        Assert.notNull(arrangementManager);
        this.arrangementManager = arrangementManager;
    }

    public AxWindow show(final String caption,
                         final ArrLevel level,
                         final ArrFindingAidVersion version,
                         final DirectionLevel directionLevel,
                         final Consumer<ScenarioOfNewLevel> consumer) {
        this.consumer = consumer;

        CssLayout layout = new CssLayout();
        layout.setStyleName("level-box");

        List<ScenarioOfNewLevel> scenarioOfNewLevelList;

        switch (directionLevel) {
            case CHILD:
                scenarioOfNewLevelList = arrangementManager
                        .getDescriptionItemTypesForNewLevelChild(level.getNode().getNodeId(),
                                version.getFindingAidVersionId());
                break;
            case BEFORE:
                scenarioOfNewLevelList = arrangementManager
                        .getDescriptionItemTypesForNewLevelBefore(level.getNode().getNodeId(),
                                version.getFindingAidVersionId());
                break;
            case AFTER:
                scenarioOfNewLevelList = arrangementManager
                        .getDescriptionItemTypesForNewLevelAfter(level.getNode().getNodeId(),
                                version.getFindingAidVersionId());
                break;
            default:
                throw new IllegalStateException("Chybny DirectionLevel: " + directionLevel);
        }

        caption(caption).components(layout)
                .buttonClose().modal().style("level-window-detail");

        if (scenarioOfNewLevelList.size() == 0) {

            layout.addComponent(newLabel("Nebyl nalezen žádný zakládací scénář", "h3"));
            buttonPrimary(new AxAction().caption("Vložit prázdný").exception(new ConcurrentUpdateExceptionHandler())
                    .run(() -> consumer.accept(new ScenarioOfNewLevel())));
        } else {
            for (ScenarioOfNewLevel scenarioOfNewLevel : scenarioOfNewLevelList) {
                layout.addComponent(addScenario(scenarioOfNewLevel));
            }
        }

        return super.show();
    }

    private Component addScenario(final ScenarioOfNewLevel scenarioOfNewLevel) {

        CssLayout layout = new CssLayout();
        layout.setStyleName("level-line");

        AxAction action = new AxAction().run(() -> {
            consumer.accept(scenarioOfNewLevel);
            close();
        }).caption("Přidat").icon(FontAwesome.PLUS);

        layout.addComponent(action.button());

        Label name = newLabel(scenarioOfNewLevel.getName());
        name.setStyleName("level-name");
        layout.addComponent(name);

        return layout;
    }

}
