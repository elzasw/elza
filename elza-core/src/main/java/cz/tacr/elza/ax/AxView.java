package cz.tacr.elza.ax;

import java.util.stream.Stream;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Component;
import com.vaadin.ui.TabSheet;

import cz.tacr.elza.ax.*;


public abstract class AxView extends RootLayout implements View, Navigation, Components {

    TabSheet tabSheet;
    String parameters;

    protected AxView() {
        String name = getClass().getSimpleName();
        name = name.replaceAll("View", "-view").toLowerCase();
        setStyleName("page-root");
        addStyleName(name);
    }

    public Integer getParameterInteger() {
        Integer[] values = getParameterIntegers();
        return values != null && values.length == 1 ? values[0] : null;
    }

    public Integer[] getParameterIntegers() {
        try {
            return Stream.of(getParameterStrings())
                    .map(Integer::parseInt)
                    .toArray(size -> new Integer[size]);
        } catch (Exception ex) {
            return null;
        }
    }

    public String[] getParameterStrings() {
        return parameters.split("/");
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        parameters = event.getParameters();
    }

    //TODO Refactorize
    public AxView actions(cz.tacr.elza.ax.AxAction... actions) {
        menuActions(actions);
        return this;
    }

    //TODO Refactorize
    public AxView components(Component... components) {
        mainComponents(components);
        return this;
    }

    //TODO Refactorize
    public AxView components(String layoutName, Component... components) {
        bodyLayout(layoutName).addComponents(components);
        return this;
    }

    public TabSheet tabSheet() {
        if (tabSheet == null) {
            tabSheet = new TabSheet();
            tabSheet.setWidth(100, Unit.PERCENTAGE);
            tabSheet.setHeightUndefined();
            tabSheet.addSelectedTabChangeListener(event -> Refresh.tryRefresh(tabSheet.getSelectedTab()));
            components(tabSheet);
        }
        return tabSheet;
    }

    public TabSheet.Tab addTabSheet(String caption, FontAwesome awesome, final ComponentWrapper component) {
        tabSheet().addSelectedTabChangeListener(event -> {
            if (tabSheet().getSelectedTab().equals(component.getComponent())) {
                Refresh.tryRefresh(component);
            }
        });
        return tabSheet().addTab(component.getComponent(), caption, awesome);
    }

    public TabSheet.Tab addTabSheet(String caption, FontAwesome awesome, Component component) {
        return tabSheet().addTab(component, caption, awesome);
    }

    public void removeAllComponents() {
        mainPanel().removeAllComponents();
    }

}
